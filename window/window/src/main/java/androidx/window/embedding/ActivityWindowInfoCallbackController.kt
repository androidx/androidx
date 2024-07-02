/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.embedding

import android.app.Activity
import android.graphics.Rect
import android.util.ArrayMap
import androidx.annotation.GuardedBy
import androidx.annotation.VisibleForTesting
import androidx.core.util.Consumer as JetpackConsumer
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.extensions.core.util.function.Consumer
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.EmbeddedActivityWindowInfo as ExtensionsActivityWindowInfo
import androidx.window.reflection.Consumer2
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Manages and dispatches update of [EmbeddedActivityWindowInfo]. */
@RequiresWindowSdkExtension(6)
internal open class ActivityWindowInfoCallbackController(
    private val embeddingExtension: ActivityEmbeddingComponent,
) {
    private val globalLock = ReentrantLock()

    @GuardedBy("globalLock") private val extensionsCallback: Consumer<ExtensionsActivityWindowInfo>

    @VisibleForTesting
    @GuardedBy("globalLock")
    internal var callbacks:
        MutableMap<JetpackConsumer<EmbeddedActivityWindowInfo>, CallbackWrapper> =
        ArrayMap()

    init {
        WindowSdkExtensions.getInstance().requireExtensionVersion(6)
        extensionsCallback =
            Consumer2<ExtensionsActivityWindowInfo> { info ->
                globalLock.withLock {
                    for (callbackWrapper in callbacks.values) {
                        callbackWrapper.accept(info)
                    }
                }
            }
    }

    fun addCallback(activity: Activity, callback: JetpackConsumer<EmbeddedActivityWindowInfo>) {
        globalLock.withLock {
            if (callbacks.isEmpty()) {
                // Register when the first callback is added.
                embeddingExtension.setEmbeddedActivityWindowInfoCallback(
                    Runnable::run,
                    extensionsCallback
                )
            }

            val callbackWrapper = CallbackWrapper(activity, callback)
            callbacks[callback] = callbackWrapper
            embeddingExtension.getEmbeddedActivityWindowInfo(activity)?.apply {
                // Trigger with the latest info if the window exists.
                callbackWrapper.accept(this)
            }
        }
    }

    fun removeCallback(callback: JetpackConsumer<EmbeddedActivityWindowInfo>) {
        globalLock.withLock {
            if (callbacks.remove(callback) == null) {
                // Early return if the callback is not registered.
                return
            }
            if (callbacks.isEmpty()) {
                // Unregister when the last callback is removed.
                embeddingExtension.clearEmbeddedActivityWindowInfoCallback()
            }
        }
    }

    /** Translates from Extensions info to Jetpack info. */
    @VisibleForTesting
    internal open fun translate(info: ExtensionsActivityWindowInfo): EmbeddedActivityWindowInfo {
        val parentHostBounds = Rect(info.taskBounds)
        val boundsInParentHost = Rect(info.activityStackBounds)
        // Converting to host container coordinate.
        boundsInParentHost.offset(-parentHostBounds.left, -parentHostBounds.top)
        return EmbeddedActivityWindowInfo(
            isEmbedded = info.isEmbedded,
            parentHostBounds = parentHostBounds,
            boundsInParentHost = boundsInParentHost
        )
    }

    @VisibleForTesting
    internal inner class CallbackWrapper(
        private val activity: Activity,
        val callback: JetpackConsumer<EmbeddedActivityWindowInfo>
    ) {
        var lastReportedInfo: EmbeddedActivityWindowInfo? = null

        fun accept(extensionsActivityWindowInfo: ExtensionsActivityWindowInfo) {
            val updatedActivity = extensionsActivityWindowInfo.activity
            if (activity != updatedActivity) {
                return
            }

            val newInfo = translate(extensionsActivityWindowInfo)
            if (shouldReportInfo(newInfo)) {
                lastReportedInfo = newInfo
                callback.accept(newInfo)
            }
        }

        private fun shouldReportInfo(newInfo: EmbeddedActivityWindowInfo): Boolean =
            lastReportedInfo?.let {
                if (it.isEmbedded != newInfo.isEmbedded) {
                    // Always report if the embedded status changes
                    return true
                }
                if (!newInfo.isEmbedded) {
                    // Do not report if the activity is not embedded
                    return false
                }
                return it != newInfo
            } ?: newInfo.isEmbedded // Always report the first available info if it is embedded
    }
}
