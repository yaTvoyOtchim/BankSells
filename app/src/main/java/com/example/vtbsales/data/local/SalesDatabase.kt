package com.example.vtbsales.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OfficeEntity::class,
        UserEntity::class,
        TeamLinkEntity::class,
        PlanEntity::class,
        ProductEntity::class,
        ClientSessionEntity::class,
        SaleEntity::class,
        SaleEditHistoryEntity::class,
        ReportExportEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SalesDatabase : RoomDatabase() {
    abstract fun salesDao(): SalesDao

    companion object {
        @Volatile
        private var instance: SalesDatabase? = null

        fun get(context: Context): SalesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SalesDatabase::class.java,
                    "vtb-sales-local-v2.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
