package com.bank.salestracker.data.api

import com.bank.salestracker.data.local.TokenStore
import com.bank.salestracker.data.model.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/** Подставляет access-токен в каждый запрос. */
class AuthInterceptor(private val store: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = store.accessToken
        val request = if (token != null && !chain.request().url.encodedPath.contains("auth/login")) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else chain.request()
        return chain.proceed(request)
    }
}

/**
 * При 401 пытается обновить пару токенов по refresh-токену.
 * Если refresh тоже мёртв — сессия сброшена, пользователь увидит экран входа.
 */
class TokenAuthenticator(
    private val store: TokenStore,
    private val apiProvider: () -> ApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // не зацикливаемся
        if (response.request.header("X-Retried") != null) return null
        val refresh = store.refreshToken ?: return null

        val newTokens = runCatching {
            runBlocking { apiProvider().refresh(RefreshRequest(refresh, store.deviceId)) }
        }.getOrNull() ?: run {
            store.clear()
            return null
        }

        store.accessToken = newTokens.accessToken
        store.refreshToken = newTokens.refreshToken

        return response.request.newBuilder()
            .header("Authorization", "Bearer ${newTokens.accessToken}")
            .header("X-Retried", "1")
            .build()
    }
}
