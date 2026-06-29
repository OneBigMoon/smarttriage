package com.wcjk.triage.util

import android.content.Context
import com.wcjk.triage.TriageApp

object Config {
    private val prefs by lazy { TriageApp.instance.getSharedPreferences("triage", Context.MODE_PRIVATE) }

    fun get(key: String, def: String = ""): String = prefs.getString(key, def) ?: def
    fun set(key: String, v: String) = prefs.edit().putString(key, v).apply()

    const val KEY_IP = "ServerIp"; const val KEY_PORT = "ServerPort"
    const val KEY_NO = "no"; const val KEY_NAME = "name"; const val KEY_STYLE = "style"
    const val KEY_SOURCE = "datasource"; const val KEY_ROTATION = "rotation"
    const val KEY_VOLUME = "volume"; const val KEY_POWER_ON = "powerontime"
    const val KEY_POWER_OFF = "powerofftime"; const val KEY_HORSELAMP = "horselamp"
    const val KEY_TITLE = "title"; const val KEY_PARAMS = "Params"
    const val KEY_TEMPLATE = "template_config"
}
