package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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

private const val PANEL_ANIMATION_MS = 1000
private const val CONTENT_FADE_MS = 625
private val LOGO_REVEALED_OFFSET = 96.dp

/**
 * First screen shown to a signed-out user: a full-bleed photo with a rounded
 * panel resting over its bottom portion. Tapping "Get Started" slides that
 * panel up to cover the whole screen while the welcome copy fades away and
 * the real [LoginScreen] fades in -- the logo itself is a single persistent
 * element that glides from its resting spot up into the login header instead
 * of cross-fading away with the rest of the welcome content.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
  onLoginSuccess: (name: String, phone: String, token: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var revealed by remember { mutableStateOf(false) }
  val imeVisible = WindowInsets.isImeVisible

  // Back collapses the login panel back to the welcome screen rather than
  // exiting the app; on the welcome screen itself there's nowhere earlier
  // to go, so back is left to its default (system) behavior there.
  BackHandler(enabled = revealed) { revealed = false }

  BoxWithConstraints(modifier = modifier.fillMaxSize().background(SlateBackground)) {
    val collapsedOffset = maxHeight * 0.52f
    val panelOffset by animateDpAsState(
      targetValue = if (revealed) 0.dp else collapsedOffset,
      animationSpec = tween(durationMillis = PANEL_ANIMATION_MS, easing = FastOutSlowInEasing),
      label = "OnboardingPanelOffset"
    )
    val logoOffset by animateDpAsState(
      targetValue = if (revealed) LOGO_REVEALED_OFFSET else collapsedOffset + 36.dp,
      animationSpec = tween(durationMillis = PANEL_ANIMATION_MS, easing = FastOutSlowInEasing),
      label = "OnboardingLogoOffset"
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
      AnimatedVisibility(
        visible = !revealed,
        enter = fadeIn(tween(CONTENT_FADE_MS)),
        exit = fadeOut(tween(CONTENT_FADE_MS))
      ) {
        WelcomeCopy(onGetStarted = { revealed = true })
      }

      AnimatedVisibility(
        visible = revealed,
        enter = fadeIn(tween(CONTENT_FADE_MS, delayMillis = CONTENT_FADE_MS / 2)),
        exit = fadeOut(tween(CONTENT_FADE_MS))
      ) {
        LoginScreen(onLoginSuccess = onLoginSuccess, modifier = Modifier.fillMaxSize(), showLogo = false)
      }
    }

    // The logo lives outside the fading content so it can glide continuously
    // from its resting spot (over the welcome copy) up into the login
    // header's position, instead of disappearing with the rest of the copy.
    // Once revealed, it also fades out for a small corner mark whenever the
    // keyboard comes up, freeing vertical space for the login form.
    AnimatedVisibility(
      visible = !imeVisible,
      modifier = Modifier.fillMaxWidth().offset(y = logoOffset).align(Alignment.TopCenter),
      enter = fadeIn(tween(CONTENT_FADE_MS)),
      exit = fadeOut(tween(CONTENT_FADE_MS))
    ) {
      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        ElectroWorldLogo(iconSize = 64f)
      }
    }

    AnimatedVisibility(
      visible = revealed && imeVisible,
      modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(top = 12.dp, end = 24.dp),
      enter = fadeIn(tween(CONTENT_FADE_MS)),
      exit = fadeOut(tween(CONTENT_FADE_MS))
    ) {
      Image(
        painter = painterResource(id = R.drawable.ew_logo_small),
        contentDescription = "Electro World",
        modifier = Modifier.size(32.dp)
      )
    }
  }
}

@Composable
private fun WelcomeCopy(onGetStarted: () -> Unit) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 28.dp)
      .padding(top = 116.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Welcomes You",
      color = OnSlateText,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 12.dp)
    )

    Text(
      text = "Sign in to access your orders, schedule installations, or request support.",
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
