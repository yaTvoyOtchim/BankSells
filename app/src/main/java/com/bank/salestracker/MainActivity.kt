package com.bank.salestracker

import android.app.Application
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.navigation.AppNavHost
import com.bank.salestracker.ui.theme.BankTheme

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BankTheme {
                var unlocked by remember {
                    // Биометрия нужна только если уже есть активная сессия
                    mutableStateOf(!ServiceLocator.tokenStore.isLoggedIn)
                }

                LaunchedEffect(Unit) {
                    if (!unlocked) askBiometric(
                        onSuccess = { unlocked = true },
                        onUnavailable = { unlocked = true } // нет биометрии — пропускаем, защита на уровне токена
                    )
                }

                if (unlocked) AppNavHost()
            }
        }
    }

    private fun askBiometric(onSuccess: () -> Unit, onUnavailable: () -> Unit) {
        val can = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (can != BiometricManager.BIOMETRIC_SUCCESS) { onUnavailable(); return }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    // Пользователь отменил — выходим из сессии, пусть входит по паролю
                    ServiceLocator.tokenStore.clear()
                    recreate()
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Вход в приложение")
                .setSubtitle("Подтвердите личность")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }
}
