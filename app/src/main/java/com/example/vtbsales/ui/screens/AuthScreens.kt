package com.example.vtbsales.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.ui.VtbAppState
import com.example.vtbsales.ui.components.AppCard
import com.example.vtbsales.ui.components.PrimaryButton
import com.example.vtbsales.ui.components.SecondaryButton
import com.example.vtbsales.ui.components.VtbLogoMark
import com.example.vtbsales.ui.components.VtbTextField
import com.example.vtbsales.ui.theme.VtbBlue
import com.example.vtbsales.ui.theme.VtbBlueDark
import com.example.vtbsales.ui.theme.VtbCyan
import com.example.vtbsales.ui.theme.VtbMuted
import com.example.vtbsales.ui.theme.VtbSurface
import com.example.vtbsales.ui.theme.VtbText

@Composable
fun WelcomeScreen(state: VtbAppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(VtbBlueDark, VtbBlue, VtbCyan)))
            .padding(28.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            VtbLogoMark()
            Spacer(Modifier.height(22.dp))
            Column {
                Text("ВТБ", style = MaterialTheme.typography.displayLarge, color = Color.White)
                Text(
                    "Учет продаж, план и рейтинг прямо на смене",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
            Spacer(Modifier.height(26.dp))
            WelcomeBullet("Вносите продажи в пару касаний")
            Spacer(Modifier.height(14.dp))
            WelcomeBullet("Смотрите отчет за день и месяц")
            Spacer(Modifier.height(14.dp))
            WelcomeBullet("Рейтинг отдела и личный план всегда рядом")
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            WelcomePrimaryButton(
                text = "Создать аккаунт",
                onClick = { state.screen = AppScreen.Register }
            )
            WelcomeSecondaryButton(
                text = "У меня уже есть аккаунт",
                onClick = { state.screen = AppScreen.PinLogin }
            )
        }
    }
}

@Composable
private fun WelcomePrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = VtbBlue)
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun WelcomeSecondaryButton(text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun WelcomeBullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun RegisterScreen(state: VtbAppState) {
    var name by remember { mutableStateOf("Якименко Никита Дмитриевич") }
    var office by remember { mutableStateOf("office-8617-0290") }
    var pin by remember { mutableStateOf("1111") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VtbSurface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Регистрация", style = MaterialTheme.typography.headlineMedium, color = VtbText)
        Text(
            "Введите UID офиса, который выдал руководитель. Так приложение сразу привяжет профиль к нужному отделению.",
            style = MaterialTheme.typography.bodyMedium,
            color = VtbMuted
        )
        AppCard {
            VtbTextField(name, "ФИО") { name = it }
            Spacer(Modifier.height(10.dp))
            VtbTextField(office, "UID офиса от руководителя") { office = it }
            Spacer(Modifier.height(10.dp))
            VtbTextField(pin, "PIN-код") { pin = it.take(4) }
        }
        PrimaryButton("Продолжить") {
            state.registerEmployee(name.ifBlank { "Сотрудник ВТБ" }, office.ifBlank { "office-8617-0290" }, pin.ifBlank { "1111" })
        }
        SecondaryButton("Назад") { state.screen = AppScreen.Welcome }
    }
}

@Composable
fun UidCreatedScreen(state: VtbAppState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VtbSurface)
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VtbLogoMark()
        Spacer(Modifier.height(20.dp))
        Text("Аккаунт создан", style = MaterialTheme.typography.headlineMedium, color = VtbText)
        Text(
            "Профиль создан и привязан к офису. Личный UID можно использовать для ручной проверки у руководителя.",
            style = MaterialTheme.typography.bodyMedium,
            color = VtbMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        AppCard {
            Text("Ваш UID", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
            Text(
                state.lastCreatedUid.orEmpty(),
                style = MaterialTheme.typography.headlineMedium,
                color = VtbBlue
            )
            Spacer(Modifier.height(12.dp))
            Text("Офис", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
            Text(
                state.currentUser?.office.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = VtbText
            )
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Перейти в приложение") { state.screen = AppScreen.EmployeeHome }
    }
}

@Composable
fun PinLoginScreen(state: VtbAppState) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VtbSurface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VtbLogoMark()
        Spacer(Modifier.height(18.dp))
        Text("С возвращением", style = MaterialTheme.typography.headlineMedium, color = VtbText)
        Text("Введите PIN-код", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { index ->
                Box(
                    Modifier
                        .size(15.dp)
                        .clip(CircleShape)
                        .background(if (index < pin.length) VtbBlue else Color(0xFFDCE4F2))
                )
            }
        }
        if (error) {
            Spacer(Modifier.height(12.dp))
            Text("PIN не найден. Для демо: 1111 сотрудник, 0000 руководитель.", color = Color(0xFFF05268))
        }
        Spacer(Modifier.height(28.dp))
        listOf("123", "456", "789", "←0").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { ch ->
                    PinButton(ch.toString()) {
                        when (ch) {
                            '←' -> pin = pin.dropLast(1)
                            else -> if (pin.length < 4) pin += ch
                        }
                        if (pin.length == 4) {
                            val ok = state.loginWithPin(pin)
                            error = !ok
                            if (!ok) pin = ""
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(
            "Войти как руководитель: PIN 0000",
            modifier = Modifier.clickable {
                state.loginWithPin("0000")
            },
            style = MaterialTheme.typography.labelLarge,
            color = VtbBlue
        )
    }
}

@Composable
private fun PinButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = VtbText, fontWeight = FontWeight.ExtraBold)
    }
}
