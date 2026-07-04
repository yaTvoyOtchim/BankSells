package com.example.vtbsales.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.vtbsales.model.AppScreen
import com.example.vtbsales.model.ProductType
import com.example.vtbsales.model.ReportSummary
import com.example.vtbsales.model.TeamMemberSummary
import com.example.vtbsales.ui.theme.VtbAmber
import com.example.vtbsales.ui.theme.VtbBlue
import com.example.vtbsales.ui.theme.VtbBlueDark
import com.example.vtbsales.ui.theme.VtbBlueDeep
import com.example.vtbsales.ui.theme.VtbCard
import com.example.vtbsales.ui.theme.VtbCyan
import com.example.vtbsales.ui.theme.VtbDanger
import com.example.vtbsales.ui.theme.VtbLine
import com.example.vtbsales.ui.theme.VtbMuted
import com.example.vtbsales.ui.theme.VtbSurface
import com.example.vtbsales.ui.theme.VtbText
import java.util.Locale

@Composable
fun ScreenFrame(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .background(VtbSurface)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium, color = VtbText)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
            }
            trailing?.invoke()
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
fun VtbLogoMark(text: String = "ВТБ") {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.linearGradient(listOf(VtbBlue, VtbCyan))),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = VtbCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = { Column(Modifier.padding(16.dp)) { content() } }
    )
}

@Composable
fun GradientHeroCard(
    title: String,
    value: String,
    subtitle: String,
    badge: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = VtbBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(VtbBlueDark, VtbBlue, VtbCyan)
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(title, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.bodyMedium)
                    Text(value, color = Color.White, style = MaterialTheme.typography.displayLarge)
                    Text(subtitle, color = Color.White.copy(alpha = 0.84f), style = MaterialTheme.typography.bodyMedium)
                }
                Pill(text = badge, color = Color.White.copy(alpha = 0.18f), textColor = Color.White)
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    AppCard(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = VtbText)
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
    }
}

@Composable
fun Pill(text: String, color: Color = Color(0xFFE8F0FF), textColor: Color = VtbBlue) {
    Surface(
        color = color,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VtbBlue)
    ) {
        Text(text, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text, color = VtbBlue, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun VtbTextField(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun ProductChip(product: ProductType, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        color = if (selected) VtbBlue else VtbCard,
        shape = RoundedCornerShape(18.dp),
        border = if (selected) null else BorderStroke(1.dp, VtbLine)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "продукт",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) Color.White.copy(alpha = 0.78f) else VtbMuted
            )
            Text(
                product.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) Color.White else VtbText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProgressLine(label: String, value: Double, target: Double, color: Color = VtbBlue) {
    val progress = if (target <= 0.0) 0f else (value / target).toFloat().coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
            Text("${value.format0()} / ${target.format0()}", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = color,
            trackColor = VtbSurface
        )
    }
}

@Composable
fun SaleRow(title: String, caption: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = VtbText, maxLines = 1)
            Text(caption, style = MaterialTheme.typography.bodyMedium, color = VtbMuted, maxLines = 1)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = VtbText)
    }
}

@Composable
fun EmployeeRow(summary: TeamMemberSummary, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFE8F0FF)),
            contentAlignment = Alignment.Center
        ) {
            Text(initials(summary.user.name), color = VtbBlue, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(summary.user.name, style = MaterialTheme.typography.titleMedium, color = VtbText, maxLines = 1)
            Text(
                "${summary.dailyReport.clients} клиентов · сегодня ${summary.dailyReport.points.format1()}",
                style = MaterialTheme.typography.bodyMedium,
                color = VtbMuted,
                maxLines = 1
            )
        }
        Pill(
            text = if (summary.dailyReport.products == 0) "нет продаж" else "#${summary.rank}",
            color = if (summary.dailyReport.products == 0) Color(0xFFFFEDF1) else Color(0xFFE8F0FF),
            textColor = if (summary.dailyReport.products == 0) VtbDanger else VtbBlue
        )
    }
}

@Composable
fun ReportSummaryCard(report: ReportSummary, title: String) {
    AppCard {
        Text(title, style = MaterialTheme.typography.titleLarge, color = VtbText)
        Text("${report.from} — ${report.to} · ${report.clients} клиентов", style = MaterialTheme.typography.bodyMedium, color = VtbMuted)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Продукты", report.products.toString(), "шт", Modifier.weight(1f))
            MetricCard("Баллы", report.points.format1(), "итого", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        report.productBreakdown.forEach { (type, count) ->
            SaleRow(type.title, "по формату", "$count шт")
        }
        report.otherProducts.forEach { (name, count) ->
            SaleRow(name, "другие продукты", "$count шт")
        }
    }
}

@Composable
fun BottomNav(items: List<Pair<AppScreen, String>>, current: AppScreen, onSelect: (AppScreen) -> Unit) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { (screen, label) ->
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(screen) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 26.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(if (screen == current) VtbBlue else Color.Transparent)
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (screen == current) VtbBlue else VtbMuted,
                        fontWeight = if (screen == current) FontWeight.ExtraBold else FontWeight.Bold
                    )
                }
            }
        }
    }
}

fun Double.format0(): String = String.format(Locale.US, "%.0f", this)
fun Double.format1(): String = String.format(Locale.US, "%.1f", this)
fun Double.format2(): String = String.format(Locale.US, "%.2f", this)

private fun initials(name: String): String =
    name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
