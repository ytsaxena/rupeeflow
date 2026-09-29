package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ChartEntry

@Composable
fun PeriodBarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    val creditColor = CreditColor
    val debitColor = DebitColor

    LaunchedEffect(entries) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("period_bar_chart_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendDot(color = DebitColor, label = "Debit")
                LegendDot(color = CreditColor, label = "Credit")
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxVal = entries.maxOfOrNull { maxOf(it.credit, it.debit) }?.takeIf { it > 0 } ?: 1000.0
            val scrollState = rememberScrollState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                entries.forEach { entry ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.testTag("chart_bar_${entry.label}")
                    ) {
                        Box(
                            modifier = Modifier.height(140.dp).width(30.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.size(30.dp, 140.dp)) {
                                val totalH = size.height
                                val barW = 9.dp.toPx()
                                val cornerR = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                                drawRoundRect(
                                    color = Color.LightGray.copy(alpha = 0.18f),
                                    topLeft = Offset(1.dp.toPx(), 0f),
                                    size = Size(barW, totalH),
                                    cornerRadius = cornerR
                                )
                                drawRoundRect(
                                    color = Color.LightGray.copy(alpha = 0.18f),
                                    topLeft = Offset(15.dp.toPx(), 0f),
                                    size = Size(barW, totalH),
                                    cornerRadius = cornerR
                                )

                                val debitRatio = (entry.debit / maxVal).coerceIn(0.0, 1.0) * animatedProgress.value
                                val debitH = (totalH * debitRatio).toFloat()
                                if (debitH > 0) {
                                    drawRoundRect(
                                        color = debitColor.copy(alpha = 0.85f),
                                        topLeft = Offset(1.dp.toPx(), totalH - debitH),
                                        size = Size(barW, debitH),
                                        cornerRadius = cornerR
                                    )
                                }

                                val creditRatio = (entry.credit / maxVal).coerceIn(0.0, 1.0) * animatedProgress.value
                                val creditH = (totalH * creditRatio).toFloat()
                                if (creditH > 0) {
                                    drawRoundRect(
                                        color = creditColor.copy(alpha = 0.85f),
                                        topLeft = Offset(15.dp.toPx(), totalH - creditH),
                                        size = Size(barW, creditH),
                                        cornerRadius = cornerR
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
