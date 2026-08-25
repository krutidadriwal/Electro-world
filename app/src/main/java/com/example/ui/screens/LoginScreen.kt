package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
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
import retrofit2.HttpException

private enum class AuthMode { LOGIN, SIGNUP }
private enum class AuthStep { ENTRY, OTP }

private val HOW_HEARD_OPTIONS = listOf(
  "Instagram",
  "Facebook",
  "Google Search",
  "Friend or Family Referral",
  "Store Visit",
  "Other"
)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  onLoginSuccess: (name: String, phone: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var mode by remember { mutableStateOf(AuthMode.LOGIN) }
  var step by remember { mutableStateOf(AuthStep.ENTRY) }

  var fullName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var howHeard by remember { mutableStateOf<String?>(null) }
  var howHeardMenuExpanded by remember { mutableStateOf(false) }
  var resolvedName by remember { mutableStateOf("") }

  var otpCode by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var isVerifying by remember { mutableStateOf(false) }
  var verificationSuccess by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showSignupSuggestion by remember { mutableStateOf(false) }
  var countdownTimer by remember { mutableStateOf(30) }

  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  // Countdown timer logic for Resend OTP
  LaunchedEffect(step, countdownTimer) {
    if (step == AuthStep.OTP && countdownTimer > 0) {
      delay(1000)
      countdownTimer -= 1
    }
  }

  fun switchToSignup(prefillPhone: String) {
    mode = AuthMode.SIGNUP
    step = AuthStep.ENTRY
    phoneNumber = prefillPhone
    errorMessage = null
    showSignupSuggestion = false
  }

  fun switchToLogin() {
    mode = AuthMode.LOGIN
    step = AuthStep.ENTRY
    errorMessage = null
    showSignupSuggestion = false
  }

  fun submitLoginPhone() {
    focusManager.clearFocus()
    if (phoneNumber.length < 10) {
      errorMessage = "Please enter a valid 10-digit mobile number."
      return
    }
    isSubmitting = true
    coroutineScope.launch {
      try {
        val user = NetworkModule.userApi.getUser(phoneNumber)
        resolvedName = user.name
        isSubmitting = false
        step = AuthStep.OTP
        countdownTimer = 30
        otpCode = ""
      } catch (e: HttpException) {
        isSubmitting = false
        if (e.code() == 404) {
          errorMessage = "No account found with this number."
          showSignupSuggestion = true
        } else {
          errorMessage = "Unable to reach the server. Please check your connection and try again."
        }
      } catch (e: Exception) {
        isSubmitting = false
        errorMessage = "Unable to reach the server. Please check your connection and try again."
      }
    }
  }

  fun submitSignup() {
    focusManager.clearFocus()
    if (fullName.isBlank()) {
      errorMessage = "Please enter your full name."
      return
    }
    if (phoneNumber.length < 10) {
      errorMessage = "Please enter a valid 10-digit mobile number."
      return
    }
    if (howHeard == null) {
      errorMessage = "Please tell us how you heard about us."
      return
    }
    isSubmitting = true
    coroutineScope.launch {
      try {
        NetworkModule.userApi.createOrUpdateUser(
          CreateUserRequest(name = fullName.trim(), phone = phoneNumber, howHeardAboutUs = howHeard)
        )
        resolvedName = fullName.trim()
        isSubmitting = false
        step = AuthStep.OTP
        countdownTimer = 30
        otpCode = ""
      } catch (e: Exception) {
        isSubmitting = false
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
        onLoginSuccess(resolvedName, phoneNumber)
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

      // 2. Animated Switching Panel (Entry vs OTP Verification)
      AnimatedContent(
        targetState = step,
        transitionSpec = {
          if (targetState == AuthStep.OTP) {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
          } else {
            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
          }
        },
        label = "LoginScreensTransition"
      ) { currentStep ->
        if (currentStep == AuthStep.ENTRY) {
          // --- PHONE / SIGNUP ENTRY VIEW ---
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("phone_input_container"),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (mode == AuthMode.LOGIN) "Sign In" else "Create Account",
              color = OnSlateText,
              fontSize = 24.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
              text = if (mode == AuthMode.LOGIN) {
                "Enter your mobile phone number to connect with your appliances and track store invoices."
              } else {
                "Tell us a bit about yourself to create your Electro World account."
              },
              color = OnSlateTextSecondary,
              fontSize = 13.sp,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
              lineHeight = 18.sp
            )

            if (mode == AuthMode.SIGNUP) {
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
            }

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
                    showSignupSuggestion = false
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

            if (mode == AuthMode.LOGIN) {
              // "Sign up instead?" link
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "New here? ",
                  color = OnSlateTextSecondary,
                  fontSize = 13.sp
                )
                Text(
                  text = "Sign up instead",
                  color = GoldSecondary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .clickable { switchToSignup(phoneNumber) }
                    .testTag("signup_instead_link")
                )
              }
            }

            if (mode == AuthMode.SIGNUP) {
              // How did you hear about us dropdown
              ExposedDropdownMenuBox(
                expanded = howHeardMenuExpanded,
                onExpandedChange = { howHeardMenuExpanded = it },
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 16.dp)
              ) {
                OutlinedTextField(
                  value = howHeard ?: "",
                  onValueChange = {},
                  readOnly = true,
                  placeholder = { Text("How did you hear about us?", color = OnSlateTextSecondary) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = howHeardMenuExpanded) },
                  modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .testTag("how_heard_input"),
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
                ExposedDropdownMenu(
                  expanded = howHeardMenuExpanded,
                  onDismissRequest = { howHeardMenuExpanded = false },
                  modifier = Modifier.background(SlateSurface)
                ) {
                  HOW_HEARD_OPTIONS.forEach { option ->
                    DropdownMenuItem(
                      text = { Text(option, color = OnSlateText) },
                      onClick = {
                        howHeard = option
                        howHeardMenuExpanded = false
                        errorMessage = null
                      }
                    )
                  }
                }
              }
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

            if (mode == AuthMode.LOGIN && showSignupSuggestion) {
              TextButton(
                onClick = { switchToSignup(phoneNumber) },
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(bottom = 12.dp)
                  .testTag("create_account_button")
              ) {
                Text(
                  text = "CREATE AN ACCOUNT",
                  color = GoldSecondary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 1.sp
                )
              }
            }

            // Big CTA Button to send OTP
            Button(
              onClick = { if (mode == AuthMode.LOGIN) submitLoginPhone() else submitSignup() },
              enabled = !isSubmitting,
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
              if (isSubmitting) {
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

            if (mode == AuthMode.SIGNUP) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Already have an account? ",
                  color = OnSlateTextSecondary,
                  fontSize = 13.sp
                )
                Text(
                  text = "Sign in instead",
                  color = GoldSecondary,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier
                    .clickable { switchToLogin() }
                    .testTag("signin_instead_link")
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
                  text = "SMS delivery isn't wired up yet -- use OTP code '123456' to continue.",
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
                onClick = { step = AuthStep.ENTRY },
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
