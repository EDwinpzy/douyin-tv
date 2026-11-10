package com.dongpiandi.douyintv

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dongpiandi.douyintv.ui.DouyinTvApp
import com.dongpiandi.douyintv.ui.MainViewModel
import com.dongpiandi.douyintv.ui.theme.DouyinTvTheme
import com.dongpiandi.douyintv.voice.DouyinVoiceCommandQueue
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            DouyinTvTheme {
                DouyinTvApp(viewModel = viewModel)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                DouyinVoiceCommandQueue.incoming.collect { queued ->
                    if (System.currentTimeMillis() - queued.enqueuedAtMs <= COMMAND_MAX_AGE_MS) {
                        viewModel.applyVoiceCommand(queued.command)
                    }
                }
            }
        }
    }

    private companion object {
        const val COMMAND_MAX_AGE_MS = 30_000L
    }
}
