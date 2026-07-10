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

package androidx.pdf.utils

import android.os.Build
import android.os.ext.SdkExtensions

private fun isRequiredSdkExtensionAvailable(extensionVersion: Int): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S) >= extensionVersion

internal fun isAnnotationsFeatureAvailable(): Boolean = isRequiredSdkExtensionAvailable(18)

internal fun isFormFillingAvailable(): Boolean = isRequiredSdkExtensionAvailable(13)

internal fun isRenderFormContentModeAvailable(): Boolean = isRequiredSdkExtensionAvailable(19)

internal fun isGetTopObjectAvailable(): Boolean = isRequiredSdkExtensionAvailable(19)

internal fun areCorePdfApisAvailableInSdk(): Boolean = isRequiredSdkExtensionAvailable(13)

internal fun isLinearizationStatusAvailable(): Boolean = isRequiredSdkExtensionAvailable(13)
