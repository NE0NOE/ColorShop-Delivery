package com.rapiflete.delivery.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rapiflete.delivery.R
import com.rapiflete.delivery.ui.theme.ColorShopDeepPlum
import com.rapiflete.delivery.ui.theme.ColorShopMagenta
import com.rapiflete.delivery.ui.theme.ColorShopMutedPlum
import com.rapiflete.delivery.ui.theme.ColorShopPrimary
import com.rapiflete.delivery.ui.theme.ColorShopSurface
import kotlinx.coroutines.delay

/**
 * Renderiza el logotipo vectorial de la Mariposa de ColorShop (extraído del SVG oficial en Stitch).
 */
@Composable
fun ColorShopButterflyLogo(
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        fun sx(v: Float) = (v / 100f) * w
        fun sy(v: Float) = (v / 100f) * h

        val wingGrad1 = Brush.linearGradient(
            colors = listOf(Color(0xFFFF65A5), Color(0xFFC73FA7), Color(0xFF7928CA)),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        val wingGrad2 = Brush.linearGradient(
            colors = listOf(Color(0xFFFFB2D9), Color(0xFFFF4081), Color(0xFFB80058)),
            start = Offset(w, 0f),
            end = Offset(0f, h)
        )

        // Ala superior izquierda
        val leftUpper = Path().apply {
            moveTo(sx(50f), sy(48f))
            cubicTo(sx(45f), sy(28f), sx(22f), sy(15f), sx(12f), sy(25f))
            cubicTo(sx(2f), sy(35f), sx(10f), sy(58f), sx(35f), sy(60f))
            cubicTo(sx(44f), sy(61f), sx(48f), sy(53f), sx(50f), sy(48f))
            close()
        }
        drawPath(leftUpper, brush = wingGrad1)

        // Ala inferior izquierda
        val leftLower = Path().apply {
            moveTo(sx(48f), sy(54f))
            cubicTo(sx(40f), sy(60f), sx(20f), sy(62f), sx(16f), sy(75f))
            cubicTo(sx(12f), sy(88f), sx(30f), sy(94f), sx(42f), sy(82f))
            cubicTo(sx(47f), sy(77f), sx(48f), sy(64f), sx(48f), sy(54f))
            close()
        }
        drawPath(leftLower, brush = wingGrad2, alpha = 0.92f)

        // Ala superior derecha
        val rightUpper = Path().apply {
            moveTo(sx(50f), sy(48f))
            cubicTo(sx(55f), sy(28f), sx(78f), sy(15f), sx(88f), sy(25f))
            cubicTo(sx(98f), sy(35f), sx(90f), sy(58f), sx(65f), sy(60f))
            cubicTo(sx(56f), sy(61f), sx(52f), sy(53f), sx(50f), sy(48f))
            close()
        }
        drawPath(rightUpper, brush = wingGrad1)

        // Ala inferior derecha
        val rightLower = Path().apply {
            moveTo(sx(52f), sy(54f))
            cubicTo(sx(60f), sy(60f), sx(80f), sy(62f), sx(84f), sy(75f))
            cubicTo(sx(88f), sy(88f), sx(70f), sy(94f), sx(58f), sy(82f))
            cubicTo(sx(53f), sy(77f), sx(52f), sy(64f), sx(52f), sy(54f))
            close()
        }
        drawPath(rightLower, brush = wingGrad2, alpha = 0.92f)

        // Cuerpo y antenas
        val bodyColor = Color(0xFF4A154B)
        drawLine(
            color = bodyColor,
            start = Offset(sx(50f), sy(40f)),
            end = Offset(sx(50f), sy(75f)),
            strokeWidth = sx(3.2f),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = bodyColor,
            radius = sx(3.2f),
            center = Offset(sx(50f), sy(36f))
        )

        val leftAntenna = Path().apply {
            moveTo(sx(49f), sy(34f))
            cubicTo(sx(46f), sy(28f), sx(41f), sy(26f), sx(38f), sy(28f))
        }
        drawPath(leftAntenna, color = bodyColor, style = Stroke(width = sx(2.2f), cap = StrokeCap.Round))

        val rightAntenna = Path().apply {
            moveTo(sx(51f), sy(34f))
            cubicTo(sx(54f), sy(28f), sx(59f), sy(26f), sx(62f), sy(28f))
        }
        drawPath(rightAntenna, color = bodyColor, style = Stroke(width = sx(2.2f), cap = StrokeCap.Round))
    }
}

/**
 * Barra superior translúcida de ColorShop Delivery tal como se diseñó en Stitch.
 */
@Composable
fun ColorShopTopBar(
    showQuickSearchButton: Boolean = true,
    onQuickSearchClick: () -> Unit = {}
) {
    Surface(
        color = ColorShopSurface.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorShopButterflyLogo(size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = ColorShopDeepPlum,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("ColorShop ")
                        }
                        withStyle(
                            SpanStyle(
                                color = ColorShopPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append("Delivery")
                        }
                    },
                    fontSize = 19.sp
                )
            }

            if (showQuickSearchButton) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, CircleShape, ambientColor = ColorShopPrimary.copy(alpha = 0.2f))
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onQuickSearchClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar o pegar ubicación",
                        tint = ColorShopMutedPlum,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pantalla de Carga (Portada / Splash Screen) diseñada en Stitch ("portada").
 */
@Composable
fun ColorShopSplashScreen(
    onFinished: () -> Unit
) {
    var progress by remember { mutableFloatStateOf(0.55f) }
    var statusText by remember { mutableStateOf("Cargando mapa y tarifas...") }
    var startAnim by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (startAnim) 1.0f else 0.92f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "logoScale"
    )

    LaunchedEffect(Unit) {
        startAnim = true
        delay(450)
        progress = 0.85f
        statusText = "Preparando tu experiencia..."
        delay(500)
        progress = 1.0f
        statusText = "¡Todo listo! Bienvenido"
        delay(300)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        Color(0xFFFFF2F8),
                        Color(0xFFFBE8F2)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .scale(scale)
        ) {
            Image(
                painter = painterResource(id = R.drawable.colorshop_logo),
                contentDescription = "ColorShop",
                modifier = Modifier
                    .width(230.dp)
                    .height(170.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(190.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = ColorShopMagenta,
                trackColor = Color(0xFFFFD6EA)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = ColorShopMutedPlum
            )
        }
    }
}
