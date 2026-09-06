package com.example.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.ElectroWorldLogo
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground

/**
 * First screen shown to a signed-out user: a full-bleed photo with a rounded
 * panel resting over its bottom portion. Tapping "Get Started" slides that
 * panel up to cover the whole screen, and its content cross-fades from the
 * welcome copy into the real [LoginScreen] -- one continuous surface rather
 * than a screen swap.
 */
@Composable
fun OnboardingScreen(
  onLoginSuccess: (name: String, phone: String, token: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var revealed by remember { mutableStateOf(false) }

  BoxWithConstraints(modifier = modifier.fillMaxSize().background(SlateBackground)) {
    val collapsedOffset = maxHeight * 0.52f
    val panelOffset by animateDpAsState(
      targetValue = if (revealed) 0.dp else collapsedOffset,
      animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
      label = "OnboardingPanelOffset"
    )

    Image(
      painter = painterResource(id = R.drawable.onboarding_building),
      contentDescription = "Electro World showroom",
      contentScale = ContentScale.FillWidth,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .offset(y = panelOffset)
        .background(SlateBackground, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
      Crossfade(targetState = revealed, label = "OnboardingContentCrossfade") { isRevealed ->
        if (isRevealed) {
          LoginScreen(onLoginSuccess = onLoginSuccess, modifier = Modifier.fillMaxSize())
        } else {
          WelcomeContent(onGetStarted = { revealed = true })
        }
      }
    }
  }
}

@Composable
private fun WelcomeContent(onGetStarted: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 28.dp)
      .padding(top = 36.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    ElectroWorldLogo(iconSize = 56f, modifier = Modifier.padding(bottom = 20.dp))

    Text(
      text = "Welcome to Electro World",
      color = OnSlateText,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 12.dp)
    )

    Text(
      text = "Your trusted destination for premium smart electronics. Sign in to manage your purchases, complaints, and installations.",
      color = OnSlateTextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      lineHeight = 18.sp,
      modifier = Modifier.padding(bottom = 32.dp)
    )

    Button(
      onClick = onGetStarted,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp),
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = GoldDark),
      shape = RoundedCornerShape(12.dp)
    ) {
      Text(
        text = "GET STARTED",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = SlateBackground,
        letterSpacing = 1.sp
      )
    }
  }
}
