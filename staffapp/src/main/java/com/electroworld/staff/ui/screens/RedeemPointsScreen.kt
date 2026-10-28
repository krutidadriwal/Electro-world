package com.electroworld.staff.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.electroworld.staff.data.network.NetworkModule
import com.electroworld.staff.data.network.PointsCustomerResponse
import com.electroworld.staff.data.network.RedeemPointsRequest
import com.electroworld.staff.ui.theme.staffCardColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

// The server's own { error } text where it sent one ("Customer only has
// 420 valid points", ...), which says more than a generic failure.
private fun serverErrorMessage(e: Exception): String? = try {
  (e as? HttpException)?.response()?.errorBody()?.string()?.let { JSONObject(it).optString("error") }?.ifBlank { null }
} catch (parseError: Exception) {
  null
}

// "2026-11-03" -> "03-11-2026", as the customer app shows dates.
private fun formatDate(isoDate: String): String {
  val parts = isoDate.split("-")
  return if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else isoDate
}

// Admin-only tab (listed under ADMIN in HomeScreen): look a registered
// customer up by phone, see their valid (unexpired) points, and redeem some
// on an invoice. The server takes them from the soonest-expiring points
// first and enforces the minimum balance (see server/lib/points.js).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedeemPointsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
  val scope = rememberCoroutineScope()

  var phone by remember { mutableStateOf("") }
  var customer by remember { mutableStateOf<PointsCustomerResponse?>(null) }
  var isLookingUp by remember { mutableStateOf(false) }
  var lookupError by remember { mutableStateOf<String?>(null) }
  // Bumped after a redemption so the same customer is looked up again.
  var lookupTrigger by remember { mutableStateOf(0) }

  var pointsInput by remember { mutableStateOf("") }
  var confirmingPoints by remember { mutableStateOf<Int?>(null) }
  var isRedeeming by remember { mutableStateOf(false) }
  var redeemError by remember { mutableStateOf<String?>(null) }
  var successMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(phone, lookupTrigger) {
    customer = null
    lookupError = null
    if (phone.length != 10) return@LaunchedEffect
    isLookingUp = true
    try {
      customer = NetworkModule.pointsApi.customer(phone)
    } catch (e: Exception) {
      lookupError = if ((e as? HttpException)?.code() == 404) {
        "No customer account for this number. Only registered customers have points."
      } else {
        serverErrorMessage(e) ?: "Failed to load the customer's points."
      }
    } finally {
      isLookingUp = false
    }
  }

  val current = customer
  val points = pointsInput.toIntOrNull()
  val canRedeem = current != null && current.balance >= current.minBalanceToRedeem
  val pointsError = when {
    current == null || pointsInput.isEmpty() -> null
    points == null || points <= 0 -> "Enter a whole number of points"
    points > current.balance -> "Only ${current.balance} valid points available"
    else -> null
  }

  confirmingPoints?.let { toRedeem ->
    if (current != null) {
      AlertDialog(
        onDismissRequest = { confirmingPoints = null },
        title = { Text("Redeem points?") },
        text = {
          Text(
            "Redeem $toRedeem points for ${current.name} (${phone})? " +
              "Their balance will be ${current.balance - toRedeem} points. This can't be undone from the app."
          )
        },
        confirmButton = {
          Button(onClick = {
            confirmingPoints = null
            isRedeeming = true
            redeemError = null
            successMessage = null
            val name = current.name
            scope.launch {
              try {
                val result = NetworkModule.pointsApi.redeem(RedeemPointsRequest(phone = phone, points = toRedeem))
                successMessage = "Redeemed ${result.redeemed} points for $name. Remaining balance: ${result.balance}."
                pointsInput = ""
              } catch (e: Exception) {
                redeemError = serverErrorMessage(e) ?: "Failed to redeem points."
              } finally {
                isRedeeming = false
                lookupTrigger++
              }
            }
          }) { Text("Redeem") }
        },
        dismissButton = { TextButton(onClick = { confirmingPoints = null }) { Text("Cancel") } }
      )
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Redeem Points") },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
          }
        }
      )
    },
    modifier = modifier
  ) { padding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      OutlinedTextField(
        value = phone,
        onValueChange = { input ->
          phone = input.filter { it.isDigit() }.take(10)
          pointsInput = ""
          redeemError = null
          successMessage = null
        },
        label = { Text("Customer phone (10 digits)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth()
      )

      successMessage?.let {
        Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
      }

      when {
        isLookingUp && current == null -> CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        lookupError != null -> Text(lookupError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        current != null -> {
          Card(colors = staffCardColors(), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(current.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              Text(phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
              Text("Valid points", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
              Text(
                current.balance.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
              current.nextExpiry?.let {
                Text(
                  "${it.points} expire on ${formatDate(it.expiresOn)}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.outline
                )
              }
              if (current.lots.size > 1) {
                Text("Expiry breakdown", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
                current.lots.forEach { lot ->
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${lot.points} points", style = MaterialTheme.typography.bodySmall)
                    Text("expires ${formatDate(lot.expiresOn)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                  }
                }
              }
            }
          }

          if (!canRedeem) {
            Text(
              "At least ${current.minBalanceToRedeem} valid points are needed to redeem.",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium
            )
          } else {
            OutlinedTextField(
              value = pointsInput,
              onValueChange = { input ->
                pointsInput = input.filter { it.isDigit() }.take(7)
                redeemError = null
                successMessage = null
              },
              label = { Text("Points to redeem on this invoice") },
              singleLine = true,
              isError = pointsError != null,
              supportingText = pointsError?.let { error -> { Text(error) } },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.fillMaxWidth()
            )
            redeemError?.let {
              Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
              enabled = !isRedeeming && points != null && pointsError == null,
              onClick = { confirmingPoints = points },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(if (isRedeeming) "Redeeming..." else "Redeem")
            }
          }
        }
      }
    }
  }
}
