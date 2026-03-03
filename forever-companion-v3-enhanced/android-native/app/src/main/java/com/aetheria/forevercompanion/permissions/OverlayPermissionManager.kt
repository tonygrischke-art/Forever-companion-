package com.aetheria.forevercompanion.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles SYSTEM_ALERT_WINDOW permission with user education.
 */
class OverlayPermissionManager(private val activity: FragmentActivity) {

    private val _state = MutableStateFlow<PermissionState>(PermissionState.Unknown)
    val state: StateFlow<PermissionState> = _state
    private var pendingAction: (() -> Unit)? = null

    private val launcher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkState()
            if (_state.value is PermissionState.Granted) {
                pendingAction?.invoke()
                pendingAction = null
            }
        }

    init { checkState() }

    fun checkState() {
        _state.value = if (Settings.canDrawOverlays(activity)) {
            PermissionState.Granted
        } else {
            PermissionState.Denied
        }
    }

    fun requestPermission(onGranted: () -> Unit) {
        if (Settings.canDrawOverlays(activity)) {
            onGranted()
            return
        }
        pendingAction = onGranted

        AlertDialog.Builder(activity)
            .setTitle("Enable Companion Overlay")
            .setMessage("To keep your companion visible while using other apps, Forever Companion needs permission to draw over other applications.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}")
                )
                launcher.launch(intent)
            }
            .setNegativeButton("Not Now") { _, _ ->
                _state.value = PermissionState.Denied
                pendingAction = null
            }
            .show()
    }

    sealed class PermissionState {
        object Unknown : PermissionState()
        object Granted : PermissionState()
        object Denied : PermissionState()
    }
}
