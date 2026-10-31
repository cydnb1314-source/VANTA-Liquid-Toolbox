package com.vanta.toolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() { override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { VantaApp() } } }

@Composable fun VantaApp() {
 var english by remember { mutableStateOf(false) }; var entered by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }; var code by remember { mutableStateOf("") }; var page by remember { mutableStateOf("home") }
 val bg = Brush.linearGradient(listOf(Color(0xFFF8FAFF),Color(0xFFE6ECFA),Color(0xFFF7F4FF)))
 MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF5267A8),background=Color(0xFFF3F5FB))) {
 Box(Modifier.fillMaxSize().background(bg).systemBarsPadding()) {
  Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
   Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
    Column { Text("VANTA",fontSize=28.sp,fontWeight=FontWeight.Black,color=Color(0xFF202A45)); Text("LIQUID TOOLBOX",fontSize=10.sp,letterSpacing=3.sp,color=Color.Gray) }
    TextButton(onClick={english=!english}) { Text(if(english) "中文" else "ENGLISH") }
   }
   Spacer(Modifier.height(26.dp))
   if(!entered) {
    Spacer(Modifier.weight(1f)); GlassCard(Modifier.fillMaxWidth()) {
     Text(if(english) "Welcome back" else "欢迎回来",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Color(0xFF202A45)); Text(if(english) "Enter your access code" else "输入访问卡密以进入工具箱",color=Color.Gray,fontSize=14.sp)
     Spacer(Modifier.height(18.dp)); OutlinedTextField(value=code,onValueChange={code=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text(if(english) "Access code" else "访问卡密")},visualTransformation=PasswordVisualTransformation(),shape=RoundedCornerShape(18.dp))
     Spacer(Modifier.height(14.dp)); Button(onClick={ if(code.isNotBlank()) { loading=true } },modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp)) { Text(if(english) "Continue" else "欢迎进入") }
     Text(if(english) "Connect your card-code service API to enable real verification." else "当前尚未接入卡密服务器 API，暂不能进行真实卡密验证。",fontSize=11.sp,color=Color.Gray)
    }; Spacer(Modifier.weight(1.3f))
   } else {
    Text(if(english) "Your workspace" else "你的工具空间",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color(0xFF202A45)); Text(if(english) "Useful tools, beautifully arranged" else "简洁 · 高效 · 安全",color=Color.Gray)
    Spacer(Modifier.height(20.dp)); LazyVerticalGrid(columns=GridCells.Fixed(2),verticalArrangement=Arrangement.spacedBy(14.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),modifier=Modifier.weight(1f)) {
     val items=listOf(("文件加密","File encryption","🔐"),("文件解密","File decryption","🔓"),("图片工具","Image tools","▧"),("PDF 工具","PDF tools","▤"),("设备信息","Device info","⌁"),("二维码","QR code","▦"))
     items(items.size){i-> GlassCard(Modifier.fillMaxWidth().height(132.dp).clickable{page=items[i].first}) { Text(items[i].third,fontSize=26.sp); Spacer(Modifier.height(10.dp)); Text(if(english)items[i].second else items[i].first,fontWeight=FontWeight.SemiBold,color=Color(0xFF26314D)) } }
    }
    GlassCard(Modifier.fillMaxWidth().padding(bottom=18.dp).clickable{entered=false;page="home"}) { Text(if(english) "Log out" else "退出登录",fontWeight=FontWeight.SemiBold,color=Color(0xFF9A4F5C),modifier=Modifier.fillMaxWidth()) }
   }
  }
  if(loading) LaunchedEffect(Unit) { delay(450); loading=false; entered=true }
  if(loading) Box(Modifier.fillMaxSize().background(Color(0xFFF3F5FB)),contentAlignment=Alignment.Center) { Column(Modifier.fillMaxWidth().padding(36.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text("VANTA",fontSize=30.sp,fontWeight=FontWeight.Black); var p by remember { mutableFloatStateOf(0f) }; LaunchedEffect(Unit){ while(p<1f){p=(p+0.025f).coerceAtMost(1f);delay(12)} }; Spacer(Modifier.height(25.dp)); LinearProgressIndicator(progress={p},modifier=Modifier.fillMaxWidth(),color=Color(0xFF687EB9)); Text("${(p*100).toInt()}%",modifier=Modifier.padding(top=10.dp)) } }
  if(entered && page!="home") AlertDialog(onDismissRequest={page="home"},title={Text(page)},text={Text(if(page.contains("加密")||page.contains("encryption")) "文件加密模块将在下一阶段接入 Android 文件选择器与 AES-256-GCM。" else "此工具模块尚在开发中。")},confirmButton={TextButton(onClick={page="home"}){Text("OK")}})
 }
 }
}

@Composable fun GlassCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit){ Column(modifier.clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha=.68f)).border(1.dp,Color.White.copy(alpha=.9f),RoundedCornerShape(26.dp)).padding(18.dp),content=content) }
