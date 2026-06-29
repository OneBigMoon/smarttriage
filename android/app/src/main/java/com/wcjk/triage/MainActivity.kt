package com.wcjk.triage

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wcjk.triage.ui.SetupActivity
import com.wcjk.triage.data.websocket.TriageSocket
import com.wcjk.triage.util.Config
import com.wcjk.triage.util.NetworkUtils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

/**
 * Main Activity — thin WebView shell.
 * Receives template + data from server via Socket.IO, renders in WebView.
 * Native controls: screen on/off, restart, volume, rotation, upgrade.
 */
class MainActivity : AppCompatActivity(), TriageSocket.Listener {

    private lateinit var webView: WebView
    private val handler = Handler(Looper.getMainLooper())
    private var timer: Timer? = null
    private var tick = 0

    // Current template state
    private var currentTemplateId: String? = null
    private var currentTemplateHtml: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If no server configured, go to setup
        if (Config.get(Config.KEY_IP).isEmpty()) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_main)
        setupWebView()
        startTimer()
        handler.postDelayed({
            TriageSocket.getInstance().let { it.listener = this; it.connect() }
        }, 2000)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = findViewById(R.id.webview)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Inject data update function
                view?.evaluateJavascript(
                    "if(typeof window.onDataUpdate === 'function') window.onDataUpdate();", null
                )
            }
        }
        webView.webChromeClient = WebChromeClient()
        // Long press to open settings
        webView.setOnLongClickListener {
            showSettingsDialog()
            true
        }
        webView.isHapticFeedbackEnabled = false
        // Load default blank page
        webView.loadDataWithBaseURL(null, DEFAULT_HTML, "text/html", "utf-8", null)
    }

    /** Push data to WebView — calls window.updateData(data) in JS */
    fun pushData(data: JSONObject) {
        val escaped = data.toString()
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        handler.post {
            webView.evaluateJavascript("if(typeof window.updateData==='function') window.updateData($escaped);", null)
        }
    }

    /** Load a web template into WebView */
    fun loadTemplate(html: String, css: String?, js: String?) {
        val fullHtml = buildString {
            append("<!DOCTYPE html><html><head>")
            append("<meta charset=\"utf-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,user-scalable=no\">")
            if (!css.isNullOrBlank()) append("<style>$css</style>")
            append("</head><body>")
            append(html)
            if (!js.isNullOrBlank()) append("<script>$js</script>")
            append("</body></html>")
        }
        currentTemplateHtml = fullHtml
        handler.post {
            webView.loadDataWithBaseURL(null, fullHtml, "text/html", "utf-8", null)
        }
    }

    private fun startTimer() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                val now = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                if (now == Config.get(Config.KEY_POWER_ON, "00:00:00")) handler.post { screenOn() }
                if (now == Config.get(Config.KEY_POWER_OFF, "23:59:59")) handler.post { screenOff() }
                tick++; if (tick % 20 == 4) handler.post { checkNet() }
            }
        }, 0, 1000)
    }

    // ── Screen Control ──

    private fun screenOn() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        TriageSocket.getInstance().sendCmd(JSONObject().put("cmd", "on"))
    }

    private fun screenOff() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        TriageSocket.getInstance().sendCmd(JSONObject().put("cmd", "off"))
    }

    private fun setVolume() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val vol = Config.get(Config.KEY_VOLUME, "9").toIntOrNull() ?: 9
        am.setStreamVolume(AudioManager.STREAM_MUSIC, vol * max / 9, 0)
    }

    private fun checkNet() {
        val ok = TriageSocket.getInstance().isConnected
        // Could show/hide a connection indicator overlay
    }

    private fun applyRotation(rotation: String) {
        when (rotation) {
            "auto" -> requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            "0" -> requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "270" -> requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "180" -> requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "90" -> requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        }
    }

    private fun reboot() {
        try {
            @Suppress("DEPRECATION")
            (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager).reboot(null)
        } catch (e: Exception) {
            Toast.makeText(this, "重启失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSettingsDialog() {
        val items = arrayOf("重新配置服务器", "重启", "关于")
        AlertDialog.Builder(this)
            .setTitle("设置")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        // Go back to setup
                        Config.set(Config.KEY_IP, "")
                        startActivity(Intent(this, SetupActivity::class.java))
                        finish()
                    }
                    1 -> reboot()
                    2 -> {
                        val no = Config.get(Config.KEY_NO, "未分配")
                        val ip = Config.get(Config.KEY_IP, "未配置")
                        Toast.makeText(this, "终端: $no\n服务器: $ip", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    // ── Socket.IO Callbacks ──

    override fun onConnected() { checkNet() }

    override fun onConnectError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        handler.postDelayed({ TriageSocket.getInstance().connect() }, 20000)
    }

    override fun onConfig(content: JSONObject) {
        // Update basic config
        for (k in listOf(Config.KEY_NO, Config.KEY_NAME, Config.KEY_SOURCE,
            Config.KEY_POWER_ON, Config.KEY_POWER_OFF, Config.KEY_VOLUME,
            Config.KEY_HORSELAMP, Config.KEY_TITLE, Config.KEY_ROTATION)) {
            if (content.has(k) && !content.isNull(k)) Config.set(k, content.optString(k))
        }

        // Apply rotation
        val rot = content.optString("rotation", "")
        if (rot.isNotEmpty()) applyRotation(rot)

        // Apply volume
        setVolume()

        // Apply template
        val tmpl = content.optJSONObject("template")
        if (tmpl != null) {
            val kind = tmpl.optString("kind", "native")
            val tmplId = tmpl.optString("url", "")

            if (kind == "web") {
                // Web template — load HTML into WebView
                val html = tmpl.optString("html", "")
                val css = tmpl.optString("css", "")
                val js = tmpl.optString("js", "")
                if (html.isNotEmpty()) {
                    loadTemplate(html, css, js)
                } else if (tmplId.isNotEmpty()) {
                    // Load from server URL
                    val ip = Config.get(Config.KEY_IP, BuildConfig.SERVER)
                    val port = Config.get(Config.KEY_PORT)
                    val host = if (port.isNotEmpty()) "http://$ip:$port" else "http://$ip"
                    handler.post { webView.loadUrl("$host$tmplId") }
                }
            }
            // kind="native" → keep current display (no change)
        } else {
            // No template — show default blank page
            if (currentTemplateHtml == null) {
                handler.post {
                    webView.loadDataWithBaseURL(null, DEFAULT_HTML, "text/html", "utf-8", null)
                }
            }
        }
    }

    override fun onCommand(cmd: String) {
        when (cmd) {
            "restart" -> {
                Toast.makeText(this, "重启中...", Toast.LENGTH_SHORT).show()
                handler.postDelayed({ reboot() }, 1500)
            }
            "on" -> screenOn()
            "off" -> screenOff()
            "upgrade" -> { /* TODO: check for upgrade */ }
            "cleardata" -> {
                handler.post {
                    webView.evaluateJavascript("if(typeof window.clearData==='function') window.clearData();", null)
                }
            }
        }
    }

    override fun onData(data: JSONObject) {
        pushData(data)
    }

    override fun onServerError(code: String) {
        Toast.makeText(this, "服务器错误: $code", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        TriageSocket.getInstance().disconnect()
        timer?.cancel()
        webView.destroy()
    }

    companion object {
        private const val DEFAULT_HTML = """<!DOCTYPE html>
<html><head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1,user-scalable=no">
<style>
body { margin:0; padding:0; background:#0a0a1a; color:#fff; font-family:sans-serif;
       display:flex; align-items:center; justify-content:center; height:100vh; }
.waiting { text-align:center; font-size:24px; color:#666; }
</style>
</head><body>
<div class="waiting">等待模板配置...</div>
<script>
// Called by Android when new data arrives
function updateData(data) {
    console.log('Data received:', JSON.stringify(data));
    // Override this function in your template to handle data
}
// Called by Android to clear displayed data
function clearData() {
    console.log('Data cleared');
    // Override this function in your template to clear display
}
</script>
</body></html>"""
    }
}
