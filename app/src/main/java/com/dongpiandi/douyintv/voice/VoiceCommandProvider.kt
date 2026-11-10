package com.dongpiandi.douyintv.voice

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.util.Log

/** Authenticates the helper by UID, then queues commands inside the app process. */
class VoiceCommandProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(
        method: String,
        arg: String?,
        extras: Bundle?,
    ): Bundle {
        if (method != DouyinVoiceContract.COMMAND_PROVIDER_METHOD) {
            return Bundle().apply {
                putBoolean(DouyinVoiceContract.EXTRA_ACCEPTED, false)
            }
        }
        if (!isTrustedCaller()) {
            Log.w(TAG, "Rejected command from untrusted UID=${Binder.getCallingUid()}")
            return Bundle().apply {
                putBoolean(DouyinVoiceContract.EXTRA_ACCEPTED, false)
            }
        }
        val command =
            DouyinVoiceCommand.fromExtras(
                operation = extras?.getString(DouyinVoiceContract.EXTRA_OPERATION),
                query = extras?.getString(DouyinVoiceContract.EXTRA_QUERY),
                openFirstResult =
                    extras?.getBoolean(DouyinVoiceContract.EXTRA_OPEN_FIRST_RESULT) ?: false,
            )
        val accepted = command != null && DouyinVoiceCommandQueue.offer(command)
        if (!accepted) {
            Log.w(TAG, "Rejected malformed or full voice command")
        }
        return Bundle().apply {
            putBoolean(DouyinVoiceContract.EXTRA_ACCEPTED, accepted)
        }
    }

    private fun isTrustedCaller(): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid()) return true
        val context = context ?: return false
        val packages =
            context.packageManager.getPackagesForUid(callingUid)
                .orEmpty()
                .toSet()
        return TRUSTED_HELPER_PACKAGE in packages
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    private companion object {
        const val TAG = "DouyinVoiceCommand"
        const val TRUSTED_HELPER_PACKAGE = "com.dongpiandi.voicehelper"
    }
}
