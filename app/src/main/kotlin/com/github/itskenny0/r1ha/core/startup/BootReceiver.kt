package com.github.itskenny0.r1ha.core.startup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.itskenny0.r1ha.App
import com.github.itskenny0.r1ha.MainActivity
import com.github.itskenny0.r1ha.core.util.R1Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Optional boot autostart. When [AppSettings.behavior.startOnBoot] is on,
 * relaunch MainActivity after BOOT_COMPLETED / package replace. This keeps
 * wall-panel installs coming back without a manual tap after reboot.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        scope.launch {
            try {
                val app = context.applicationContext as? App
                if (app == null) {
                    R1Log.w("BootReceiver", "context is not App; ignoring")
                    return@launch
                }
                val settings = app.graph.settings.settings.first()
                if (!settings.behavior.startOnBoot) {
                    R1Log.i("BootReceiver", "autostart disabled; ignoring $action")
                    return@launch
                }
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                context.startActivity(launch)
                R1Log.i("BootReceiver", "started MainActivity after $action")
            } catch (t: Throwable) {
                R1Log.w("BootReceiver", "autostart failed: ${t.message}", t)
            } finally {
                pending.finish()
            }
        }
    }
}
