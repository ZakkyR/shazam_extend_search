package com.example.shazamextendsearch

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvDebugLog: TextView
    private lateinit var btnPermission: Button
    private lateinit var btnSettings: Button

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 結果は onResume で確認 */ }

    private val debugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val text = intent.getStringExtra(ShazamListenerService.EXTRA_DEBUG_TEXT) ?: return
            tvDebugLog.append("---\n$text\n")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus      = findViewById(R.id.tv_status)
        tvDebugLog    = findViewById(R.id.tv_debug_log)
        btnPermission = findViewById(R.id.btn_permission)
        btnSettings   = findViewById(R.id.btn_settings)

        btnPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        checkForUpdateIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            debugReceiver,
            IntentFilter(ShazamListenerService.BROADCAST_ACTION)
        )
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(debugReceiver)
    }

    private fun checkForUpdateIfNeeded() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val lastCheck = prefs.getLong("last_update_check_ms", 0L)
        val oneDayMs = 24 * 60 * 60 * 1000L
        if (System.currentTimeMillis() - lastCheck < oneDayMs) return

        prefs.edit().putLong("last_update_check_ms", System.currentTimeMillis()).apply()
        UpdateChecker.checkAsync(BuildConfig.VERSION_NAME) { result ->
            if (result is UpdateChecker.Result.Available) {
                showUpdateDialog(result.tag)
            }
        }
    }

    fun checkForUpdateManually() {
        UpdateChecker.checkAsync(BuildConfig.VERSION_NAME) { result ->
            when (result) {
                is UpdateChecker.Result.Available -> showUpdateDialog(result.tag)
                UpdateChecker.Result.UpToDate -> showUpToDateDialog()
                UpdateChecker.Result.Error -> showUpdateErrorDialog()
            }
        }
    }

    private fun showUpdateDialog(tag: String) {
        AlertDialog.Builder(this)
            .setTitle("アップデートがあります")
            .setMessage("$tag が利用可能です。ダウンロードページを開きますか？")
            .setPositiveButton("開く") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_URL)))
            }
            .setNegativeButton("後で", null)
            .show()
    }

    private fun showUpToDateDialog() {
        AlertDialog.Builder(this)
            .setTitle("最新版です")
            .setMessage("${BuildConfig.VERSION_NAME} は最新バージョンです。")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showUpdateErrorDialog() {
        AlertDialog.Builder(this)
            .setTitle("確認できませんでした")
            .setMessage("アップデートの確認に失敗しました。ネットワーク接続を確認してください。")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updatePermissionStatus() {
        val granted = isNotificationListenerEnabled()
        tvStatus.text = if (granted) "✅ 通知アクセス：許可済み" else "❌ 通知アクセス：未許可"
        btnPermission.text = if (granted) "通知アクセス設定を開く" else "通知アクセスを許可する"
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val cn = ComponentName(this, ShazamListenerService::class.java).flattenToString()
        return flat.split(":").any { it == cn }
    }
}
