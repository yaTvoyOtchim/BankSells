@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.ProductCategory
import com.bank.salestracker.data.model.Sale
import com.bank.salestracker.di.ServiceLocator
import kotlinx.coroutines.launch

class AddSaleVm : ViewModel() {
    var clientLast4 by mutableStateOf("")
    var category by mutableStateOf<ProductCategory?>(null)
    var amount by mutableStateOf("")
    var quantity by mutableStateOf("1")
    var comment by mutableStateOf("")
    var loading by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    private val needsAmount get() = category in setOf(
        ProductCategory.CONSUMER_LOAN,
        ProductCategory.MORTGAGE,
        ProductCategory.DEPOSIT,
        ProductCategory.INVESTMENT
    )

    fun save(onSaved: () -> Unit) {
        val last4 = clientLast4.filter(Char::isDigit).take(4)
        if (last4.length != 4) {
            message = "Введите последние 4 цифры телефона клиента"
            return
        }
        val cat = category ?: run {
            message = "Выберите продукт"
            return
        }
        val qty = quantity.toIntOrNull()?.coerceIn(1, 99) ?: 1
        val amt = amount.replace(",", ".").toDoubleOrNull()
        if (needsAmount && (amt == null || amt <= 0)) {
            message = "Укажите сумму сделки"
            return
        }

        viewModelScope.launch {
            loading = true
            message = null
            val sentOnline = runCatching {
                ServiceLocator.salesRepo.addSale(
                    Sale(
                        category = cat,
                        clientLast4 = last4,
                        amount = amt,
                        quantity = qty,
                        comment = comment.ifBlank { null }
                    )
                )
            }.getOrDefault(false)
            loading = false
            message = if (sentOnline) "Продажа записана" else "Сохранено офлайн, отправится при появлении сети"
            onSaved()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSaleScreen(onSaved: () -> Unit, vm: AddSaleVm = viewModel()) {
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Новая продажа") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = vm.clientLast4,
                onValueChange = { vm.clientLast4 = it.filter(Char::isDigit).take(4) },
                label = { Text("Последние 4 цифры телефона") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Продукт", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProductCategory.entries.forEach { category ->
                    FilterChip(
                        selected = vm.category == category,
                        onClick = { vm.category = category },
                        label = { Text(category.title) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vm.amount,
                    onValueChange = { vm.amount = it },
                    label = { Text("Сумма, ₽") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextField(
                    value = vm.quantity,
                    onValueChange = { vm.quantity = it.filter(Char::isDigit).take(2) },
                    label = { Text("Кол-во") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = vm.comment,
                onValueChange = { vm.comment = it.take(200) },
                label = { Text("Комментарий") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Button(
                onClick = { vm.save(onSaved) },
                enabled = !vm.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (vm.loading) "Сохранение..." else "Записать продажу")
            }
        }
    }
}
