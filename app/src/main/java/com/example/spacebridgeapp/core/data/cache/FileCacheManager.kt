package com.example.spacebridgeapp.core.data.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface FileCacheManager {
    suspend fun getCacheSizeBytes(): Long

    suspend fun clearCache(): Boolean
}

class DefaultFileCacheManager(
    private val context: Context,
) : FileCacheManager {
    override suspend fun getCacheSizeBytes(): Long =
        withContext(Dispatchers.IO) {
            calculateDirectorySize(context.cacheDir)
        }

    override suspend fun clearCache(): Boolean =
        withContext(Dispatchers.IO) {
            var allSuccess = true
            context.cacheDir.listFiles()?.forEach { file ->
                if (!file.deleteRecursively()) {
                    allSuccess = false
                }
            }
            allSuccess
        }

    private fun calculateDirectorySize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var total = 0L
        dir.listFiles()?.forEach { file ->
            total +=
                if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
        }
        return total
    }
}
