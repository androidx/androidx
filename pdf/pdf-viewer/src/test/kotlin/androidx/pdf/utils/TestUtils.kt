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

fun isRequiredSdkExtensionAvailable(extensionVersion: Int = REQUIRED_EXTENSION_VERSION): Boolean {
    // Get the device's version for the specified SDK extension
    val deviceExtensionVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.S)
    return deviceExtensionVersion >= extensionVersion
}

private const val REQUIRED_EXTENSION_VERSION = 18
