package com.beechatstore.nativeapp

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.File
import java.util.Locale

data class StoreApp(
    val id: String,
    val name: String,
    val developer: String,
    val version: String,
    val downloadUrl: String,
    val iconUrl: String = "",
    val rating: String = "4.0"
)

class MainActivity : AppCompatActivity() {
    private lateinit var root: LinearLayout
    private lateinit var search: EditText
    private lateinit var list: LinearLayout
    private lateinit var status: TextView
    private val apps = mutableListOf<StoreApp>()
    private val downloadIds = mutableMapOf<Long, StoreApp>()
    private val handler = Handler(Looper.getMainLooper())
    private var darkMode = false
    private var downloading = false
    private var pendingInstallUri: Uri? = null

    private val progressPoll = object : Runnable {
        override fun run() {
            val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val iterator = downloadIds.toMap().iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val cursor: Cursor? = dm.query(DownloadManager.Query().setFilterById(entry.key))
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val downloaded = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val downloadStatus = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val percent = if (total > 0) (downloaded * 100 / total).toInt() else -1
                        status.text = if (percent >= 0) {
                            "ဒေါင်းလုဒ်လုပ်နေသည် — $percent% (${formatBytes(downloaded)} / ${formatBytes(total)})"
                        } else {
                            "ဒေါင်းလုဒ်လုပ်နေသည် — ${formatBytes(downloaded)}"
                        }
                        if (downloadStatus == DownloadManager.STATUS_SUCCESSFUL) {
                            downloadIds.remove(entry.key)
                            status.text = "ဒေါင်းလုဒ်ပြီးပါပြီ: ${entry.value.name}"
                            promptInstall(entry.key, entry.value)
                        } else if (downloadStatus == DownloadManager.STATUS_FAILED) {
                            downloadIds.remove(entry.key)
                            status.text = "ဒေါင်းလုဒ် မအောင်မြင်ပါ။ Link ကို စစ်ဆေးပါ။"
                            Toast.makeText(this@MainActivity, "Download မအောင်မြင်ပါ", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        downloadIds.remove(entry.key)
                    }
                }
            }
            if (downloadIds.isNotEmpty()) handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebase()
        buildUi()
        loadApps()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyCBQwK-J4bY0Ol9iEpWu8bYHTLtZTllcGA")
                    .setApplicationId("1:471091789082:web:8f832b97912f4baed3c0a3")
                    .setProjectId("beechatstore")
                    .setStorageBucket("beechatstore.firebasestorage.app")
                    .build()
                FirebaseApp.initializeApp(this, options)
            }
        } catch (_: Exception) {
            // loadApps() displays a useful message if Firebase initialization fails.
        }
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            setBackgroundColor(Color.rgb(16, 24, 39))
        }
        val header = TextView(this).apply {
            text = "🐝 BeeChat Store"
            textSize = 25f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(4))
        }
        val subtitle = TextView(this).apply {
            text = "APK များကို ရှာဖွေ၊ Download လုပ်ပြီး Install လုပ်ပါ"
            textSize = 13f
            setTextColor(Color.rgb(190, 202, 220))
            setPadding(0, 0, 0, dp(14))
        }
        search = EditText(this).apply {
            hint = "App တွေကို ရှာဖွေပါ..."
            singleLine = true
            textSize = 16f
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(Color.WHITE)
            setTextColor(Color.BLACK)
            setHintTextColor(Color.GRAY)
        }
        search.addTextChangedListener(SimpleTextWatcher { renderApps() })

        status = TextView(this).apply {
            text = "App စာရင်းကို ဖွင့်နေသည်..."
            textSize = 13f
            setTextColor(Color.rgb(190, 202, 220))
            setPadding(0, dp(12), 0, dp(8))
        }

        val tools = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }
        val refresh = Button(this).apply {
            text = "Refresh"
            setOnClickListener { loadApps() }
        }
        val theme = Button(this).apply {
            text = "Dark / Light"
            setOnClickListener {
                darkMode = !darkMode
                root.setBackgroundColor(if (darkMode) Color.BLACK else Color.rgb(16, 24, 39))
                renderApps()
            }
        }
        tools.addView(refresh, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        tools.addView(theme, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(header)
        root.addView(subtitle)
        root.addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(status)
        root.addView(tools)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun loadApps() {
        status.text = "App စာရင်းကို Firebase မှ ရယူနေသည်..."
        FirebaseFirestore.getInstance().collection("apps")
            .get()
            .addOnSuccessListener { snapshot ->
                apps.clear()
                for (doc in snapshot.documents) {
                    val name = doc.getString("name")?.trim().orEmpty()
                    val link = doc.getString("link")?.trim().orEmpty()
                    if (name.isNotEmpty() && link.startsWith("https://")) {
                        apps.add(
                            StoreApp(
                                id = doc.id,
                                name = name,
                                developer = doc.getString("dev") ?: "Developer",
                                version = doc.getString("version") ?: "Unknown",
                                downloadUrl = link,
                                iconUrl = doc.getString("iconUrl") ?: "",
                                rating = doc.get("rating")?.toString() ?: "4.0"
                            )
                        )
                    }
                }
                status.text = if (apps.isEmpty()) {
                    "App မတွေ့ပါ။ Firebase ရဲ့ apps collection ထဲမှာ name, version, link ထည့်ထားပါ။"
                } else {
                    "App ${apps.size} ခု ရှိသည်"
                }
                renderApps()
            }
            .addOnFailureListener {
                status.text = "App စာရင်း မရပါ။ Internet/Firebase ခွင့်ပြုချက်နှင့် Firestore Rules ကို စစ်ဆေးပါ။"
                renderApps()
            }
    }

    private fun renderApps() {
        if (!::list.isInitialized) return
        list.removeAllViews()
        val query = if (::search.isInitialized) search.text.toString().trim().lowercase(Locale.ROOT) else ""
        val filtered = apps.filter {
            it.name.lowercase(Locale.ROOT).contains(query) ||
                it.developer.lowercase(Locale.ROOT).contains(query)
        }
        if (filtered.isEmpty()) {
            val empty = TextView(this).apply {
                text = if (apps.isEmpty()) "App စာရင်း မရသေးပါ။ Refresh ကိုနှိပ်ပါ။" else "ရှာဖွေမှုနှင့် ကိုက်ညီသော App မတွေ့ပါ။"
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(0, dp(24), 0, dp(24))
            }
            list.addView(empty)
            return
        }
        filtered.forEach { app ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(if (darkMode) Color.rgb(32, 32, 32) else Color.rgb(31, 43, 62))
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.bottomMargin = dp(10)
                layoutParams = params
            }
            val name = TextView(this).apply {
                text = app.name
                textSize = 17f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            val meta = TextView(this).apply {
                text = "${app.developer}  •  Version ${app.version}  •  ★ ${app.rating}"
                textSize = 12f
                setTextColor(Color.rgb(190, 202, 220))
                setPadding(0, dp(5), 0, dp(10))
            }
            val button = Button(this).apply {
                text = "Download APK"
                setOnClickListener { startDownload(app) }
            }
            card.addView(name)
            card.addView(meta)
            card.addView(button)
            list.addView(card)
        }
    }

    private fun startDownload(app: StoreApp) {
        val uri = runCatching { Uri.parse(app.downloadUrl) }.getOrNull()
        if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank()) {
            Toast.makeText(this, "HTTPS Download Link မှန်ကန်ကြောင်း စစ်ဆေးပါ", Toast.LENGTH_LONG).show()
            return
        }
        try {
            val request = DownloadManager.Request(uri)
                .setTitle(app.name)
                .setDescription("BeeChat Store မှ APK ဒေါင်းလုဒ်လုပ်နေသည်")
                .setMimeType("application/vnd.android.package-archive")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, safeFileName(app.name) + ".apk")
            val id = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            downloadIds[id] = app
            status.text = "ဒေါင်းလုဒ် စတင်ပါပြီ: ${app.name}"
            handler.removeCallbacks(progressPoll)
            handler.post(progressPoll)
        } catch (e: Exception) {
            Toast.makeText(this, "Download စတင်၍ မရပါ: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun promptInstall(downloadId: Long, app: StoreApp) {
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val downloadedUri = dm.getUriForDownloadedFile(downloadId)
        if (downloadedUri != null) {
            openInstaller(downloadedUri)
            return
        }
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), safeFileName(app.name) + ".apk")
        if (file.exists()) {
            openInstaller(FileProvider.getUriForFile(this, "$packageName.fileprovider", file))
        } else {
            Toast.makeText(this, "APK ဖိုင်ကို မတွေ့ပါ။ My Downloads ထဲမှ ပြန်စမ်းပါ။", Toast.LENGTH_LONG).show()
        }
    }

    private fun openInstaller(uri: Uri) {
        pendingInstallUri = uri
        if (android.os.Build.VERSION.SDK_INT >= 26 && !packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, "Install လုပ်ရန် 'Allow from this source' ကို ခွင့်ပြုပါ။", Toast.LENGTH_LONG).show()
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            startActivity(settingsIntent)
            return
        }
        pendingInstallUri = null
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Android Installer ကို ဖွင့်၍မရပါ။", Toast.LENGTH_LONG).show()
        }
    }

    private fun safeFileName(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(70).ifBlank { "download" }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        return String.format(Locale.US, "%.1f MB", kb / 1024)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        if (android.os.Build.VERSION.SDK_INT >= 26 && packageManager.canRequestPackageInstalls()) {
            pendingInstallUri?.let { openInstaller(it) }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(progressPoll)
        super.onDestroy()
    }
}

private class SimpleTextWatcher(private val changed: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { changed() }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
