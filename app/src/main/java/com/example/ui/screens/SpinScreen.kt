package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.network.NetworkModule
import com.example.data.network.SpinPrize
import com.example.data.network.SpinRequest
import com.example.data.network.SpinStateResponse
import com.example.data.network.SpinWin
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.ObsidianCharcoal
import com.example.ui.theme.PaleGold
import com.example.ui.theme.PlatinumMuted
import com.example.ui.theme.VividAmber
import com.example.ui.theme.appCardColors
import com.example.ui.theme.contrastTextColor
import kotlin.math.floor
import kotlin.random.Random
import kotlinx.coroutines.launch
import retrofit2.HttpException

// The wheel never shows what the prizes are -- every slice is a mystery
// "?" -- so the slice count is purely cosmetic.
private const val SEGMENT_COUNT = 8
private const val MYSTERY_LABEL = "?"

private const val SPIN_DURATION_MS = 4200
private const val SPIN_FULL_TURNS = 6

// Same ease-out curve the old web spinner used for its CSS transition.
private val SpinEasing = CubicBezierEasing(0.17f, 0.67f, 0.12f, 0.99f)

// Raw palette colors rather than theme roles: the wheel is a fixed piece of
// brand artwork. Deep Slate (not Obsidian) for the dark slices, so they
// don't vanish into the page background behind the wheel.
private val WheelColors = listOf(VividAmber, DeepSlate, PaleGold, PlatinumMuted)

/**
 * Spin to Win: a spin is unlocked by staff when the customer makes a
 * purchase, and the prize is decided by the server (POST /api/spin). Prize
 * names are kept secret until then: the wheel is all mystery slices, its
 * spin is pure theatre, and the name only appears in the "You Won" dialog
 * (and afterwards under My Prizes).
 */
@Composable
fun SpinScreen(
  userPhone: String,
  sessionToken: String,
  onBack: () -> Unit,
  onGetSpinAccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var spinState by remember { mutableStateOf<SpinStateResponse?>(null) }
  var isLoading by remember { mutableStateOf(true) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var isSpinning by remember { mutableStateOf(false) }
  var wonPrize by remember { mutableStateOf<SpinPrize?>(null) }
  var wonForItem by remember { mutableStateOf("") }
  val rotation = remember { Animatable(0f) }

  LaunchedEffect(userPhone) {
    try {
      val fresh = NetworkModule.userApi.getSpinState(userPhone)
      spinState = fresh
      errorMessage = null
    } catch (e: Exception) {
      errorMessage = "Unable to load Spin to Win. Please check your connection and try again."
    } finally {
      isLoading = false
    }
  }

  fun spin() {
    val current = spinState ?: return
    if (isSpinning || current.availableSpins <= 0) return
    isSpinning = true
    coroutineScope.launch {
      val result = try {
        NetworkModule.userApi.spin("Bearer $sessionToken", SpinRequest(phone = userPhone))
      } catch (e: Exception) {
        val message = when ((e as? HttpException)?.code()) {
          401 -> "Your session has expired. Please sign out and sign in again to spin."
          403 -> "No spin available. Make a purchase to unlock one."
          409 -> "Prizes are out of stock right now. Please ask our staff."
          else -> "Unable to spin. Please check your connection and try again."
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        isSpinning = false
        return@launch
      }

      // Every slice is the same mystery "?", so where the wheel stops
      // means nothing -- it just stops somewhere random, away from the
      // slice edges so the pointer never rests on a dividing line.
      val sweep = 360f / SEGMENT_COUNT
      val index = Random.nextInt(SEGMENT_COUNT)
      val jitter = (Random.nextFloat() - 0.5f) * sweep * 0.6f
      val base = (floor(rotation.value / 360f) + SPIN_FULL_TURNS) * 360f
      rotation.animateTo(
        targetValue = base + 360f - (index + 0.5f) * sweep + jitter,
        animationSpec = tween(durationMillis = SPIN_DURATION_MS, easing = SpinEasing)
      )

      wonForItem = result.boughtItem
      wonPrize = result.prize
      spinState = current.copy(availableSpins = result.availableSpins)
      isSpinning = false
      try {
        spinState = NetworkModule.userApi.getSpinState(userPhone)
      } catch (e: Exception) {
        // The win is already recorded server-side; My Prizes just catches
        // up next time the screen is opened.
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(MaterialTheme.colorScheme.surface)
          .padding(top = 40.dp, bottom = 16.dp, start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onBack) {
          Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.secondary)
        }
        Text(
          text = "Spin to Win",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 12.dp)
        )
      }
    }
  ) { paddingValues ->
    val state = spinState
    when {
      isLoading -> {
        Box(
          modifier = Modifier.fillMaxSize().padding(paddingValues),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
      }
      state == null -> {
        Column(
          modifier = Modifier.fillMaxSize().padding(paddingValues).padding(vertical = 60.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Icon(Icons.Default.Warning, "Error", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.height(40.dp))
          Text(
            text = errorMessage ?: "",
            color = MaterialTheme.colorScheme.outline,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp)
          )
        }
      }
      else -> {
        val isUnlocked = state.availableSpins > 0
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          SpinStatusBanner(availableSpins = state.availableSpins)

          Spacer(modifier = Modifier.height(24.dp))

          SpinWheel(
            rotationDegrees = rotation.value,
            modifier = Modifier
              .fillMaxWidth()
              .widthIn(max = 340.dp)
              .aspectRatio(1f)
          )

          Spacer(modifier = Modifier.height(24.dp))

          if (!isUnlocked) {
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onGetSpinAccess() },
              colors = appCardColors(),
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
              Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = "GET SPIN ACCESS (via purchase)",
                  color = MaterialTheme.colorScheme.onBackground,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Visit Electroworld to shop in-store",
                  color = MaterialTheme.colorScheme.outline,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(top = 2.dp)
                )
              }
            }
            Spacer(modifier = Modifier.height(12.dp))
          }

          SpinButton(isUnlocked = isUnlocked, isSpinning = isSpinning, onClick = { spin() })

          if (state.wins.isNotEmpty()) {
            Text(
              text = "MY PRIZES",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.sp,
              modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              state.wins.forEach { win -> SpinWinRow(win = win) }
            }
          }
        }
      }
    }
  }

  wonPrize?.let { prize ->
    AlertDialog(
      onDismissRequest = { wonPrize = null },
      containerColor = MaterialTheme.colorScheme.surface,
      // Everything centred, with extra room above the title and around the
      // prize so the popup doesn't feel cramped.
      title = {
        Text(
          text = "You Won!",
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)
        )
      },
      text = {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
          Text(
            text = prize.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
          )
          Text(
            text = "Free with your $wonForItem. Show this at the Electroworld counter to collect it.",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
          )
          Text(
            text = "Saved under My Prizes.",
            color = MaterialTheme.colorScheme.outline,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
          )
        }
      },
      confirmButton = {
        // The button slot is end-aligned by default; filling the width and
        // centring the button lines it up with the text above.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          TextButton(onClick = { wonPrize = null }) {
            Text("Done", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
          }
        }
      }
    )
  }
}

@Composable
private fun SpinStatusBanner(availableSpins: Int) {
  // primary with onPrimary text -- Vivid Amber/Obsidian, the same pairing
  // PointsScreen's balance card relies on.
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      imageVector = if (availableSpins > 0) Icons.Default.Star else Icons.Default.Lock,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier.size(22.dp)
    )
    Text(
      text = when {
        availableSpins > 1 -> "YOU HAVE $availableSpins SPINS READY. GOOD LUCK!"
        availableSpins == 1 -> "YOUR SPIN IS UNLOCKED. GOOD LUCK!"
        else -> "TO UNLOCK YOUR SPIN, MAKE A PURCHASE FROM ELECTROWORLD."
      },
      color = MaterialTheme.colorScheme.onPrimary,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(start = 12.dp)
    )
  }
}

@Composable
private fun SpinButton(isUnlocked: Boolean, isSpinning: Boolean, onClick: () -> Unit) {
  val enabled = isUnlocked && !isSpinning
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        RoundedCornerShape(14.dp)
      )
      .clickable(enabled = enabled) { onClick() }
      .padding(vertical = 14.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = if (isSpinning) "SPINNING..." else "PRESS TO SPIN",
      color = if (isUnlocked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
      fontSize = 16.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = 1.sp
    )
    if (!isUnlocked) {
      Text(text = "Unlock first", color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
    }
  }
}

// Hub and logo sizes, as fractions of the wheel: the hub is a disc of
// HUB_RADIUS x the wheel's radius, and the EW logo is fitted into a square
// LOGO_SIZE x the wheel's full width -- small enough that the logo's corners
// stay inside the hub.
private const val HUB_RADIUS = 0.28f
private const val LOGO_SIZE = 0.19f

/**
 * The wheel itself: SEGMENT_COUNT identical mystery slices. The pointer is
 * fixed at 12 o'clock and only the disc turns; the EW logo sits on the hub
 * and stays upright while the disc spins under it.
 */
@Composable
private fun SpinWheel(rotationDegrees: Float, modifier: Modifier = Modifier) {
  val textMeasurer = rememberTextMeasurer()
  val pointerColor = MaterialTheme.colorScheme.onBackground

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer { rotationZ = rotationDegrees }
    ) {
      val rim = size.minDimension * 0.035f
      val radius = size.minDimension / 2f - rim
      val topLeft = Offset(center.x - radius, center.y - radius)
      val sweep = 360f / SEGMENT_COUNT

      repeat(SEGMENT_COUNT) { index ->
        // SEGMENT_COUNT is a multiple of WheelColors.size, so the last
        // slice never lands next to slice 0 in the same color.
        val color = WheelColors[index % WheelColors.size]
        val startAngle = -90f + index * sweep
        drawArc(color = color, startAngle = startAngle, sweepAngle = sweep, useCenter = true, topLeft = topLeft, size = Size(radius * 2, radius * 2))

        val label = textMeasurer.measure(
          text = MYSTERY_LABEL,
          style = TextStyle(
            color = contrastTextColor(color),
            fontSize = (radius * 0.24f).toSp(),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
          )
        )
        // Centred on the slice's middle line, about two-thirds of the way
        // out, and turned so its top points at the rim.
        rotate(degrees = startAngle + sweep / 2f + 90f, pivot = center) {
          drawText(
            textLayoutResult = label,
            topLeft = Offset(center.x - label.size.width / 2f, center.y - radius * 0.66f - label.size.height / 2f)
          )
        }
      }

      drawCircle(color = VividAmber, radius = radius + rim / 2f, style = Stroke(width = rim))
      // Hub: a dark disc with a gold ring, the backing for the logo below.
      drawCircle(color = ObsidianCharcoal, radius = radius * HUB_RADIUS)
      drawCircle(color = VividAmber, radius = radius * HUB_RADIUS, style = Stroke(width = rim * 0.5f))
    }

    Image(
      painter = painterResource(id = R.drawable.ew_logo_amber),
      contentDescription = null,
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxSize(LOGO_SIZE)
    )

    // Pointer: fixed, drawn over the rim at 12 o'clock.
    Canvas(modifier = Modifier.fillMaxSize()) {
      val pointerWidth = size.minDimension * 0.08f
      val pointerHeight = size.minDimension * 0.11f
      val path = Path().apply {
        moveTo(center.x - pointerWidth / 2f, 0f)
        lineTo(center.x + pointerWidth / 2f, 0f)
        lineTo(center.x, pointerHeight)
        close()
      }
      drawPath(path = path, color = pointerColor)
      drawPath(path = path, color = VividAmber, style = Stroke(width = size.minDimension * 0.008f))
    }
  }
}

// Server timestamps are full ISO-8601; rows only need the date, as
// DD-MM-YYYY -- same as PointsScreen's formatPointsDate (duplicated since
// that one is file-private).
private fun formatSpinDate(isoTimestamp: String): String {
  val datePart = isoTimestamp.substringBefore('T')
  val parts = datePart.split("-")
  return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else isoTimestamp
}

@Composable
private fun SpinWinRow(win: SpinWin) {
  Card(
    colors = appCardColors(),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape)
          .padding(8.dp)
      ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.height(16.dp))
      }
      Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
        Text(
          text = win.prizeName ?: "Prize",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = "With ${win.boughtItem}",
          color = MaterialTheme.colorScheme.outline,
          fontSize = 11.sp,
          modifier = Modifier.padding(top = 2.dp)
        )
      }
      Text(text = formatSpinDate(win.wonAt), color = MaterialTheme.colorScheme.outline, fontSize = 11.sp)
    }
  }
}
