package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant

private const val PIN_LENGTH = 4

/**
 * A 4-box PIN input. A single invisible [BasicTextField] captures the digits
 * (so the system numeric keypad works normally); the boxes below just render
 * whatever has been typed so far.
 */
@Composable
fun PinEntryField(
  pin: String,
  onPinChange: (String) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String = "pin_input"
) {
  Box(modifier = modifier.fillMaxWidth()) {
    BasicTextField(
      value = pin,
      onValueChange = { input ->
        if (input.all { it.isDigit() } && input.length <= PIN_LENGTH) {
          onPinChange(input)
        }
      },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
      textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent),
      cursorBrush = androidx.compose.ui.graphics.SolidColor(androidx.compose.ui.graphics.Color.Transparent),
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .testTag(testTag)
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      repeat(PIN_LENGTH) { index ->
        val filled = index < pin.length
        Box(
          modifier = Modifier
            .size(56.dp)
            .background(SlateSurface, RoundedCornerShape(12.dp))
            .border(
              width = 1.dp,
              color = if (filled) GoldPrimary else SlateSurfaceVariant,
              shape = RoundedCornerShape(12.dp)
            ),
          contentAlignment = Alignment.Center
        ) {
          if (filled) {
            Text(
              text = "●",
              color = OnSlateText,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold,
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }
  }
}
