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

package androidx.credentials.provider

import android.content.pm.PackageInfo
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import androidx.annotation.DeprecatedSinceApi
import androidx.annotation.RequiresApi
import java.security.PublicKey

/**
 * Signing information for an app package.
 *
 * Backward compatible representation of a [SigningInfo]. On Android 27 and below where
 * [SigningInfo] isn't available, this will be initialized from the app's [PackageInfo.signatures],
 * in which case only signingCertificateHistory will be set.
 *
 * @property signingCertificateHistory for API 28 and above, this is the same as the app's
 *   [SigningInfo.getSigningCertificateHistory]; for before API 28, this is the list of [Signature]s
 *   of the app initialized from the app's [PackageInfo.signatures]
 * @property apkContentsSigners for API 28 and above, this is the same as the app's
 *   [SigningInfo.getApkContentsSigners]; for before API 35, this is empty
 * @property publicKeys for API 35 and above, this is the same as the app's
 *   [SigningInfo.getPublicKeys]; for before API 28, this is empty
 * @property schemeVersion for API 35 and above, this is the same as the app's
 *   [SigningInfo.getSchemeVersion]; for before API 35, this is 0 (unknown)
 * @property hasPastSigningCertificates for API 28 and above, this is the same as the app's
 *   [SigningInfo.hasPastSigningCertificates]; for before API 28, this is false
 * @property hasMultipleSigners for API 28 and above, this is the same as the app's
 *   [SigningInfo.hasMultipleSigners]; for before API 28, this is false
 */
class SigningInfoCompat
internal constructor(
    val signingCertificateHistory: List<Signature>,
    @RequiresApi(28) val apkContentsSigners: List<Signature>,
    @RequiresApi(35) val publicKeys: Collection<PublicKey>,
    @RequiresApi(35) val schemeVersion: Int,
    @RequiresApi(28)
    @get:JvmName("hasPastSigningCertificates")
    val hasPastSigningCertificates: Boolean,
    @RequiresApi(28) @get:JvmName("hasMultipleSigners") val hasMultipleSigners: Boolean,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is SigningInfoCompat) {
            return false
        }
        return signingCertificateHistory == other.signingCertificateHistory &&
            apkContentsSigners == other.apkContentsSigners &&
            publicKeys == other.publicKeys &&
            schemeVersion == other.schemeVersion &&
            hasPastSigningCertificates == other.hasPastSigningCertificates &&
            hasMultipleSigners == other.hasMultipleSigners
    }

    override fun hashCode(): Int {
        var result = signingCertificateHistory.hashCode()
        result = 31 * result + apkContentsSigners.hashCode()
        result = 31 * result + publicKeys.hashCode()
        result = 31 * result + schemeVersion
        result = 31 * result + hasPastSigningCertificates.hashCode()
        result = 31 * result + hasMultipleSigners.hashCode()
        return result
    }

    companion object {
        /**
         * Creates a [SigningInfoCompat] from [SigningInfo].
         *
         * @param signingInfo the signing information of an app package
         */
        @JvmStatic
        @RequiresApi(28)
        fun fromSigningInfo(signingInfo: SigningInfo): SigningInfoCompat =
            SigningInfoCompat(
                apkContentsSigners = signingInfo.apkContentsSigners?.filterNotNull() ?: emptyList(),
                publicKeys =
                    @Suppress("USELESS_ELVIS") // This getter is declared as NonNull but it
                    // actually returns null when I test on API 35. Hence adding this safeguard.
                    if (Build.VERSION.SDK_INT >= 35) signingInfo.publicKeys ?: emptySet()
                    else emptySet(),
                schemeVersion =
                    if (Build.VERSION.SDK_INT >= 35) signingInfo.schemeVersion else 0 /* unknown */,
                signingCertificateHistory =
                    signingInfo.signingCertificateHistory?.filterNotNull() ?: emptyList(),
                hasPastSigningCertificates = signingInfo.hasPastSigningCertificates(),
                hasMultipleSigners = signingInfo.hasMultipleSigners()
            )

        /**
         * Creates a [SigningInfoCompat] from a list of [Signature].
         *
         * @param signatures the app signatures, which should be retrieved from the app's
         *   [PackageInfo.signatures]
         * @throws IllegalArgumentException If this is invoked on >= API 28
         */
        @JvmStatic
        @DeprecatedSinceApi(28, "Use SigningInfoCompat.fromSigningInfo(SigningInfo) instead")
        fun fromSignatures(signatures: List<Signature>): SigningInfoCompat {
            if (Build.VERSION.SDK_INT >= 28) {
                throw IllegalArgumentException(
                    "Use SigningInfoCompat.fromSigningInfo(SigningInfo) instead"
                )
            }
            return SigningInfoCompat(
                apkContentsSigners = emptyList(),
                publicKeys = emptySet(),
                schemeVersion = 0 /* unknown */,
                signingCertificateHistory = signatures,
                hasPastSigningCertificates = false,
                hasMultipleSigners = false
            )
        }
    }
}
