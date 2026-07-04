@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bank.salestracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bank.salestracker.data.model.ProductSetting
import com.bank.salestracker.data.model.SaleBatchItem
import com.bank.salestracker.di.ServiceLocator
import com.bank.salestracker.ui.theme.AppBackground
import com.bank.salestracker.ui.theme.GlassSurface
import com.bank.salestracker.ui.theme.GlassTopBar
import com.bank.salestracker.ui.theme.GradientActionButton
import kotlinx.coroutines.launch

class AddSaleVm : ViewModel() {
    var clientLast4 by mutableStateOf("")
    var products by mutableStateOf<List<ProductSetting>>(emptyList())
    var selectedProducts = mutableStateListOf<ProductSetting>()
    var amounts = mutableStateMapOf<String, String>()
    var comment by mutableStateOf("")
    var loading by mutableStateOf(false)
    var loadingProducts by mutableStateOf(true)
    var message by mutableStateOf<String?>(null)

    init {
        loadProducts()
    }

    fun loadProducts() = viewModelScope.launch {
        loadingProducts = true
        runCatching { products = ServiceLocator.salesRepo.activeProducts() }
            .onFailure { message = "Не удалось загрузить продукты офиса" }
        loadingProducts = false
    }

    fun toggleProduct(product: ProductSetting) {
        if (selectedProducts.any { it.productId == product.productId }) {
            selectedProducts.removeAll { it.productId == product.productId }
            amounts.remove(product.productId)
        } else {
            selectedProducts.add(product)
        }
    }

    fun save(onSaved: () -> Unit) {
        val last4 = clientLast4.filter(Char::isDigit).take(4)
        if (last4.length != 4) {
            message = "Введите последние 4 цифры телефона клиента"
            return
        }
        if (selectedProducts.isEmpty()) {
            message = "Выберите хотя бы один продукт"
            return
        }

        val items = mutableListOf<SaleBatchItem>()
        for (product in selectedProducts) {
            val amount = amounts[product.productId]?.replace(",", ".")?.toDoubleOrNull()
            if (product.requiresAmount && (amount == null || amount <= 0)) {
                message = "Укажите сумму для ${product.title}"
                return
            }
            items.add(
                SaleBatchItem(
                    productId = product.productId,
                    amount = amount,
                    quantity = 1,
                    comment = comment.ifBlank { null }
                )
            )
        }

        viewModelScope.launch {
            loading = true
            message = null
            val sentOnline = ServiceLocator.salesRepo.addSalesBatch(last4, items)
            loading = false
            message = if (sentOnline) {
                "Продажи записаны"
            } else {
                "Сохранено офлайн, отправится при появлении сети"
            }
            onSaved()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSaleScreen(onSaved: () -> Unit, vm: AddSaleVm = viewModel()) {
    val snackbar = remember { SnackbarHostState() }
    val selectedPoints = vm.selectedProducts.sumOf { it.points }
    val amountProducts = vm.selectedProducts.filter { it.requiresAmount }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    GlassTopBar(
                        title = "Новая продажа",
                        subtitle = "Последние 4 цифры и все продукты клиента"
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pad ->
            Column(
                Modifier
                    .padding(pad)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Клиент", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = vm.clientLast4,
                            onValueChange = { vm.clientLast4 = it.filter(Char::isDigit).take(4) },
                            label = { Text("Последние 4 цифры телефона") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Продукты", style = MaterialTheme.typography.titleMedium)
                            Text("${vm.selectedProducts.size} выбрано", color = MaterialTheme.colorScheme.primary)
                        }
                        when {
                            vm.loadingProducts -> Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }

                            vm.products.isEmpty() -> {
                                Text("Для вашего офиса пока нет активных продуктов", color = MaterialTheme.colorScheme.outline)
                                OutlinedButton(onClick = vm::loadProducts) { Text("Обновить") }
                            }

                            else -> FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                vm.products.sortedBy { it.sortOrder }.forEach { product ->
                                    FilterChip(
                                        selected = vm.selectedProducts.any { it.productId == product.productId },
                                        onClick = { vm.toggleProduct(product) },
                                        label = { Text(product.title) },
                                        shape = MaterialTheme.shapes.medium,
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                if (vm.selectedProducts.isNotEmpty()) {
                    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Итог", style = MaterialTheme.typography.titleMedium)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SummaryPill("${vm.selectedProducts.size}", "продуктов", Modifier.weight(1f))
                                SummaryPill(formatSalePoints(selectedPoints), "баллов", Modifier.weight(1f))
                            }
                            Text(
                                vm.selectedProducts.joinToString { it.title },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (amountProducts.isNotEmpty()) {
                    GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Суммы по продуктам", style = MaterialTheme.typography.titleMedium)
                            amountProducts.forEach { product ->
                                OutlinedTextField(
                                    value = vm.amounts[product.productId].orEmpty(),
                                    onValueChange = {
                                        vm.amounts[product.productId] = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }
                                    },
                                    label = { Text("${product.title}: сумма, ₽") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                GlassSurface(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                    OutlinedTextField(
                        value = vm.comment,
                        onValueChange = { vm.comment = it.take(200) },
                        label = { Text("Комментарий ко всему набору") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }

                GradientActionButton(
                    text = if (vm.loading) "Сохранение..." else "Записать продажи (${vm.selectedProducts.size})",
                    onClick = { vm.save(onSaved) },
                    enabled = !vm.loading && !vm.loadingProducts && vm.products.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                )

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddSaleBottomSheet(
    sessionKey: Int,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
    vm: AddSaleVm = viewModel(key = "add_sale_sheet_$sessionKey")
) {
    var stage by rememberSaveable(sessionKey) { mutableStateOf("phone") }
    val selectedPoints = vm.selectedProducts.sumOf { it.points }
    val amountProducts = vm.selectedProducts.filter { it.requiresAmount }
    val purple = Color(0xFF6246FF)
    val darkTheme = isSystemInDarkTheme()
    val titleColor = if (darkTheme) Color.White else Color(0xFF171B2A)

    val sheetModifier = Modifier
        .fillMaxWidth()
        .imePadding()
        .padding(horizontal = 22.dp)
        .then(
            if (stage == "products") {
                Modifier
                    .fillMaxHeight(0.92f)
                    .padding(bottom = 12.dp)
            } else {
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            }
        )

    Column(
        modifier = sheetModifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (stage == "products") {
                IconButton(onClick = { stage = "phone" }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = titleColor)
                }
                Spacer(Modifier.width(2.dp))
            }
            Text(
                "Новая продажа",
                style = MaterialTheme.typography.titleLarge,
                color = titleColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }

        if (stage == "phone") {
            SalePhoneStep(
                value = vm.clientLast4,
                onValueChange = { vm.clientLast4 = it.filter(Char::isDigit).take(4) },
                onNext = { stage = "products" },
                message = vm.message
            )
        } else {
            SaleProductsStep(
                vm = vm,
                selectedPoints = selectedPoints,
                amountProducts = amountProducts,
                purple = purple,
                onSaved = onSaved,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun SalePhoneStep(
    value: String,
    onValueChange: (String) -> Unit,
    onNext: () -> Unit,
    message: String?
) {
    val purple = Color(0xFF6246FF)
    val darkTheme = isSystemInDarkTheme()
    val cardColor = if (darkTheme) Color(0xFF1B2236) else Color(0xFFFCFCFF)
    val textColor = if (darkTheme) Color.White else Color(0xFF171B2A)
    val mutedColor = if (darkTheme) Color(0xFFBBC2D8) else Color(0xFF737995)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cardColor,
        tonalElevation = 0.dp,
        shadowElevation = if (darkTheme) 0.dp else 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = purple.copy(alpha = if (darkTheme) 0.18f else 0.10f)
            ) {
                Icon(
                    Icons.Default.PhoneAndroid,
                    null,
                    tint = purple,
                    modifier = Modifier.padding(13.dp).size(28.dp)
                )
            }
            Text(
                "Телефон клиента",
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Введите последние 4 цифры номера - руководитель видит продажи по клиентам",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedColor,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            PhoneLast4Field(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )
            message?.takeIf { value.length == 4 }.let { msg ->
                if (msg != null) {
                    Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }

    Button(
        onClick = onNext,
        enabled = value.length == 4,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = purple.copy(alpha = 0.60f),
            contentColor = Color.White,
            disabledContainerColor = purple.copy(alpha = 0.32f),
            disabledContentColor = Color.White.copy(alpha = 0.86f)
        )
    ) {
        Text("К выбору продукта", fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SaleProductsStep(
    vm: AddSaleVm,
    selectedPoints: Double,
    amountProducts: List<ProductSetting>,
    purple: Color,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val cardColor = if (darkTheme) Color(0xFF1B2236) else Color(0xFFFCFCFF)
    val textColor = if (darkTheme) Color.White else Color(0xFF171B2A)
    val mutedColor = if (darkTheme) Color(0xFFBBC2D8) else Color(0xFF737995)
    val canSave = vm.selectedProducts.isNotEmpty() && amountProducts.all { product ->
        val amount = vm.amounts[product.productId]?.replace(",", ".")?.toDoubleOrNull()
        !product.requiresAmount || (amount != null && amount > 0)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(22.dp),
            color = cardColor,
            tonalElevation = 0.dp,
            shadowElevation = if (darkTheme) 0.dp else 8.dp
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = purple.copy(alpha = if (darkTheme) 0.18f else 0.10f)
                    ) {
                        Icon(Icons.Default.ShoppingBag, null, tint = purple, modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Продукты", style = MaterialTheme.typography.titleMedium, color = textColor, fontWeight = FontWeight.Bold)
                        Text("Клиент **** ${vm.clientLast4}", color = mutedColor, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${vm.selectedProducts.size} выбрано", color = purple, fontWeight = FontWeight.Bold)
                }

                when {
                    vm.loadingProducts -> Box(
                        Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    vm.products.isEmpty() -> {
                        Text("Для вашего офиса пока нет активных продуктов", color = mutedColor)
                        OutlinedButton(onClick = vm::loadProducts) { Text("Обновить") }
                    }

                    else -> FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        vm.products.sortedBy { it.sortOrder }.forEach { product ->
                            FilterChip(
                                selected = vm.selectedProducts.any { it.productId == product.productId },
                                onClick = { vm.toggleProduct(product) },
                                label = { Text(product.title) },
                                shape = RoundedCornerShape(18.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = purple,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        if (vm.selectedProducts.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = cardColor.copy(alpha = if (darkTheme) 0.94f else 0.86f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Выбрано: ${vm.selectedProducts.size} · +${formatSalePoints(selectedPoints)} б.",
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            vm.selectedProducts.joinToString { it.title },
                            color = mutedColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        if (amountProducts.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = cardColor.copy(alpha = if (darkTheme) 0.94f else 0.86f)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Суммы по продуктам", color = textColor, fontWeight = FontWeight.Bold)
                    amountProducts.forEach { product ->
                        OutlinedTextField(
                            value = vm.amounts[product.productId].orEmpty(),
                            onValueChange = {
                                vm.amounts[product.productId] = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }
                            },
                            label = { Text("${product.title}: сумма, ₽") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = vm.comment,
            onValueChange = { vm.comment = it.take(200) },
            label = { Text("Комментарий ко всему набору") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 2
        )

        vm.message?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { vm.save(onSaved) },
            enabled = canSave && !vm.loading && !vm.loadingProducts,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = purple,
                contentColor = Color.White,
                disabledContainerColor = purple.copy(alpha = 0.34f),
                disabledContentColor = Color.White.copy(alpha = 0.82f)
            )
        ) {
            Text(
                if (vm.loading) "Сохранение..." else "Сохранить чек · +${formatSalePoints(selectedPoints)} б.",
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            "Закрыть",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onDismiss)
                .padding(6.dp),
            color = mutedColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PhoneLast4Field(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val purple = Color(0xFF6246FF)
    val focusRequester = remember { FocusRequester() }
    val borderColor = if (value.length == 4) purple else purple.copy(alpha = 0.42f)
    val dotColor = if (isSystemInDarkTheme()) Color(0xFFE8EAFF) else Color(0xFF707070)

    Box(
        modifier = modifier
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSystemInDarkTheme()) Color(0xFF11182A) else Color.White)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { focusRequester.requestFocus() },
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(4)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                Text(
                    "•",
                    color = if (index < value.length) dotColor else dotColor.copy(alpha = 0.38f),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatSalePoints(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
