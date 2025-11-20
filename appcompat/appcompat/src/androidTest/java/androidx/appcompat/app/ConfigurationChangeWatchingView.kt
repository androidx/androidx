/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appcompat.app

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import java.util.concurrent.Semaphore

class ConfigurationChangeWatchingView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attrs, defStyleAttr, defStyleRes) {

    private val onConfigurationChangeSemaphore = Semaphore(0)

    var effectiveConfiguration: Configuration? = Configuration(context.resources.configuration)
        private set

    private var lastConfigurationChange: Configuration? = null

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val copiedConfiguration = Configuration(newConfig)
        lastConfigurationChange = copiedConfiguration
        effectiveConfiguration = copiedConfiguration
        onConfigurationChangeSemaphore.release()
    }

    fun getLastConfigurationChangeAndClear(): Configuration? =
        lastConfigurationChange.also { lastConfigurationChange = null }
}
