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
package androidx.xr.runtime.openxr

import android.content.Context
import androidx.xr.runtime.interfaces.Feature
import androidx.xr.runtime.interfaces.XrNativeInstanceProvider

/** Implementation of native data provision for the OpenXR runtime. */
internal class OpenXrInstanceManager : XrNativeInstanceProvider {
    private val LIBRARY_NAME: String = "androidx.xr.runtime.openxr"

    override val requirements: Set<Feature> = setOf(Feature.FULLSTACK, Feature.OPEN_XR)

    internal var nativeManager: Long = 0L

    override var xrInstanceProcAddr: Long = 0L
        private set

    override var xrInstanceHandle: Long = 0L
        private set

    override fun initialize(context: Context, extraExtensions: List<String>) {
        // Attempt to load the test library instead if it was added based on the Gradle AndroidTest
        // variant. Else this is a non-test environment.
        try {
            System.loadLibrary("${LIBRARY_NAME}.test")
        } catch (e: UnsatisfiedLinkError) {
            System.loadLibrary(LIBRARY_NAME)
        }
        nativeManager = nativeCreateOpenXrInstanceManager(extraExtensions.toTypedArray())

        xrInstanceHandle = nativeGetOpenXrInstanceHandle(context, nativeManager)
        xrInstanceProcAddr = nativeGetGetInstanceProcAddr(nativeManager)
    }

    private external fun nativeCreateOpenXrInstanceManager(extensions: Array<String>): Long

    private external fun nativeGetOpenXrInstanceHandle(context: Context, nativeManager: Long): Long

    private external fun nativeGetGetInstanceProcAddr(nativeManager: Long): Long
}
