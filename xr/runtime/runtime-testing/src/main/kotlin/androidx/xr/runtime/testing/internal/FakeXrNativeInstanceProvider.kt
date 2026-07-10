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

package androidx.xr.runtime.testing.internal

import android.content.Context
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.interfaces.XrNativeInstanceProvider

internal class FakeXrNativeInstanceProvider : XrNativeInstanceProvider {

    override val requirements: Set<Feature> = emptySet()

    internal val addedExtensions: MutableList<String> = mutableListOf()

    override fun initialize(context: Context, extraExtensions: List<String>) {
        addedExtensions.addAll(extraExtensions)
    }

    /** The handle of the native instance if available for the runtime. */
    override val xrInstanceHandle: Long = 1111L

    /** The handle of the function table if available for the runtime. */
    override val xrInstanceProcAddr: Long = 2222L
}
