package com.vanta.toolbox

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 9, 31)
        window.navigationBarColor = android.graphics.Color.rgb(3, 9, 31)
        setContent { VantaApp() }
    }
}

private enum class FileMode { ENCRYPT, DECRYPT }

private val Navy = Color(0xFF03091F)
private val Blue = Color(0xFF167BFF)
private val Cyan = Color(0xFF38D9FF)
private val Violet = Color(0xFF875BFF)
private val Muted = Color(0xFF9FB5E8)

@Composable
fun VantaApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var english by remember { mutableStateOf(false) }
    var entered by remember { mutableStateOf(false) }
    var page by remember { mutableStateOf("home") }
    var tab by remember { mutableStateOf("home") }
    var mode by remember { mutableStateOf(FileMode.ENCRYPT) }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { selectedUri = uri; showPassword = true }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val source = selectedUri
        if (uri != null && source != null) {
            busy = true
            showPassword = false
            val pass = password
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching { processFile(context, source, uri, pass, mode) }
                }
                busy = false
                message = if (result.isSuccess) {
                    if (english) "Completed. Your file has been saved." else "处理完成，文件已保存。"
                } else {
                    (if (english) "Failed: " else "处理失败：") + (result.exceptionOrNull()?.message ?: "Unknown error")
                }
                password = ""
                selectedUri = null
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = Cyan, secondary = Violet, background = Navy, surface = Color(0xFF101D43),
        onPrimary = Navy, onBackground = Color.White, onSurface = Color.White
    )) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF061943), Navy, Color(0xFF090D2D))))) {
            CosmicBackdrop()
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                TopBar(english, { english = !english }, page != "home" && page != "dashboard") {
                    page = "dashboard"; tab = "home"
                }
                when (page) {
                    "home" -> WelcomeScreen(english) {
                        entered = true
                        page = "dashboard"
                        tab = "home"
                    }
                    "dashboard" -> Dashboard(english, onEncrypt = { mode = FileMode.ENCRYPT; openFile.launch(arrayOf("*/*")) },
                        onDecrypt = { mode = FileMode.DECRYPT; openFile.launch(arrayOf("*/*")) },
                        onTools = { page = "tools"; tab = "tools" }, onSettings = { page = "profile"; tab = "profile" })
                    "file" -> FileScreen(english, mode, onPick = { openFile.launch(arrayOf("*/*")) })
                    "tools" -> ToolsScreen(english, onDevice = { page = "device" }, onZip = {
                        message = if (english) "ZIP compression is not implemented in this build yet." else "ZIP 压缩尚未实现，本版本不会显示为可用功能。"
                    })
                    "profile" -> ProfileScreen(english, onLanguage = { english = !english }, onAbout = { message = "VANTA Liquid Toolbox · 1.0.0\nOn-device file encryption and decryption." })
                    "device" -> DeviceScreen(english)
                    else -> Dashboard(english, {}, {}, {}, {})
                }
                if (page == "dashboard" || page == "tools" || page == "profile") {
                    Spacer(Modifier.height(8.dp))
                    BottomNavigation(tab, english, onSelect = {
                        tab = it
                        page = when (it) { "home" -> "dashboard"; "tools" -> "tools"; else -> "profile" }
                    })
                }
            }

            if (showPassword) {
                AlertDialog(
                    onDismissRequest = { showPassword = false; password = "" },
                    containerColor = Color(0xFF142653),
                    title = { Text(if (mode == FileMode.ENCRYPT) if (english) "Set encryption password" else "设置加密密码" else if (english) "Enter password" else "输入解密密码", color = Color.White) },
                    text = {
                        Column {
                            OutlinedTextField(value = password, onValueChange = { password = it },
                                label = { Text(if (english) "Password (8+ characters)" else "密码（至少 8 位）") },
                                singleLine = true, shape = RoundedCornerShape(16.dp))
                            Text(if (english) "Keep this password safe. It cannot be recovered." else "请妥善保管密码，忘记密码无法恢复文件。", color = Muted, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = password.length >= 8, onClick = {
                            val name = if (mode == FileMode.ENCRYPT) "secured.vnta" else "decrypted_file"
                            saveFile.launch(name)
                        }) { Text(if (english) "Continue" else "继续") }
                    },
                    dismissButton = { TextButton(onClick = { showPassword = false; password = "" }) { Text(if (english) "Cancel" else "取消") } }
                )
            }
            if (busy) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f)), contentAlignment = Alignment.Center) {
                    GlassPanel(Modifier.padding(28.dp)) {
                        CircularProgressIndicator(color = Cyan)
                        Spacer(Modifier.height(14.dp))
                        Text(if (english) "Processing securely…" else "正在安全处理文件…", color = Color.White)
                    }
                }
            }
            if (message.isNotBlank()) AlertDialog(
                onDismissRequest = { message = "" }, containerColor = Color(0xFF142653),
                text = { Text(message, color = Color.White) },
                confirmButton = { TextButton(onClick = { message = "" }) { Text("OK") } }
            )
        }
    }
}

@Composable
private fun CosmicBackdrop() {
    val infinite = rememberInfiniteTransition(label = "ambient")
    val drift by infinite.animateFloat(initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(tween(6500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "drift")
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.offset(x = (drift).dp, y = 90.dp).size(260.dp).align(Alignment.TopEnd)
            .background(Brush.radialGradient(listOf(Color(0x553078FF), Color.Transparent)), CircleShape))
        Box(Modifier.offset(x = (-70).dp, y = 400.dp).size(280.dp)
            .background(Brush.radialGradient(listOf(Color(0x443E3DFF), Color.Transparent)), CircleShape))
        repeat(22) { i ->
            val x = ((i * 47) % 360).dp
            val y = ((i * 83) % 760).dp
            Box(Modifier.offset(x = x, y = y).size(if (i % 4 == 0) 4.dp else 2.dp)
                .background(Color(0xFF7CCBFF).copy(alpha = if (i % 3 == 0) .8f else .35f), CircleShape))
        }
    }
}

@Composable
private fun TopBar(english: Boolean, onLanguage: () -> Unit, backVisible: Boolean, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (backVisible) Text("‹", Modifier.clickable { onBack() }.padding(end = 14.dp), color = Color.White, fontSize = 32.sp)
        Column(Modifier.weight(1f)) {
            Text("VANTA", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("LIQUID TOOLBOX", color = Muted, fontSize = 9.sp, letterSpacing = 3.sp)
        }
        Surface(onClick = onLanguage, color = Color(0x552B4C8C), shape = CircleShape, border = BorderStroke(1.dp, Color(0x667BAEFF))) {
            Text(if (english) "中文 ⌄" else "EN ⌄", Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun WelcomeScreen(english: Boolean, onEnter: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(190.dp).background(Brush.radialGradient(listOf(Color(0xCC1E9FFF), Color(0x557C4DFF), Color.Transparent)), CircleShape),
            contentAlignment = Alignment.Center) {
            Box(Modifier.size(128.dp).clip(RoundedCornerShape(38.dp)).background(Brush.linearGradient(listOf(Color(0xCC40DFFF), Color(0xAA245BFF), Color(0xBB8B4FFF))))
                .border(2.dp, Color(0xFF9BEAFF), RoundedCornerShape(38.dp)), contentAlignment = Alignment.Center) {
                Text("V", fontSize = 76.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("VANTA", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
        Text("Liquid Toolbox", color = Color(0xFFB8D9FF), fontSize = 20.sp)
        Spacer(Modifier.height(12.dp))
        Text("Powerful · Secure · Elegant", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(58.dp))
        GlowButton(if (english) "Enter toolbox  →" else "立即进入  →", onEnter)
        Spacer(Modifier.height(22.dp))
        Text(if (english) "Private tools · No account required" else "本地工具 · 无需账号 · 无需卡密", color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun Dashboard(english: Boolean, onEncrypt: () -> Unit, onDecrypt: () -> Unit, onTools: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(68.dp).background(Brush.radialGradient(listOf(Color(0xFF39DFFF), Color(0xFF2547B7), Color(0xFF15204D)), CircleShape), contentAlignment = Alignment.Center) {
                    Text("✓", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("VANTA", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Color(0xFF38E6A5), CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text(if (english) "Ready · Protected workspace" else "已就绪 · 安全工作空间", color = Color(0xFFB6D5FF), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(if (english) "Your private space" else "你的专属工具空间", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(if (english) "Files are processed locally on this device." else "文件在本机完成处理，不会上传服务器。", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(22.dp))
        Text(if (english) "CORE FEATURES" else "核心功能", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("▣", if (english) "File encryption" else "文件加密", if (english) "AES-GCM protection" else "安全加密保护", Modifier.weight(1f), onEncrypt)
            FeatureCard("◇", if (english) "File decryption" else "文件解密", if (english) "Restore your files" else "快速解密恢复", Modifier.weight(1f), onDecrypt)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("⌘", if (english) "Tool center" else "工具中心", if (english) "More utilities" else "更多实用工具", Modifier.weight(1f), onTools)
            FeatureCard("⚙", if (english) "Settings" else "个人中心", if (english) "About and language" else "语言与应用信息", Modifier.weight(1f), onSettings)
        }
        Spacer(Modifier.height(18.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Text("VANTA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(if (english) "A quieter, safer way to work with files." else "让文件处理更简单、更安全。", color = Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun FeatureCard(icon: String, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    GlassPanel(modifier.height(145.dp).clickable { onClick() }) {
        Text(icon, fontSize = 28.sp, color = Cyan)
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun FileScreen(english: Boolean, mode: FileMode, onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(170.dp).background(Brush.radialGradient(listOf(Color(0xAA147FFF), Color(0x55834EFF), Color.Transparent)), CircleShape), contentAlignment = Alignment.Center) {
            Text(if (mode == FileMode.ENCRYPT) "▣" else "◇", color = Color(0xFF8DEAFF), fontSize = 90.sp)
        }
        Text(if (mode == FileMode.ENCRYPT) if (english) "Protect your private files" else "保护你的隐私文件" else if (english) "Restore encrypted files" else "恢复你的加密文件",
            color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(if (mode == FileMode.ENCRYPT) "AES-256 · GCM" else "VANTA VNT1 format", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(34.dp))
        GlassPanel(Modifier.fillMaxWidth().clickable { onPick() }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("▤", color = Cyan, fontSize = 26.sp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (english) "Choose a file" else "选择文件", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(if (english) "Browse files on your device" else "从设备中选择需要处理的文件", color = Muted, fontSize = 11.sp)
                }
                Text("›", color = Color.White, fontSize = 26.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Text(if (english) "Security notes" else "安全说明", color = Color.White, fontWeight = FontWeight.Bold)
            Text(if (english) "• Password must contain at least 8 characters.\n• Encryption uses AES-256-GCM.\n• The password is never stored by the app." else "• 密码至少需要 8 位。\n• 使用 AES-256-GCM 加密。\n• 应用不会保存你的密码。", color = Muted, fontSize = 12.sp, lineHeight = 21.sp)
        }
        Spacer(Modifier.weight(1f))
        GlowButton(if (english) "Choose file" else "选择文件开始", onPick)
    }
}

@Composable
private fun ToolsScreen(english: Boolean, onDevice: () -> Unit, onZip: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(if (english) "Tool center" else "工具中心", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text(if (english) "Useful utilities, all in one place" else "更多实用工具，等你来发现", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FeatureCard("▣", if (english) "Device info" else "设备信息", if (english) "Hardware & Android" else "查看设备信息", Modifier.weight(1f), onDevice)
            FeatureCard("▤", if (english) "ZIP archive" else "文件压缩", if (english) "Coming soon" else "尚未开放", Modifier.weight(1f), onZip)
        }
        Spacer(Modifier.height(12.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Text(if (english) "Available now" else "当前可用", color = Color.White, fontWeight = FontWeight.Bold)
            Text(if (english) "File encryption · File decryption · Device information" else "文件加密 · 文件解密 · 设备信息", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfileScreen(english: Boolean, onLanguage: () -> Unit, onAbout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Spacer(Modifier.height(24.dp))
        GlassPanel(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(66.dp).background(Brush.linearGradient(listOf(Blue, Violet)), CircleShape), contentAlignment = Alignment.Center) {
                    Text("V", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(if (english) "VANTA User" else "VANTA 用户", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text("Liquid Toolbox · 1.0.0", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        ProfileRow(if (english) "Language" else "语言设置", if (english) "English / 中文" else "中文 / English", onLanguage)
        Spacer(Modifier.height(10.dp))
        ProfileRow(if (english) "About VANTA" else "关于我们", "Version 1.0.0", onAbout)
        Spacer(Modifier.height(10.dp))
        ProfileRow(if (english) "Privacy" else "隐私说明", if (english) "Local file processing" else "文件仅在本机处理", onAbout)
    }
}

@Composable
private fun ProfileRow(title: String, subtitle: String, onClick: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth().clickable { onClick() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 11.sp)
            }
            Text("›", color = Color.White, fontSize = 24.sp)
        }
    }
}

@Composable
private fun DeviceScreen(english: Boolean) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp)) {
        Text(if (english) "Device information" else "设备信息", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        val rows = listOf(
            (if (english) "Manufacturer" else "制造商") to android.os.Build.MANUFACTURER,
            (if (english) "Model" else "设备型号") to android.os.Build.MODEL,
            ("Android") to android.os.Build.VERSION.RELEASE,
            ("SDK") to android.os.Build.VERSION.SDK_INT.toString(),
            (if (english) "Device" else "设备代号") to android.os.Build.DEVICE
        )
        rows.forEach { (k, v) -> ProfileRow(k, v) {} ; Spacer(Modifier.height(9.dp)) }
    }
}

@Composable
private fun BottomNavigation(selected: String, english: Boolean, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(28.dp))
        .background(Brush.horizontalGradient(listOf(Color(0xCC102A65), Color(0xCC251B65))))
        .border(1.dp, Color(0x6684BFFF), RoundedCornerShape(28.dp))
        .padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(Triple("home", "⌂", if (english) "Home" else "首页"),
            Triple("tools", "⌘", if (english) "Tools" else "工具"),
            Triple("profile", "⚙", if (english) "Me" else "我的")).forEach { (id, icon, label) ->
            Column(Modifier.weight(1f).clickable { onSelect(id) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(icon, color = if (selected == id) Cyan else Muted, fontSize = 21.sp)
                Text(label, color = if (selected == id) Color.White else Muted, fontSize = 10.sp, fontWeight = if (selected == id) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
private fun GlowButton(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(56.dp).shadow(18.dp, RoundedCornerShape(30.dp), ambientColor = Blue, spotColor = Violet)
        .clip(RoundedCornerShape(30.dp)).background(Brush.horizontalGradient(listOf(Cyan, Blue, Violet)))
        .border(1.dp, Color(0xFFB6F5FF), RoundedCornerShape(30.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(25.dp))
        .background(Brush.linearGradient(listOf(Color(0xB3264C86), Color(0xAA101D48), Color(0xAA35266E))))
        .border(1.dp, Color(0x667FBFFF), RoundedCornerShape(25.dp))
        .padding(17.dp), content = content)
}

private fun processFile(context: Context, input: Uri, output: Uri, password: String, mode: FileMode) {
    require(password.length >= 8) { "Password must be at least 8 characters." }
    val resolver = context.contentResolver
    val random = SecureRandom()
    if (mode == FileMode.ENCRYPT) {
        val salt = ByteArray(16).also(random::nextBytes)
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
        resolver.openInputStream(input).use { src ->
            requireNotNull(src) { "Cannot read input" }
            resolver.openOutputStream(output).use { raw ->
                requireNotNull(raw) { "Cannot write output" }
                DataOutputStream(raw).use { out ->
                    out.write(byteArrayOf(0x56, 0x4E, 0x54, 0x31))
                    out.write(salt); out.write(iv)
                    CipherOutputStream(out, cipher).use { encrypted -> src.copyTo(encrypted, 64 * 1024) }
                }
            }
        }
    } else {
        val temp = File.createTempFile("vanta-dec-", ".tmp", context.cacheDir)
        try {
            resolver.openInputStream(input).use { raw ->
                requireNotNull(raw) { "Cannot read input" }
                val data = DataInputStream(raw)
                val magic = ByteArray(4); data.readFully(magic)
                require(magic.contentEquals(byteArrayOf(0x56, 0x4E, 0x54, 0x31))) { "Not a VANTA encrypted file" }
                val salt = ByteArray(16); val iv = ByteArray(12)
                data.readFully(salt); data.readFully(iv)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(password, salt), GCMParameterSpec(128, iv))
                FileOutputStream(temp).use { tempOut ->
                    CipherInputStream(data, cipher).use { decrypted -> decrypted.copyTo(tempOut, 64 * 1024) }
                }
            }
            resolver.openOutputStream(output).use { dest ->
                requireNotNull(dest) { "Cannot write output" }
                FileInputStream(temp).use { it.copyTo(dest, 64 * 1024) }
            }
        } finally { temp.delete() }
    }
}

private fun deriveKey(password: String, salt: ByteArray): javax.crypto.SecretKey {
    val spec = PBEKeySpec(password.toCharArray(), salt, 150000, 256)
    return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec)
        .let { javax.crypto.spec.SecretKeySpec(it.encoded, "AES") }
}
