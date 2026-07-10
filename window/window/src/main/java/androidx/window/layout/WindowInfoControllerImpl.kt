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

package androidx.window.layout

import android.content.Context
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.annotation.UiContext

/** Implementation of [WindowInfoController]. */
@RequiresApi(37)
internal class WindowInfoControllerImpl : WindowInfoController() {

    @android.annotation.SuppressLint("BanUncheckedReflection")
    override fun requestVisualState(@UiContext context: Context, request: VisualStateRequest) {
        check(Looper.getMainLooper() == Looper.myLooper()) {
            "requestVisualState must be called from the main thread"
        }

        try {
            var flags = 0
            if (request.isSustainedVisualsEnabled()) {
                flags = flags or ENGAGEMENT_CONTROL_FLAG_SUSTAIN_VISUALS
            }

            val windowManager = context.getSystemService(WindowManager::class.java)
            if (windowManager == null) {
                Log.w(TAG, "WindowManager is null. Cannot request visual state.")
                return
            }

            // Using reflection because the new API is not yet in the AndroidX prebuilts.
            // TODO(b/494436620): Remove reflection and lint suppression once compileSdk is bumped
            // and the requestEngagementControlState API is available in the prebuilt stubs.
            getDispatchMethod(windowManager)?.invoke(windowManager, flags)
        } catch (e: Exception) {
            Log.e(TAG, "Exception while requesting visual state", e)
        }
    }

    private companion object {
        val TAG = WindowInfoControllerImpl::class.java.simpleName
        // TODO(b/494436620): Replace this with the platform API value once available.
        const val ENGAGEMENT_CONTROL_FLAG_SUSTAIN_VISUALS = 1 shl 0

        private var dispatchMethod: java.lang.reflect.Method? = null
        private var isMethodLoaded = false

        @android.annotation.SuppressLint("BanUncheckedReflection")
        fun getDispatchMethod(windowManager: WindowManager): java.lang.reflect.Method? {
            if (!isMethodLoaded) {
                try {
                    dispatchMethod =
                        windowManager.javaClass.getMethod(
                            "requestEngagementControlState",
                            Int::class.javaPrimitiveType,
                        )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to find requestEngagementControlState method", e)
                }
                isMethodLoaded = true
            }
            return dispatchMethod
        }
    }
}
