package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.CreateUserRequest
import com.example.data.network.NetworkModule
import com.example.ui.components.ElectroWorldLogo
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.OnSlateText
import com.example.ui.theme.OnSlateTextSecondary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
  onLoginSuccess: (name: String, phone: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var isOtpState by remember { mutableStateOf(false) }
  var fullName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var otpCode by remember { mutableStateOf("") }
  var isVerifying by remember { mutableStateOf(false) }
  var verificationSuccess by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var countdownTimer by remember { mutableStateOf(30) }

  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  // Countdown timer logic for Resend OTP
  LaunchedEffect(isOtpState, countdownTimer) {
    if (isOtpState && countdownTimer > 0) {
      delay(1000)
      countdownTimer -= 1
    }
  }

  fun submitNameAndPhone() {
    focusManager.clearFocus()
    if (fullName.isBlank()) {
      errorMessage = "Please enter your full name."
      return
    }
    if (phoneNumber.length < 10) {
      errorMessage = "Please enter a valid 10-digit mobile number."
      return
    }
    isVerifying = true
    coroutineScope.launch {
      try {
        NetworkModule.userApi.createOrUpdateUser(
          CreateUserRequest(name = fullName.trim(), phone = phoneNumber)
        )
        isVerifying = false
        isOtpState = true
        countdownTimer = 30
        otpCode = ""
      } catch (e: Exception) {
        isVerifying = false
        errorMessage = "Unable to reach the server. Please check your connection and try again."
      }
    }
  }

  fun submitOtp() {
    focusManager.clearFocus()
    if (otpCode.length < 6) {
      errorMessage = "OTP must be exactly 6 digits."
      return
    }
    isVerifying = true
    coroutineScope.launch {
      delay(600)
      isVerifying = false
      if (otpCode == "123456") {
        verificationSuccess = true
        delay(500)
        onLoginSuccess(fullName.trim(), phoneNumber)
      } else {
        errorMessage = "Invalid verification code. Please enter 123456."
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SlateBackground)
      .padding(24.dp)
  ) {
    // Top-right indicator
    Text(
      text = "SECURE AUTH",
      color = GoldPrimary.copy(alpha = 0.5f),
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 2.sp,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 16.dp)
    )

    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // 1. Brand Logo Header
      ElectroWorldLogo(
        modifier = Modifier.padding(bottom = 8.dp),
        iconSize = 72f
      )

      Text(
        text = "PREMIUM SMART ELECTRONICS RETAIL",
        color = OnSlateTextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 32.dp)
      )

      // 2. Animated Switching Panel (Phone Entry vs OTP Verification)
      AnimatedContent(
        targetState = isOtpState,
        transitionSpec = {
          if (targetState) {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
          } else {
            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() with
                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
          }
        },
        label = "LoginScreensTransition"
      ) { showOtp ->
        if (!showOtp) {
          // --- PHONE INPUT VIEW ---
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("phone_input_container"),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Sign In / Sign Up",
              color = OnSlateText,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
              text = "Enter your mobile phone number to connect with your appliances and track store invoices.",
              color = OnSlateTextSecondary,
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
              lineHeight = 18.sp
            )

            // Full Name field
            OutlinedTextField(
              value = fullName,
              onValueChange = { input ->
                fullName = input
                errorMessage = null
              },
              placeholder = { Text("Full Name", color = OnSlateTextSecondary) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = "Name Icon",
                  tint = GoldPrimary
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("full_name_input"),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = SlateSurfaceVariant,
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedTextColor = OnSlateText,
                unfocusedTextColor = OnSlateText
              ),
              shape = RoundedCornerShape(12.dp)
            )

            // Country Code + Phone field row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Standard country selection dummy
              Box(
                modifier = Modifier
                  .height(56.dp)
                  .background(SlateSurface, RoundedCornerShape(12.dp))
                  .border(1.dp, SlateSurfaceVariant, RoundedCornerShape(12.dp))
                  .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "🇮🇳 +91",
                  color = OnSlateText,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              // Phone text input
              OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input ->
                  // Allow only numbers, max 10 digits
                  if (input.all { it.isDigit() } && input.length <= 10) {
                    phoneNumber = input
                    errorMessage = null
                  }
                },
                placeholder = { Text("Mobile Phone Number", color = OnSlateTextSecondary) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Phone Icon",
                    tint = GoldPrimary
                  )
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("phone_number_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = GoldPrimary,
                  unfocusedBorderColor = SlateSurfaceVariant,
                  focusedContainerColor = SlateSurface,
                  unfocusedContainerColor = SlateSurface,
                  focusedTextColor = OnSlateText,
                  unfocusedTextColor = OnSlateText
                ),
                shape = RoundedCornerShape(12.dp)
              )
            }

            AnimatedVisibility(visible = errorMessage != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = "Error icon",
                  tint = AlertRed,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = errorMessage ?: "",
                  color = AlertRed,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            // Big CTA Button to send OTP
            Button(
              onClick = { submitNameAndPhone() },
              enabled = !isVerifying,
              modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("send_otp_button"),
              colors = ButtonDefaults.buttonColors(
                containerColor = GoldPrimary,
                disabledContainerColor = GoldDark
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              if (isVerifying) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = SlateBackground,
                  strokeWidth = 2.5.dp
                )
              } else {
                Text(
                  text = "SEND ONE-TIME PASSWORD",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateBackground,
                  letterSpacing = 1.sp
                )
              }
            }

            // Trust Credentials Floating Info Card
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
              colors = CardDefaults.cardColors(
                containerColor = SlateSurface.copy(alpha = 0.5f)
              ),
              shape = RoundedCornerShape(12.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.15f))
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                Text(
                  text = "⚡ QUICK DEMO MODE ACTIVE",
                  color = GoldSecondary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp,
                  modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                  text = "Your name and number are saved to our system. SMS delivery isn't wired up yet -- use OTP code '123456' to continue.",
                  color = OnSlateTextSecondary,
                  fontSize = 11.sp,
                  lineHeight = 15.sp
                )
              }
            }
          }
        } else {
          // --- OTP VERIFICATION VIEW ---
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("otp_input_container"),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = { isOtpState = false },
                modifier = Modifier.background(SlateSurface, CircleShape)
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowBack,
                  contentDescription = "Back",
                  tint = GoldSecondary
                )
              }
              Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Enter Verification Code",
              color = OnSlateText,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
              text = "We have dispatched a 6-digit verification code to +91 $phoneNumber. Check your messages.",
              color = OnSlateTextSecondary,
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
              lineHeight = 18.sp
            )

            // OTP Input Field
            OutlinedTextField(
              value = otpCode,
              onValueChange = { input ->
                if (input.all { it.isDigit() } && input.length <= 6) {
                  otpCode = input
                  errorMessage = null
                  // Auto verify if they typed 6 digits
                  if (input.length == 6) {
                    submitOtp()
                  }
                }
              },
              placeholder = {
                Text(
                  text = "● ● ● ● ● ●",
                  color = OnSlateTextSecondary.copy(alpha = 0.4f),
                  textAlign = TextAlign.Center,
                  letterSpacing = 4.sp
                )
              },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Secure lock icon",
                  tint = GoldPrimary
                )
              },
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .testTag("otp_code_input"),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldPrimary,
                unfocusedBorderColor = SlateSurfaceVariant,
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedTextColor = GoldSecondary,
                unfocusedTextColor = OnSlateText,
                focusedLabelColor = GoldSecondary
              ),
              shape = RoundedCornerShape(12.dp),
              textStyle = androidx.compose.ui.text.TextStyle(
                textAlign = TextAlign.Center,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 8.sp
              )
            )

            AnimatedVisibility(visible = errorMessage != null) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = "Error icon",
                  tint = AlertRed,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = errorMessage ?: "",
                  color = AlertRed,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            // Verification Processing State / Verify Button
            if (isVerifying) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
              ) {
                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "Verifying security credentials...",
                  color = GoldSecondary,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            } else if (verificationSuccess) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Verified success",
                  tint = SuccessGreen,
                  modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Verification Successful!",
                  color = SuccessGreen,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            } else {
              Button(
                onClick = { submitOtp() },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(54.dp)
                  .testTag("verify_otp_button"),
                colors = ButtonDefaults.buttonColors(
                  containerColor = GoldPrimary
                ),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(
                  text = "VERIFY AND LOGIN",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = SlateBackground,
                  letterSpacing = 1.sp
                )
              }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Resend text / Timer row
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = "Didn't receive the SMS? ",
                color = OnSlateTextSecondary,
                fontSize = 13.sp
              )
              if (countdownTimer > 0) {
                Text(
                  text = "Resend in ${countdownTimer}s",
                  color = GoldPrimary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold
                )
              } else {
                TextButton(
                  onClick = {
                    countdownTimer = 30
                    otpCode = ""
                    errorMessage = null
                  }
                ) {
                  Text(
                    text = "RESEND OTP CODE",
                    color = GoldSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }

    // Secure protocol footer
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Shield SECURE",
        tint = OnSlateTextSecondary.copy(alpha = 0.5f),
        modifier = Modifier.size(12.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Text(
        text = "Electro World Shield • End-To-End Encrypted OTP Verification",
        color = OnSlateTextSecondary.copy(alpha = 0.5f),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
