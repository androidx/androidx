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

val syntheticUiLibraryStubs = listOf(
    Source.kotlin(
        "androidx/privacysandbox/ui/core/SandboxedUiAdapter.kt", """
        |package androidx.privacysandbox.ui.core
        |
        |import android.os.IBinder
        |
        |interface SdkActivityLauncher {
        |    suspend fun launchSdkActivity(sdkActivityHandlerToken: IBinder): Boolean
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/ui/client/SdkActivityLaunchers.kt", """
        |@file:JvmName("SdkActivityLaunchers")
        |
        |package androidx.privacysandbox.ui.client
        |
        |import android.os.Bundle
        |import androidx.privacysandbox.ui.core.SdkActivityLauncher
        |
        |fun SdkActivityLauncher.toLauncherInfo(): Bundle {
        |    TODO("Stub!")
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/ui/provider/SdkActivityLauncherFactory.kt", """
        |package androidx.privacysandbox.ui.provider
        |
        |import android.os.Bundle
        |import androidx.privacysandbox.ui.core.SdkActivityLauncher
        |
        |object SdkActivityLauncherFactory {
        |
        |    @JvmStatic
        |    @Suppress("UNUSED_PARAMETER")
        |    fun fromLauncherInfo(launcherInfo: Bundle): SdkActivityLauncher {
        |        TODO("Stub!")
        |    }
        |}""".trimMargin()
    ),
    Source.kotlin(
        "androidx/core/os/BundleCompat.kt", """
        |package androidx.core.os
        |
        |import android.os.IBinder
        |import android.os.Bundle
        |
        |object BundleCompat {
        |    @Suppress("UNUSED_PARAMETER")
        |    fun getBinder(bundle: Bundle, key: String?): IBinder? {
        |        TODO("Stub!")
        |    }
        |}
        |""".trimMargin()
    ),
)
