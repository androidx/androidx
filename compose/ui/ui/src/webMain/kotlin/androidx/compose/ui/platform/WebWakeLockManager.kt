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

package androidx.compose.ui.platform

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.documentIsVisible
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.asJsException
import kotlin.js.js
import kotlinx.browser.document

private val webWakeLockSupported: Boolean by lazy {
    isSecureContext() && isFullWakeLockApiSupported()
}

/**
 * Manages the WakeLock API for keeping the screen on in web browsers.
 *
 * This class handles requesting and releasing wake locks using the Screen Wake Lock API,
 * which prevents the device screen from turning off while content is being displayed.
 * The API is only available in secure contexts (such as HTTPS or localhost).
 */

internal object WebWakeLockManager {

    private var wakeLockSentinel: WakeLockSentinel? = null

    @VisibleForTesting
    internal var requestingLock = false
    private var alreadyLoggedWarning = false

    private val requests = mutableSetOf<Any>()

    init {
        document.addEventListener("visibilitychange") {
            if (documentIsVisible() && enoughRequestsForLock() && webWakeLockSupported) {
                requestWakeLock()
            }
        }
    }

    fun sendWakeLockRequest(client: Any, keepScreenOn: Boolean) {
        if (!webWakeLockSupported) {
            if (!alreadyLoggedWarning) {
                alreadyLoggedWarning = true
                println("Wake Lock API not supported or not in a secure context")
            }
            return
        }
        if (keepScreenOn) {
            requests.add(client)
        } else {
            requests.remove(client)
        }
        if (enoughRequestsForLock()) {
            requestWakeLock()
        } else {
            releaseWakeLock()
        }
    }


    private fun requestWakeLock() {
        if (wakeLockSentinel != null || requestingLock) {
            //A lock is already active or a request is in progress
            return
        }

        requestingLock = true
        requestScreenWakeLock()
            .then { sentinel ->
                //Prevents race condition where a release requestLock could come in before the lock is granted
                if (requestingLock) {
                    requestingLock = false
                    wakeLockSentinel = sentinel

                    sentinel.addEventListener("release") {
                        wakeLockSentinel = null
                    }
                } else {
                    sentinel.release()
                }

                sentinel
            }
            .catch { error ->
                requestingLock = false
                println("Failed to acquire wake lock: ${error.asJsException().message}")
                null
            }
    }

    private fun releaseWakeLock() {
        if (requestingLock) {
            requestingLock = false
        }
        wakeLockSentinel?.let { sentinel ->
            sentinel
                .release()
                .then {
                    wakeLockSentinel = null
                    null
                }
                .catch { error ->
                    println("Failed to release wake lock: ${error.asJsException().message}")
                    wakeLockSentinel = null
                    null
                }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun enoughRequestsForLock(): Boolean = requests.isNotEmpty()

    fun isWakeLockActive(): Boolean =
        wakeLockSentinel != null && !(wakeLockSentinel?.released ?: true) && enoughRequestsForLock()

    internal fun reset() {
        requestingLock = false
        requests.clear()
        releaseWakeLock()
    }

}

//language=javascript
private fun requestScreenWakeLock(): Promise<WakeLockSentinel> = js(
    """{
        return navigator.wakeLock.request('screen')
    }
    """
)

//language=javascript
internal fun isFullWakeLockApiSupported(): Boolean =
    js(
        """Boolean(
        window.navigator.wakeLock && 
        typeof(WakeLockSentinel) !== 'undefined'
        )
    """
    )

private external interface WakeLockSentinel : JsAny {
    @Suppress("unused")
    val released: Boolean

    @Suppress("unused")
    val type: String
    fun release(): Promise<JsAny?>
    fun addEventListener(type: String, listener: () -> Unit)
}



