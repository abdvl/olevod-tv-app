package com.olevod.tv.data

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Release tags accept v0.2 and v0.2.0; preview tags never count as stable updates. */
internal data class ReleaseVersion(val major: Long, val minor: Long, val patch: Long) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion) = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    companion object {
        fun parse(value: String): ReleaseVersion? {
            val match = Regex("^v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(?:\\.(0|[1-9][0-9]*))?(?:\\+[0-9A-Za-z.-]+)?$").matchEntire(value) ?: return null
            return ReleaseVersion(match.groupValues[1].toLongOrNull() ?: return null,
                match.groupValues[2].toLongOrNull() ?: return null,
                match.groupValues[3].ifEmpty { "0" }.toLongOrNull() ?: return null)
        }
    }
}
internal data class AppRelease(val tag: String, val notes: String, val url: String, val size: Long, val sha256: String?)

internal class GitHubUpdateRepository(private val client: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build(),
    private val latestUrl: String = LATEST_URL) {
    companion object {
        const val REPOSITORY = "abdvl/olevod-tv-app"
        const val LATEST_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"
        internal fun parseRelease(json: JSONObject, installed: String): AppRelease? {
            check(!json.optBoolean("draft") && !json.optBoolean("prerelease")) { "最新发布不是正式版本" }
            val tag = json.getString("tag_name")
            val version = ReleaseVersion.parse(tag) ?: error("无法识别发布版本号：$tag")
            val local = ReleaseVersion.parse(installed) ?: error("无法识别本地版本号")
            if (version <= local) return null
            val assets = json.getJSONArray("assets")
            val apks = (0 until assets.length()).map { assets.getJSONObject(it) }
                .filter { it.optString("name").endsWith(".apk", ignoreCase = true) && it.optString("state") == "uploaded" }
            check(apks.size == 1) { "发布中没有唯一的通用 APK，请检查 GitHub 发布附件" }
            val apk = apks.single()
            val url = apk.getString("browser_download_url")
            check(url.startsWith("https://github.com/$REPOSITORY/releases/download/")) { "安装包下载地址不属于官方仓库" }
            val size = apk.getLong("size")
            check(size in 1..512L * 1024 * 1024) { "安装包大小无效" }
            val digest = apk.optString("digest").takeIf { it.isNotEmpty() && it != "null" }
            check(digest == null || Regex("sha256:[0-9a-fA-F]{64}").matches(digest)) { "发布校验信息无效" }
            return AppRelease(tag, json.optString("body").take(12_000), url, size, digest?.substringAfter(':'))
        }
    }
    suspend fun latest(installed: String): AppRelease? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(latestUrl).header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Olevod-TV/$installed").build()
        client.newCall(request).execute().use { response ->
            when (response.code) {
                404 -> error("仓库暂无正式发布版本")
                403, 429 -> error("GitHub 请求受限，请稍后重试")
            }
            check(response.isSuccessful) { "检查失败（HTTP ${response.code}），请稍后重试" }
            currentCoroutineContext().ensureActive()
            parseRelease(JSONObject(response.body?.string() ?: error("GitHub 返回内容为空")), installed)
        }
    }
    suspend fun download(context: Context, release: AppRelease, progress: (Int) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val partial = File(directory, "update.part")
        val target = File(directory, "update.apk")
        target.delete()
        try {
            client.newCall(Request.Builder().url(release.url).build()).execute().use { response ->
                check(response.isSuccessful) { "下载失败（HTTP ${response.code}）" }
                val body = response.body ?: error("安装包内容为空")
                val digest = MessageDigest.getInstance("SHA-256")
                body.byteStream().use { input -> partial.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    var lastPercent = -1
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count == -1) break
                        total += count
                        check(total <= release.size) { "安装包大小不匹配" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        val percent = (total * 100 / release.size).toInt()
                        if (percent != lastPercent) { progress(percent); lastPercent = percent }
                    }
                    check(total == release.size) { "下载不完整，请重试" }
                } }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                check(release.sha256 == null || hash.equals(release.sha256, ignoreCase = true)) { "安装包校验失败，请重新下载" }
            }
            validateApk(context, partial, release)
            check(partial.renameTo(target)) { "无法保存安装包，请检查剩余存储空间" }
            target
        } finally { partial.delete() }
    }
}

/** Also protects older releases that have no GitHub asset digest. Android verifies again on install. */
@Suppress("DEPRECATION")
internal fun validateApk(context: Context, file: File, release: AppRelease) {
    val pm = context.packageManager
    val apk = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
        ?: error("无法读取安装包")
    val installed = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
    check(apk.packageName == context.packageName) { "安装包不属于欧乐 TV" }
    check(PackageInfoCompat.getLongVersionCode(apk) > PackageInfoCompat.getLongVersionCode(installed)) { "安装包内部版本号未递增，无法更新" }
    check(ReleaseVersion.parse(apk.versionName.orEmpty()) == ReleaseVersion.parse(release.tag)) { "安装包版本与发布标签不一致" }
    val currentSigners = installed.signatures?.map { it.toCharsString() }?.toSet().orEmpty()
    val newSigners = apk.signatures?.map { it.toCharsString() }?.toSet().orEmpty()
    check(currentSigners.isNotEmpty() && currentSigners == newSigners) { "安装包签名与本机版本不同，无法覆盖安装；请使用同一签名的正式版" }
    check((apk.applicationInfo?.minSdkVersion ?: Int.MAX_VALUE) <= android.os.Build.VERSION.SDK_INT) { "新版本不支持当前 Android 系统" }
}
