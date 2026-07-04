@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.R
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import com.bank.salestracker.ui.theme.GradientActionButton
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginVm : ViewModel() {
    var employeeId by mutableStateOf("")
    var password by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun login(onSuccess: (mustChangePassword: Boolean) -> Unit) {
        if (employeeId.isBlank() || password.isBlank()) {
            error = "Введите табельный номер и пароль"
            return
        }
        viewModelScope.launch {
            loading = true
            error = null
            try {
                val response = ServiceLocator.authRepo.login(employeeId, password)
                onSuccess(response.mustChangePassword)
            } catch (e: HttpException) {
                error = when (e.code()) {
                    401 -> "Неверный табельный номер или пароль"
                    423 -> "Учетная запись заблокирована"
                    else -> "Ошибка сервера (${e.code()})"
                }
            } catch (e: IOException) {
                error = "Нет соединения с сервером"
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun LoginScreen(
    onLoggedIn: (Boolean) -> Unit,
    onRegisterClick: () -> Unit,
    vm: LoginVm = viewModel()
) {
    var showPassword by remember { mutableStateOf(false) }

    AppBackground {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.vtb_logo),
                        contentDescription = null,
                        modifier = Modifier.size(82.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ВТБ Продажи", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Учет продаж для сотрудников",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = vm.employeeId,
                        onValueChange = { vm.employeeId = it },
                        label = { Text("Табельный номер") },
                        placeholder = { Text("vtb70336144") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vm.password,
                        onValueChange = { vm.password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    vm.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    GradientActionButton(
                        text = if (vm.loading) "Вход..." else "Войти",
                        onClick = { vm.login(onLoggedIn) },
                        enabled = !vm.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )
                    TextButton(onClick = onRegisterClick, enabled = !vm.loading) {
                        Text("Зарегистрироваться")
                    }
                }
            }
        }
    }
}

class RegisterVm : ViewModel() {
    var employeeId by mutableStateOf("")
    var fullName by mutableStateOf("")
    var password by mutableStateOf("")
    var repeatPassword by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun register(onSuccess: () -> Unit) {
        val normalizedEmployeeId = employeeId.trim().lowercase()
        when {
            !normalizedEmployeeId.matches(Regex("^vtb\\d+$")) -> {
                error = "Табельный должен быть в формате vtb70336144"
                return
            }
            fullName.trim().length < 2 -> {
                error = "Введите ФИО"
                return
            }
            password.length < 8 || !password.any(Char::isDigit) || !password.any(Char::isLetter) -> {
                error = "Пароль минимум 8 символов, буквы и цифры"
                return
            }
            password != repeatPassword -> {
                error = "Пароли не совпадают"
                return
            }
        }

        viewModelScope.launch {
            loading = true
            error = null
            try {
                ServiceLocator.authRepo.register(normalizedEmployeeId, fullName, password)
                onSuccess()
            } catch (e: HttpException) {
                error = when (e.code()) {
                    409 -> "Такой табельный уже зарегистрирован"
                    400 -> "Проверьте данные регистрации"
                    else -> "Ошибка сервера (${e.code()})"
                }
            } catch (e: IOException) {
                error = "Нет соединения с сервером"
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    vm: RegisterVm = viewModel()
) {
    AppBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Регистрация", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Введите свой табельный номер ВТБ и придумайте пароль",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = vm.employeeId,
                        onValueChange = { vm.employeeId = it },
                        label = { Text("Табельный номер") },
                        placeholder = { Text("vtb70336144") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vm.fullName,
                        onValueChange = { vm.fullName = it },
                        label = { Text("ФИО") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vm.password,
                        onValueChange = { vm.password = it },
                        label = { Text("Пароль") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vm.repeatPassword,
                        onValueChange = { vm.repeatPassword = it },
                        label = { Text("Повторите пароль") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    vm.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    GradientActionButton(
                        text = if (vm.loading) "Создание..." else "Создать профиль",
                        onClick = { vm.register(onRegistered) },
                        enabled = !vm.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )
                    TextButton(onClick = onBack, enabled = !vm.loading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Назад ко входу")
                    }
                }
            }
        }
    }
}

class ChangePasswordVm : ViewModel() {
    var oldPass by mutableStateOf("")
    var newPass by mutableStateOf("")
    var repeat by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun submit(onDone: () -> Unit) {
        when {
            newPass.length < 8 -> {
                error = "Минимум 8 символов"
                return
            }
            !newPass.any { it.isDigit() } || !newPass.any { it.isLetter() } -> {
                error = "Пароль должен содержать буквы и цифры"
                return
            }
            newPass != repeat -> {
                error = "Пароли не совпадают"
                return
            }
        }
        viewModelScope.launch {
            loading = true
            error = null
            try {
                ServiceLocator.authRepo.changePassword(oldPass, newPass)
                onDone()
            } catch (e: Exception) {
                error = "Не удалось сменить пароль. Проверьте текущий пароль"
            } finally {
                loading = false
            }
        }
    }
}

@Composable
fun ChangePasswordScreen(onDone: () -> Unit, vm: ChangePasswordVm = viewModel()) {
    AppBackground {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Смена временного пароля", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "При первом входе нужно задать свой пароль",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = vm.oldPass,
                        onValueChange = { vm.oldPass = it },
                        label = { Text("Текущий пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vm.newPass,
                        onValueChange = { vm.newPass = it },
                        label = { Text("Новый пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = vm.repeat,
                        onValueChange = { vm.repeat = it },
                        label = { Text("Повторите пароль") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    vm.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Button(
                        onClick = { vm.submit(onDone) },
                        enabled = !vm.loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (vm.loading) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Сохранить")
                        }
                    }
                }
            }
        }
    }
}
