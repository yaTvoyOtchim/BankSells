package com.bank.salestracker.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bank.salestracker.R
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import kotlinx.coroutines.launch

@Composable
fun WaitingScreen(onLogout: () -> Unit) {
    val user = ServiceLocator.authRepo.currentUser()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    AppBackground {
        Box(
            Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.vtb_logo),
                        contentDescription = null,
                        modifier = Modifier.size(82.dp)
                    )
                    Text("Профиль создан", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Передайте табельный номер руководителю. После привязки к офису откроются продажи и отчеты.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), elevation = 0.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(user?.fullName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                            Text(user?.employeeId.orEmpty(), style = MaterialTheme.typography.headlineSmall)
                            Text("Статус: ожидает привязки", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("employee_id", user?.employeeId.orEmpty()))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
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
        }
    }
}
