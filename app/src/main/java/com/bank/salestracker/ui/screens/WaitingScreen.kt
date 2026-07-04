package com.bank.salestracker.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bank.salestracker.R
import com.bank.salestracker.di.ServiceLocator
import kotlinx.coroutines.launch

@Composable
fun WaitingScreen(onLogout: () -> Unit) {
    val user = ServiceLocator.authRepo.currentUser()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.vtb_logo),
            contentDescription = null,
            modifier = Modifier.size(88.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text("Профиль создан", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Передайте табельный номер руководителю. После привязки к офису откроются продажи и отчёты.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(user?.fullName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text(user?.employeeId.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                Text("Статус: ожидает привязки", color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("employee_id", user?.employeeId.orEmpty()))
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.ContentCopy, null)
            Spacer(Modifier.width(8.dp))
            Text("Скопировать табельный")
        }
        TextButton(
            onClick = {
                scope.launch {
                    ServiceLocator.authRepo.logout()
                    onLogout()
                }
            }
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Выйти")
        }
    }
}
