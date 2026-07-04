package com.bank.salestracker.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bank.salestracker.data.model.ProductCategory
import com.bank.salestracker.data.model.Sale
import com.bank.salestracker.data.model.SaleBatchItem
import kotlinx.coroutines.flow.Flow

/**
 * Офлайн-очередь: если в отделении пропал интернет, продажа сохраняется
 * локально и отправляется на сервер при следующей синхронизации.
 */
@Entity(tableName = "pending_sales")
data class PendingSale(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val saleGroupId: String?,
    val category: String,
    val clientLast4: String?,
    val amount: Double?,
    val quantity: Int,
    val comment: String?,
    val createdAtLocal: Long = System.currentTimeMillis()
) {
    fun toSale() = Sale(
        category = ProductCategory.entries.firstOrNull { it.name == category } ?: when (category) {
            "DEBIT_CARD" -> ProductCategory.DEBIT_CARD_STICKER_APPLICATION
            "CREDIT_CARD" -> ProductCategory.CREDIT_CARD_SALE
            "CONSUMER_LOAN", "MORTGAGE" -> ProductCategory.CASH_LOAN_SALE
            "DEPOSIT" -> ProductCategory.SAVINGS_ACCOUNT
            "INSURANCE" -> ProductCategory.CREDIT_CARD_INSURANCE
            "INVESTMENT" -> ProductCategory.OPIF
            "MOBILE_APP" -> ProductCategory.SUBSCRIPTION
            else -> ProductCategory.SOM
        },
        clientLast4 = clientLast4 ?: "0000",
        amount = amount,
        quantity = quantity,
        comment = comment
    )

    fun toBatchItem() = SaleBatchItem(
        category = productCategory(),
        amount = amount,
        quantity = quantity,
        comment = comment
    )

    private fun productCategory() =
        ProductCategory.entries.firstOrNull { it.name == category } ?: when (category) {
            "DEBIT_CARD" -> ProductCategory.DEBIT_CARD_STICKER_APPLICATION
            "CREDIT_CARD" -> ProductCategory.CREDIT_CARD_SALE
            "CONSUMER_LOAN", "MORTGAGE" -> ProductCategory.CASH_LOAN_SALE
            "DEPOSIT" -> ProductCategory.SAVINGS_ACCOUNT
            "INSURANCE" -> ProductCategory.CREDIT_CARD_INSURANCE
            "INVESTMENT" -> ProductCategory.OPIF
            "MOBILE_APP" -> ProductCategory.SUBSCRIPTION
            else -> ProductCategory.SOM
        }
}

@Dao
interface PendingSaleDao {
    @Insert suspend fun insert(s: PendingSale): Long
    @Delete suspend fun delete(s: PendingSale)
    @Query("SELECT * FROM pending_sales ORDER BY createdAtLocal") suspend fun all(): List<PendingSale>
    @Query("SELECT COUNT(*) FROM pending_sales") fun countFlow(): Flow<Int>
    @Query("DELETE FROM pending_sales") suspend fun clear()
}

@Database(entities = [PendingSale::class], version = 3, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun pendingSaleDao(): PendingSaleDao

    companion object {
        @Volatile private var instance: AppDb? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_sales ADD COLUMN clientLast4 TEXT")
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_sales ADD COLUMN saleGroupId TEXT")
            }
        }

        fun get(context: Context): AppDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, AppDb::class.java, "sales.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build().also { instance = it }
        }
    }
}
