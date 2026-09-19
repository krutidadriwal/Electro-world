package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.SupabasePasswordLoginRequest
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
  val scope = rememberCoroutineScope()

  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "Electro World Staff", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 24.dp))

    OutlinedTextField(
      value = email,
      onValueChange = { email = it; errorMessage = null },
      label = { Text("Email") },
      modifier = Modifier.fillMaxWidth()
    )
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 12.dp))
    OutlinedTextField(
      value = password,
      onValueChange = { password = it; errorMessage = null },
      label = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
      modifier = Modifier.fillMaxWidth()
    )

    errorMessage?.let {
      androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
      Text(text = it, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
    }

    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 20.dp))
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
