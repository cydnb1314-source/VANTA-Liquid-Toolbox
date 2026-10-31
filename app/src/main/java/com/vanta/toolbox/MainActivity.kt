package com.vanta.toolbox

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { VantaApp() } }
}

private enum class FileMode { ENCRYPT, DECRYPT }

@Composable fun VantaApp() {
 val context = LocalContext.current
 val scope = rememberCoroutineScope()
 var english by remember { mutableStateOf(false) }
 var entered by remember { mutableStateOf(false) }
 var loading by remember { mutableStateOf(false) }
 var progress by remember { mutableFloatStateOf(0f) }
 var page by remember { mutableStateOf("home") }
 var mode by remember { mutableStateOf(FileMode.ENCRYPT) }
 var password by remember { mutableStateOf("") }
 var showPassword by remember { mutableStateOf(false) }
 var selectedUri by remember { mutableStateOf<Uri?>(null) }
 var outputName by remember { mutableStateOf("secured.vnta") }
 var message by remember { mutableStateOf("") }
 var busy by remember { mutableStateOf(false) }

 val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if (uri != null) { selectedUri = uri; showPassword = true }
 }
 val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
  val source = selectedUri
  if (uri != null && source != null) {
   val pass = password
   busy = true; showPassword = false
   scope.launch {
    val result = withContext(Dispatchers.IO) { runCatching { processFile(context, source, uri, pass, mode) } }
    busy = false
    message = if (result.isSuccess) (if (english) "Finished. Output saved." else "处理完成，文件已保存。") else (if (english) "Failed: " else "处理失败：") + (result.exceptionOrNull()?.message ?: "unknown")
    password = ""; selectedUri = null
   }
  }
 }
 val bg = Brush.linearGradient(listOf(Color(0xFFF8FAFF),Color(0xFFE6ECFA),Color(0xFFF7F4FF)))
 MaterialTheme(colorScheme=lightColorScheme(primary=Color(0xFF596FAF),background=Color(0xFFF3F5FB))) {
  Box(Modifier.fillMaxSize().background(bg).systemBarsPadding()) {
   Column(Modifier.fillMaxSize().padding(horizontal=22.dp)) {
    Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
     Column { Text("VANTA",fontSize=28.sp,fontWeight=FontWeight.Black,color=Color(0xFF202A45)); Text("LIQUID TOOLBOX",fontSize=10.sp,letterSpacing=3.sp,color=Color.Gray) }
     TextButton(onClick={english=!english}) { Text(if(english) "中文" else "ENGLISH") }
    }
    Spacer(Modifier.height(24.dp))
    if (!entered) {
     Spacer(Modifier.weight(1f))
     GlassCard(Modifier.fillMaxWidth()) {
      Text(if(english) "Welcome" else "欢迎使用 VANTA",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Color(0xFF202A45))
      Text(if(english) "Your private toolbox is ready." else "你的工具空间已经准备就绪。",color=Color.Gray,fontSize=14.sp)
      Spacer(Modifier.height(18.dp))
      Button(onClick={loading=true;progress=0f},modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(18.dp)) { Text(if(english) "Enter toolbox" else "欢迎进入") }
      Text(if(english) "Test build · No account or access code required" else "测试版 · 无需账号或卡密，直接进入",fontSize=11.sp,color=Color.Gray)
     }
     Spacer(Modifier.weight(1.2f))
    } else {
     Text(if(english) "Your workspace" else "你的工具空间",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color(0xFF202A45))
     Text(if(english) "Private tools · On-device processing" else "本地处理 · 文件不上传服务器",color=Color.Gray)
     Spacer(Modifier.height(18.dp))
     LazyVerticalGrid(columns=GridCells.Fixed(2),verticalArrangement=Arrangement.spacedBy(13.dp),horizontalArrangement=Arrangement.spacedBy(13.dp),modifier=Modifier.weight(1f)) {
      val entries = listOf(
       Triple("文件加密","File encryption","🔐"), Triple("文件解密","File decryption","🔓"),
       Triple("设备信息","Device info","⌁"), Triple("文件压缩","File ZIP","▣")
      )
      items(entries.size) { i ->
       GlassCard(Modifier.fillMaxWidth().height(132.dp).clickable {
        when(i) {
         0 -> { mode=FileMode.ENCRYPT; outputName="secured.vnta"; openFile.launch(arrayOf("*/*")) }
         1 -> { mode=FileMode.DECRYPT; outputName="restored_file"; openFile.launch(arrayOf("*/*")) }
         2 -> page="device"
         else -> message=if(english) "ZIP tool is scheduled for the next build." else "ZIP 压缩功能将在下一版加入。"
        }
       }) {
        Text(entries[i].third,fontSize=27.sp)
        Spacer(Modifier.height(10.dp))
        Text(if(english)entries[i].second else entries[i].first,fontWeight=FontWeight.SemiBold,color=Color(0xFF26314D))
       }
      }
     }
     GlassCard(Modifier.fillMaxWidth().padding(bottom=16.dp).clickable{entered=false;page="home"}) {
      Text(if(english) "Log out" else "退出工具箱",fontWeight=FontWeight.SemiBold,color=Color(0xFF9A4F5C),modifier=Modifier.fillMaxWidth())
     }
    }
   }
   if(loading) {
    LaunchedEffect(Unit) { while(progress<1f) { progress=(progress+0.025f).coerceAtMost(1f); delay(25) }; delay(180); loading=false; entered=true }
    Box(Modifier.fillMaxSize().background(Color(0xFFF3F5FB)),contentAlignment=Alignment.Center) {
     Column(Modifier.fillMaxWidth().padding(36.dp),horizontalAlignment=Alignment.CenterHorizontally) {
      Text("VANTA",fontSize=32.sp,fontWeight=FontWeight.Black,color=Color(0xFF202A45))
      Text(if(english) "Preparing your workspace" else "正在准备工具空间",color=Color.Gray)
      Spacer(Modifier.height(24.dp))
      LinearProgressIndicator(progress={progress},modifier=Modifier.fillMaxWidth().height(7.dp),color=Color(0xFF687EB9),trackColor=Color.White)
      Text("${(progress*100).toInt()}%",modifier=Modifier.padding(top=12.dp),fontWeight=FontWeight.SemiBold)
     }
    }
   }
   if(showPassword) AlertDialog(onDismissRequest={showPassword=false;password=""},title={Text(if(mode==FileMode.ENCRYPT) if(english)"Encrypt file" else "设置加密密码" else if(english)"Decrypt file" else "输入解密密码")},text={
    Column { OutlinedTextField(value=password,onValueChange={password=it},label={Text(if(english)"Password" else "密码")},singleLine=true); Text(if(english)"Use the same password to decrypt. Keep it safe." else "解密时必须使用相同密码，请妥善保管。",fontSize=12.sp,color=Color.Gray) }
   },confirmButton={TextButton(enabled=password.length>=8,onClick={
    val name=if(mode==FileMode.ENCRYPT) "secured.vnta" else "decrypted_file"
    outputName=name; saveFile.launch(name)
   }){Text(if(english)"Choose save location" else "选择保存位置")}},dismissButton={TextButton(onClick={showPassword=false}){Text(if(english)"Cancel" else "取消")}})
   if(page=="device") AlertDialog(onDismissRequest={page="home"},title={Text(if(english)"Device information" else "设备信息")},text={Text("Android ${android.os.Build.VERSION.RELEASE}\nSDK ${android.os.Build.VERSION.SDK_INT}\n${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")},confirmButton={TextButton(onClick={page="home"}){Text("OK")}})
   if(busy) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha=.15f)),contentAlignment=Alignment.Center){CircularProgressIndicator()}
   if(message.isNotBlank()) AlertDialog(onDismissRequest={message=""},text={Text(message)},confirmButton={TextButton(onClick={message=""}){Text("OK")}})
  }
 }
}

private fun processFile(context: Context, input: Uri, output: Uri, password: String, mode: FileMode) {
 require(password.length >= 8) { "Password must be at least 8 characters." }
 val resolver=context.contentResolver
 val random=SecureRandom()
 if(mode==FileMode.ENCRYPT) {
  val salt=ByteArray(16).also(random::nextBytes); val iv=ByteArray(12).also(random::nextBytes)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding")
  cipher.init(Cipher.ENCRYPT_MODE,deriveKey(password,salt),GCMParameterSpec(128,iv))
  resolver.openInputStream(input).use { src -> requireNotNull(src){"Cannot read input"}; resolver.openOutputStream(output).use { raw ->
   requireNotNull(raw){"Cannot write output"}; DataOutputStream(raw).use { out ->
    out.write(byteArrayOf(0x56,0x4E,0x54,0x31)); out.write(salt); out.write(iv)
    CipherOutputStream(out,cipher).use { encrypted -> src.copyTo(encrypted,64*1024) }
   }
  } }
 } else {
  resolver.openInputStream(input).use { raw ->
   requireNotNull(raw){"Cannot read input"}; val data=DataInputStream(raw)
   val magic=ByteArray(4); data.readFully(magic); require(magic.contentEquals(byteArrayOf(0x56,0x4E,0x54,0x31))){"Not a VANTA encrypted file"}
   val salt=ByteArray(16); val iv=ByteArray(12); data.readFully(salt); data.readFully(iv)
   val cipher=Cipher.getInstance("AES/GCM/NoPadding")
   cipher.init(Cipher.DECRYPT_MODE,deriveKey(password,salt),GCMParameterSpec(128,iv))
   resolver.openOutputStream(output).use { dest -> requireNotNull(dest){"Cannot write output"}; CipherInputStream(data,cipher).use { decrypted -> decrypted.copyTo(dest,64*1024) } }
  }
 }
}
private fun deriveKey(password:String,salt:ByteArray):javax.crypto.SecretKey {
 val spec=PBEKeySpec(password.toCharArray(),salt,150000,256)
 return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).let { javax.crypto.spec.SecretKeySpec(it.encoded,"AES") }
}

@Composable fun GlassCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Column(modifier.clip(RoundedCornerShape(26.dp)).background(Color.White.copy(alpha=.68f)).border(1.dp,Color.White.copy(alpha=.9f),RoundedCornerShape(26.dp)).padding(18.dp),content=content)
}
