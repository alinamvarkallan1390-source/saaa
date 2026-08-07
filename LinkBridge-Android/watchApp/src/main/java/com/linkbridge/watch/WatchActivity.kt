package com.linkbridge.watch
import android.Manifest
import android.content.Intent
import android.os.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.linkbridge.core.bluetooth.BluetoothLinkManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint class WatchActivity:ComponentActivity(){@Inject lateinit var link:BluetoothLinkManager;private val permissions=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){startForegroundService(Intent(this,WatchLinkService::class.java))};override fun onCreate(b:Bundle?){super.onCreate(b);if(Build.VERSION.SDK_INT>=31)permissions.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT));else startForegroundService(Intent(this,WatchLinkService::class.java));setContent{MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF7CB7FF))){WatchHome{link.send("FIND_PHONE".toByteArray())}}}}}
@Composable fun WatchHome(find:()->Unit){Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1D3555),Color(0xFF07090D)))),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)){Text("LINKBRIDGE",style=MaterialTheme.typography.titleMedium);Button(find,Modifier.size(132.dp),shape=CircleShape,contentPadding=PaddingValues(16.dp)){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Rounded.PhoneInTalk,null,Modifier.size(42.dp));Spacer(Modifier.height(8.dp));Text("Find My Phone")}};Text("برای توقف دوباره لمس کنید",color=Color.LightGray,style=MaterialTheme.typography.bodySmall)}}}
