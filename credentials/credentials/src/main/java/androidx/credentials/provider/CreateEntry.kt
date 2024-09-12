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
@file:Suppress("deprecation") // For usage of Slice

package androidx.credentials.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.CredentialManager
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.provider.CreateEntry.Api28Impl.addToSlice
import androidx.credentials.provider.utils.CryptoObjectUtils.getOperationHandle
import java.time.Instant
import java.util.Collections

/**
 * An entry to be shown on the selector during a create flow initiated when an app calls
 * [CredentialManager.createCredential]
 *
 * A [CreateEntry] points to a location such as an account, or a group where the credential can be
 * registered. When user selects this entry, the corresponding [PendingIntent] is fired, and the
 * credential creation can be completed.
 *
 * @property accountName the name of the account where the credential will be saved
 * @property pendingIntent the [PendingIntent] that will get invoked when the user selects this
 *   entry, must be created with a unique request code per entry, with flag
 *   [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the final request, and NOT
 *   with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
 * @property description the localized description shown on UI about where the credential is stored
 * @property icon the icon to be displayed with this entry on the UI, must be created using
 *   [Icon.createWithResource] when possible, and especially not with [Icon.createWithBitmap] as the
 *   latter consumes more memory and may cause undefined behavior due to memory implications on
 *   internal transactions
 * @property lastUsedTime the last time the account underlying this entry was used by the user,
 *   distinguishable up to the milli second mark only such that if two entries have the same
 *   millisecond precision, they will be considered to have been used at the same time
 * @property isAutoSelectAllowed whether this entry should be auto selected if it is the only entry
 *   on the selector
 * @property biometricPromptData the data that is set optionally to utilize a credential manager
 *   flow that directly handles the biometric verification and presents back the response; set to
 *   null by default, so if not opted in, the embedded biometric prompt flow will not show
 * @throws IllegalArgumentException If [accountName] is empty
 */
@RequiresApi(23)
class CreateEntry
internal constructor(
    val accountName: CharSequence,
    val pendingIntent: PendingIntent,
    val icon: Icon?,
    val description: CharSequence?,
    val lastUsedTime: Instant?,
    private val credentialCountInformationMap: MutableMap<String, Int?>,
    val isAutoSelectAllowed: Boolean,
    val biometricPromptData: BiometricPromptData? = null,
) {
    /**
     * Creates an entry to be displayed on the selector during create flows.
     *
     * @param accountName the name of the account where the credential will be saved
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     *   entry, must be created with a unique request code per entry, with flag
     *   [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the final request, and
     *   NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
     * @param description the localized description shown on UI about where the credential is stored
     * @param icon the icon to be displayed with this entry on the UI, must be created using
     *   [Icon.createWithResource] when possible, and especially not with [Icon.createWithBitmap] as
     *   the latter consumes more memory and may cause undefined behavior due to memory implications
     *   on internal transactions
     * @param lastUsedTime the last time the account underlying this entry was used by the user,
     *   distinguishable up to the milli second mark only such that if two entries have the same
     *   millisecond precision, they will be considered to have been used at the same time
     * @param passwordCredentialCount the no. of password credentials contained by the provider
     * @param publicKeyCredentialCount the no. of public key credentials contained by the provider
     * @param totalCredentialCount the total no. of credentials contained by the provider
     * @param isAutoSelectAllowed whether this entry should be auto selected if it is the only entry
     *   on the selector
     * @constructor constructs an instance of [CreateEntry]
     * @throws IllegalArgumentException If [accountName] is empty, or if [description] is longer
     *   than 300 characters (important: make sure your descriptions across all locales are within
     *   this limit)
     * @throws NullPointerException If [accountName] or [pendingIntent] is null
     */
    constructor(
        accountName: CharSequence,
        pendingIntent: PendingIntent,
        description: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon? = null,
        @Suppress("AutoBoxing") passwordCredentialCount: Int? = null,
        @Suppress("AutoBoxing") publicKeyCredentialCount: Int? = null,
        @Suppress("AutoBoxing") totalCredentialCount: Int? = null,
        isAutoSelectAllowed: Boolean = false
    ) : this(
        accountName = accountName,
        pendingIntent = pendingIntent,
        icon = icon,
        description = description,
        lastUsedTime = lastUsedTime,
        credentialCountInformationMap =
            mutableMapOf(
                PasswordCredential.TYPE_PASSWORD_CREDENTIAL to passwordCredentialCount,
                PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL to publicKeyCredentialCount,
                TYPE_TOTAL_CREDENTIAL to totalCredentialCount
            ),
        isAutoSelectAllowed = isAutoSelectAllowed
    )

    /**
     * Creates an entry to be displayed on the selector during create flows.
     *
     * @param accountName the name of the account where the credential will be saved
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     *   entry, must be created with a unique request code per entry, with flag
     *   [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the final request, and
     *   NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
     * @param description the localized description shown on UI about where the credential is stored
     * @param icon the icon to be displayed with this entry on the UI, must be created using
     *   [Icon.createWithResource] when possible, and especially not with [Icon.createWithBitmap] as
     *   the latter consumes more memory and may cause undefined behavior due to memory implications
     *   on internal transactions
     * @param lastUsedTime the last time the account underlying this entry was used by the user,
     *   distinguishable up to the milli second mark only such that if two entries have the same
     *   millisecond precision, they will be considered to have been used at the same time
     * @param passwordCredentialCount the no. of password credentials contained by the provider
     * @param publicKeyCredentialCount the no. of public key credentials contained by the provider
     * @param totalCredentialCount the total no. of credentials contained by the provider
     * @param isAutoSelectAllowed whether this entry should be auto selected if it is the only entry
     *   on the selector
     * @param biometricPromptData the data that is set optionally to utilize a credential manager
     *   flow that directly handles the biometric verification and presents back the response; set
     *   to null by default, so if not opted in, the embedded biometric prompt flow will not show
     * @constructor constructs an instance of [CreateEntry]
     * @throws IllegalArgumentException If [accountName] is empty, or if [description] is longer
     *   than 300 characters (important: make sure your descriptions across all locales are within
     *   this limit)
     * @throws NullPointerException If [accountName] or [pendingIntent] is null
     */
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    constructor(
        accountName: CharSequence,
        pendingIntent: PendingIntent,
        description: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon? = null,
        @Suppress("AutoBoxing") passwordCredentialCount: Int? = null,
        @Suppress("AutoBoxing") publicKeyCredentialCount: Int? = null,
        @Suppress("AutoBoxing") totalCredentialCount: Int? = null,
        isAutoSelectAllowed: Boolean = false,
        biometricPromptData: BiometricPromptData? = null,
    ) : this(
        accountName = accountName,
        pendingIntent = pendingIntent,
        icon = icon,
        description = description,
        lastUsedTime = lastUsedTime,
        credentialCountInformationMap =
            mutableMapOf(
                PasswordCredential.TYPE_PASSWORD_CREDENTIAL to passwordCredentialCount,
                PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL to publicKeyCredentialCount,
                TYPE_TOTAL_CREDENTIAL to totalCredentialCount
            ),
        isAutoSelectAllowed = isAutoSelectAllowed,
        biometricPromptData = biometricPromptData,
    )

    init {
        require(accountName.isNotEmpty()) { "accountName must not be empty" }
        if (description != null) {
            require(description.length <= DESCRIPTION_MAX_CHAR_LIMIT) {
                "Description must follow a limit of 300 characters."
            }
        }
    }

    /** Returns the no. of password type credentials that the provider with this entry has. */
    @Suppress("AutoBoxing")
    fun getPasswordCredentialCount(): Int? {
        return credentialCountInformationMap[PasswordCredential.TYPE_PASSWORD_CREDENTIAL]
    }

    /** Returns the no. of public key type credentials that the provider with this entry has. */
    @Suppress("AutoBoxing")
    fun getPublicKeyCredentialCount(): Int? {
        return credentialCountInformationMap[PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL]
    }

    /**
     * Returns the no. of total credentials that the provider with this entry has.
     *
     * This total count is not necessarily equal to the sum of [getPasswordCredentialCount] and
     * [getPublicKeyCredentialCount].
     */
    @Suppress("AutoBoxing")
    fun getTotalCredentialCount(): Int? {
        return credentialCountInformationMap[TYPE_TOTAL_CREDENTIAL]
    }

    /**
     * A builder for [CreateEntry]
     *
     * @param accountName the name of the account where the credential will be registered
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     *   entry, must be created with a unique request code per entry, with flag
     *   [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the final request, and
     *   NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
     * @constructor constructs an instance of [CreateEntry.Builder]
     */
    class Builder
    constructor(private val accountName: CharSequence, private val pendingIntent: PendingIntent) {
        private var credentialCountInformationMap: MutableMap<String, Int?> = mutableMapOf()
        private var icon: Icon? = null
        private var description: CharSequence? = null
        private var lastUsedTime: Instant? = null
        private var passwordCredentialCount: Int? = null
        private var publicKeyCredentialCount: Int? = null
        private var totalCredentialCount: Int? = null
        private var autoSelectAllowed: Boolean = false
        private var biometricPromptData: BiometricPromptData? = null

        /** Sets whether the entry should be auto-selected. The value is false by default. */
        @Suppress("MissingGetterMatchingBuilder")
        fun setAutoSelectAllowed(autoSelectAllowed: Boolean): Builder {
            this.autoSelectAllowed = autoSelectAllowed
            return this
        }

        /**
         * Sets the password credential count, denoting how many credentials of type
         * [PasswordCredential.TYPE_PASSWORD_CREDENTIAL] does the provider have stored.
         *
         * This information will be displayed on the [CreateEntry] to help the user make a choice.
         */
        fun setPasswordCredentialCount(count: Int): Builder {
            passwordCredentialCount = count
            credentialCountInformationMap[PasswordCredential.TYPE_PASSWORD_CREDENTIAL] = count
            return this
        }

        /**
         * Sets the password credential count, denoting how many credentials of type
         * [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL] does the provider have stored.
         *
         * This information will be displayed on the [CreateEntry] to help the user make a choice.
         */
        fun setPublicKeyCredentialCount(count: Int): Builder {
            publicKeyCredentialCount = count
            credentialCountInformationMap[PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL] = count
            return this
        }

        /**
         * Sets the total credential count, denoting how many credentials in total does the provider
         * have stored.
         *
         * This total count no. does not need to be a total of the counts set through
         * [setPasswordCredentialCount] and [setPublicKeyCredentialCount].
         *
         * This information will be displayed on the [CreateEntry] to help the user make a choice.
         */
        fun setTotalCredentialCount(count: Int): Builder {
            totalCredentialCount = count
            credentialCountInformationMap[TYPE_TOTAL_CREDENTIAL] = count
            return this
        }

        /** Sets an icon to be displayed with the entry on the UI */
        fun setIcon(icon: Icon?): Builder {
            this.icon = icon
            return this
        }

        /**
         * Sets a localized description to be displayed on the UI at the time of credential
         * creation.
         *
         * Typically this description should contain information informing the user of the
         * credential being created, and where it is being stored. Providers are free to phrase this
         * however they see fit.
         *
         * @throws IllegalArgumentException if [description] is longer than 300 characters (
         *   important: make sure your descriptions across all locales are within this limit).
         */
        fun setDescription(description: CharSequence?): Builder {
            if (description?.length != null && description.length > DESCRIPTION_MAX_CHAR_LIMIT) {
                throw IllegalArgumentException("Description must follow a limit of 300 characters.")
            }
            this.description = description
            return this
        }

        /** Sets the last time this account was used */
        fun setLastUsedTime(lastUsedTime: Instant?): Builder {
            this.lastUsedTime = lastUsedTime
            return this
        }

        /**
         * Sets the biometric prompt data to optionally utilize a credential manager flow that
         * directly handles the biometric verification for you and gives you the response; set to
         * null by default, indicating the default behavior is to not utilize this embedded
         * biometric prompt flow.
         */
        @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
        fun setBiometricPromptData(biometricPromptData: BiometricPromptData): Builder {
            this.biometricPromptData = biometricPromptData
            return this
        }

        /**
         * Builds an instance of [CreateEntry]
         *
         * @throws IllegalArgumentException If [accountName] is empty
         */
        fun build(): CreateEntry {
            return CreateEntry(
                accountName = accountName,
                pendingIntent = pendingIntent,
                icon = icon,
                description = description,
                lastUsedTime = lastUsedTime,
                credentialCountInformationMap = credentialCountInformationMap,
                isAutoSelectAllowed = autoSelectAllowed,
                biometricPromptData = biometricPromptData
            )
        }
    }

    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromCreateEntry(createEntry: android.service.credentials.CreateEntry): CreateEntry? {
            val slice = createEntry.slice
            return fromSlice(slice)
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private object Api35Impl {
        private fun addToSlice(createEntry: CreateEntry, sliceBuilder: Slice.Builder) {
            val biometricPromptData = createEntry.biometricPromptData
            if (biometricPromptData != null) {
                // TODO(b/353798766) : Remove non bundles once beta users have finalized testing
                sliceBuilder.addInt(
                    biometricPromptData.allowedAuthenticators,
                    /*subType=*/ null,
                    listOf(SLICE_HINT_ALLOWED_AUTHENTICATORS)
                )
                biometricPromptData.cryptoObject?.let {
                    sliceBuilder.addLong(
                        getOperationHandle(biometricPromptData.cryptoObject),
                        /*subType=*/ null,
                        listOf(SLICE_HINT_CRYPTO_OP_ID)
                    )
                }
                val biometricBundle = BiometricPromptData.toBundle(biometricPromptData)
                sliceBuilder.addBundle(
                    biometricBundle,
                    /*subType=*/ null,
                    listOf(SLICE_HINT_BIOMETRIC_PROMPT_DATA)
                )
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(createEntry: CreateEntry): Slice {
            val sliceBuilder = Api28Impl.addToSlice(createEntry)
            addToSlice(createEntry, sliceBuilder)
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [CustomCredentialEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [addToSlice]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CreateEntry? {
            val createEntry = Api28Impl.fromSlice(slice) ?: return null
            var biometricPromptDataBundle: Bundle? = null
            slice.items.forEach {
                if (it.hasHint(CredentialEntry.SLICE_HINT_BIOMETRIC_PROMPT_DATA)) {
                    biometricPromptDataBundle = it.bundle
                }
            }
            return try {
                CreateEntry(
                    accountName = createEntry.accountName,
                    pendingIntent = createEntry.pendingIntent,
                    icon = createEntry.icon,
                    description = createEntry.description,
                    lastUsedTime = createEntry.lastUsedTime,
                    credentialCountInformationMap = createEntry.credentialCountInformationMap,
                    isAutoSelectAllowed = createEntry.isAutoSelectAllowed,
                    biometricPromptData =
                        if (biometricPromptDataBundle != null)
                            BiometricPromptData.fromBundle(biometricPromptDataBundle!!)
                        else null
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }
    }

    @RequiresApi(28)
    private object Api28Impl {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(createEntry: CreateEntry): Slice {
            val sliceBuilder = addToSlice(createEntry)
            return sliceBuilder.build()
        }

        // Specific to only this create entry, but shared across API levels > P
        fun addToSlice(createEntry: CreateEntry): Slice.Builder {
            val accountName = createEntry.accountName
            val icon = createEntry.icon
            val description = createEntry.description
            val lastUsedTime = createEntry.lastUsedTime
            val credentialCountInformationMap = createEntry.credentialCountInformationMap
            val pendingIntent = createEntry.pendingIntent
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec(SLICE_SPEC_TYPE, REVISION_ID))
            val autoSelectAllowed =
                if (createEntry.isAutoSelectAllowed) {
                    AUTO_SELECT_TRUE_STRING
                } else {
                    AUTO_SELECT_FALSE_STRING
                }
            sliceBuilder.addText(accountName, /* subType= */ null, listOf(SLICE_HINT_ACCOUNT_NAME))
            if (lastUsedTime != null) {
                sliceBuilder.addLong(
                    lastUsedTime.toEpochMilli(),
                    /*subType=*/ null,
                    listOf(SLICE_HINT_LAST_USED_TIME_MILLIS)
                )
            }
            if (description != null) {
                sliceBuilder.addText(description, null, listOf(SLICE_HINT_NOTE))
            }
            if (icon != null) {
                sliceBuilder.addIcon(icon, /* subType= */ null, listOf(SLICE_HINT_ICON))
            }
            val credentialCountBundle =
                convertCredentialCountInfoToBundle(credentialCountInformationMap)
            if (credentialCountBundle != null) {
                sliceBuilder.addBundle(
                    convertCredentialCountInfoToBundle(credentialCountInformationMap),
                    null,
                    listOf(SLICE_HINT_CREDENTIAL_COUNT_INFORMATION)
                )
            }
            sliceBuilder
                .addAction(
                    pendingIntent,
                    Slice.Builder(sliceBuilder)
                        .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                        .build(),
                    /*subType=*/ null
                )
                .addText(
                    autoSelectAllowed,
                    /*subType=*/ null,
                    listOf(SLICE_HINT_AUTO_SELECT_ALLOWED)
                )
            return sliceBuilder
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CreateEntry? {
            var accountName: CharSequence? = null
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var credentialCountInfo: MutableMap<String, Int?> = mutableMapOf()
            var description: CharSequence? = null
            var lastUsedTime: Instant? = null
            var autoSelectAllowed = false
            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_ACCOUNT_NAME)) {
                    accountName = it.text
                } else if (it.hasHint(SLICE_HINT_ICON)) {
                    icon = it.icon
                } else if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_CREDENTIAL_COUNT_INFORMATION)) {
                    credentialCountInfo =
                        convertBundleToCredentialCountInfo(it.bundle) as MutableMap<String, Int?>
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTime = Instant.ofEpochMilli(it.long)
                } else if (it.hasHint(SLICE_HINT_NOTE)) {
                    description = it.text
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTime = Instant.ofEpochMilli(it.long)
                } else if (it.hasHint(SLICE_HINT_AUTO_SELECT_ALLOWED)) {
                    val autoSelectValue = it.text
                    if (autoSelectValue == AUTO_SELECT_TRUE_STRING) {
                        autoSelectAllowed = true
                    }
                }
            }
            return try {
                CreateEntry(
                    accountName = accountName!!,
                    pendingIntent = pendingIntent!!,
                    icon = icon,
                    description = description,
                    lastUsedTime = lastUsedTime,
                    credentialCountInformationMap = credentialCountInfo,
                    isAutoSelectAllowed = autoSelectAllowed,
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }
    }

    companion object {
        private const val TAG = "CreateEntry"
        private const val DESCRIPTION_MAX_CHAR_LIMIT = 300
        internal const val TYPE_TOTAL_CREDENTIAL = "TOTAL_CREDENTIAL_COUNT_TYPE"
        private const val SLICE_HINT_ACCOUNT_NAME =
            "androidx.credentials.provider.createEntry.SLICE_HINT_USER_PROVIDER_ACCOUNT_NAME"
        private const val SLICE_HINT_NOTE =
            "androidx.credentials.provider.createEntry.SLICE_HINT_NOTE"
        private const val SLICE_HINT_ICON =
            "androidx.credentials.provider.createEntry.SLICE_HINT_PROFILE_ICON"
        private const val SLICE_HINT_CREDENTIAL_COUNT_INFORMATION =
            "androidx.credentials.provider.createEntry.SLICE_HINT_CREDENTIAL_COUNT_INFORMATION"
        private const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.createEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"
        private const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.createEntry.SLICE_HINT_PENDING_INTENT"
        private const val SLICE_HINT_AUTO_SELECT_ALLOWED =
            "androidx.credentials.provider.createEntry.SLICE_HINT_AUTO_SELECT_ALLOWED"
        private const val SLICE_HINT_BIOMETRIC_PROMPT_DATA =
            "androidx.credentials.provider.createEntry.SLICE_HINT_BIOMETRIC_PROMPT_DATA"
        private const val SLICE_HINT_ALLOWED_AUTHENTICATORS =
            "androidx.credentials.provider.createEntry.SLICE_HINT_ALLOWED_AUTHENTICATORS"
        private const val SLICE_HINT_CRYPTO_OP_ID =
            "androidx.credentials.provider.createEntry.SLICE_HINT_CRYPTO_OP_ID"
        private const val AUTO_SELECT_TRUE_STRING = "true"
        private const val AUTO_SELECT_FALSE_STRING = "false"
        private const val SLICE_SPEC_TYPE = "CreateEntry"
        private const val REVISION_ID = 1

        /**
         * Converts an instance of [CreateEntry] to a [Slice].
         *
         * This method is only expected to be called on an API > 28 impl, hence returning null for
         * other levels as the visibility is only restricted to the library.
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toSlice(createEntry: CreateEntry): Slice? {
            if (Build.VERSION.SDK_INT >= 35) {
                return Api35Impl.toSlice(createEntry)
            } else if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.toSlice(createEntry)
            }
            return null
        }

        /**
         * Returns an instance of [CreateEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromSlice(slice: Slice): CreateEntry? {
            if (Build.VERSION.SDK_INT >= 35) {
                return Api35Impl.fromSlice(slice)
            } else if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.fromSlice(slice)
            }
            return null
        }

        /**
         * Converts a framework [android.service.credentials.CreateEntry] class to a Jetpack
         * [CreateEntry] class
         *
         * Note that this API is not needed in a general credential creation flow that is
         * implemented using this jetpack library, where you are only required to construct an
         * instance of [CreateEntry] to populate the [BeginCreateCredentialResponse].
         *
         * @param createEntry the instance of framework class to be converted
         */
        @JvmStatic
        fun fromCreateEntry(createEntry: android.service.credentials.CreateEntry): CreateEntry? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromCreateEntry(createEntry)
            }
            return null
        }

        @JvmStatic
        internal fun convertBundleToCredentialCountInfo(bundle: Bundle?): Map<String, Int?> {
            val credentialCountMap = HashMap<String, Int?>()
            if (bundle == null) {
                return credentialCountMap
            }
            bundle.keySet().forEach {
                try {
                    credentialCountMap[it] = bundle.getInt(it)
                } catch (e: Exception) {
                    Log.i(TAG, "Issue unpacking credential count info bundle: " + e.message)
                }
            }
            return credentialCountMap
        }

        @JvmStatic
        internal fun convertCredentialCountInfoToBundle(
            credentialCountInformationMap: Map<String, Int?>
        ): Bundle? {
            var foundCredentialCount = false
            val bundle = Bundle()
            credentialCountInformationMap.forEach {
                if (it.value != null) {
                    bundle.putInt(it.key, it.value!!)
                    foundCredentialCount = true
                }
            }
            if (!foundCredentialCount) {
                return null
            }
            return bundle
        }

        private const val EXTRA_CREATE_ENTRY_SIZE =
            "androidx.credentials.provider.extra.CREATE_ENTRY_SIZE"
        private const val EXTRA_CREATE_ACCOUNT_NAME_PREFIX =
            "androidx.credentials.provider.extra.ACCOUNT_NAME_"
        private const val EXTRA_CREATE_ENTRY_PENDING_INTENT_PREFIX =
            "androidx.credentials.provider.extra.PENDING_INTENT_"
        private const val EXTRA_CREATE_ENTRY_IS_AUTO_SELECT_ALLOWED_PREFIX =
            "androidx.credentials.provider.extra.IS_AUTO_SELECT_ALLOWED_"
        private const val EXTRA_CREATE_ENTRY_LAST_USED_TIME_PREFIX =
            "androidx.credentials.provider.extra.LAST_USED_TIME_"
        private const val EXTRA_CREATE_DESCRIPTION_PREFIX =
            "androidx.credentials.provider.extra.DESCRIPTION_"
        private const val EXTRA_CREATE_TYPE_ICON_PREFIX =
            "androidx.credentials.provider.extra.ICON_"
        private const val EXTRA_CREATE_CREDENTIAL_COUNT_INFO_PREFIX =
            "androidx.credentials.provider.extra.CREDENTIAL_COUNT_INFO_"

        @RequiresApi(23)
        internal fun List<CreateEntry>.marshall(bundle: Bundle) {
            bundle.putInt(EXTRA_CREATE_ENTRY_SIZE, this.size)
            this.forEachIndexed { index, entry ->
                bundle.putCharSequence("$EXTRA_CREATE_ACCOUNT_NAME_PREFIX$index", entry.accountName)
                bundle.putParcelable(
                    "$EXTRA_CREATE_ENTRY_PENDING_INTENT_PREFIX$index",
                    entry.pendingIntent
                )
                entry.icon?.let { bundle.putParcelable("$EXTRA_CREATE_TYPE_ICON_PREFIX$index", it) }
                entry.description?.let {
                    bundle.putCharSequence("$EXTRA_CREATE_DESCRIPTION_PREFIX$index", it)
                }
                // TODO: b/356939416 - provide backward compatible timestamp API.
                if (Build.VERSION.SDK_INT >= 26) {
                    entry.lastUsedTime?.let {
                        bundle.putSerializable(
                            "$EXTRA_CREATE_ENTRY_LAST_USED_TIME_PREFIX$index",
                            it
                        )
                    }
                }
                val countInfo =
                    convertCredentialCountInfoToBundle(entry.credentialCountInformationMap)
                countInfo?.let {
                    bundle.putBundle("$EXTRA_CREATE_CREDENTIAL_COUNT_INFO_PREFIX$index", it)
                }
                bundle.putBoolean(
                    "$EXTRA_CREATE_ENTRY_IS_AUTO_SELECT_ALLOWED_PREFIX$index",
                    entry.isAutoSelectAllowed
                )
            }
        }

        @RequiresApi(23)
        internal fun Bundle.unmarshallCreateEntries(): List<CreateEntry> {
            try {
                val entries = mutableListOf<CreateEntry>()
                val size = this.getInt(EXTRA_CREATE_ENTRY_SIZE, 0)
                for (index in 0 until size) {
                    val accountName =
                        this.getCharSequence("$EXTRA_CREATE_ACCOUNT_NAME_PREFIX$index")
                            ?: return emptyList()
                    val pendingIntent: PendingIntent =
                        this.getParcelable("$EXTRA_CREATE_ENTRY_PENDING_INTENT_PREFIX$index")
                            ?: return emptyList()
                    val icon: Icon? = this.getParcelable("$EXTRA_CREATE_TYPE_ICON_PREFIX$index")
                    val description: CharSequence? =
                        this.getCharSequence("$EXTRA_CREATE_DESCRIPTION_PREFIX$index")
                    val countInfo: MutableMap<String, Int?> =
                        convertBundleToCredentialCountInfo(
                                this.getBundle("$EXTRA_CREATE_CREDENTIAL_COUNT_INFO_PREFIX$index")
                            )
                            .toMutableMap()
                    val isAutoSelectAllowed =
                        this.getBoolean(
                            "$EXTRA_CREATE_ENTRY_IS_AUTO_SELECT_ALLOWED_PREFIX$index",
                            false
                        )
                    // TODO: b/356939416 - provide backward compatible timestamp API.
                    if (Build.VERSION.SDK_INT >= 26) {
                        val lastUsedTime: Instant? =
                            this.getSerializable("$EXTRA_CREATE_ENTRY_LAST_USED_TIME_PREFIX$index")
                                as Instant?
                        entries.add(
                            CreateEntry(
                                accountName = accountName,
                                pendingIntent = pendingIntent,
                                icon = icon,
                                description = description,
                                lastUsedTime = lastUsedTime,
                                credentialCountInformationMap = countInfo,
                                isAutoSelectAllowed = isAutoSelectAllowed,
                            )
                        )
                    } else {
                        entries.add(
                            CreateEntry(
                                accountName = accountName,
                                pendingIntent = pendingIntent,
                                icon = icon,
                                description = description,
                                lastUsedTime = null,
                                credentialCountInformationMap = countInfo,
                                isAutoSelectAllowed = isAutoSelectAllowed,
                            )
                        )
                    }
                }
                return entries
            } catch (e: Exception) {
                return emptyList()
            }
        }
    }
}
