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
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteState
import androidx.compose.remote.creation.compose.state.withGlobalScope
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
        globalScope: Boolean = false,
        content: @RemoteComposable @Composable () -> Unit,
    ): ByteArray {
        val cacheKey = if (globalScope) "${name}_global" else name
        synchronized(memoryCache) { memoryCache[cacheKey] }
            ?.let {
                return it
            }

        val appStamp = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime
        }
            .getOrDefault(0L)
        val dir = File(context.cacheDir, "rcbench/$appStamp")
        val file = File(dir, "$cacheKey.rc")
        val existing =
            withContext(Dispatchers.IO) {
                if (file.exists() && file.length() > 0L) file.readBytes() else null
            }
        if (existing != null) {
            synchronized(memoryCache) { memoryCache[cacheKey] = existing }
            return existing
        }

        val captured = captureBytes(context, globalScope, content)

        withContext(Dispatchers.IO) {
            dir.mkdirs()
            val tmp = File(dir, "$cacheKey.rc.tmp")
            tmp.writeBytes(captured)
            if (!tmp.renameTo(file)) {
                file.writeBytes(captured)
                tmp.delete()
            }
        }
        synchronized(memoryCache) { memoryCache[cacheKey] = captured }
        return captured
    }

    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    private suspend fun captureBytes(
        context: Context,
        globalScope: Boolean,
        content: @RemoteComposable @Composable () -> Unit,
    ): ByteArray {
        if (!globalScope) {
            return captureSingleRemoteDocument(
                    profile = RcPlatformProfiles.ANDROIDX,
                    context = context,
                    content = content,
                )
                .bytes
        }
        val prevFlag = RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled
        RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = false
        return try {
            captureSingleRemoteDocument(
                    profile = RcPlatformProfiles.ANDROIDX,
                    context = context,
                    content = content,
                )
                .bytes
        } finally {
            RemoteComposeCreationComposeFlags.isEnforceCleanRecompositionEnabled = prevFlag
        }
    }
}

@Composable
@RemoteComposable
internal fun <T : RemoteState<*>> T.withGlobalScopeIf(enabled: Boolean): T =
    if (enabled) withGlobalScope() else this
