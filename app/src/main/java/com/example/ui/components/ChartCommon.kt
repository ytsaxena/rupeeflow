package com.example.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CreditGreenDark
import com.example.ui.theme.CreditGreenLight
import com.example.ui.theme.DebitRedDark
import com.example.ui.theme.DebitRedLight

// A darker, saturated tone reads clearly on a light surface; a brighter tone is
// needed for the same clarity against a near-black surface, so these switch with theme.
val CreditColor: Color
    @Composable get() = if (isSystemInDarkTheme()) CreditGreenDark else CreditGreenLight

val DebitColor: Color
    @Composable get() = if (isSystemInDarkTheme()) DebitRedDark else DebitRedLight

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = RoundedCornerShape(4.dp), color = color, modifier = Modifier.size(12.dp)) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
