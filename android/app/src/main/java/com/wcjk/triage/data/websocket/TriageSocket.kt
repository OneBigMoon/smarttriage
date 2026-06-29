package com.wcjk.triage.data.websocket

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.wcjk.triage.BuildConfig
import com.wcjk.triage.TriageApp
import com.wcjk.triage.util.Config
import com.wcjk.triage.util.NetworkUtils
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import java.net.URISyntaxException

class TriageSocket private constructor() {
    interface Listener {
        fun onConnected(); fun onConnectError(msg: String)
        fun onConfig(content: JSONObject); fun onCommand(cmd: String)
        fun onData(data: JSONObject); fun onServerError(code: String)
    }

    private var socket: Socket? = null
    private val handler = Handler(Looper.getMainLooper())
    private var server = ""
    private var hbStarted = false
    var isConnected = false; private set
    var listener: Listener? = null

    private val hbRun = object : Runnable {
        override fun run() {
            if (!hbStarted) return
            socket?.let { s ->
                try {
                    val o = JSONObject().put("type", "HEARTBEAT")
                    o.put("content", JSONObject().put("ip", NetworkUtils.localIp(TriageApp.instance)))
                    s.emit("apiv1_heartbeat", o)
                } catch (_: Exception) {}
            }
            handler.postDelayed(this, 9000)
        }
    }
    private val reconnectRun = Runnable { if (!isConnected) connect() }

    fun connect() {
        try {
            disconnect()
            val ip = Config.get(Config.KEY_IP, BuildConfig.SERVER)
            val port = Config.get(Config.KEY_PORT)
            server = if (port.isNotEmpty()) "$ip:$port" else ip
            val no = Config.get(Config.KEY_NO)
            val path = if (no.isEmpty()) "/socketio?ip=\$ip&mac=\$mac&model=\$model&appversion=\$ver"
            else "/socketio?no=$no&ip=\$ip&mac=\$mac&model=\$model&appversion=\$ver"
            val localIp = NetworkUtils.localIp(TriageApp.instance)
            val mac = NetworkUtils.macAddress()
            val model = BuildConfig.FLAVOR
            val ver = try { TriageApp.instance.packageManager.getPackageInfo(TriageApp.instance.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
            val full = path.replace("\$ip", localIp).replace("\$mac", mac).replace("\$model", model).replace("\$ver", ver)
            val opts = IO.Options(); opts.transports = arrayOf("websocket")
            socket = IO.socket("http://$server$full", opts)
            setupListeners(); socket?.connect()
        } catch (e: URISyntaxException) { listener?.onConnectError("连接参数异常：${e.message}") }
    }

    fun disconnect() {
        handler.removeCallbacks(hbRun); handler.removeCallbacks(reconnectRun)
        hbStarted = false; isConnected = false
        socket?.let { it.off(); it.disconnect() }; socket = null
    }

    fun sendConfig(content: JSONObject) {
        try { socket?.emit("apiv1_message", JSONObject().put("type", "CONFIG").put("content", content)) }
        catch (_: JSONException) {}
    }

    fun sendCmd(content: JSONObject) {
        try { socket?.emit("apiv1_message", JSONObject().put("type", "COMMAND").put("content", content)) }
        catch (_: JSONException) {}
    }

    private fun setupListeners() {
        socket?.let { s ->
            s.on(Socket.EVENT_CONNECT) {
                isConnected = true; handler.removeCallbacks(reconnectRun)
                hbStarted = true; handler.post(hbRun)
                handler.post { listener?.onConnected() }
            }
            s.on(Socket.EVENT_CONNECT_ERROR) { a ->
                val msg = if (a.isNotEmpty()) a[0].toString() else ""
                isConnected = false; hbStarted = false; handler.removeCallbacks(hbRun)
                handler.post { listener?.onConnectError("连接失败 $server：$msg") }
                handler.postDelayed(reconnectRun, 15000)
            }
            s.on("apiv1_message") { a ->
                if (a.isEmpty()) return@on
                val raw = a[0]
                val data = when (raw) {
                    is JSONObject -> raw
                    is String -> try { JSONObject(raw) } catch (_: Exception) { null }
                    else -> null
                } ?: return@on
                dispatch(data)
            }
        }
    }

    private fun dispatch(data: JSONObject) {
        val type = data.optString("type", "")
        val content = data.opt("content")
        if (type == "apiv1_error") {
            val c = content as? JSONObject ?: return
            val code = c.optString("code", "")
            if (code == "11001" || code == "11002") { disconnect(); handler.post { listener?.onServerError(code) } }
            return
        }
        when (type) {
            "CONFIG" -> handler.post { listener?.onConfig(content as? JSONObject ?: return@post) }
            "COMMAND" -> handler.post { listener?.onCommand((content as? JSONObject)?.optString("cmd", "") ?: "") }
            "DATA" -> handler.post { listener?.onData(content as? JSONObject ?: return@post) }
            "ERROR" -> {
                val c = content as? JSONObject ?: return
                val code = c.optString("code", "")
                if (code == "11001" || code == "11002") { disconnect(); handler.post { listener?.onServerError(code) } }
            }
        }
    }

    companion object {
        @Volatile private var inst: TriageSocket? = null
        fun getInstance() = inst ?: synchronized(this) { inst ?: TriageSocket().also { inst = it } }
    }
}
