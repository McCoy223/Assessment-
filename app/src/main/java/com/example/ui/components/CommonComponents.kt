package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentScore
import com.example.domain.GradeDistribution
import com.example.domain.TrendType
import com.example.ui.theme.GradeBlue
import com.example.ui.theme.GradeGreen
import com.example.ui.theme.GradeOrange
import com.example.ui.theme.GradeRed
import com.example.ui.theme.GradeYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LetterGradeBadge(
    grade: String,
    score: Double,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (grade) {
        "A" -> GradeGreen to Color.White
        "B" -> GradeBlue to Color.White
        "C" -> GradeYellow to Color(0xFF1E293B)
        "D" -> GradeOrange to Color.White
        else -> GradeRed to Color.White
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = grade,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 18.sp
            )
            Text(
                text = "${score.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StudentAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")
        .ifEmpty { "S" }

    // Consistent pseudo-random color based on name hash
    val colors = listOf(
        Color(0xFF3B82F6),
        Color(0xFF8B5CF6),
        Color(0xFFEC4899),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFF06B6D4)
    )
    val avatarColor = colors[kotlin.math.abs(name.hashCode()) % colors.size]

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(avatarColor)
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun TrendBadge(
    trendType: TrendType,
    delta: Double,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (trendType) {
        TrendType.IMPROVING -> Triple(
            Icons.AutoMirrored.Filled.TrendingUp,
            GradeGreen,
            if (delta > 0) "+${delta}%" else "${delta}%"
        )
        TrendType.DECLINING -> Triple(
            Icons.AutoMirrored.Filled.TrendingDown,
            GradeRed,
            "${delta}%"
        )
        TrendType.STABLE -> Triple(
            Icons.AutoMirrored.Filled.TrendingFlat,
            GradeBlue,
            "Stable"
        )
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun GradeDistributionChart(
    distribution: GradeDistribution,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("A (90-100)", distribution.aCount, GradeGreen),
        Triple("B (80-89)", distribution.bCount, GradeBlue),
        Triple("C (70-79)", distribution.cCount, GradeYellow),
        Triple("D (60-69)", distribution.dCount, GradeOrange),
        Triple("F (< 60)", distribution.fCount, GradeRed)
    )

    val maxCount = items.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grade Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${distribution.total} Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            items.forEach { (label, count, color) ->
                val fraction = count.toFloat() / maxCount.toFloat()
                val percentage = if (distribution.total > 0) (count.toFloat() / distribution.total * 100).toInt() else 0

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(90.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                                .height(18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(color)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "$count ($percentage%)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(55.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun HistoricalScoreChart(
    scores: List<StudentScore>,
    modifier: Modifier = Modifier
) {
    if (scores.isEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = modifier
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Text(
                    text = "No assessment history recorded yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val avgScore = scores.map { it.score }.average()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Historical Progress Trajectory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Score progression across ${scores.size} assessments",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Avg: ${avgScore.toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Line Chart with Points & Average Reference Line
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val width = size.width
                val height = size.height

                // Draw horizontal benchmark guidelines (100%, 80%, 60%, 0%)
                val guideSteps = listOf(100f, 80f, 60f, 40f, 0f)
                guideSteps.forEach { benchmark ->
                    val y = height - (benchmark / 100f * height)
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Draw Average reference dashed line
                val avgY = height - (avgScore.toFloat() / 100f * height)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.6f),
                    start = Offset(0f, avgY),
                    end = Offset(width, avgY),
                    strokeWidth = 2f
                )

                if (scores.size == 1) {
                    val singleY = height - (scores[0].score.toFloat() / 100f * height)
                    drawCircle(
                        color = primaryColor,
                        radius = 8.dp.toPx(),
                        center = Offset(width / 2f, singleY)
                    )
                    return@Canvas
                }

                // Plot points
                val stepX = width / (scores.size - 1)
                val points = scores.mapIndexed { index, item ->
                    val x = index * stepX
                    val y = height - (item.score.toFloat().coerceIn(0f, 100f) / 100f * height)
                    Offset(x, y)
                }

                // Draw Connecting Line
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 3.5.dp.toPx()
                    )
                }

                // Draw Points with outer circle
                points.forEachIndexed { index, pt ->
                    val score = scores[index]
                    val pointColor = score.gradeColor

                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = pointColor,
                        radius = 4.5.dp.toPx(),
                        center = pt
                    )
                }
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "First: ${scores.first().assessmentName.take(16)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Latest: ${scores.last().assessmentName.take(16)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
