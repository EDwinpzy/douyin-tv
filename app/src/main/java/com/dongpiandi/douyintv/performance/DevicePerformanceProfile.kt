package com.dongpiandi.douyintv.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build

internal data class DevicePerformanceProfile(
    val lowTier: Boolean,
    val danmakuFrameIntervalMs: Long,
    val playbackProgressIntervalMs: Long,
    val preloadForwardCount: Int,
) {
    companion object {
        fun detect(context: Context): DevicePerformanceProfile {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
            return forDevice(
                isLowRamDevice = activityManager.isLowRamDevice,
                memoryClassMb = activityManager.memoryClass,
                totalMemoryBytes = memoryInfo.totalMem,
                supports64Bit = Build.SUPPORTED_64_BIT_ABIS.isNotEmpty(),
            )
        }

        internal fun forDevice(
            isLowRamDevice: Boolean,
            memoryClassMb: Int,
            totalMemoryBytes: Long,
            supports64Bit: Boolean,
        ): DevicePerformanceProfile {
            val lowTier =
                isLowRamDevice ||
                    memoryClassMb <= LOW_MEMORY_CLASS_MB ||
                    totalMemoryBytes in 1..LOW_TOTAL_MEMORY_BYTES ||
                    !supports64Bit
            return if (lowTier) LOW_TIER else STANDARD
        }

        private const val LOW_MEMORY_CLASS_MB = 192
        private const val LOW_TOTAL_MEMORY_BYTES = 3L * 1024L * 1024L * 1024L

        internal val STANDARD = DevicePerformanceProfile(
            lowTier = false,
            danmakuFrameIntervalMs = 33L,
            playbackProgressIntervalMs = 3_000L,
            preloadForwardCount = 4,
        )

        internal val LOW_TIER = DevicePerformanceProfile(
            lowTier = true,
            danmakuFrameIntervalMs = 66L,
            playbackProgressIntervalMs = 5_000L,
            preloadForwardCount = 2,
        )
    }
}
