/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.biometric

import android.Manifest.permission.SET_BIOMETRIC_DIALOG_ADVANCED
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

/**
 * Types for configuring authentication prompt with options that are commonly used together. For
 * example, to perform a basic biometric authentication request do:
 * ```
 * val request = biometricRequest(
 *     title = "title",
 *     authFallback = BiometricRequest.Fallback.NegativeButton("cancel")
 * ) {
 *     setSubtitle("sub title")
 *     setMinStrength(BiometricRequest.Strength.Class2)
 * }
 * ```
 *
 * to perform a basic credential authentication request do:
 * ```
 * val request = credentialRequest(
 *     title = "title",
 * ) {
 *     setSubtitle("sub title")
 * }
 * ```
 *
 * Backward Compatibility Notes:
 * 1. < API 30: [Credential] (device credential only) is not supported.
 * 2. < API 28: For [Biometric.Strength.Class3] + [Biometric.Fallback.DeviceCredential],
 *    [Biometric.Strength.Class3.cryptoObject] cannot be used
 * 3. API 28/29: [Biometric.Strength.Class3] + [Biometric.Fallback.DeviceCredential] is not
 *    supported.
 */
public abstract class AuthenticationRequest internal constructor() {
    public companion object {
        /**
         * Construct an instance of [Biometric] that includes a set of configurable options for how
         * the biometric prompt should appear and behave with biometric authentication with
         * fallbacks.
         */
        @JvmSynthetic
        @Suppress("MissingJvmstatic")
        public inline fun biometricRequest(
            title: String,
            authFallback: Biometric.Fallback,
            init: Biometric.Builder.() -> Unit,
        ): Biometric = Biometric.Builder(title, authFallback).apply(init).build()

        /**
         * Construct an instance of [Credential] that includes a set of configurable options for how
         * the prompt should appear and behave with device credential authentication.
         */
        @RequiresApi(Build.VERSION_CODES.R)
        @JvmSynthetic
        @Suppress("MissingJvmstatic")
        public fun credentialRequest(
            title: String,
            init: Credential.Builder.() -> Unit,
        ): Credential = Credential.Builder(title).apply(init).build()
    }

    /**
     * A set of configurable options for how the [BiometricPrompt] should appear and behave with
     * biometric authentication with fallbacks.
     *
     * @property title The title of the prompt.
     * @property authFallback The [Fallback] for the biometric authentication.
     * @property minStrength The minimum biometric strength for the authentication. Note that
     *   **Class 3** biometrics are guaranteed to meet the requirements for **Class 2** and thus
     *   will also be accepted.
     * @property subtitle The optional subtitle of the prompt.
     * @property content The optional [BodyContent] of the prompt.
     * @property isConfirmationRequired Whether user confirmation should be required for passive
     *   biometrics.
     */
    public class Biometric
    private constructor(
        public val title: String,
        public val authFallback: Fallback,
        public val minStrength: Strength = Strength.Class2,
        public val subtitle: String? = null,
        public val content: BodyContent? = null,
        public val isConfirmationRequired: Boolean = true,
        @get:RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        public val logoBitmap: Bitmap? = null,
        @get:RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        @DrawableRes
        public val logoRes: Int = 0,
        @get:RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        public val logoDescription: String? = null,
    ) : AuthenticationRequest() {

        /**
         * Builder used to create an instance of [Biometric].
         *
         * @param title The title of the prompt.
         * @param authFallback The [Fallback] for the biometric authentication.
         */
        public class Builder(private val title: String, private val authFallback: Fallback) {
            private var minStrength: Strength = Strength.Class2
            private var subtitle: String? = null
            private var content: BodyContent? = null
            private var isConfirmationRequired: Boolean = true
            private var logoBitmap: Bitmap? = null
            @DrawableRes private var logoRes: Int = 0
            private var logoDescription: String? = null

            /**
             * The minimum biometric strength for the authentication. Note that **Class 3**
             * biometrics are guaranteed to meet the requirements for **Class 2** and thus will also
             * be accepted.
             */
            public fun setMinStrength(minStrength: Strength): Builder = apply {
                this.minStrength = minStrength
            }

            /** The optional subtitle of the prompt. */
            public fun setSubtitle(subtitle: String?): Builder = apply { this.subtitle = subtitle }

            /** The optional [AuthenticationRequest.BodyContent] of the prompt. */
            public fun setContent(content: BodyContent?): Builder = apply { this.content = content }

            /** Whether user confirmation should be required for passive biometrics. */
            @Suppress("MissingGetterMatchingBuilder")
            public fun setIsConfirmationRequired(isConfirmationRequired: Boolean): Builder = apply {
                this.isConfirmationRequired = isConfirmationRequired
            }

            /**
             * The optional bitmap drawable of the logo that will be shown on the prompt. Note that
             * using this method is not recommended in most scenarios because the calling
             * application's icon will be used by default. Setting the logo is intended for large
             * bundled applications that perform a wide range of functions and need to show distinct
             * icons for each function. This requires [SET_BIOMETRIC_DIALOG_ADVANCED] permission.
             */
            @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
            public fun setLogoBitmap(logoBitmap: Bitmap?): Builder = apply {
                this.logoBitmap = logoBitmap
            }

            /**
             * The optional drawable resource of the logo that will be shown on the prompt. Note
             * that using this method is not recommended in most scenarios because the calling
             * application's icon will be used by default. Setting the logo is intended for large
             * bundled applications that perform a wide range of functions and need to show distinct
             * icons for each function. This requires [SET_BIOMETRIC_DIALOG_ADVANCED] permission.
             */
            @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
            public fun setLogoRes(@DrawableRes logoRes: Int): Builder = apply {
                this.logoRes = logoRes
            }

            /**
             * The optional logo description text that will be shown on the prompt. Note that using
             * this method is not recommended in most scenarios because the calling application's
             * name will be used by default. Setting the logo description is intended for large
             * bundled applications that perform a wide range of functions and need to show distinct
             * description for each function. This requires [SET_BIOMETRIC_DIALOG_ADVANCED]
             * permission.
             */
            @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
            public fun setLogoDescription(logoDescription: String?): Builder = apply {
                this.logoDescription = logoDescription
            }

            /** Construct an instance of [Biometric]. */
            public fun build(): Biometric {
                return Biometric(
                    title = title,
                    authFallback = authFallback,
                    minStrength = minStrength,
                    subtitle = subtitle,
                    content = content,
                    isConfirmationRequired = isConfirmationRequired,
                    logoBitmap = logoBitmap,
                    logoRes = logoRes,
                    logoDescription = logoDescription,
                )
            }
        }

        /**
         * Fallback options for the biometric authentication. This may be triggered by the user
         * manually pressing the button, or biometric fails too many times and is locked out.
         */
        public abstract class Fallback private constructor() {
            /**
             * Device credential as the fallback.
             *
             * Note that on API 28-29, device credential fallback cannot be used if biometric
             * strength is Class3
             */
            public object DeviceCredential : Fallback()

            /**
             * A customized negative button as the fallback.
             *
             * @property negativeButtonText The text of the button.
             */
            public class NegativeButton(public val negativeButtonText: String) : Fallback()
        }

        /** Types of biometric strength for the prompt. */
        public abstract class Strength private constructor() {
            /**
             * Class 2 (formerly Weak).
             *
             * @see BiometricManager.Authenticators.BIOMETRIC_WEAK
             */
            public object Class2 : Strength()

            /**
             * Class 3 (formerly Strong).
             *
             * @property cryptoObject An optional cryptographic object to be associated with the
             *   authentication. Note that prior to API 30, crypto object cannot be used together
             *   with device credential fallback.
             * @see BiometricManager.Authenticators.BIOMETRIC_STRONG
             */
            public class Class3
            @JvmOverloads
            public constructor(public val cryptoObject: BiometricPrompt.CryptoObject? = null) :
                Strength()

            /** [Strength] to [BiometricManager.Authenticators]. */
            @BiometricManager.AuthenticatorTypes
            internal fun toAuthenticationType(): Int {
                return if (this is Class2) {
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
                } else {
                    BiometricManager.Authenticators.BIOMETRIC_STRONG
                }
            }
        }
    }

    /**
     * A set of configurable options for how the [BiometricPrompt] should appear and behave with
     * device credential only.
     *
     * Note that Device-credential-only authentication is not supported prior to API 30.
     *
     * @property title The title of the prompt.
     * @property subtitle The optional subtitle of the prompt.
     * @property content The optional [BodyContent] of the prompt.
     * @property cryptoObject An optional cryptographic object to be associated with the
     *   authentication.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public class Credential
    private constructor(
        public val title: String,
        public val subtitle: String?,
        public val content: BodyContent?,
        public val cryptoObject: BiometricPrompt.CryptoObject?,
    ) : AuthenticationRequest() {

        /**
         * Builder used to create an instance of [Credential].
         *
         * @param title The title of the prompt.
         */
        public class Builder(private val title: String) {
            private var subtitle: String? = null
            private var content: BodyContent? = null
            private var cryptoObject: BiometricPrompt.CryptoObject? = null

            /** The optional subtitle of the prompt. */
            public fun setSubtitle(subtitle: String?): Builder = apply { this.subtitle = subtitle }

            /** The optional [BodyContent] of the prompt. */
            public fun setContent(content: BodyContent?): Builder = apply { this.content = content }

            /** An optional cryptographic object to be associated with the authentication. */
            public fun setCryptoObject(cryptoObject: BiometricPrompt.CryptoObject?): Builder =
                apply {
                    this.cryptoObject = cryptoObject
                }

            /** Construct an instance of [Credential]. */
            public fun build(): Credential {
                return Credential(
                    title = title,
                    subtitle = subtitle,
                    content = content,
                    cryptoObject = cryptoObject,
                )
            }
        }
    }

    /** Types of the body content to be displayed on the prompt. */
    public abstract class BodyContent private constructor() {
        /**
         * Plain text description as body content.
         *
         * @property description The description to display.
         */
        public class PlainText public constructor(public val description: String) : BodyContent()

        /**
         * A vertical list as body content.
         *
         * @property description The description of this list.
         * @property items The [PromptContentItem] to display on the list.
         */
        public class VerticalList
        @JvmOverloads
        public constructor(
            public val description: String? = null,
            public val items: List<PromptContentItem> = listOf(),
        ) : BodyContent()

        /**
         * A view with "more options" button.
         *
         * This button should be used to provide more options for sign in or other purposes, such as
         * when a user needs to select between multiple app-specific accounts or profiles that are
         * available for sign in.
         *
         * Apps should avoid using this when possible because it will create additional steps that
         * the user must navigate through - clicking the more options button will dismiss the
         * prompt, provide the app an opportunity to ask the user for the correct option, and
         * finally allow the app to decide how to proceed once selected. This requires
         * [SET_BIOMETRIC_DIALOG_ADVANCED] permission.
         *
         * @property description The description of this view.
         */
        public class ContentViewWithMoreOptionsButton
        @RequiresPermission(SET_BIOMETRIC_DIALOG_ADVANCED)
        @JvmOverloads
        public constructor(public val description: String? = null) : BodyContent()
    }
}
