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

package androidx.privacysandbox.sdkruntime.testsdk.v2

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.view.View
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat

@Suppress("unused") // Reflection usage from tests in privacysandbox:sdkruntime:sdkruntime-client
class CompatProvider : SandboxedSdkProviderCompat() {
    @JvmField
    var onLoadSdkBinder: Binder? = null

    @JvmField
    var lastOnLoadSdkParams: Bundle? = null

    @JvmField
    var isBeforeUnloadSdkCalled = false

    @Throws(LoadSdkCompatException::class)
    override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
        val result = SdkImpl(context!!)
        onLoadSdkBinder = result

        lastOnLoadSdkParams = params
        if (params.getBoolean("needFail", false)) {
            throw LoadSdkCompatException(RuntimeException(), params)
        }
        return SandboxedSdkCompat(result)
    }

    override fun beforeUnloadSdk() {
        isBeforeUnloadSdkCalled = true
    }

    override fun getView(
        windowContext: Context,
        params: Bundle,
        width: Int,
        height: Int
    ): View {
        return View(windowContext)
    }

    internal class SdkImpl(
        private val context: Context
    ) : Binder() {
        fun getSandboxedSdks(): List<SandboxedSdkCompat> =
            SdkSandboxControllerCompat.from(context).getSandboxedSdks()
    }
}
