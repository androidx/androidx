/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.mediateesdkprovider

import android.annotation.SuppressLint
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SandboxedSdkProvider
import android.content.Context
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.view.View
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.ui.integration.sdkproviderutils.MediateeSdkApiImpl

// TODO(b/257429573): Remove this line once fixed.
@SuppressLint("ClassVerificationFailure")
@RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
class SdkProviderImpl : SandboxedSdkProvider() {
    override fun onLoadSdk(p0: Bundle): SandboxedSdk {
        return SandboxedSdk(MediateeSdkApiImpl(context!!))
    }

    override fun getView(p0: Context, p1: Bundle, p2: Int, p3: Int): View {
        throw IllegalStateException("This getView method will not be used.")
    }
}
