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
import androidx.core.os.BuildCompat

/**
 * Temporary replacement for BuildCompat.AD_SERVICES_EXTENSION_INT.
 * TODO(b/249981547) Replace with AD_SERVICES_EXTENSION_INT after new core library release
 *
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object AdServicesInfo {

    fun version(): Int {
        return if (Build.VERSION.SDK_INT >= 30) {
            Extensions30Impl.getAdServicesVersion()
        } else {
            0
        }
    }

    @androidx.annotation.OptIn(markerClass = [BuildCompat.PrereleaseSdkCheck::class])
    @ChecksSdkIntAtLeast(codename = "UpsideDownCake")
    fun isAtLeastV5(): Boolean {
        // Can't use only version check until SDK rollout (see b/260334264).
        // Using only isAtLeastU() is correct, but make testing of V4 functionality complicated.
        // Combination of 2 checks allows to test V4/V5 using different U builds.
        // TODO (b/265295473): Remove BuildCompat.isAtLeastU() after SDK finalisation.
        return BuildCompat.isAtLeastU() && version() >= 5
    }

    @RequiresApi(30)
    private object Extensions30Impl {
        @DoNotInline
        fun getAdServicesVersion() =
            SdkExtensions.getExtensionVersion(SdkExtensions.AD_SERVICES)
    }
}