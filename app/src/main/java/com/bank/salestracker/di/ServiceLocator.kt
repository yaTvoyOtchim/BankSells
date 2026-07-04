package com.bank.salestracker.di

import android.content.Context
import com.bank.salestracker.BuildConfig
import com.bank.salestracker.data.api.ApiService
import com.bank.salestracker.data.api.AuthInterceptor
import com.bank.salestracker.data.api.TokenAuthenticator
import com.bank.salestracker.data.local.AppDb
import com.bank.salestracker.data.local.TokenStore
import com.bank.salestracker.data.repository.AuthRepository
import com.bank.salestracker.data.repository.SalesRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Простой Service Locator вместо Hilt — меньше магии, проще поддерживать.
 * При желании легко мигрировать на Hilt.
 */
object ServiceLocator {

    lateinit var tokenStore: TokenStore
        private set
    lateinit var authRepo: AuthRepository
        private set
    lateinit var salesRepo: SalesRepository
        private set

    private lateinit var api: ApiService

    fun init(context: Context) {
        tokenStore = TokenStore(context)

        val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(tokenStore))
            .authenticator(TokenAuthenticator(tokenStore) { api })
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
                }
                // В проде добавьте certificate pinning:
                // .certificatePinner(CertificatePinner.Builder()
                //     .add("sales.your-bank.local", "sha256/AAAA...").build())
            }
            .build()

        api = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)

        val db = AppDb.get(context)
        authRepo = AuthRepository(api, tokenStore)
        salesRepo = SalesRepository(api, db.pendingSaleDao())
    }
}
