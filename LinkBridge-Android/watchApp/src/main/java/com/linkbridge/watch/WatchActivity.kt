package com.linkbridge.watch

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WatchActivity : ComponentActivity() {

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        startWatchService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 31) {
            permissions.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            if (Build.VERSION.SDK_INT >= 23) {
                permissions.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN))
            } else {
                startWatchService()
            }
        }

        val prefs = getSharedPreferences("watch_prefs", Context.MODE_PRIVATE)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF7CB7FF),
                    surface = Color(0xFF141821),
                    background = Color.Black
                )
            ) {
                WatchHomeScreen(
                    prefs = prefs,
                    onFindPhone = {
                        // Use real peripheral via service instance
                        WatchLinkService.instance?.sendFindPhone()
                    },
                    onStopFind = {
                        WatchLinkService.instance?.sendStopFind()
                    }
                )
            }
        }
    }

    private fun startWatchService() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(Intent(this, WatchLinkService::class.java))
            } else {
                startService(Intent(this, WatchLinkService::class.java))
            }
        } catch (_: Exception) {
            try {
                startService(Intent(this, WatchLinkService::class.java))
            } catch (_: Exception) {}
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun WatchHomeScreen(
    prefs: SharedPreferences,
    onFindPhone: () -> Unit,
    onStopFind: () -> Unit
) {
    val context = LocalContext.current
    var selectedBgId by remember { mutableStateOf(prefs.getInt("bg_id", 99)) }
    var showBgPicker by remember { mutableStateOf(false) }
    var showBtDialog by remember { mutableStateOf(false) }
    var isFinding by remember { mutableStateOf(false) }
    val currentBg = remember(selectedBgId) {
        WatchBackgroundRepository.all.find { it.id == selectedBgId } ?: WatchBackgroundRepository.all.last()
    }

    val btInfo = remember {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = manager.adapter
            BtInfo(
                name = adapter?.name ?: "LinkBridge Watch",
                address = adapter?.address ?: "نامشخص",
                isEnabled = adapter?.isEnabled == true
            )
        } catch (_: Exception) {
            BtInfo("LinkBridge Watch", "نامشخص", false)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(currentBg.brush)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBtDialog = true },
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Bluetooth,
                            contentDescription = null,
                            tint = if (btInfo.isEnabled) currentBg.accent else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(btInfo.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("برای جزئیات بزن", color = Color.White.copy(0.6f), fontSize = 8.sp)
                        }
                    }
                    Icon(Icons.Rounded.Info, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("LINKBRIDGE", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(140.dp)
                        .scale(if (isFinding) pulse else 1f)
                        .background(currentBg.accent.copy(alpha = 0.25f), CircleShape)
                )
                Box(
                    Modifier
                        .size(120.dp)
                        .scale(if (isFinding) pulse * 1.05f else 1f)
                        .background(currentBg.accent.copy(alpha = 0.15f), CircleShape)
                )

                FilledTonalButton(
                    onClick = {
                        if (isFinding) {
                            onStopFind()
                            isFinding = false
                        } else {
                            onFindPhone()
                            isFinding = true
                        }
                    },
                    modifier = Modifier
                        .size(110.dp)
                        .scale(if (isFinding) pulse else 1f),
                    shape = CircleShape,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isFinding) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.92f),
                        contentColor = if (isFinding) Color.White else Color.Black
                    ),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(if (isFinding) Icons.Rounded.Stop else Icons.Rounded.PhoneInTalk, null, Modifier.size(36.dp))
                        Spacer(Modifier.height(4.dp))
                        Text(if (isFinding) "توقف" else "پیدا کن", fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = isFinding,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3B30).copy(alpha = 0.9f)), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Vibration, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("در حال زنگ گوشی...", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!isFinding) {
                Text("برای پیدا کردن گوشی بزن", color = Color.White.copy(alpha = 0.5f), fontSize = 9.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SmallActionButton(Icons.Rounded.Palette, "بک‌گراند", currentBg.accent) { showBgPicker = true }
                SmallActionButton(Icons.Rounded.Watch, "${WatchBackgroundRepository.all.size} طرح", currentBg.accent) { showBgPicker = true }
                SmallActionButton(Icons.Rounded.Bluetooth, "بلوتوث", currentBg.accent) { showBtDialog = true }
            }
        }
    }

    if (showBgPicker) {
        BackgroundPickerDialog(selectedId = selectedBgId, onDismiss = { showBgPicker = false }, onSelect = { bg ->
            selectedBgId = bg.id
            prefs.edit().putInt("bg_id", bg.id).apply()
            showBgPicker = false
        })
    }

    if (showBtDialog) {
        BluetoothInfoDialog(info = btInfo, onDismiss = { showBtDialog = false })
    }
}

data class BtInfo(val name: String, val address: String, val isEnabled: Boolean)

@Composable
fun SmallActionButton(icon: ImageVector, label: String, accent: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(0.5.dp, accent.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(0.7f), fontSize = 8.sp)
    }
}

@Composable
fun BackgroundPickerDialog(selectedId: Int, onDismiss: () -> Unit, onSelect: (WatchBackground) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Column {
                Text("انتخاب بک‌گراند", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${WatchBackgroundRepository.all.size} بک‌گراند زیبا", fontSize = 10.sp, color = Color.White.copy(0.6f))
            }
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .height(300.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WatchBackgroundRepository.all) { bg ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg.brush)
                            .border(
                                width = if (bg.id == selectedId) 2.dp else 0.5.dp,
                                color = if (bg.id == selectedId) Color.White else Color.White.copy(0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(bg) },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (bg.id == selectedId) {
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.3f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                        Text(
                            bg.name,
                            color = Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.5f)).padding(2.dp),
                            maxLines = 1
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("بستن", color = Color(0xFF7CB7FF)) }
        }
    )
}

@Composable
fun BluetoothInfoDialog(info: BtInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121212),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bluetooth, null, tint = Color(0xFF7CB7FF))
                Spacer(Modifier.width(8.dp))
                Text("اطلاعات بلوتوث ساعت", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow("اسم بلوتوث ساعت:", info.name, Icons.Rounded.Watch)
                InfoRow("آدرس MAC:", info.address, Icons.Rounded.Tag)
                InfoRow("وضعیت:", if (info.isEnabled) "روشن ✅" else "خاموش ❌", Icons.Rounded.PowerSettingsNew)
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF7CB7FF).copy(alpha = 0.15f)), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("راهنما:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF7CB7FF))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "برای اتصال:\n1. بلوتوث گوشی رو روشن کن\n2. تو برنامه گوشی دکمه اتصال مجدد رو بزن\n3. اسم ساعتت (${info.name}) رو تو لیست پیدا کن و انتخاب کن\n\nاین اسم همون چیزیه که تو تنظیمات بلوتوث گوشیت هم می‌بینی.",
                            fontSize = 10.sp,
                            color = Color.White.copy(0.8f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7CB7FF))) {
                Text("فهمیدم", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, null, tint = Color(0xFF7CB7FF), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = Color.White.copy(0.6f))
            Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
