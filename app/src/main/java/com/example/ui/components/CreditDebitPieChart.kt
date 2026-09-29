package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CreditDebitPieChart(
    totalCredit: Double,
    totalDebit: Double,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }
    val total = totalCredit + totalDebit
    val creditColor = CreditColor
    val debitColor = DebitColor

    LaunchedEffect(totalCredit, totalDebit) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("credit_debit_pie_chart_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendDot(color = DebitColor, label = "Debit")
                LegendDot(color = CreditColor, label = "Credit")
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                if (total <= 0.0) {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        drawArc(
                            color = Color.LightGray.copy(alpha = 0.25f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.size(180.dp)) {
                        val strokeWidth = 28.dp.toPx()
                        val debitSweep = (360f * (totalDebit / total)).toFloat() * animatedProgress.value
                        val creditSweep = (360f * (totalCredit / total)).toFloat() * animatedProgress.value
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2, strokeWidth / 2)

                        var startAngle = -90f
                        if (debitSweep > 0f) {
                            drawArc(
                                color = debitColor,
                                startAngle = startAngle,
                                sweepAngle = debitSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += debitSweep
                        }
                        if (creditSweep > 0f) {
                            drawArc(
                                color = creditColor,
                                startAngle = startAngle,
                                sweepAngle = creditSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Net",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatUtils.formatInr(totalCredit - totalDebit),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Credit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatUtils.formatInr(totalCredit),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CreditColor
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Debit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatUtils.formatInr(totalDebit),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DebitColor
                    )
                }
            }
        }
    }
}
