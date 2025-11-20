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
import android.os.Bundle
import androidx.annotation.DeprecatedSinceApi
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.provider.utils.PrivilegedApp
import androidx.credentials.provider.utils.RequestValidationUtil
import java.security.MessageDigest
import org.json.JSONException
import org.json.JSONObject

/**
 * Information pertaining to the calling application.
 *
 * @property packageName the calling package name of the calling app
 * @property signingInfo the signingInfo associated with the calling app, added at API level 28
 * @property signingInfoCompat the signing information associated with the calling app, which can be
 *   used across all Android API levels
 */
class CallingAppInfo
private constructor(
    val packageName: String,
    internal val origin: String?,
    val signingInfoCompat: SigningInfoCompat,
    signingInfo: SigningInfo?,
) {

    lateinit var signingInfo: SigningInfo
        private set
        @RequiresApi(28) get

    init {
        if (Build.VERSION.SDK_INT >= 28) {
            this.signingInfo = signingInfo!!
        }
    }

    /**
     * Constructs an instance of [CallingAppInfo]
     *
     * @param packageName the calling package name of the calling app
     * @param signingInfo the signingInfo associated with the calling app
     * @param origin the origin of the calling app. This is only set when a privileged app like a
     *   browser, calls on behalf of another application.
     * @throws NullPointerException If [packageName] is null
     * @throws NullPointerException If the class is initialized with a null [signingInfo] on Android
     *   P and above
     * @throws IllegalArgumentException If [packageName] is empty
     */
    @RequiresApi(28)
    @VisibleForTesting
    @JvmOverloads
    constructor(
        packageName: String,
        signingInfo: SigningInfo,
        origin: String? = null,
    ) : this(
        packageName = packageName,
        signingInfo = signingInfo,
        origin = origin,
        signingInfoCompat = SigningInfoCompat.fromSigningInfo(signingInfo),
    )

    /**
     * Constructs an instance of [CallingAppInfo]
     *
     * @param packageName the calling package name of the calling app
     * @param signatures the app signatures, which should be retrieved from the app's
     *   [PackageInfo.signatures]
     * @param origin the origin of the calling app. This is only set when a privileged app like a
     *   browser, calls on behalf of another application.
     * @throws NullPointerException If [packageName] is null
     * @throws NullPointerException If the class is initialized with a null [signingInfo] on Android
     *   API 28 and above
     * @throws IllegalArgumentException If [packageName] is empty
     */
    @JvmOverloads
    @VisibleForTesting
    @DeprecatedSinceApi(28, "Use the SigningInfo based constructor instead")
    constructor(
        packageName: String,
        signatures: List<Signature>,
        origin: String? = null,
    ) : this(packageName, origin, SigningInfoCompat.fromSignatures(signatures), null)

    companion object {
        /**
         * Constructs an instance of [CallingAppInfo]
         *
         * @param packageName the calling package name of the calling app
         * @param signingInfo the signingInfo associated with the calling app
         * @param origin the origin of the calling app. This is only set when a privileged app like
         *   a browser, calls on behalf of another application.
         * @throws NullPointerException If [packageName] is null
         * @throws NullPointerException If the class is initialized with a null [signingInfo] on
         *   Android P and above
         * @throws IllegalArgumentException If [packageName] is empty
         */
        @RequiresApi(28)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(packageName: String, signingInfo: SigningInfo, origin: String? = null) =
            CallingAppInfo(packageName, signingInfo, origin)

        /**
         * Constructs an instance of [CallingAppInfo]
         *
         * @param packageName the calling package name of the calling app
         * @param signatures the app signatures, which should be retrieved from the app's
         *   [PackageInfo.signatures]
         * @param origin the origin of the calling app. This is only set when a privileged app like
         *   a browser, calls on behalf of another application.
         * @throws NullPointerException If [packageName] is null
         * @throws NullPointerException If the class is initialized with a null [signingInfo] on
         *   Android API 28 and above
         * @throws IllegalArgumentException If [packageName] is empty
         */
        @DeprecatedSinceApi(28, "Use the SigningInfo based constructor instead")
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun create(packageName: String, signatures: List<Signature>, origin: String? = null) =
            CallingAppInfo(packageName, signatures, origin)

        internal const val EXTRA_CREDENTIAL_REQUEST_ORIGIN =
            "androidx.credentials.provider.extra.CREDENTIAL_REQUEST_ORIGIN"
        private const val EXTRA_CREDENTIAL_REQUEST_PACKAGE_NAME =
            "androidx.credentials.provider.extra.CREDENTIAL_REQUEST_PACKAGE_NAME"
        private const val EXTRA_CREDENTIAL_REQUEST_SIGNING_INFO =
            "androidx.credentials.provider.extra.CREDENTIAL_REQUEST_SIGNING_INFO"
        private const val EXTRA_CREDENTIAL_REQUEST_SIGNATURES =
            "androidx.credentials.provider.extra.CREDENTIAL_REQUEST_SIGNATURES"

        /**
         * Sets the [info] object to the [Bundle] object in question. The [info] object must then be
         * retrieved using [extractCallingAppInfo].
         *
         * @param info the [androidx.credentials.provider.CallingAppInfo] object to be set on the
         *   bundle
         */
        internal fun Bundle.setCallingAppInfo(info: CallingAppInfo) {
            this.putString(EXTRA_CREDENTIAL_REQUEST_ORIGIN, info.origin)
            this.putString(EXTRA_CREDENTIAL_REQUEST_PACKAGE_NAME, info.packageName)
            if (Build.VERSION.SDK_INT >= 28) {
                this.putParcelable(EXTRA_CREDENTIAL_REQUEST_SIGNING_INFO, info.signingInfo)
            } else {
                this.putParcelableArray(
                    EXTRA_CREDENTIAL_REQUEST_SIGNATURES,
                    info.signingInfoCompat.signingCertificateHistory.toTypedArray(),
                )
            }
        }

        /**
         * Retrieves the [androidx.credentials.provider.CallingAppInfo] object from a [Bundle]
         * instance, only if the [androidx.credentials.provider.CallingAppInfo] object was
         * previously set through [setCallingAppInfo].
         *
         * @param bundle the [Bundle] object that holds a
         *   [androidx.credentials.provider.CallingAppInfo] object
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun extractCallingAppInfo(bundle: Bundle): CallingAppInfo? {
            val origin = bundle.getString(EXTRA_CREDENTIAL_REQUEST_ORIGIN)
            val packageName = bundle.getString(EXTRA_CREDENTIAL_REQUEST_PACKAGE_NAME) ?: return null
            return if (Build.VERSION.SDK_INT >= 28) {
                @Suppress("DEPRECATION")
                val signingInfo: SigningInfo =
                    bundle.getParcelable<SigningInfo>(EXTRA_CREDENTIAL_REQUEST_SIGNING_INFO)
                        ?: return null
                create(packageName, signingInfo, origin)
            } else {
                @Suppress("DEPRECATION")
                val signatures: List<Signature> =
                    bundle.getParcelableArray(EXTRA_CREDENTIAL_REQUEST_SIGNATURES)?.map {
                        it as Signature
                    } ?: return null
                create(packageName, signatures, origin)
            }
        }
    }

    /**
     * Returns the origin of the calling app. This is only non-null if a privileged app like a
     * browser calls Credential Manager APIs on behalf of another application.
     *
     * Additionally, in order to get the origin, the credential provider must provide an allowlist
     * of privileged browsers/apps that it trusts. This allowlist must be in the form of a valid,
     * non-empty JSON. The origin will only be returned if the [packageName] and the SHA256 hash of
     * the newest signature obtained from the [signingInfo], is present in the
     * [privilegedAllowlist].
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
     * for a given fingerprint is specified as 'userdebug', that fingerprint will only be considered
     * if the device is on a 'userdebug' build, as determined by [Build.TYPE].
     *
     * @throws IllegalArgumentException If [privilegedAllowlist] is empty, or an invalid JSON, or
     *   does not follow the format detailed above
     * @throws IllegalStateException If the origin is non-null, but the [packageName] and
     *   [signingInfo] do not have a match in the [privilegedAllowlist]
     */
    fun getOrigin(privilegedAllowlist: String): String? {
        if (!RequestValidationUtil.isValidJSON(privilegedAllowlist)) {
            throw IllegalArgumentException(
                "privilegedAllowlist must not be " + "empty, and must be a valid JSON"
            )
        }
        if (origin == null) {
            // If origin is null, then this is not a privileged call
            return origin
        }
        try {
            if (
                isAppPrivileged(
                    PrivilegedApp.extractPrivilegedApps(JSONObject(privilegedAllowlist))
                )
            ) {
                return origin
            }
        } catch (_: JSONException) {
            throw IllegalArgumentException("privilegedAllowlist must be formatted properly")
        }
        throw IllegalStateException(
            "Origin is not being returned as the calling app did not" +
                "match the privileged allowlist"
        )
    }

    /**
     * Returns true if the [origin] is populated, and false otherwise.
     *
     * Note that the [origin] is only populated if a privileged app like a browser calls Credential
     * Manager APIs on behalf of another application.
     */
    fun isOriginPopulated(): Boolean {
        return origin != null
    }

    private fun isAppPrivileged(candidateApps: List<PrivilegedApp>): Boolean {
        for (app in candidateApps) {
            if (app.packageName == packageName) {
                return isAppPrivileged(app.fingerprints)
            }
        }
        return false
    }

    private fun isAppPrivileged(candidateFingerprints: Set<String>): Boolean {
        return SignatureVerifier(signingInfoCompat)
            .verifySignatureFingerprints(candidateFingerprints)
    }

    init {
        require(packageName.isNotEmpty()) { "packageName must not be empty" }
    }

    private class SignatureVerifier(private val signingInfoCompat: SigningInfoCompat) {

        private fun getSignatureFingerprints(): Set<String> {
            val fingerprints = mutableSetOf<String>()
            val apkContentsSigners = signingInfoCompat.apkContentsSigners
            if (signingInfoCompat.hasMultipleSigners && apkContentsSigners.isNotEmpty()) {
                fingerprints.addAll(convertToFingerprints(apkContentsSigners))
            } else if (signingInfoCompat.signingCertificateHistory.isNotEmpty()) {
                fingerprints.addAll(
                    convertToFingerprints(listOf(signingInfoCompat.signingCertificateHistory[0]))
                )
            }
            return fingerprints
        }

        private fun convertToFingerprints(signatures: List<Signature>): Set<String> {
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
            return if (signingInfoCompat.hasMultipleSigners) {
                candidateSigFingerprints.containsAll(appSigFingerprints)
            } else {
                candidateSigFingerprints.intersect(appSigFingerprints).isNotEmpty()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is CallingAppInfo) {
            return false
        }
        return packageName == other.packageName &&
            origin == other.origin &&
            signingInfoCompat == other.signingInfoCompat
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + (origin?.hashCode() ?: 0)
        result = 31 * result + signingInfoCompat.hashCode()
        return result
    }
}
