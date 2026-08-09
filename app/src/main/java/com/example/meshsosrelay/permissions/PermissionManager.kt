package com.example.meshsosrelay.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meshsosrelay.theme.CanvasNearBlack
import com.example.meshsosrelay.theme.SignalSafeTeal
import com.example.meshsosrelay.theme.SignalSosEmber
import com.example.meshsosrelay.theme.SurfaceNearBlack
import com.example.meshsosrelay.ui.premiumElevation
import com.example.meshsosrelay.ui.ElevationLevel
import com.example.meshsosrelay.ui.premiumPress

class PermissionManager(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun missingPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (!hasLocationPermission()) missing.add("Location")
        if (!hasAudioPermission()) missing.add("Microphone")
        if (!hasSmsPermission()) missing.add("SMS")
        if (!hasNotificationPermission()) missing.add("Notifications")
        if (!hasBluetoothPermission()) missing.add("Bluetooth")
        return missing
    }
    fun getMissingManifestPermissions(): Array<String> {
        val missing = mutableListOf<String>()
        if (!hasLocationPermission()) missing.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasAudioPermission()) missing.add(android.Manifest.permission.RECORD_AUDIO)
        if (!hasSmsPermission()) missing.add(android.Manifest.permission.SEND_SMS)
        if (!hasNotificationPermission() && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            missing.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasBluetoothPermission()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                missing.add(android.Manifest.permission.BLUETOOTH_SCAN)
                missing.add(android.Manifest.permission.BLUETOOTH_CONNECT)
                missing.add(android.Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                missing.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        return missing.toTypedArray()
    }
}

@Composable
fun PermissionStatusCard(
    permissionManager: PermissionManager = PermissionManager(LocalContext.current),
    onRequestPermissions: () -> Unit = {}
) {
    // L-2 fix: use a mutable state key so permissions are re-checked when the user
    // returns from the system permission dialog (via lifecycle resume)
    var checkKey by remember { mutableStateOf(0) }
    val missing = remember(checkKey) { permissionManager.missingPermissions() }

    if (missing.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .premiumElevation(ElevationLevel.Card, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "PERMISSIONS REQUIRED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SignalSosEmber,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Missing: ${missing.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Button(
                    onClick = onRequestPermissions,  // H-2 fix: was onClick = {}
                    colors = ButtonDefaults.buttonColors(containerColor = SignalSafeTeal),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.premiumPress(onRequestPermissions)
                ) {
                    Text(
                        text = "Grant",
                        style = MaterialTheme.typography.labelSmall,
                        color = CanvasNearBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
