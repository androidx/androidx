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

import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.provider.utils.PrivilegedApp
import androidx.credentials.provider.utils.RequestValidationUtil
import java.security.MessageDigest
import org.json.JSONException
import org.json.JSONObject

/**
 * Information pertaining to the calling application.
 *
 * @constructor constructs an instance of [CallingAppInfo]
 *
 * @param packageName the calling package name of the calling app
 * @param signingInfo the signingInfo associated with the calling app
 * @param origin the origin of the calling app. This is only set when a
 * privileged app like a browser, calls on behalf of another application.
 *
 * @throws NullPointerException If [packageName] or [signingInfo] is null
 * @throws IllegalArgumentException If [packageName] is empty
 *
 * Note : Credential providers are not expected to utilize the constructor in this class for any
 * production flow. This constructor must only be used for testing purposes.
 */
class CallingAppInfo @JvmOverloads constructor(
    val packageName: String,
    val signingInfo: SigningInfo,
    @get:RestrictTo(RestrictTo.Scope.LIBRARY)
    val origin: String? = null
) {
    internal companion object {
        private const val TAG = "CallingAppInfo"
    }

    /**
     * Returns the origin of the calling app. This is only non-null if a
     * privileged app like a browser calls Credential Manager APIs on
     * behalf of another application.
     *
     * Additionally, in order to get the origin, the credential provider must
     * provide an allowlist of privileged browsers/apps that it trusts.
     * This allowlist must be in the form of a valid, non-empty JSON. The
     * origin will only be returned if the [packageName] and the SHA256 hash of the newest
     * signature obtained from the [signingInfo], is present in the [privilegedAllowlist].
     *
     * Packages that are signed with multiple signers will only receive the origin if all of the
     * signatures are present in the [privilegedAllowlist].
     *
     * The format of this [privilegedAllowlist] JSON must adhere to the following sample.
     *
     * ```
     * {"apps": [
     *    {
     *       "type": "android",
     *       "info": {
     *          "package_name": "com.example.myapp",
     *          "signatures" : [
     *          {"build": "release",
     *              "cert_fingerprint_sha256": "59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:36:8C:5C:3A:
     *              7D:22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32"
     *          },
     *          {"build": "userdebug",
     *          "cert_fingerprint_sha256": "59:0D:2D:7B:33:6A:BD:FB:54:CD:3D:8B:36:8C:5C:3A:7D:
     *          22:67:5A:9A:85:9A:6A:65:47:FD:4C:8A:7C:30:32"
     *          }]
     *       }
     *     }
     * ]}
     * ```
     *
     * All keys in the JSON must be exactly as stated in the sample above. Note that if the build
     * for a given fingerprint is specified as 'userdebug', that fingerprint will
     * only be considered if the device is on a 'userdebug' build, as determined by [Build.TYPE].
     *
     * @throws IllegalArgumentException If [privilegedAllowlist] is empty, or an
     * invalid JSON, or does not follow the format detailed above
     * @throws IllegalStateException If the origin is non-null, but the [packageName] and
     * [signingInfo] do not have a match in the [privilegedAllowlist]
     */
    fun getOrigin(privilegedAllowlist: String): String? {
        if (!RequestValidationUtil.isValidJSON(privilegedAllowlist)) {
            throw IllegalArgumentException(
                "privilegedAllowlist must not be " +
                    "empty, and must be a valid JSON"
            )
        }
        if (origin == null) {
            // If origin is null, then this is not a privileged call
            return origin
        }
        try {
            if (isAppPrivileged(
                    PrivilegedApp.extractPrivilegedApps(
                        JSONObject(privilegedAllowlist)
                    )
                )
            ) {
                return origin
            }
        } catch (_: JSONException) {
            throw IllegalArgumentException("privilegedAllowlist must be formatted properly")
        }
        throw IllegalStateException("Origin is not being returned as the calling app did not" +
            "match the privileged allowlist")
    }

    /**
     * Returns true if the [origin] is populated, and false otherwise.
     *
     * Note that the [origin] is only populated if a privileged app like a browser calls
     * Credential Manager APIs on behalf of another application.
     */
    fun isOriginPopulated(): Boolean {
        return origin != null
    }

    private fun isAppPrivileged(
        candidateApps: List<PrivilegedApp>
    ): Boolean {
        for (app in candidateApps) {
            if (app.packageName == packageName) {
                return isAppPrivileged(app.fingerprints)
            }
        }
        return false
    }

    private fun isAppPrivileged(candidateFingerprints: Set<String>): Boolean {
        if (Build.VERSION.SDK_INT >= 28) {
            return SignatureVerifierApi28(signingInfo)
                .verifySignatureFingerprints(candidateFingerprints)
        }
        // TODO("Extend to <= 28 if needed")
        return false
    }

    init {
        require(packageName.isNotEmpty()) { "packageName must not be empty" }
    }

    @RequiresApi(28)
    private class SignatureVerifierApi28(private val signingInfo: SigningInfo) {
        private fun getSignatureFingerprints(): Set<String> {
            val fingerprints = mutableSetOf<String>()
            if (signingInfo.hasMultipleSigners() && signingInfo.apkContentsSigners != null) {
                fingerprints.addAll(convertToFingerprints(signingInfo.apkContentsSigners))
            } else if (signingInfo.signingCertificateHistory != null) {
                fingerprints.addAll(convertToFingerprints(
                    arrayOf(signingInfo.signingCertificateHistory[0])))
            }
            return fingerprints
        }

        private fun convertToFingerprints(signatures: Array<Signature>): Set<String> {
            val fingerprints = mutableSetOf<String>()
            for (signature in signatures) {
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(signature.toByteArray())
                fingerprints.add(digest.joinToString(":") { "%02X".format(it) })
            }
            return fingerprints
        }

        fun verifySignatureFingerprints(candidateSigFingerprints: Set<String>): Boolean {
            val appSigFingerprints = getSignatureFingerprints()
            return if (signingInfo.hasMultipleSigners()) {
                candidateSigFingerprints.containsAll(appSigFingerprints)
            } else {
                candidateSigFingerprints.intersect(appSigFingerprints).isNotEmpty()
            }
        }
    }
}
