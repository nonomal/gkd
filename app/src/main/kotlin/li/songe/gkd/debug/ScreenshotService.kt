package li.songe.gkd.debug

import android.app.Service
import android.content.Intent
import com.blankj.utilcode.util.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import li.songe.gkd.app
import li.songe.gkd.notif.notifyService
import li.songe.gkd.notif.screenshotNotif
import li.songe.gkd.util.OnCreate
import li.songe.gkd.util.OnDestroy
import li.songe.gkd.util.ScreenshotUtil
import li.songe.gkd.util.componentName
import li.songe.gkd.util.useAliveFlow
import li.songe.gkd.util.useLogLifecycle
import java.lang.ref.WeakReference

class ScreenshotService : Service(), OnCreate, OnDestroy {
    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        onCreated()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            return super.onStartCommand(intent, flags, startId)
        } finally {
            intent?.let {
                screenshotUtil?.destroy()
                screenshotUtil = ScreenshotUtil(this, intent)
                LogUtils.d("screenshot restart")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        onDestroyed()
    }

    private var screenshotUtil: ScreenshotUtil? = null

    init {
        useLogLifecycle()
        useAliveFlow(isRunning)
        onCreated { screenshotNotif.notifyService(this) }
        onCreated { instance = WeakReference(this) }
        onDestroyed { instance = WeakReference(null) }
        onDestroyed { screenshotUtil?.destroy() }
    }

    companion object {
        private var instance = WeakReference<ScreenshotService>(null)
        val isRunning = MutableStateFlow(false)
        suspend fun screenshot() = instance.get()?.screenshotUtil?.execute()
        fun start(intent: Intent) {
            intent.component = ScreenshotService::class.componentName
            app.startForegroundService(intent)
        }

        fun stop() {
            app.stopService(Intent(app, ScreenshotService::class.java))
        }
    }
}