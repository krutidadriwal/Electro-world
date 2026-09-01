package com.example.ui.screens

import android.app.Activity
import android.content.ContextWrapper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.PhoneAuthManager
import com.example.data.network.LoginRequest
import com.example.data.network.NetworkModule
import com.example.data.network.SetPinRequest
import com.example.ui.components.ElectroWorldLogo
import com.example.ui.components.PinEntryField
import com.example.ui.theme.AlertRed
import com.example.ui.theme.GoldDark
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
private enum class AuthStep { ENTRY, OTP, SET_PIN, ENTER_PIN }

private val HOW_HEARD_OPTIONS = listOf(
  "Instagram",
  "Facebook",
  "Google Search",
  "Friend or Family Referral",
  "Store Visit",
  "Other"
)

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  onLoginSuccess: (name: String, phone: String, token: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var mode by remember { mutableStateOf(AuthMode.LOGIN) }
  var step by remember { mutableStateOf(AuthStep.ENTRY) }
  var isForgotPin by remember { mutableStateOf(false) }

  var fullName by remember { mutableStateOf("") }
  var phoneNumber by remember { mutableStateOf("") }
  var howHeard by remember { mutableStateOf<String?>(null) }
  var howHeardMenuExpanded by remember { mutableStateOf(false) }
  var resolvedName by remember { mutableStateOf("") }

  var otpCode by remember { mutableStateOf("") }
  var verificationId by remember { mutableStateOf<String?>(null) }
  var pendingIdToken by remember { mutableStateOf<String?>(null) }

  var loginPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }

  var isSubmitting by remember { mutableStateOf(false) }
  var isVerifying by remember { mutableStateOf(false) }
  var verificationSuccess by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showSignupSuggestion by remember { mutableStateOf(false) }
  var countdownTimer by remember { mutableStateOf(30) }

  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current
  val context = LocalContext.current
  val phoneAuthManager = remember { PhoneAuthManager() }

  // Countdown timer logic for Resend OTP
  LaunchedEffect(step, countdownTimer) {
    if (step == AuthStep.OTP && countdownTimer > 0) {
      delay(1000)
      countdownTimer -= 1
    }
  }

  fun resetToEntry() {
    step = AuthStep.ENTRY
    isForgotPin = false
    otpCode = ""
    loginPin = ""
    newPin = ""
    confirmPin = ""
    verificationId = null
    pendingIdToken = null
    errorMessage = null
    verificationSuccess = false
  }

  fun switchToSignup(prefillPhone: String) {
    mode = AuthMode.SIGNUP
    resetToEntry()
    phoneNumber = prefillPhone
    showSignupSuggestion = false
  }

  fun switchToLogin() {
    mode = AuthMode.LOGIN
    resetToEntry()
    showSignupSuggestion = false
  }

  fun proceedToSetPin(idToken: String) {
    pendingIdToken = idToken
    newPin = ""
    confirmPin = ""
    errorMessage = null
    verificationSuccess = false
    step = AuthStep.SET_PIN
  }

  fun sendOtp() {
    val activity = context.findActivity()
    if (activity == null) {
      errorMessage = "Unable to start verification. Please restart the app."
      return
    }
    isSubmitting = true
    phoneAuthManager.sendOtp("+91$phoneNumber", activity) { event ->
      when (event) {
        is PhoneAuthManager.OtpEvent.CodeSent -> {
          verificationId = event.verificationId
          isSubmitting = false
          otpCode = ""
          countdownTimer = 30
          errorMessage = null
          step = AuthStep.OTP
        }
        is PhoneAuthManager.OtpEvent.AutoVerified -> {
          isSubmitting = false
          proceedToSetPin(event.idToken)
        }
        is PhoneAuthManager.OtpEvent.Failed -> {
          isSubmitting = false
          errorMessage = event.message
        }
      }
    }
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
        loginPin = ""
        errorMessage = null
        step = AuthStep.ENTER_PIN
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
    isForgotPin = false
    sendOtp()
  }

  fun startForgotPin() {
    errorMessage = null
    isForgotPin = true
    sendOtp()
  }

  fun submitOtp() {
    focusManager.clearFocus()
    if (otpCode.length < 6) {
      errorMessage = "OTP must be exactly 6 digits."
      return
    }
    val vId = verificationId
    if (vId == null) {
      errorMessage = "Verification session expired. Please go back and resend the code."
      return
    }
    isVerifying = true
    coroutineScope.launch {
      try {
        val idToken = phoneAuthManager.verifyOtp(vId, otpCode)
        isVerifying = false
        verificationSuccess = true
        delay(400)
        proceedToSetPin(idToken)
      } catch (e: Exception) {
        isVerifying = false
        errorMessage = "Invalid verification code. Please try again."
      }
    }
  }

  fun submitSetPin() {
    focusManager.clearFocus()
    if (newPin.length != 4) {
      errorMessage = "PIN must be 4 digits."
      return
    }
    if (newPin != confirmPin) {
      errorMessage = "PINs do not match."
      return
    }
    val idToken = pendingIdToken
    if (idToken == null) {
      errorMessage = "Verification session expired. Please start again."
      return
    }
    isSubmitting = true
    coroutineScope.launch {
      try {
        val response = NetworkModule.authApi.setPin(
          SetPinRequest(
            idToken = idToken,
            pin = newPin,
            name = if (mode == AuthMode.SIGNUP) fullName.trim() else null,
            howHeardAboutUs = if (mode == AuthMode.SIGNUP) howHeard else null
          )
        )
        isSubmitting = false
        onLoginSuccess(response.name, response.phone, response.token)
      } catch (e: Exception) {
        isSubmitting = false
        errorMessage = "Unable to save PIN. Please try again."
      }
    }
  }

  fun submitLoginPin() {
    focusManager.clearFocus()
    if (loginPin.length != 4) {
      errorMessage = "PIN must be 4 digits."
      return
    }
    isSubmitting = true
    coroutineScope.launch {
      try {
        val response = NetworkModule.authApi.login(LoginRequest(phone = phoneNumber, pin = loginPin))
        isSubmitting = false
        onLoginSuccess(response.name, response.phone, response.token)
      } catch (e: HttpException) {
        isSubmitting = false
        errorMessage = if (e.code() == 401) {
          "Incorrect PIN. Please try again."
        } else {
          "Unable to reach the server. Please check your connection and try again."
        }
      } catch (e: Exception) {
        isSubmitting = false
        errorMessage = "Unable to reach the server. Please check your connection and try again."
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

      // 2. Animated Switching Panel across all auth steps
      AnimatedContent(
        targetState = step,
        transitionSpec = {
          if (targetState.ordinal > initialState.ordinal) {
            slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
          } else {
            slideInHorizontally(initialOffsetX = { -it }) + fadeIn() togetherWith
                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
          }
        },
        label = "LoginScreensTransition"
      ) { currentStep ->
        when (currentStep) {
          AuthStep.ENTRY -> EntryStepView(
            mode = mode,
            fullName = fullName,
            onFullNameChange = { fullName = it; errorMessage = null },
            phoneNumber = phoneNumber,
            onPhoneNumberChange = { phoneNumber = it; errorMessage = null; showSignupSuggestion = false },
            howHeard = howHeard,
            howHeardMenuExpanded = howHeardMenuExpanded,
            onHowHeardMenuExpandedChange = { howHeardMenuExpanded = it },
            onHowHeardSelected = { howHeard = it; howHeardMenuExpanded = false; errorMessage = null },
            errorMessage = errorMessage,
            showSignupSuggestion = showSignupSuggestion,
            isSubmitting = isSubmitting,
            onSwitchToSignup = { switchToSignup(phoneNumber) },
            onSwitchToLogin = { switchToLogin() },
            onSubmit = { if (mode == AuthMode.LOGIN) submitLoginPhone() else submitSignup() }
          )
          AuthStep.OTP -> OtpStepView(
            phoneNumber = phoneNumber,
            otpCode = otpCode,
            onOtpChange = { input ->
              if (input.all { it.isDigit() } && input.length <= 6) {
                otpCode = input
                errorMessage = null
                if (input.length == 6) submitOtp()
              }
            },
            errorMessage = errorMessage,
            isVerifying = isVerifying,
            verificationSuccess = verificationSuccess,
            countdownTimer = countdownTimer,
            onBack = { resetToEntry() },
            onVerify = { submitOtp() },
            onResend = { sendOtp() }
          )
          AuthStep.SET_PIN -> SetPinStepView(
            newPin = newPin,
            onNewPinChange = { newPin = it; errorMessage = null },
            confirmPin = confirmPin,
            onConfirmPinChange = { confirmPin = it; errorMessage = null },
            errorMessage = errorMessage,
            isSubmitting = isSubmitting,
            onSubmit = { submitSetPin() }
          )
          AuthStep.ENTER_PIN -> EnterPinStepView(
            phoneNumber = phoneNumber,
            pin = loginPin,
            onPinChange = { loginPin = it; errorMessage = null },
            errorMessage = errorMessage,
            isSubmitting = isSubmitting,
            onBack = { resetToEntry() },
            onSubmit = { submitLoginPin() },
            onForgotPin = { startForgotPin() }
          )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryStepView(
  mode: AuthMode,
  fullName: String,
  onFullNameChange: (String) -> Unit,
  phoneNumber: String,
  onPhoneNumberChange: (String) -> Unit,
  howHeard: String?,
  howHeardMenuExpanded: Boolean,
  onHowHeardMenuExpandedChange: (Boolean) -> Unit,
  onHowHeardSelected: (String) -> Unit,
  errorMessage: String?,
  showSignupSuggestion: Boolean,
  isSubmitting: Boolean,
  onSwitchToSignup: () -> Unit,
  onSwitchToLogin: () -> Unit,
  onSubmit: () -> Unit
) {
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
        "Enter your registered mobile number to sign in with your PIN."
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
      OutlinedTextField(
        value = fullName,
        onValueChange = onFullNameChange,
        placeholder = { Text("Full Name", color = OnSlateTextSecondary) },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Person, contentDescription = "Name Icon", tint = GoldPrimary)
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

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .height(56.dp)
          .background(SlateSurface, RoundedCornerShape(12.dp))
          .border(1.dp, SlateSurfaceVariant, RoundedCornerShape(12.dp))
          .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(text = "🇮🇳 +91", color = OnSlateText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      }

      Spacer(modifier = Modifier.width(8.dp))

      OutlinedTextField(
        value = phoneNumber,
        onValueChange = { input ->
          if (input.all { it.isDigit() } && input.length <= 10) {
            onPhoneNumberChange(input)
          }
        },
        placeholder = { Text("Mobile Phone Number", color = OnSlateTextSecondary) },
        leadingIcon = {
          Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone Icon", tint = GoldPrimary)
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
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "New here? ", color = OnSlateTextSecondary, fontSize = 13.sp)
        Text(
          text = "Sign up instead",
          color = GoldSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clickable { onSwitchToSignup() }
            .testTag("signup_instead_link")
        )
      }
    }

    if (mode == AuthMode.SIGNUP) {
      ExposedDropdownMenuBox(
        expanded = howHeardMenuExpanded,
        onExpandedChange = onHowHeardMenuExpandedChange,
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
          onDismissRequest = { onHowHeardMenuExpandedChange(false) },
          modifier = Modifier.background(SlateSurface)
        ) {
          HOW_HEARD_OPTIONS.forEach { option ->
            DropdownMenuItem(
              text = { Text(option, color = OnSlateText) },
              onClick = { onHowHeardSelected(option) }
            )
          }
        }
      }
    }

    AnimatedVisibility(visible = errorMessage != null) {
      ErrorRow(message = errorMessage ?: "")
    }

    if (mode == AuthMode.LOGIN && showSignupSuggestion) {
      TextButton(
        onClick = onSwitchToSignup,
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

    Button(
      onClick = onSubmit,
      enabled = !isSubmitting,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("send_otp_button"),
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = GoldDark),
      shape = RoundedCornerShape(12.dp)
    ) {
      if (isSubmitting) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SlateBackground, strokeWidth = 2.5.dp)
      } else {
        Text(
          text = if (mode == AuthMode.LOGIN) "CONTINUE" else "SEND ONE-TIME PASSWORD",
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          color = SlateBackground,
          letterSpacing = 1.sp
        )
      }
    }

    if (mode == AuthMode.SIGNUP) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = "Already have an account? ", color = OnSlateTextSecondary, fontSize = 13.sp)
        Text(
          text = "Sign in instead",
          color = GoldSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier
            .clickable { onSwitchToLogin() }
            .testTag("signin_instead_link")
        )
      }
    }
  }
}

@Composable
private fun OtpStepView(
  phoneNumber: String,
  otpCode: String,
  onOtpChange: (String) -> Unit,
  errorMessage: String?,
  isVerifying: Boolean,
  verificationSuccess: Boolean,
  countdownTimer: Int,
  onBack: () -> Unit,
  onVerify: () -> Unit,
  onResend: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("otp_input_container"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.background(SlateSurface, CircleShape)) {
        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldSecondary)
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

    OutlinedTextField(
      value = otpCode,
      onValueChange = onOtpChange,
      placeholder = {
        Text(text = "● ● ● ● ● ●", color = OnSlateTextSecondary.copy(alpha = 0.4f), textAlign = TextAlign.Center, letterSpacing = 4.sp)
      },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Lock, contentDescription = "Secure lock icon", tint = GoldPrimary)
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
      ErrorRow(message = errorMessage ?: "")
    }

    if (isVerifying) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
        CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Verifying security credentials...", color = GoldSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
      }
    } else if (verificationSuccess) {
      Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Verified success", tint = SuccessGreen, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Verification Successful!", color = SuccessGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
      }
    } else {
      Button(
        onClick = onVerify,
        modifier = Modifier
          .fillMaxWidth()
          .height(54.dp)
          .testTag("verify_otp_button"),
        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
        shape = RoundedCornerShape(12.dp)
      ) {
        Text(text = "VERIFY", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateBackground, letterSpacing = 1.sp)
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
      Text(text = "Didn't receive the SMS? ", color = OnSlateTextSecondary, fontSize = 13.sp)
      if (countdownTimer > 0) {
        Text(text = "Resend in ${countdownTimer}s", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
      } else {
        TextButton(onClick = onResend) {
          Text(text = "RESEND OTP CODE", color = GoldSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun SetPinStepView(
  newPin: String,
  onNewPinChange: (String) -> Unit,
  confirmPin: String,
  onConfirmPinChange: (String) -> Unit,
  errorMessage: String?,
  isSubmitting: Boolean,
  onSubmit: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("set_pin_container"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = "Set Your PIN",
      color = OnSlateText,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
      text = "Choose a 4-digit PIN. You'll use it to sign in from now on.",
      color = OnSlateTextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
      lineHeight = 18.sp
    )

    Text(
      text = "NEW PIN",
      color = OnSlateTextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp,
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    PinEntryField(
      pin = newPin,
      onPinChange = onNewPinChange,
      modifier = Modifier.padding(bottom = 20.dp),
      testTag = "new_pin_input"
    )

    Text(
      text = "CONFIRM PIN",
      color = OnSlateTextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp,
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    PinEntryField(
      pin = confirmPin,
      onPinChange = onConfirmPinChange,
      modifier = Modifier.padding(bottom = 16.dp),
      testTag = "confirm_pin_input"
    )

    AnimatedVisibility(visible = errorMessage != null) {
      ErrorRow(message = errorMessage ?: "")
    }

    Button(
      onClick = onSubmit,
      enabled = !isSubmitting,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("set_pin_button"),
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = GoldDark),
      shape = RoundedCornerShape(12.dp)
    ) {
      if (isSubmitting) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SlateBackground, strokeWidth = 2.5.dp)
      } else {
        Text(text = "SAVE PIN & CONTINUE", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateBackground, letterSpacing = 1.sp)
      }
    }
  }
}

@Composable
private fun EnterPinStepView(
  phoneNumber: String,
  pin: String,
  onPinChange: (String) -> Unit,
  errorMessage: String?,
  isSubmitting: Boolean,
  onBack: () -> Unit,
  onSubmit: () -> Unit,
  onForgotPin: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("enter_pin_container"),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.background(SlateSurface, CircleShape)) {
        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = GoldSecondary)
      }
      Spacer(modifier = Modifier.weight(1f))
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      text = "Enter Your PIN",
      color = OnSlateText,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 8.dp)
    )
    Text(
      text = "Enter the 4-digit PIN for +91 $phoneNumber.",
      color = OnSlateTextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 24.dp),
      lineHeight = 18.sp
    )

    PinEntryField(
      pin = pin,
      onPinChange = onPinChange,
      modifier = Modifier.padding(bottom = 16.dp),
      testTag = "login_pin_input"
    )

    AnimatedVisibility(visible = errorMessage != null) {
      ErrorRow(message = errorMessage ?: "")
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.End
    ) {
      Text(
        text = "Forgot PIN?",
        color = GoldSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { onForgotPin() }.testTag("forgot_pin_link")
      )
    }

    Button(
      onClick = onSubmit,
      enabled = !isSubmitting,
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .testTag("login_pin_button"),
      colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, disabledContainerColor = GoldDark),
      shape = RoundedCornerShape(12.dp)
    ) {
      if (isSubmitting) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = SlateBackground, strokeWidth = 2.5.dp)
      } else {
        Text(text = "LOGIN", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SlateBackground, letterSpacing = 1.sp)
      }
    }
  }
}

@Composable
private fun ErrorRow(message: String) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
    Icon(imageVector = Icons.Default.Warning, contentDescription = "Error icon", tint = AlertRed, modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(text = message, color = AlertRed, fontSize = 13.sp, fontWeight = FontWeight.Medium)
  }
}
