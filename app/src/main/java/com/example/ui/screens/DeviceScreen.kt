package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.ConnectionStatus
import com.example.bluetooth.DiscoveredBleDevice
import com.example.ui.WalkingPadViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmergencyRed
import com.example.ui.theme.OrangeWarning
import com.example.ui.theme.VoltGreen
import com.example.ui.theme.VoltGreenBright

@Composable
fun DeviceScreen(
    viewModel: WalkingPadViewModel,
    modifier: Modifier = Modifier
) {
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val connectedName by viewModel.connectedDeviceName.collectAsStateWithLifecycle()
    val connectedAddress by viewModel.connectedDeviceAddress.collectAsStateWithLifecycle()
    val lastError by viewModel.lastErrorMessage.collectAsStateWithLifecycle()

    val isScanning = connectionStatus == ConnectionStatus.SCANNING
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("device_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "DEVICE MANAGEMENT",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Pair and connect directly to KingSmith WalkingPad MC21 via Bluetooth FTMS",
                    fontSize = 12.sp,
                    color = Color(0xFF8B949E)
                )
            }
        }

        // Active Connection Card
        item {
            ActiveConnectionCard(
                status = connectionStatus,
                name = connectedName,
                address = connectedAddress,
                onDisconnect = { viewModel.disconnectDevice() }
            )
        }

        // Scanner Trigger & Action Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NEARBY BLUETOOTH DEVICES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B949E),
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = {
                        if (isScanning) viewModel.stopScan() else viewModel.startScan()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isScanning) OrangeWarning else VoltGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("btn_ble_scan")
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Stop Scan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Devices", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Error Banner if any
        if (lastError != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmergencyRed.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, EmergencyRed.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = lastError ?: "",
                        fontSize = 12.sp,
                        color = EmergencyRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Devices List
        if (discoveredDevices.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(DarkSurface, RoundedCornerShape(14.dp))
                        .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BluetoothSearching,
                            contentDescription = null,
                            tint = Color(0xFF6E7681),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isScanning) "Searching for WalkingPad MC21…" else "No devices found yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8B949E)
                        )
                        Text(
                            text = "Ensure your WalkingPad MC21 is plugged in and powered on",
                            fontSize = 11.sp,
                            color = Color(0xFF6E7681)
                        )
                    }
                }
            }
        } else {
            items(discoveredDevices, key = { it.address }) { device ->
                DiscoveredDeviceCard(
                    device = device,
                    isCurrentTarget = connectedAddress == device.address,
                    onConnect = { viewModel.connectDevice(device.address) }
                )
            }
        }

        // BLE Diagnostics Section
        item {
            DiagnosticsCard()
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ActiveConnectionCard(
    status: ConnectionStatus,
    name: String?,
    address: String?,
    onDisconnect: () -> Unit
) {
    val isConnected = status == ConnectionStatus.CONNECTED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.5.dp, if (isConnected) VoltGreen else DarkSurfaceVariant, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("active_connection_card"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isConnected) VoltGreen.copy(alpha = 0.2f) else DarkSurfaceElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = if (isConnected) VoltGreen else Color(0xFF8B949E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = if (isConnected) (name ?: "WalkingPad MC21") else "Not Connected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isConnected) "MAC: ${address ?: "Unknown"}" else "Select a treadmill below to connect",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8B949E)
                    )
                }
            }

            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed)
                ) {
                    Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DiscoveredDeviceCard(
    device: DiscoveredBleDevice,
    isCurrentTarget: Boolean,
    onConnect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (device.isWalkingPad) VoltGreen.copy(alpha = 0.5f) else DarkSurfaceVariant,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onConnect)
            .padding(14.dp)
            .testTag("device_item_${device.address}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (device.isWalkingPad) VoltGreen else Color.Gray, CircleShape)
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (device.isWalkingPad) {
                        Box(
                            modifier = Modifier
                                .background(VoltGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MC21 MATCH",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = VoltGreenBright
                            )
                        }
                    }
                }

                Text(
                    text = "${device.address} • RSSI: ${device.rssi} dBm",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8B949E)
                )
            }
        }

        Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrentTarget) CyanAccent else VoltGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = if (isCurrentTarget) "CONNECTED" else "CONNECT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DiagnosticsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurfaceElevated, RoundedCornerShape(14.dp))
            .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "FTMS PROTOCOL SPECIFICATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanAccent,
                letterSpacing = 0.8.sp
            )
        }

        Text(
            text = "• Service UUID: 0x1826 (Fitness Machine Service)\n" +
                    "• Treadmill Data: 0x2ACD (Instantaneous speed + KingSmith Bit-13 Step Extension)\n" +
                    "• Control Point: 0x2AD9 (Opcodes: 0x00 Request, 0x02 Speed, 0x07 Start, 0x08 Stop)\n" +
                    "• Status Notify: 0x2ADA (Belt state events)",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF8B949E),
            lineHeight = 16.sp
        )
    }
}
