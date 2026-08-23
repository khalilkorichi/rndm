package com.rndm.app.presentation.draw.wheel.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.res.ResourcesCompat
import com.rndm.app.R
import com.rndm.app.core.theme.ExtendedColors
import com.rndm.app.domain.model.ProfileItem
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun WheelCanvas(
    items: List<ProfileItem>,
    rotation: Float,
    extendedColors: ExtendedColors,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val typeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic_bold)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .rotate(rotation)
    ) {
        val sliceAngle = 360f / items.size
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            textAlign = Paint.Align.CENTER
            this.typeface = typeface
            isAntiAlias = true
        }

        items.forEachIndexed { index, item ->
            val startAngle = index * sliceAngle - 90f
            val color = extendedColors.wheelSegments[index % extendedColors.wheelSegments.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sliceAngle,
                useCenter = true,
                size = Size(size.width, size.height)
            )

            val midAngleRad = Math.toRadians((startAngle + sliceAngle / 2.0))
            val textDistance = radius * 0.65f
            val textX = center.x + (textDistance * cos(midAngleRad)).toFloat()
            val textY = center.y + (textDistance * sin(midAngleRad)).toFloat() + 10f

            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.rotate(
                (startAngle + sliceAngle / 2f) + 90f,
                textX,
                textY
            )
            val displayLabel = if (item.label.length > 10) "${item.label.take(9)}..." else item.label
            drawContext.canvas.nativeCanvas.drawText(displayLabel, textX, textY, textPaint)
            drawContext.canvas.nativeCanvas.restore()
        }
    }
}
