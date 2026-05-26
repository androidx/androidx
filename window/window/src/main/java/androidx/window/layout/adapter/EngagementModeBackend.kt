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

package androidx.window.layout.adapter

import android.content.Context
import androidx.annotation.UiContext
import androidx.core.util.Consumer
import androidx.window.layout.WindowEngagementInfo.EngagementMode
import androidx.window.layout.util.EngagementModeHelper
import androidx.window.layout.util.InputHelper
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal interface EngagementModeBackend {
    fun engagementMode(@UiContext context: Context): EngagementMode

    fun addEngagementLayoutChangeCallback(
        @UiContext context: Context,
        executor: Executor,
        callback: Consumer<EngagementMode>,
    )

    fun removeEngagementLayoutChangeCallback(callback: Consumer<EngagementMode>)

    companion object {
        @Volatile private var globalInstance: EngagementModeBackend? = null
        private val globalLock = ReentrantLock()

        /** Returns the global instance of [EngagementModeBackend]. */
        @JvmStatic
        fun getInstance(context: Context): EngagementModeBackend {
            if (globalInstance == null) {
                globalLock.withLock {
                    if (globalInstance == null) {
                        val inputHelper = InputHelper.getInstance(context)
                        val engagementModeHelper = EngagementModeHelper.getInstance()
                        val tracker = InputDeviceTracker(inputHelper)
                        globalInstance = EngagementModeBackendApi0(engagementModeHelper, tracker)
                    }
                }
            }
            return globalInstance!!
        }
    }
}
