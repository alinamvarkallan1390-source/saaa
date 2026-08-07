package com.linkbridge.phone
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkbridge.core.bluetooth.BluetoothLinkManager
import com.linkbridge.core.model.LinkState
import com.linkbridge.phone.service.LinkService
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint class MainActivity:ComponentActivity(){private val permission=registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){startForegroundService(Intent(this,LinkService::class.java))};override fun onCreate(b:Bundle?){super.onCreate(b);val p=buildList{if(Build.VERSION.SDK_INT>=31)addAll(listOf(Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT));if(Build.VERSION.SDK_INT>=33)add(Manifest.permission.POST_NOTIFICATIONS)};permission.launch(p.toTypedArray());setContent{LinkTheme{Dashboard()}}}}
@HiltViewModel class MainViewModel @Inject constructor(val link:BluetoothLinkManager):ViewModel(){val state=link.state;fun scan()=link.startSmartScan();fun find()=link.send("FIND_DEVICE".toByteArray());fun stop()=link.send("STOP_FIND".toByteArray())}
@Composable fun LinkTheme(content:@Composable ()->Unit){MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF7CB7FF),background=Color(0xFF080A0F),surface=Color(0xFF141821)),content=content)}
@Composable fun Dashboard(vm:MainViewModel= hiltViewModel()){val s by vm.state.collectAsStateWithLifecycle();Scaffold(containerColor=Color.Transparent,topBar={CenterAlignedTopAppBar(title={Text("LINKBRIDGE")},colors=TopAppBarDefaults.topAppBarColors(containerColor=Color.Transparent))}){pad->Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF101A2C),Color(0xFF080A0F)))).padding(pad).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Rounded.Watch, null,Modifier.size(44.dp),MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Column{Text(s.device.name,style=MaterialTheme.typography.titleLarge);Text(if(s.state==LinkState.CONNECTED)"متصل و رمزنگاری‌شده" else s.message,color=if(s.state==LinkState.CONNECTED)Color(0xFF71D39A)else Color.LightGray)}};Card(colors=CardDefaults.cardColors(containerColor=Color.White.copy(.07f)),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Metric("قدرت سیگنال","${s.device.rssi} dBm · ${s.quality}",Icons.Rounded.SignalCellularAlt);Metric("فاصله تقریبی","%.1f متر".format(s.distanceMeters),Icons.Rounded.NearMe);Metric("تأخیر","${s.device.latencyMs} ms",Icons.Rounded.Speed);Metric("باتری ساعت","${s.device.battery}%",Icons.Rounded.Battery5Bar)}};Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){Button({vm.find()},Modifier.weight(1f)){Icon(Icons.Rounded.Campaign,null);Spacer(Modifier.width(8.dp));Text("پیدا کردن ساعت")};OutlinedButton({vm.scan()},Modifier.weight(1f)){Text("اتصال مجدد")}};AnimatedVisibility(s.state==LinkState.CONNECTED){Text("آخرین همگام‌سازی: ${java.text.DateFormat.getTimeInstance().format(s.device.syncedAt)}",color=Color.Gray)}}}}
@Composable fun Metric(label:String,value:String,icon:androidx.compose.ui.graphics.vector.ImageVector){Row(verticalAlignment=Alignment.CenterVertically){Icon(icon,null,tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.width(12.dp));Text(label,Modifier.weight(1f),color=Color.LightGray);Text(value)}}
