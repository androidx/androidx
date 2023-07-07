/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.lifecycle

import android.content.Context
import androidx.startup.AppInitializer
import androidx.startup.Initializer

/**
 * Initializes [ProcessLifecycleOwner] using `androidx.startup`.
 */
class ProcessLifecycleInitializer : Initializer<LifecycleOwner> {
    override fun create(context: Context): LifecycleOwner {
        val appInitializer = AppInitializer.getInstance(context)
        check(appInitializer.isEagerlyInitialized(javaClass)) {
            """ProcessLifecycleInitializer cannot be initialized lazily.
               Please ensure that you have:
               <meta-data
                   android:name='androidx.lifecycle.ProcessLifecycleInitializer'
                   android:value='androidx.startup' />
               under InitializationProvider in your AndroidManifest.xml"""
        }
        LifecycleDispatcher.init(context)
        ProcessLifecycleOwner.init(context)
        return ProcessLifecycleOwner.get()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
