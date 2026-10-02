package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.electroworld.staff.BuildConfig
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.SupabasePasswordLoginRequest
import com.electroworld.staff.data.network.SupabaseRecoverRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
  onLoginSuccess: (accessToken: String, refreshToken: String) -> Unit,
  modifier: Modifier = Modifier
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showForgotPassword by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  if (showForgotPassword) {
    ForgotPasswordScreen(
      initialEmail = email,
      onBack = { showForgotPassword = false },
      modifier = modifier
    )
    return
  }

  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "Electro World Staff", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.padding(top = 24.dp))

    OutlinedTextField(
      value = email,
      onValueChange = { email = it; errorMessage = null },
      label = { Text("Email") },
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.padding(top = 12.dp))
    OutlinedTextField(
      value = password,
      onValueChange = { password = it; errorMessage = null },
      label = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
      modifier = Modifier.fillMaxWidth()
    )

    errorMessage?.let {
      Spacer(modifier = Modifier.padding(top = 8.dp))
      Text(text = it, color = MaterialTheme.colorScheme.error)
    }

    Spacer(modifier = Modifier.padding(top = 8.dp))
    Text(
      text = "Forgot password?",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.clickable { showForgotPassword = true }
    )

    Spacer(modifier = Modifier.padding(top = 20.dp))
    Button(
      onClick = {
        errorMessage = null
        isLoading = true
        scope.launch {
          try {
            val response = NetworkModule.supabaseAuthApi.login(
              request = SupabasePasswordLoginRequest(email.trim(), password)
            )
            onLoginSuccess(response.access_token, response.refresh_token)
          } catch (e: Exception) {
            errorMessage = "Login failed. Check your email and password."
          } finally {
            isLoading = false
          }
        }
      },
      enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
      modifier = Modifier.fillMaxWidth()
    ) {
      if (isLoading) {
        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
      } else {
        Text("Log In")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordScreen(
  initialEmail: String,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var email by remember { mutableStateOf(initialEmail) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var sent by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "Reset password", style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.padding(top = 12.dp))

    if (sent) {
      Text("If an account exists for that email, a password reset link has been sent.")
      Spacer(modifier = Modifier.padding(top = 20.dp))
      TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text("Back to log in")
      }
    } else {
      Text(
        "Enter your staff email and we'll send you a link to reset your password.",
        style = MaterialTheme.typography.bodyMedium
      )
      Spacer(modifier = Modifier.padding(top = 16.dp))
      OutlinedTextField(
        value = email,
        onValueChange = { email = it; errorMessage = null },
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth()
      )

      errorMessage?.let {
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(text = it, color = MaterialTheme.colorScheme.error)
      }

      Spacer(modifier = Modifier.padding(top = 20.dp))
      Button(
        onClick = {
          errorMessage = null
          isLoading = true
          scope.launch {
            try {
              NetworkModule.supabaseAuthApi.recover(
                request = SupabaseRecoverRequest(email.trim()),
                redirectTo = "${BuildConfig.SERVER_BASE_URL}/reset-password.html"
              )
              sent = true
            } catch (e: retrofit2.HttpException) {
              errorMessage = if (e.code() == 429) {
                "Too many reset attempts. Please wait a while before trying again."
              } else {
                "Couldn't send the reset email. Please try again."
              }
            } catch (e: Exception) {
              errorMessage = "Couldn't send the reset email. Please try again."
            } finally {
              isLoading = false
            }
          }
        },
        enabled = !isLoading && email.isNotBlank(),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.padding(2.dp))
        } else {
          Text("Send reset link")
        }
      }
      Spacer(modifier = Modifier.padding(top = 12.dp))
      TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
        Text("Back to log in")
      }
    }
  }
}
