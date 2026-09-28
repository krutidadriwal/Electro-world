package com.example.update

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.launch

// Play Store's "flexible" in-app update flow: downloads a newer version in
// the background while the user keeps using the app, then this shows a
// snackbar prompting a restart to apply it. This only does anything once
// the app is actually distributed through Play Store -- Play Store is what
// tells AppUpdateManager a newer version exists; there's no server piece.
@Composable
fun InAppUpdateChecker(snackbarHostState: SnackbarHostState) {
  val context = LocalContext.current
  val appUpdateManager = remember { AppUpdateManagerFactory.create(context) }
  val scope = rememberCoroutineScope()

  fun promptRestart() {
    scope.launch {
      val result = snackbarHostState.showSnackbar(
        message = "An update has been downloaded.",
        actionLabel = "RESTART",
        duration = SnackbarDuration.Indefinite
      )
      if (result == SnackbarResult.ActionPerformed) {
        appUpdateManager.completeUpdate()
      }
    }
  }

  val updateLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()
  ) { /* a cancelled/failed update flow just means we offer it again next launch */ }

  DisposableEffect(appUpdateManager) {
    val listener = InstallStateUpdatedListener { state ->
      if (state.installStatus() == InstallStatus.DOWNLOADED) promptRestart()
    }
    appUpdateManager.registerListener(listener)
    onDispose { appUpdateManager.unregisterListener(listener) }
  }

  LaunchedEffect(Unit) {
    appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
      when {
        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
          info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
          appUpdateManager.startUpdateFlowForResult(
            info,
            updateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
          )
        }
        // A flexible update already finished downloading on a previous
        // launch (e.g. the app was closed before the user hit restart).
        info.installStatus() == InstallStatus.DOWNLOADED -> promptRestart()
      }
    }
  }
}
