/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.core

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

/**
 * Temporary replacement for BuildCompat.AD_SERVICES_EXTENSION_INT.
 * TODO(b/249981547) Replace with AD_SERVICES_EXTENSION_INT after new core library release
 *
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AdServicesInfo {

    fun isAtLeastV4(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            Extensions30Impl.isAtLeastV4()
    }

    fun isAtLeastV5(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            Extensions30Impl.isAtLeastV5()
    }

    @ChecksSdkIntAtLeast(codename = "UpsideDownCake")
    fun isDeveloperPreview(): Boolean =
        Build.VERSION.CODENAME.equals("UpsideDownCakePrivacySandbox")

    @RequiresApi(30)
    private object Extensions30Impl {
        @DoNotInline
        fun isAtLeastV4() =
            SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 4

        @DoNotInline
        fun isAtLeastV5() =
            SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES) >= 5
    }
}
