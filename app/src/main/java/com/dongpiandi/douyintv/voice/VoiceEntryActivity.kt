package com.dongpiandi.douyintv.voice

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * Opens Douyin from the voice bridge. Commands themselves are accepted only by
 * [VoiceCommandProvider], which verifies the caller UID against the helper package.
 */
class VoiceEntryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(
            Intent()
                .setComponent(ComponentName(packageName, DouyinVoiceContract.MAIN_ACTIVITY))
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
