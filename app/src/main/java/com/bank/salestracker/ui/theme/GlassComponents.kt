package com.bank.salestracker.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun appBackgroundBrush(): Brush =
    if (isSystemInDarkTheme()) {
        Brush.linearGradient(listOf(Color(0xFF020918), Color(0xFF071D4D), Color(0xFF062028)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFEEF6FF), Color(0xFFE9FFF9)))
    }

@Composable
fun glassBrush(): Brush =
    if (isSystemInDarkTheme()) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.06f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.46f)))
    }

@Composable
fun glassBorder(): BorderStroke =
    if (isSystemInDarkTheme()) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.72f))
    }

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .fillMaxSize()
            .background(appBackgroundBrush())
    ) {
        content()
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    elevation: Dp = if (isSystemInDarkTheme()) 0.dp else 18.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .shadow(elevation = elevation, shape = shape, clip = false)
            .clip(shape)
            .background(glassBrush())
            .border(glassBorder(), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        elevation = 10.dp
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
fun MetricGlassCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    GlassSurface(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
        elevation = 0.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, fontWeight = FontWeight.Bold)
            supporting?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun GradientActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Surface(
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .background(HeroGradient, RoundedCornerShape(18.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                leading?.let {
                    it()
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
