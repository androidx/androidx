/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.privacysandbox.tools.testing

import androidx.room.compiler.processing.util.Source

// SDK Runtime library is not available in AndroidX prebuilts, so while that's the case we use fake
// stubs to run our compilation tests.
val syntheticSdkRuntimeLibraryStubs = listOf(
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.os.IBinder
        |
        |@Suppress("UNUSED_PARAMETER")
        |class SandboxedSdkCompat(sdkInterface: IBinder) {
        |    fun getInterface(): IBinder? = throw RuntimeException("Stub!")
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkProviderCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.content.Context
        |import android.os.Bundle
        |import android.view.View
        |
        |@Suppress("UNUSED_PARAMETER")
        |abstract class SandboxedSdkProviderCompat {
        |   var context: Context? = null
        |       private set
        |   fun attachContext(context: Context): Unit = throw RuntimeException("Stub!")
        |
        |   abstract fun onLoadSdk(params: Bundle): SandboxedSdkCompat
        |
        |   open fun beforeUnloadSdk() {}
        |
        |   abstract fun getView(
        |       windowContext: Context,
        |       params: Bundle,
        |       width: Int,
        |       height: Int
        |   ): View
        |}
        |""".trimMargin()
    ),
)

val allTestLibraryStubs = syntheticSdkRuntimeLibraryStubs