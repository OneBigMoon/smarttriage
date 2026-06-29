package com.wcjk.triage.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.wcjk.triage.MainActivity
import com.wcjk.triage.R
import com.wcjk.triage.util.Config
import org.json.JSONObject

/**
 * First-launch setup screen.
 * Shows when no server is configured. Allows:
 * 1. Manual server IP entry
 * 2. QR code scan (via camera)
 * After configuration, launches MainActivity.
 */
class SetupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already configured, skip to main
        if (Config.get(Config.KEY_IP).isNotEmpty()) {
            startMain()
            return
        }

        setContentView(R.layout.activity_setup)

        val etIp = findViewById<EditText>(R.id.et_server_ip)
        val etPort = findViewById<EditText>(R.id.et_server_port)
        val btnConnect = findViewById<Button>(R.id.btn_connect)
        val btnScan = findViewById<Button>(R.id.btn_scan_qr)

        etPort.setText("7016")

        btnConnect.setOnClickListener {
            val ip = etIp.text.toString().trim()
            val port = etPort.text.toString().trim()
            if (ip.isEmpty()) {
                Toast.makeText(this, "请输入服务器IP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Config.set(Config.KEY_IP, ip)
            Config.set(Config.KEY_PORT, port)
            startMain()
        }

        btnScan.setOnClickListener {
            showQrInputDialog()
        }
    }

    /**
     * QR code scan via manual JSON input (since camera permission adds complexity).
     * In production, you'd integrate a proper QR scanner like ZXing or ML Kit.
     * For now, we support pasting the QR content (JSON) directly.
     */
    private fun showQrInputDialog() {
        val input = EditText(this).apply {
            hint = "粘贴QR码内容 (JSON格式)"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("扫描配网")
            .setMessage("请从管理端获取QR码内容并粘贴到下方。\n格式: {\"server\":\"ip:port\",\"no\":\"BOX001\"}")
            .setView(input)
            .setPositiveButton("连接") { _, _ ->
                val json = input.text.toString().trim()
                try {
                    val config = JSONObject(json)
                    val server = config.optString("server", "")
                    if (server.isEmpty()) {
                        Toast.makeText(this, "无效的配置", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    // Parse server:port
                    val parts = server.split(":")
                    Config.set(Config.KEY_IP, parts[0])
                    if (parts.size > 1) Config.set(Config.KEY_PORT, parts[1])

                    // Parse box number
                    val no = config.optString("no", "")
                    if (no.isNotEmpty()) Config.set(Config.KEY_NO, no)

                    Toast.makeText(this, "配置成功: $server", Toast.LENGTH_SHORT).show()
                    startMain()
                } catch (e: Exception) {
                    Toast.makeText(this, "JSON格式错误: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
