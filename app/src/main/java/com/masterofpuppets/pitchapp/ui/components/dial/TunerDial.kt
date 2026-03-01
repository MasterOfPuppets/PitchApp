package com.masterofpuppets.pitchapp.ui.components.dial

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var life: Float = 1f,
    val symbol: String,
    val color: Color,
    val spawnCents: Float
)

@Composable
fun TunerDial(
    cents: Double,
    waveformData: FloatArray,
    isPitchDetected: Boolean,
    isPitchLocked: Boolean,
    modifier: Modifier = Modifier,
    dialSweepAngle: Float = 120f,
    tickColor: Color = Color.LightGray,
    textColor: Color = Color.LightGray,
    needleBaseWidth: Float = 36f,
    needleLengthRatio: Float = 0.85f
) {
    val textMeasurer = rememberTextMeasurer()
    val currentCents by rememberUpdatedState(cents)
    var strobePhase by remember { mutableFloatStateOf(0f) }

    val particles = remember { mutableStateListOf<Particle>() }
    val symbols = listOf("♪", "♫", "♬", "♩")

    val needleIntensity by animateFloatAsState(
        targetValue = if (isPitchDetected) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPitchDetected) 100 else 500),
        label = "NeedleFade"
    )

    LaunchedEffect(isPitchDetected, isPitchLocked) {
        var lastFrameTime = withFrameNanos { it }
        while (true) {
            val currentFrameTime = withFrameNanos { it }
            val dt = (currentFrameTime - lastFrameTime) / 1E9f
            lastFrameTime = currentFrameTime

            if (isPitchDetected) {
                val speed = currentCents.toFloat() * 0.4f
                strobePhase += speed * dt
            } else {
                strobePhase = 0f
            }

            if (isPitchLocked) {
                // Spawn new particles randomly along the length of the needle
                if (Random.nextFloat() < 0.6f) {
                    particles.add(
                        Particle(
                            x = 0f,
                            y = Random.nextFloat(), // 0.0 to 1.0 (relative position along the needle)
                            velocityX = (Random.nextFloat() * 100f + 200f) * (if (Random.nextBoolean()) 1f else -1f), // Drift left or right
                            velocityY = -(Random.nextFloat() * 100f + 100f) * (if (Random.nextBoolean()) 1f else -1f), // Drift upwards or downwards
                            symbol = symbols.random(),
                            color = Color.hsv(Random.nextFloat() * 120f, 1f, 1f),
                            spawnCents = currentCents.toFloat() // spawn origin
                        )
                    )
                }
            }

            val iterator = particles.iterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.velocityX * dt
                p.y += (p.velocityY * dt) / 500f
                p.life -= dt * 0.5f // Fade out speed
                if (p.life <= 0f) {
                    iterator.remove()
                }
            }

            if (!isPitchDetected && particles.isEmpty()) {
                break
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.5f)
    ) {
        val center = Offset(size.width / 2, size.height * 0.8f)
        val radius = size.width * 0.45f

        drawDialBackground(
            center = center,
            radius = radius,
            sweepAngle = dialSweepAngle,
            tickColor = tickColor,
            textColor = textColor,
            textMeasurer = textMeasurer
        )

        drawStrobeBox(
            center = center,
            radius = radius,
            waveformData = waveformData,
            isPitchDetected = isPitchDetected,
            strobePhase = strobePhase
        )

        val clampedCents = cents.coerceIn(-50.0, 50.0).toFloat()
        val isPerfectlyInTune = abs(clampedCents) <= 3f

        val targetHue = if (isPerfectlyInTune) {
            120f // Green
        } else {
            60f * (1f - (abs(clampedCents) / 50f)) // Yellow -> Red
        }

        val needleColor = Color.hsv(
            hue = targetHue,
            saturation = needleIntensity,
            value = 0.5f + (0.5f * needleIntensity),
            alpha = 0.3f + (0.7f * needleIntensity)
        )

        val maxAngleDeviation = dialSweepAngle / 2f
        val needleAngle = 270f + (clampedCents / 50f) * maxAngleDeviation

        drawNeedle(
            center = center,
            radius = radius * needleLengthRatio,
            angleDegrees = needleAngle,
            color = needleColor,
            baseWidth = needleBaseWidth
        )

        if (particles.isNotEmpty()) {
            particles.forEach { p ->
                val particleAlpha = p.life.coerceIn(0f, 1f)
                val pColor = p.color.copy(alpha = particleAlpha)
                val pClampedCents = p.spawnCents.coerceIn(-50f, 50f)
                val pAngle = 270f + (pClampedCents / 50f) * maxAngleDeviation
                rotate(degrees = pAngle - 270f, pivot = center) {
                    val startY = center.y
                    val endY = center.y - (radius * needleLengthRatio)
                    val currentY = startY - ((startY - endY) * p.y)
                    val textLayoutResult = textMeasurer.measure(
                        text = p.symbol,
                        style = TextStyle(color = pColor, fontSize = (14f + (10f * p.life)).sp) // Shrinks slightly as it dies
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(center.x + p.x - (textLayoutResult.size.width / 2), currentY - (textLayoutResult.size.height / 2))
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawDialBackground(
    center: Offset,
    radius: Float,
    sweepAngle: Float,
    tickColor: Color,
    textColor: Color,
    textMeasurer: TextMeasurer
) {
    val startAngle = 270f - (sweepAngle / 2f)
    val tickCount = 100

    for (i in 0..tickCount) {
        val currentCents = -50 + i
        val angle = startAngle + (i.toFloat() / tickCount) * sweepAngle
        val angleRad = Math.toRadians(angle.toDouble())
        val isMajor = currentCents % 10 == 0
        val isMedium = currentCents % 5 == 0 && !isMajor
        val tickLength = when {
            isMajor -> radius * 0.15f
            isMedium -> radius * 0.10f
            else -> radius * 0.05f
        }

        if (isMajor || isMedium || currentCents % 2 == 0) {
            val strokeWidth = if (isMajor) 4f else 2f
            val startRadius = radius - tickLength
            val startX = center.x + startRadius * cos(angleRad).toFloat()
            val startY = center.y + startRadius * sin(angleRad).toFloat()
            val endX = center.x + radius * cos(angleRad).toFloat()
            val endY = center.y + radius * sin(angleRad).toFloat()

            drawLine(
                color = tickColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        if (isMajor) {
            val text = if (currentCents > 0) "+$currentCents" else currentCents.toString()
            val textLayoutResult = textMeasurer.measure(
                text = text,
                style = TextStyle(color = textColor, fontSize = 14.sp)
            )
            val textRadius = radius + 20f
            val textX = center.x + textRadius * cos(angleRad).toFloat() - (textLayoutResult.size.width / 2)
            val textY = center.y + textRadius * sin(angleRad).toFloat() - (textLayoutResult.size.height / 2)

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, textY)
            )
        }
    }
}

private fun DrawScope.drawStrobeBox(
    center: Offset,
    radius: Float,
    waveformData: FloatArray,
    isPitchDetected: Boolean,
    strobePhase: Float
) {
    val boxWidth = radius * 1.1f
    val boxHeight = radius * 0.35f
    val topLeftX = center.x - boxWidth / 2f
    val topLeftY = center.y - radius * 0.65f
    val cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
    val boxRect = RoundRect(
        rect = Rect(Offset(topLeftX, topLeftY), Size(boxWidth, boxHeight)),
        cornerRadius = cornerRadius
    )

    clipPath(Path().apply { addRoundRect(boxRect) }) {
        drawRect(
            color = Color(0xFF101010),
            topLeft = Offset(topLeftX, topLeftY),
            size = Size(boxWidth, boxHeight)
        )

        if (waveformData.isNotEmpty()) {
            val midY = topLeftY + boxHeight / 2f
            val stepX = boxWidth / waveformData.size
            var maxAmplitude = 0.001f
            for (value in waveformData) {
                val absValue = abs(value)
                if (absValue > maxAmplitude) maxAmplitude = absValue
            }
            val normalizationFactor = (1f / maxAmplitude) * 0.9f
            val waveColor = Color(0xFF00BFFF)
            val waveStrokeWidth = 6f

            if (isPitchDetected) {
                val bandCount = 4
                val bandWidth = boxWidth / bandCount
                val shift = (strobePhase * bandWidth) % bandWidth

                for (i in -1..bandCount) {
                    val startX = topLeftX + (i * bandWidth) + shift
                    val endX = startX + bandWidth
                    val brush = Brush.horizontalGradient(
                        0.0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.5f),
                        1.0f to Color.Transparent,
                        startX = startX,
                        endX = endX
                    )

                    drawRect(
                        brush = brush,
                        topLeft = Offset(startX, topLeftY),
                        size = Size(bandWidth, boxHeight)
                    )
                }

                val path = Path()
                for (i in waveformData.indices) {
                    val x = topLeftX + (i * stepX)
                    val normalizedValue = (waveformData[i] * normalizationFactor).coerceIn(-1f, 1f)
                    val y = midY - (normalizedValue * (boxHeight / 2f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = waveColor.copy(alpha = 0.8f),
                    style = Stroke(width = waveStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                val path = Path()
                for (i in waveformData.indices) {
                    val x = topLeftX + (i * stepX)
                    val normalizedValue = (waveformData[i] * normalizationFactor).coerceIn(-1f, 1f)
                    val y = midY - (normalizedValue * (boxHeight / 2f))
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = waveColor.copy(alpha = 0.6f),
                    style = Stroke(width = waveStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFB0B0B0),
                Color(0xFF505050),
                Color(0xFF202020)
            ),
            start = Offset(topLeftX, topLeftY),
            end = Offset(topLeftX + boxWidth, topLeftY + boxHeight)
        ),
        topLeft = Offset(topLeftX, topLeftY),
        size = Size(boxWidth, boxHeight),
        cornerRadius = cornerRadius,
        style = Stroke(width = 6f)
    )

    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF101010),
                Color(0xFF404040)
            ),
            start = Offset(topLeftX, topLeftY),
            end = Offset(topLeftX + boxWidth, topLeftY + boxHeight)
        ),
        topLeft = Offset(topLeftX + 3f, topLeftY + 3f),
        size = Size(boxWidth - 6f, boxHeight - 6f),
        cornerRadius = CornerRadius(cornerRadius.x - 3f, cornerRadius.y - 3f),
        style = Stroke(width = 3f)
    )
}

private fun DrawScope.drawNeedle(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    color: Color,
    baseWidth: Float
) {
    rotate(degrees = angleDegrees - 270f, pivot = center) {
        val tip = Offset(center.x, center.y - radius)
        val baseLeft = Offset(center.x - baseWidth / 2f, center.y)
        val baseRight = Offset(center.x + baseWidth / 2f, center.y)
        val path = Path().apply {
            moveTo(tip.x, tip.y)
            lineTo(baseRight.x, baseRight.y)
            lineTo(baseLeft.x, baseLeft.y)
            close()
        }

        drawPath(
            path = path,
            color = color.copy(alpha = color.alpha * 0.35f)
        )

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}