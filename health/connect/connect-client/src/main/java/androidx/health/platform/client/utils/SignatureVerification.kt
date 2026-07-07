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

@file:JvmName("SignatureVerification")
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.health.platform.client.utils

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.health.platform.client.service.HealthDataServiceConstants.DEFAULT_PROVIDER_DEV_CERT_SHA256
import androidx.health.platform.client.service.HealthDataServiceConstants.DEFAULT_PROVIDER_PACKAGE_NAME
import androidx.health.platform.client.service.HealthDataServiceConstants.DEFAULT_PROVIDER_RELEASE_CERT_SHA256

@VisibleForTesting @JvmField var sBypassSignatureCheckForTesting = false

/** Returns whether the target package's signature is valid. */
fun isTargetSignatureValid(packageManager: PackageManager, packageName: String): Boolean {
    if (sBypassSignatureCheckForTesting) {
        return true
    }
    if (DEFAULT_PROVIDER_PACKAGE_NAME != packageName) {
        return true
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        if (
            packageManager.hasSigningCertificate(
                packageName,
                DEFAULT_PROVIDER_RELEASE_CERT_SHA256,
                PackageManager.CERT_INPUT_SHA256,
            )
        ) {
            return true
        }

        if ("userdebug" == Build.TYPE || "eng" == Build.TYPE) {
            return packageManager.hasSigningCertificate(
                packageName,
                DEFAULT_PROVIDER_DEV_CERT_SHA256,
                PackageManager.CERT_INPUT_SHA256,
            )
        }
    }
    return false
}
