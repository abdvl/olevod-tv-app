package com.olevod.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text

/** Sections own stable keys and arbitrary row content, so player controls can be added independently. */
internal data class SettingsEntry(val key: String, val content: @Composable () -> Unit)
internal data class SettingsSection(val key: String, val title: String, val entries: List<SettingsEntry>)

@Composable
internal fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var installMessage by remember { mutableStateOf<String?>(null) }
    var pendingPermission by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val entry = remember { FocusRequester() }
    val page = LocalPageFocus.current
    DisposableEffect(page) {
        page?.enter = { entry.requestFocus() }
        onDispose { page?.enter = null }
    }
    fun install() {
        val ready = state as? UpdateState.Ready ?: return
        if (!ready.file.isFile) { installMessage = "安装包缓存已清理，请重新检查并下载"; return }
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", ready.file)
            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            installMessage = "请在系统页面确认安装；如果取消，可点击重试安装"
        } catch (_: ActivityNotFoundException) { installMessage = "当前设备未提供安装程序" }
        catch (_: SecurityException) { installMessage = "系统禁止安装，请检查安装权限" }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (pendingPermission) {
            pendingPermission = false
            if (context.packageManager.canRequestPackageInstalls()) install()
            else installMessage = "尚未允许安装，请点击“允许安装”重试"
        }
    }
    fun requestInstall() {
        installMessage = null
        if (context.packageManager.canRequestPackageInstalls()) install()
        else try {
            pendingPermission = true
            permission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
        } catch (_: ActivityNotFoundException) {
            pendingPermission = false
            installMessage = "请在电视系统设置中允许欧乐 TV 安装未知来源应用，再返回重试"
        } catch (_: SecurityException) {
            pendingPermission = false
            installMessage = "系统限制了安装授权，请在电视系统设置中检查"
        }
    }
    // Download completion launches Android's user-confirmed installer once, never on page re-entry.
    var handledFile by remember(vm) { mutableStateOf((state as? UpdateState.Ready)?.file) }
    LaunchedEffect(state) {
        if (state is UpdateState.Downloading) handledFile = null
        val ready = state as? UpdateState.Ready
        if (ready != null && handledFile != ready.file) { handledFile = ready.file; requestInstall() }
    }
    val sections = listOf(SettingsSection("application", "应用", listOf(SettingsEntry("updates") {
        Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("检查新版本", color = White, fontSize = 21.sp)
            Text("当前版本  ${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 15.sp)
            val message = when (val value = state) {
                UpdateState.Idle -> "从 GitHub 获取最新正式版本"
                UpdateState.Checking -> "正在检查新版本…"
                UpdateState.Current -> "已是最新版本"
                is UpdateState.Available -> "发现新版本 ${value.release.tag} · ${value.release.size / (1024 * 1024)} MB"
                is UpdateState.Downloading -> "正在下载 ${value.percent}%${if (value.percent == 100) " · 正在校验安装包" else ""}"
                is UpdateState.Ready -> "安装包已就绪"
                is UpdateState.Error -> value.message
            }
            Text(message, color = if (state is UpdateState.Error) TvDesign.error else Green, fontSize = 16.sp,
                modifier = Modifier.testTag("update-status"))
            installMessage?.let { Text(it, color = Muted, fontSize = 14.sp) }
            val busy = state is UpdateState.Checking || state is UpdateState.Downloading
            val label = when (state) {
                is UpdateState.Available -> "下载并安装"
                is UpdateState.Ready -> if (context.packageManager.canRequestPackageInstalls()) "重试安装" else "允许安装"
                is UpdateState.Checking -> "正在检查…"
                is UpdateState.Downloading -> "正在下载…"
                is UpdateState.Error -> "重新检查"
                else -> "检查新版本"
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Keep the action focusable while busy so D-pad focus never drops off the page.
                TvAction(label, modifier = Modifier.focusRequester(entry).restoreContentFocus("updates", entry)
                    .testTag("check-update").focusProperties { up = page?.header ?: FocusRequester.Default }) {
                    if (!busy) {
                        installMessage = null
                        when (val value = state) {
                            is UpdateState.Available -> vm.download(value.release)
                            is UpdateState.Ready -> requestInstall()
                            else -> vm.check()
                        }
                    }
                }
                if (state is UpdateState.Ready || state is UpdateState.Available) TvAction("重新检查") {
                    installMessage = null; vm.check()
                }
            }
            (state as? UpdateState.Available)?.release?.notes?.takeIf { it.isNotBlank() }?.let {
                Text("更新说明", color = White, fontSize = 16.sp)
                Text(it, color = Muted, fontSize = 14.sp, lineHeight = 21.sp, maxLines = 6,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    })))
    SettingsContent(sections)
}

@Composable
internal fun SettingsContent(sections: List<SettingsSection>) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = TvDesign.safeHorizontal).testTag("settings-page"),
        contentPadding = PaddingValues(top = 18.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item("heading") { SectionHeading("设置") }
        sections.forEach { section ->
            item("section:${section.key}") { Text(section.title, color = Muted, fontSize = 15.sp) }
            items(section.entries, key = { "${section.key}:${it.key}" }) { it.content() }
        }
    }
}
