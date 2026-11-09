package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.WalkingPadViewModel
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ControlScreen
import com.example.ui.screens.DeviceScreen
import com.example.ui.screens.IntervalsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.VoltGreen
import com.example.ui.theme.WalkingPadTheme

enum class MainTab {
    CONTROL,
    INTERVALS,
    ANALYTICS,
    DEVICE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WalkingPadTheme(darkTheme = true) {
                WalkingPadApp()
            }
        }
    }
}

@Composable
fun WalkingPadApp(
    viewModel: WalkingPadViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(MainTab.CONTROL) }

    // Bluetooth Permission Launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions handled; can automatically start scanning if granted
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    // Android back button handling: navigate back to Control tab before exiting
    if (currentTab != MainTab.CONTROL) {
        BackHandler {
            currentTab = MainTab.CONTROL
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.CONTROL,
                    onClick = { currentTab = MainTab.CONTROL },
                    icon = { Icon(Icons.Default.Speed, contentDescription = "Control") },
                    label = { Text("Control", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = VoltGreen,
                        indicatorColor = VoltGreen,
                        unselectedIconColor = Color(0xFF8B949E),
                        unselectedTextColor = Color(0xFF8B949E)
                    ),
                    modifier = Modifier.testTag("tab_control")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.INTERVALS,
                    onClick = { currentTab = MainTab.INTERVALS },
                    icon = { Icon(Icons.Default.Timer, contentDescription = "Intervals") },
                    label = { Text("Intervals", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = VoltGreen,
                        indicatorColor = VoltGreen,
                        unselectedIconColor = Color(0xFF8B949E),
                        unselectedTextColor = Color(0xFF8B949E)
                    ),
                    modifier = Modifier.testTag("tab_intervals")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.ANALYTICS,
                    onClick = { currentTab = MainTab.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = VoltGreen,
                        indicatorColor = VoltGreen,
                        unselectedIconColor = Color(0xFF8B949E),
                        unselectedTextColor = Color(0xFF8B949E)
                    ),
                    modifier = Modifier.testTag("tab_analytics")
                )

                NavigationBarItem(
                    selected = currentTab == MainTab.DEVICE,
                    onClick = { currentTab = MainTab.DEVICE },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = "Device") },
                    label = { Text("Device", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = VoltGreen,
                        indicatorColor = VoltGreen,
                        unselectedIconColor = Color(0xFF8B949E),
                        unselectedTextColor = Color(0xFF8B949E)
                    ),
                    modifier = Modifier.testTag("tab_device")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.CONTROL -> {
                    ControlScreen(
                        viewModel = viewModel,
                        onNavigateToDevice = { currentTab = MainTab.DEVICE }
                    )
                }
                MainTab.INTERVALS -> {
                    IntervalsScreen(viewModel = viewModel)
                }
                MainTab.ANALYTICS -> {
                    AnalyticsScreen(viewModel = viewModel)
                }
                MainTab.DEVICE -> {
                    DeviceScreen(viewModel = viewModel)
                }
            }
        }
    }
}
