package com.linkbridge.phone

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkbridge.core.bluetooth.BluetoothLinkManager
import com.linkbridge.core.model.LinkState
import com.linkbridge.phone.service.LinkService
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        startForegroundService(Intent(this, LinkService::class.java))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= 31) addAll(listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
            else addAll(listOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION))
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permission.launch(perms.toTypedArray())

        setContent {
            LinkTheme {
                Dashboard()
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(val link: BluetoothLinkManager) : ViewModel() {
    val state = link.state
    fun scan() = link.startSmartScan()
    fun find() = link.send("FIND_DEVICE".toByteArray())
    fun stop() = link.send("STOP_FIND".toByteArray())
}

@Composable
fun LinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF7CB7FF),
            secondary = Color(0xFF9D4EDD),
            background = Color(0xFF080A0F),
            surface = Color(0xFF141821),
            surfaceVariant = Color(0xFF1E2535)
        ),
        content = content
    )
}

@Composable
fun Dashboard(vm: MainViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val isConnected = s.state == LinkState.CONNECTED
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glow by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "glow"
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF7CB7FF), Color(0xFF9D4EDD))))
                            ,
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Link, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("LINKBRIDGE", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp, fontSize = 16.sp)
                            Text("v1.1.0 • Android 8.1+ Ready", fontSize = 10.sp, color = Color.White.copy(0.5f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF101A2C), Color(0xFF080A0F), Color(0xFF0A0E1A))
                    )
                )
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Card - Pretty
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        // Glow
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            (if (isConnected) Color(0xFF71D39A) else Color(0xFF7CB7FF)).copy(alpha = 0.15f * glow),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isConnected) Color(0xFF71D39A).copy(0.2f) else Color.White.copy(0.08f)
                                        )
                                        .border(1.dp, if (isConnected) Color(0xFF71D39A).copy(0.5f) else Color.White.copy(0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Watch,
                                        null,
                                        Modifier.size(28.dp),
                                        tint = if (isConnected) Color(0xFF71D39A) else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        s.device.name.ifBlank { "ساعت هوشمند" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isConnected) Color(0xFF71D39A) else Color(0xFFFFC107))
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (isConnected) "متصل و رمزنگاری‌شده ✅" else s.message.ifBlank { "در حال جستجو..." },
                                            color = if (isConnected) Color(0xFF71D39A) else Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                if (isConnected) {
                                    Icon(Icons.Rounded.VerifiedUser, null, tint = Color(0xFF71D39A), modifier = Modifier.size(20.dp))
                                }
                            }

                            // Bluetooth name info - important for watch
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF7CB7FF).copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.scan() }
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Bluetooth, null, tint = Color(0xFF7CB7FF), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("اسم بلوتوث ساعت:", fontSize = 10.sp, color = Color.White.copy(0.6f))
                                        Text(
                                            s.device.name.ifBlank { "برای دیدن اسم، اتصال را بزن - معمولاً TC4G یا LinkBridge" },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Icon(Icons.Rounded.Refresh, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Metrics Grid - Pretty
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("وضعیت لحظه‌ای", fontWeight = FontWeight.Bold, color = Color.White.copy(0.9f), fontSize = 14.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            "سیگنال",
                            "${s.device.rssi} dBm",
                            s.quality,
                            Icons.Rounded.SignalCellularAlt,
                            Color(0xFF7CB7FF),
                            Modifier.weight(1f)
                        )
                        MetricCard(
                            "فاصله",
                            "%.1f m".format(s.distanceMeters),
                            "تقریبی",
                            Icons.Rounded.NearMe,
                            Color(0xFF9D4EDD),
                            Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            "تأخیر",
                            "${s.device.latencyMs} ms",
                            if (s.device.latencyMs < 100) "عالی" else "متوسط",
                            Icons.Rounded.Speed,
                            Color(0xFF00D4FF),
                            Modifier.weight(1f)
                        )
                        MetricCard(
                            "باتری ساعت",
                            "${s.device.battery}%",
                            if (s.device.battery > 20) "خوب" else "کم",
                            Icons.Rounded.Battery5Bar,
                            if (s.device.battery > 20) Color(0xFF71D39A) else Color(0xFFFF5E62),
                            Modifier.weight(1f)
                        )
                    }
                }
            }

            // Action Buttons - Pretty
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Find Watch
                    Button(
                        onClick = { vm.find() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7CB7FF))
                    ) {
                        Icon(Icons.Rounded.Campaign, null)
                        Spacer(Modifier.width(10.dp))
                        Text("پیدا کردن ساعت (بوق + ویبره)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { vm.scan() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Rounded.BluetoothSearching, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("اتصال مجدد", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { vm.stop() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(0.8f))
                        ) {
                            Icon(Icons.Rounded.Stop, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("توقف", fontSize = 12.sp)
                        }
                    }

                    // Info card for Android 8.1
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF71D39A).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF71D39A), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("سازگار با اندروید 8.1", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF71D39A))
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "این نسخه برای ساعت‌های با اندروید 8.1 (مثل پوکو) بهینه شده و minSdk روی 26 تنظیم شده. اگر ساعتت اندروید 8.1 داره، الان باید نصب بشه. اگر نشد، مطمئن شو «نصب از منابع ناشناس» فعاله.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(0.7f),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isConnected) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Update, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "آخرین همگام‌سازی: ${java.text.DateFormat.getTimeInstance().format(s.device.syncedAt)}",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    sub: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.06f)),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = Color.White.copy(0.6f), fontSize = 11.sp)
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            Text(sub, fontSize = 10.sp, color = accent.copy(alpha = 0.8f))
        }
    }
}
