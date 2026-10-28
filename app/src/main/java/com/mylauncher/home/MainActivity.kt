package com.mylauncher.home

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class AppEntry(val title: String, val packageName: String)
private val accents = listOf(Color(0xFF8BB5F1), Color(0xFFBCA6F7), Color(0xFF8ED2BB), Color(0xFFF4B68A))
private val background = Color(0xFF101215)

class MainActivity : ComponentActivity() {
    private fun installedApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        return packageManager.queryIntentActivities(intent, 0)
            .map { AppEntry(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }
            .distinctBy { it.packageName }.sortedBy { it.title.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("mylauncher", MODE_PRIVATE)
        setContent {
            var page by remember { mutableStateOf("home") }
            var panel by remember { mutableStateOf(false) }
            var columns by remember { mutableIntStateOf(prefs.getInt("columns", 4)) }
            var accentIndex by remember { mutableIntStateOf(prefs.getInt("accent", 0)) }
            var labels by remember { mutableStateOf(prefs.getBoolean("labels", true)) }
            var search by remember { mutableStateOf("") }
            val apps = remember { installedApps() }
            val accent = accents[accentIndex]
            BackHandler(page != "home" || panel) {
                if (panel) panel = false else page = "home"
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = background, surface = Color(0xFF20242A))) {
                Column(
                    Modifier.fillMaxSize().background(background)
                        .pointerInput(page) {
                            var drag = 0f
                            detectVerticalDragGestures(
                                onVerticalDrag = { _, delta -> drag += delta },
                                onDragEnd = {
                                    if (drag > 100) panel = true
                                    if (drag < -100 && page == "home") page = "apps"
                                    drag = 0f
                                }
                            )
                        }
                        .statusBarsPadding().navigationBarsPadding().padding(20.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("MY LAUNCHER", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent)
                        TextButton(onClick = { page = "settings"; panel = false }) { Text("Customize") }
                    }
                    Spacer(Modifier.height(18.dp))
                    if (panel) {
                        Text("Quick panel", fontSize = 28.sp, color = Color.White)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { startActivity(Intent(Settings.ACTION_SETTINGS)) }) { Text("System settings") }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { panel = false }) { Text("Close panel") }
                    } else when (page) {
                        "settings" -> {
                            Text("Make it yours", fontSize = 30.sp, color = Color.White)
                            Spacer(Modifier.height(20.dp))
                            Text("Accent color", color = Color.White)
                            Row {
                                accents.forEachIndexed { index, color ->
                                    Box(Modifier.padding(end = 12.dp, top = 12.dp).size(42.dp)
                                        .background(color, RoundedCornerShape(14.dp))
                                        .clickable { accentIndex = index; prefs.edit().putInt("accent", index).apply() })
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text("Grid columns: $columns", color = Color.White)
                            Slider(value = columns.toFloat(), onValueChange = {
                                columns = it.toInt().coerceIn(3, 6)
                                prefs.edit().putInt("columns", columns).apply()
                            }, valueRange = 3f..6f, steps = 2)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Show labels", color = Color.White)
                                Spacer(Modifier.weight(1f))
                                Switch(checked = labels, onCheckedChange = {
                                    labels = it; prefs.edit().putBoolean("labels", it).apply()
                                })
                            }
                            Button(onClick = { page = "home" }) { Text("Done") }
                        }
                        "apps" -> {
                            Text("All apps", fontSize = 30.sp, color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            BasicTextField(value = search, onValueChange = { search = it },
                                singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 18.sp),
                                modifier = Modifier.fillMaxWidth().background(Color(0xFF292D33), RoundedCornerShape(14.dp)).padding(14.dp),
                                decorationBox = { inner ->
                                    Box {
                                        if (search.isEmpty()) Text("Search apps", color = Color.LightGray)
                                        inner()
                                    }
                                })
                            Spacer(Modifier.height(16.dp))
                            LazyVerticalGrid(columns = GridCells.Fixed(columns), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(apps.filter { it.title.contains(search, ignoreCase = true) }) { app ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.clickable {
                                            packageManager.getLaunchIntentForPackage(app.packageName)?.let { startActivity(it) }
                                        }) {
                                        Box(Modifier.size(52.dp).background(accent.copy(alpha = 0.18f), RoundedCornerShape(18.dp)),
                                            contentAlignment = Alignment.Center) {
                                            Text(app.title.take(1).uppercase(), color = accent, fontSize = 24.sp)
                                        }
                                        if (labels) Text(app.title, color = Color.White, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }
                        }
                        else -> {
                            Spacer(Modifier.weight(1f))
                            Text("Your space.", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
                            Text("Clean. Personal. Yours.", color = Color.LightGray, fontSize = 15.sp)
                            Spacer(Modifier.height(32.dp))
                            Button(onClick = { page = "apps" }, modifier = Modifier.fillMaxWidth()) { Text("Open app drawer") }
                            Text("Swipe up for apps · Swipe down for quick panel", color = Color.Gray, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (page != "home" && !panel) {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { page = "home" }) { Text("Home") }
                    }
                }
            }
        }
    }
}
