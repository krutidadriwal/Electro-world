package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.network.LoginRequest
import com.example.data.network.NetworkModule
import com.example.data.network.SendOtpRequest
import com.example.data.network.SetPinRequest
import com.example.data.network.VerifyOtpRequest
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

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
  onLoginSuccess: (name: String, phone: String, token: String) -> Unit,
  modifier: Modifier = Modifier,
  showLogo: Boolean = true
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
  var pendingVerificationToken by remember { mutableStateOf<String?>(null) }

  var loginPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }

  var isSubmitting by remember { mutableStateOf(false) }
  var isVerifying by remember { mutableStateOf(false) }
  var verificationSuccess by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showSignupSuggestion by remember { mutableStateOf(false) }
  var showLoginSuggestion by remember { mutableStateOf(false) }
  var countdownTimer by remember { mutableStateOf(30) }

  val coroutineScope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  // Shrinks the branding header out of the way while the keyboard is up, so
  // the card and its fields have room to breathe instead of getting squished.
  val imeVisible = WindowInsets.isImeVisible
  val headerHeight by animateDpAsState(if (imeVisible) 88.dp else 240.dp, tween(250), label = "HeaderHeight")
  val headerTopPadding by animateDpAsState(if (imeVisible) 12.dp else 40.dp, tween(250), label = "HeaderTopPadding")

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
    pendingVerificationToken = null
    errorMessage = null
    verificationSuccess = false
  }

  // Hardware/gesture back navigates one step back within the auth flow
  // instead of falling through to the system default (closing the app).
  // At ENTRY there's no earlier step here, so the handler is disabled and
  // the event propagates up to whatever hosts this screen (e.g. the
  // onboarding welcome panel).
  BackHandler(enabled = step != AuthStep.ENTRY) { resetToEntry() }

  fun switchToSignup(prefillPhone: String) {
    mode = AuthMode.SIGNUP
    resetToEntry()
    phoneNumber = prefillPhone
    showSignupSuggestion = false
    showLoginSuggestion = false
  }

  fun switchToLogin() {
    mode = AuthMode.LOGIN
    resetToEntry()
    showSignupSuggestion = false
    showLoginSuggestion = false
  }

  fun proceedToSetPin(verificationToken: String) {
    pendingVerificationToken = verificationToken
    newPin = ""
    confirmPin = ""
    errorMessage = null
    verificationSuccess = false
    step = AuthStep.SET_PIN
  }

  fun sendOtp() {
    isSubmitting = true
    coroutineScope.launch {
      try {
        NetworkModule.authApi.sendOtp(SendOtpRequest(phone = phoneNumber))
        isSubmitting = false
        otpCode = ""
        countdownTimer = 30
        errorMessage = null
        step = AuthStep.OTP
      } catch (e: HttpException) {
        isSubmitting = false
        errorMessage = if (e.code() == 429) {
          "Please wait a moment before requesting another code."
        } else {
          "Unable to send verification code. Please try again."
        }
      } catch (e: Exception) {
        isSubmitting = false
        errorMessage = "Unable to send verification code. Please try again."
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
    showLoginSuggestion = false
    isSubmitting = true
    coroutineScope.launch {
      try {
        NetworkModule.userApi.getUser(phoneNumber)
        // A 200 here means the phone is already registered.
        isSubmitting = false
        errorMessage = "An account with this number already exists."
        showLoginSuggestion = true
      } catch (e: HttpException) {
        if (e.code() == 404) {
          isSubmitting = false
          sendOtp()
        } else {
          isSubmitting = false
          errorMessage = "Unable to reach the server. Please check your connection and try again."
        }
      } catch (e: Exception) {
        isSubmitting = false
        errorMessage = "Unable to reach the server. Please check your connection and try again."
      }
    }
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
    isVerifying = true
    coroutineScope.launch {
      try {
        val response = NetworkModule.authApi.verifyOtp(VerifyOtpRequest(phone = phoneNumber, code = otpCode))
        isVerifying = false
        verificationSuccess = true
        delay(400)
        proceedToSetPin(response.verificationToken)
      } catch (e: HttpException) {
        isVerifying = false
        errorMessage = if (e.code() == 429) {
          "Too many incorrect attempts. Please request a new code."
        } else {
          "Invalid verification code. Please try again."
        }
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
    val verificationToken = pendingVerificationToken
    if (verificationToken == null) {
      errorMessage = "Verification session expired. Please start again."
      return
    }
    isSubmitting = true
    coroutineScope.launch {
      try {
        val response = NetworkModule.authApi.setPin(
          SetPinRequest(
            verificationToken = verificationToken,
            pin = newPin,
            name = if (mode == AuthMode.SIGNUP) fullName.trim() else null,
            howHeardAboutUs = if (mode == AuthMode.SIGNUP) howHeard else null
          )
        )
        isSubmitting = false
        // Other endpoints (invoices/wishlist/complaints/installations) expect the
        // bare 10-digit phone, not the E.164 form the server stores/returns.
        onLoginSuccess(response.name, phoneNumber, response.token)
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
        onLoginSuccess(response.name, phoneNumber, response.token)
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

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SlateBackground)
  ) {
    // 1. Header band: brand logo, on the dark background. While the keyboard
    // is open, the full logo fades out (freeing up vertical space) and a
    // small mark fades in at the top-right as a minimal persistent brand cue.
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(headerHeight)
        .padding(top = headerTopPadding, start = 24.dp, end = 24.dp)
    ) {
      if (showLogo) {
        androidx.compose.animation.AnimatedVisibility(
          visible = !imeVisible,
          modifier = Modifier.align(Alignment.Center),
          enter = fadeIn(tween(250)),
          exit = fadeOut(tween(250))
        ) {
          ElectroWorldLogo(iconSize = 72f)
        }

        androidx.compose.animation.AnimatedVisibility(
          visible = imeVisible,
          modifier = Modifier.align(Alignment.TopEnd),
          enter = fadeIn(tween(250)),
          exit = fadeOut(tween(250))
        ) {
          Image(
            painter = painterResource(id = R.drawable.ew_logo_small),
            contentDescription = "Electro World",
            modifier = Modifier.size(32.dp)
          )
        }
      }
    }

    // 2. Rounded content sheet holding the active auth step
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .background(SlateSurface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // 2a. Scrollable body: fields, links, error messages -- everything
        // except the primary action, which is pinned below so it never
        // scrolls out of reach.
        Column(
          modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          // 3. Animated Switching Panel across all auth steps
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
                imeVisible = imeVisible,
                fullName = fullName,
                onFullNameChange = { fullName = it; errorMessage = null },
                phoneNumber = phoneNumber,
                onPhoneNumberChange = { phoneNumber = it; errorMessage = null; showSignupSuggestion = false; showLoginSuggestion = false },
                howHeard = howHeard,
                howHeardMenuExpanded = howHeardMenuExpanded,
                onHowHeardMenuExpandedChange = { howHeardMenuExpanded = it },
                onHowHeardSelected = { howHeard = it; howHeardMenuExpanded = false; errorMessage = null },
                errorMessage = errorMessage,
                showSignupSuggestion = showSignupSuggestion,
                showLoginSuggestion = showLoginSuggestion,
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
                onBack = { resetToEntry() }
              )
              AuthStep.SET_PIN -> SetPinStepView(
                newPin = newPin,
                onNewPinChange = { newPin = it; errorMessage = null },
                confirmPin = confirmPin,
                onConfirmPinChange = { confirmPin = it; errorMessage = null },
                errorMessage = errorMessage,
                onSubmit = { submitSetPin() }
              )
              AuthStep.ENTER_PIN -> EnterPinStepView(
                phoneNumber = phoneNumber,
                pin = loginPin,
                onPinChange = { loginPin = it; errorMessage = null },
                errorMessage = errorMessage,
                onBack = { resetToEntry() },
                onSubmit = { submitLoginPin() },
                onForgotPin = { startForgotPin() }
              )
            }
          }
        }

        // 2b. Pinned action area: rides above the keyboard (imePadding) so
        // the primary button and trust footer stay visible and tap-ready
        // no matter how tall the keyboard is or how the body scrolls.
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = if (imeVisible) 8.dp else 16.dp)
        ) {
          when (step) {
            AuthStep.ENTRY -> {
              Button(
                onClick = { if (mode == AuthMode.LOGIN) submitLoginPhone() else submitSignup() },
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
                    text = if (mode == AuthMode.LOGIN) "Continue" else "Send One-Time Password",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SlateBackground
                  )
                }
              }

              if (mode == AuthMode.SIGNUP) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
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
                      .clickable { switchToLogin() }
                      .testTag("signin_instead_link")
                  )
                }
              }

              if (mode == AuthMode.LOGIN) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
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
                      .clickable { switchToSignup(phoneNumber) }
                      .testTag("signup_instead_link")
                  )
                }
              }
            }
            AuthStep.OTP -> {
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
                  onClick = { submitOtp() },
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("verify_otp_button"),
                  colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text(text = "Verify", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SlateBackground)
                }
              }

              Spacer(modifier = Modifier.height(20.dp))

              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Didn't receive the SMS? ", color = OnSlateTextSecondary, fontSize = 13.sp)
                if (countdownTimer > 0) {
                  Text(text = "Resend in ${countdownTimer}s", color = GoldPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                  TextButton(onClick = { sendOtp() }) {
                    Text(text = "Resend OTP Code", color = GoldSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                  }
                }
              }
            }
            AuthStep.SET_PIN -> {
              Button(
                onClick = { submitSetPin() },
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
                  Text(text = "Save PIN & Continue", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SlateBackground)
                }
              }
            }
            AuthStep.ENTER_PIN -> {
              Button(
                onClick = { submitLoginPin() },
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
                  Text(text = "Log In", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SlateBackground)
                }
              }
            }
          }
        }

        // Secure protocol footer -- a normal (non-pinned) row that sits
        // right after the action area. It has no imePadding of its own, so
        // when the keyboard opens and pushes the action area up, this footer
        // is simply the first thing to slide out of view behind it, rather
        // than competing with it for space.
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 12.dp),
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secure",
            tint = Color(0xFFA0A0A0),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Electro World Shield • End-To-End Encrypted OTP Verification",
            color = Color(0xFFA0A0A0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryStepView(
  mode: AuthMode,
  imeVisible: Boolean,
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
  showLoginSuggestion: Boolean,
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
      modifier = Modifier.padding(bottom = 12.dp)
    )

    AnimatedVisibility(visible = mode == AuthMode.LOGIN || !imeVisible) {
      Text(
        text = if (mode == AuthMode.LOGIN) {
          "Enter your registered mobile number to sign in with your PIN."
        } else {
          "Tell us a bit about yourself to create your Electro World account."
        },
        color = OnSlateTextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 28.dp),
        lineHeight = 18.sp
      )
    }

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
          .padding(bottom = 20.dp)
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

    OutlinedTextField(
      value = phoneNumber,
      onValueChange = { input ->
        if (input.all { it.isDigit() } && input.length <= 10) {
          onPhoneNumberChange(input)
        }
      },
      placeholder = { Text("Mobile Number", color = OnSlateTextSecondary) },
      leadingIcon = {
        Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone Icon", tint = GoldPrimary)
      },
      prefix = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .padding(end = 10.dp)
              .width(1.dp)
              .height(20.dp)
              .background(SlateSurfaceVariant)
          )
          Text(text = "+91", color = OnSlateText, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
      },
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 20.dp)
        .testTag("phone_number_input"),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { if (mode == AuthMode.LOGIN) onSubmit() }),
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

    if (mode == AuthMode.SIGNUP) {
      ExposedDropdownMenuBox(
        expanded = howHeardMenuExpanded,
        onExpandedChange = onHowHeardMenuExpandedChange,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp)
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
          .padding(bottom = 16.dp)
          .testTag("create_account_button")
      ) {
        Text(
          text = "Create an Account",
          color = GoldSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    if (mode == AuthMode.SIGNUP && showLoginSuggestion) {
      TextButton(
        onClick = onSwitchToLogin,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp)
          .testTag("signin_suggestion_button")
      ) {
        Text(
          text = "Sign In Instead",
          color = GoldSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
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
  onBack: () -> Unit
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

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Enter Verification Code",
      color = OnSlateText,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 12.dp)
    )

    Text(
      text = "We have dispatched a 6-digit verification code to +91 $phoneNumber. Check your messages.",
      color = OnSlateTextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 28.dp),
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
        .padding(bottom = 20.dp)
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
  }
}

@Composable
private fun SetPinStepView(
  newPin: String,
  onNewPinChange: (String) -> Unit,
  confirmPin: String,
  onConfirmPinChange: (String) -> Unit,
  errorMessage: String?,
  onSubmit: () -> Unit
) {
  val confirmPinFocusRequester = remember { FocusRequester() }

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
      modifier = Modifier.padding(bottom = 12.dp)
    )
    Text(
      text = "Choose a 4-digit PIN. You'll use it to sign in from now on.",
      color = OnSlateTextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 28.dp),
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
      testTag = "new_pin_input",
      imeAction = ImeAction.Next,
      onImeAction = { confirmPinFocusRequester.requestFocus() }
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
      modifier = Modifier.padding(bottom = 20.dp),
      testTag = "confirm_pin_input",
      focusRequester = confirmPinFocusRequester,
      onImeAction = onSubmit
    )

    AnimatedVisibility(visible = errorMessage != null) {
      ErrorRow(message = errorMessage ?: "")
    }
  }
}

@Composable
private fun EnterPinStepView(
  phoneNumber: String,
  pin: String,
  onPinChange: (String) -> Unit,
  errorMessage: String?,
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

    Spacer(modifier = Modifier.height(20.dp))

    Text(
      text = "Enter Your PIN",
      color = OnSlateText,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 12.dp)
    )
    Text(
      text = "Enter the 4-digit PIN for +91 $phoneNumber.",
      color = OnSlateTextSecondary,
      fontSize = 13.sp,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 28.dp),
      lineHeight = 18.sp
    )

    PinEntryField(
      pin = pin,
      onPinChange = onPinChange,
      modifier = Modifier.padding(bottom = 20.dp),
      testTag = "login_pin_input",
      onImeAction = onSubmit
    )

    AnimatedVisibility(visible = errorMessage != null) {
      ErrorRow(message = errorMessage ?: "")
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
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
