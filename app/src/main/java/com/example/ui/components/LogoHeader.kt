package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary

// The official ElectroWorld base64-encoded logo image uploaded by the user
const val LOGO_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAI8AAAB5CAYAAAAAqHKXAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAAVPSURBVHhe7dRbjiM3DEbh2Wa2l2wp+0mQzLQx/bls100iJfEA56GNEvmTEvrHj8X4+68//jmjdYrJ8QHcrf2KgfFye2ueIjleYBbNWSTBi8qu+YsAvJTRdJ6iA17C6Dpf0QgXP5POmgEzZs35FgeYVeeOxGxZc77E0KvoHnpjHvX7dBh4Nd1HL8yxpWdSYdhVdS+tsf+WnkmDQVfVvfTADK/0XAoMubLupiX2fqdnU2DI1XU/rbDvJz0fjgFX1b20xv6f9Hw4BlxN99ESex/RWuEYcBXdQ0vs/fDPjd/e/G7dUAw3u87fEns/fPEwPmn9UAw3o87cGvs/uffhbHxnrzAMNpPO2hr7t9CeoRhudJ2vNfbf7cZ/lCc3vrF/GAYbUWfqgRl6aY4wDDaSztIa+9/uxn8YfzNTGE9BB9AZWmP/aM0XhsEyauYemCGL5gzDYJk0aw/MEK35UmHYaM3XAzNEa76UGDpKc/XADNGaLzWG7615WmP/LJpzCByip2ZphX0zaub0OEBPzXI39htBZ0iN4Xtqljuwx2g6T1oM3lOzXMHao+t8KTF0T81yFOvNpLOmxNC9NMderDOrzp0OA/fSHO/w7Eq6i1QYtpfmEL9fVfeSCsP20Axf+F35elfhGLSXGTL09veZj87t2RQYsodRfSN031/43Sc9nwJDltd0v1t4Zo/WSIEhy+O60094fq/WCcVw5X7d5V6sc0RrhWK48rXu7gzWPKr1QjFc+V33dQVrn9GaoRhudd3PndjrjNYMxXAr6k5aYM+zWjcMg62ie2iN/a9q/RAMNbPO3gtz3KE9QjDUTDprT8xyt/YLwVCj63y9MEdr7R+CoUbTeXpilp6aJQRDjaAz9MQsUZorBENl1dw9MUsGzRiCobJozt6YJ5vmDcFQkZqtN+bJrNlDMFRPzRKBmUbROUIwVGvtH4GZRtSZQjDU3dovCnONrvOFYKg7tEcU5ppJZw3BUFe0dgRmmlXnDsNgZ7RmT8wyu84fiuHOaM3W2H8l3UUohjuq9Vpgz5V1N6EY7ojWuhN7lT91T6EY7ojWuor1y2fdWTgG3KM1zmLd8r3uLxwDftLzR7HeSEbO8H2LSTDkJz2/B2uMZJZZzJECQ77Ts+/w7Eg6y+/4bS/NkQaDbumZLTwzks6yhWd6aY5UGHZLz3zhdyPpLO/wbE/NkgrD6tHvM+sse7BGb82TDgMb3N9H8vukx7FeT82SEkOPrvOdxbq9NU9KDD2iznQV60doprQYfASd4U7s1VvzpMbwWTV3C+wZoZnS4wBZNGdL7B2hmYbAISI1Ww/MEKW5hsFBemqW1tjbPBGacSgcprX2b4m9M2rm4XCgu7VfS+ydWbMPi4Nd1fotsfcIOsPQONwZrdkSe4+m8wyPA+7RGi2x96g61xQ45Cs91xJ7j67zTYODRg1t75l01qmIGtQlz6gzFydxsbPr/MVBXOgquodiJy5yNd1H8QEXuKrupXiBi1td91OACyt/6p6KX7io8rvua2lczpN/7vxtAd3dkriUVR/DEd3hUriMSw/mytnBdI/L4CL+9+rFXz0/kO5zelzAw6uXfvX8QLrTqXH4hwtd+F262ylx6IctHkyLmsl0v1PhsE/eecE7a+3KlVz3PB0O/M2dF/3RnXXM9oXfZdf80+Lgey96lztqmecTns+iOafHBVxyx0P5TzNcwdq9Nc8yuIiHPgL/PqG9W2DPVtp3SVzKndorErPt1TrFL1zUHdqjmBAv/YrWLibHB3BEaxWL4YP4pOeLhfFxqN8XxQMfSz2YYjf1aIqiKIqiKIriNf8CMwll/1zjlWgAAAAASUVORK5CYII="

@Composable
fun ElectroWorldLogo(
  modifier: Modifier = Modifier,
  showText: Boolean = true,
  iconSize: Float = 60f
) {
  val logoBitmap = remember {
    try {
      val decodedBytes = Base64.decode(LOGO_BASE64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
    } catch (e: Exception) {
      null
    }
  }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    if (logoBitmap != null) {
      Image(
        bitmap = logoBitmap,
        contentDescription = "Electro World Logo",
        modifier = Modifier
          .size(iconSize.dp)
          .padding(4.dp)
      )
    } else {
      // Custom Canvas to draw the Electro World Logo perfectly matching the official design
      Canvas(
        modifier = Modifier
          .size(iconSize.dp)
          .padding(4.dp)
          .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
      ) {
      val width = size.width
      val height = size.height
      val center = Offset(width / 2f, height / 2f)
      val radius = width * 0.35f

      // Slant angle (rising left-to-right, around -21 degrees)
      val angleDegrees = -21f
      val angleRad = angleDegrees * (Math.PI.toFloat() / 180f)
      val dx = kotlin.math.cos(angleRad)
      val dy = kotlin.math.sin(angleRad)

      // Perpendicular vector (normal) pointing up-left
      val nx = -dy
      val ny = dx

      // Golden gradient
      val goldBrush = Brush.linearGradient(
        colors = listOf(GoldLight, GoldPrimary, GoldDark),
        start = Offset(width * 0.1f, height * 0.9f),
        end = Offset(width * 0.9f, height * 0.1f)
      )

      // 1. Draw main circle
      drawCircle(
        brush = goldBrush,
        radius = radius,
        center = center
      )

      // 2. Draw the diagonal bar that extends to form the top-right and bottom-left wings
      val barLength = radius * 1.55f
      val barWidth = radius * 0.38f

      // Define the four corners of the slanted bar
      val p1 = center - Offset(dx * barLength, dy * barLength)
      val p2 = center + Offset(dx * barLength, dy * barLength)

      val barPath = Path().apply {
        // Bottom-left end cut with a steeper vertical-ish slant for sleekness
        val bl1 = p1 - Offset(nx * barWidth * 0.5f, ny * barWidth * 0.5f)
        val bl2 = p1 + Offset(nx * barWidth * 0.5f, ny * barWidth * 0.5f)

        // Top-right end cut
        val tr1 = p2 + Offset(nx * barWidth * 0.5f, ny * barWidth * 0.5f)
        val tr2 = p2 - Offset(nx * barWidth * 0.5f, ny * barWidth * 0.5f)

        moveTo(bl1.x, bl1.y)
        lineTo(bl2.x, bl2.y)
        lineTo(tr1.x, tr1.y)
        lineTo(tr2.x, tr2.y)
        close()
      }

      drawPath(
        path = barPath,
        brush = goldBrush
      )

      // 3. Draw the two diagonal parallel cutouts using BlendMode.Clear to make them transparent
      val slashWidth = radius * 0.16f
      val offsetDist = radius * 0.28f
      val slashLength = width * 2f

      // Upper slash
      val c1Center = center + Offset(nx * offsetDist, ny * offsetDist)
      val c1Start = c1Center - Offset(dx * slashLength, dy * slashLength)
      val c1End = c1Center + Offset(dx * slashLength, dy * slashLength)

      drawLine(
        color = Color.Transparent,
        start = c1Start,
        end = c1End,
        strokeWidth = slashWidth,
        blendMode = BlendMode.Clear
      )

      // Lower slash
      val c2Center = center - Offset(nx * offsetDist, ny * offsetDist)
      val c2Start = c2Center - Offset(dx * slashLength, dy * slashLength)
      val c2End = c2Center + Offset(dx * slashLength, dy * slashLength)

      drawLine(
        color = Color.Transparent,
        start = c2Start,
        end = c2End,
        strokeWidth = slashWidth,
        blendMode = BlendMode.Clear
      )
    }
  }

    if (showText) {
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text(
          text = "Electro",
          color = GoldSecondary,
          fontSize = (iconSize * 0.4f).sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.sp,
          lineHeight = (iconSize * 0.42f).sp
        )
        Text(
          text = "WORLD",
          color = GoldLight,
          fontSize = (iconSize * 0.18f).sp,
          fontWeight = FontWeight.Light,
          letterSpacing = 5.sp,
          lineHeight = (iconSize * 0.2f).sp,
          modifier = Modifier.padding(start = 2.dp)
        )
      }
    }
  }
}
