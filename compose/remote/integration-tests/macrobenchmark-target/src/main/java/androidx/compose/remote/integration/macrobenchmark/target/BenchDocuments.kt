/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.remote.integration.macrobenchmark.target

import android.content.Context
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * On-disk and in-memory cache of captured Remote Compose templates (`cacheDir/rcbench/<name>.rc`)
 * so benchmark startup and scroll iterations do not pay document-capture cost.
 */
object BenchDocuments {
    private val memoryCache = HashMap<String, ByteArray>()

    suspend fun getOrCapture(
        context: Context,
        name: String,
        content: @RemoteComposable @Composable () -> Unit,
    ): ByteArray {
        synchronized(memoryCache) { memoryCache[name] }
            ?.let {
                return it
            }

        val dir = File(context.cacheDir, "rcbench")
        val file = File(dir, "$name.rc")
        val existing =
            withContext(Dispatchers.IO) {
                if (file.exists() && file.length() > 0L) file.readBytes() else null
            }
        if (existing != null) {
            synchronized(memoryCache) { memoryCache[name] = existing }
            return existing
        }

        val captured =
            captureSingleRemoteDocument(
                    profile = RcPlatformProfiles.ANDROIDX,
                    context = context,
                    content = content,
                )
                .bytes

        withContext(Dispatchers.IO) {
            dir.mkdirs()
            val tmp = File(dir, "$name.rc.tmp")
            tmp.writeBytes(captured)
            if (!tmp.renameTo(file)) {
                file.writeBytes(captured)
                tmp.delete()
            }
        }
        synchronized(memoryCache) { memoryCache[name] = captured }
        return captured
    }
}
