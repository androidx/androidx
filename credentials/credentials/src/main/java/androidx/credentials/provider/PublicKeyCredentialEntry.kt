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

package androidx.credentials.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.content.Context
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.credentials.CredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.R
import androidx.credentials.provider.PublicKeyCredentialEntry.Companion.toSlice
import java.time.Instant
import java.util.Collections

/**
 * A public key credential entry that is displayed on the account selector UI. This
 * entry denotes that a credential of type [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL]
 * is available for the user to select.
 *
 * Once this entry is selected, the corresponding [pendingIntent] will be invoked. The provider
 * can then show any activity they wish to. Before finishing the activity, provider must
 * set the final [androidx.credentials.GetCredentialResponse] through the
 * [PendingIntentHandler.setGetCredentialResponse] helper API.
 *
 * @property username the username of the account holding the public key credential
 * @property displayName the displayName of the account holding the public key credential
 * @property lastUsedTime the last used time of this entry. Note that this value will only be
 * distinguishable up to the milli second mark. If two entries have the same millisecond precision,
 * they will be considered to have been used at the same time
 * @property icon the icon to be displayed with this entry on the UI, must be created using
 * [Icon.createWithResource] when possible, and especially not with [Icon.createWithBitmap] as
 * the latter consumes more memory and may cause undefined behavior due to memory implications
 * on internal transactions; defaulted to a fallback public key credential icon if not provided
 * @property pendingIntent the [PendingIntent] that will get invoked when the user selects this
 * entry, must be created with a unique request code per entry,
 * with flag [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the
 * final request, and NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple
 * times
 * @property affiliatedDomain the user visible affiliated domain, a CharSequence
 * representation of a web domain or an app package name that the given credential in this
 * entry is associated with when it is different from the requesting entity, default null
 * @property entryGroupId an ID used for deduplication or grouping entries during display, always
 * set to [username]; for more info on this id, see [CredentialEntry]
 * @property isAutoSelectAllowedFromOption whether the [beginGetCredentialOption] request
 * for which this entry was created allows this entry to be auto-selected
 * @property hasDefaultIcon whether this entry was created without a custom icon and hence
 * contains a default icon set by the library, only to be used in Android API levels >= 28
 *
 * @throws IllegalArgumentException If [username] is empty
 *
 * @see CredentialEntry
 */
@RequiresApi(26)
class PublicKeyCredentialEntry internal constructor(
    val username: CharSequence,
    val displayName: CharSequence?,
    val typeDisplayName: CharSequence,
    val pendingIntent: PendingIntent,
    val icon: Icon,
    val lastUsedTime: Instant?,
    val isAutoSelectAllowed: Boolean,
    beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption,
    isDefaultIconPreferredAsSingleProvider: Boolean,
    entryGroupId: CharSequence? = username,
    affiliatedDomain: CharSequence? = null,
    autoSelectAllowedFromOption: Boolean = CredentialOption.extractAutoSelectValue(
        beginGetPublicKeyCredentialOption.candidateQueryData
    ),
    private val isCreatedFromSlice: Boolean = false,
    private val isDefaultIconFromSlice: Boolean = false,
) : CredentialEntry(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    beginGetPublicKeyCredentialOption,
    entryGroupId ?: username,
    isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
    affiliatedDomain = affiliatedDomain,
) {
    val isAutoSelectAllowedFromOption = autoSelectAllowedFromOption

    @get:JvmName("hasDefaultIcon")
    val hasDefaultIcon: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.isDefaultIcon(this)
            }
            return false
        }

    init {
        require(username.isNotEmpty()) { "username must not be empty" }
        require(typeDisplayName.isNotEmpty()) { "typeDisplayName must not be empty" }
    }

    /**
     * @constructor constructs an instance of [PublicKeyCredentialEntry]
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param username the username of the account holding the public key credential
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with a unique request code per entry,
     * with flag [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the
     * final request, and NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple
     * times
     * @param beginGetPublicKeyCredentialOption the option from the original
     * [BeginGetCredentialRequest], for which this credential entry is being added
     * @param displayName the displayName of the account holding the public key credential
     * @param lastUsedTime the last used time the credential underlying this entry was
     * used by the user, distinguishable up to the milli second mark only such that if two
     * entries have the same millisecond precision, they will be considered to have been used at
     * the same time
     * @param icon the icon to be displayed with this entry on the selector, if not set, a
     * default icon representing a public key credential type is set by the library
     * @param isAutoSelectAllowed whether this entry is allowed to be auto
     * selected if it is the only one on the UI, only takes effect if the app requesting for
     * credentials also opts for auto select
     * @param isDefaultIconPreferredAsSingleProvider when set to true, the UI prefers to render the
     * default credential type icon (see the default value of [icon]) when you are the
     * only available provider; false by default
     *
     * @throws NullPointerException If [context], [username], [pendingIntent], or
     * [beginGetPublicKeyCredentialOption] is null
     * @throws IllegalArgumentException if [username] is empty
     */
    constructor(
        context: Context,
        username: CharSequence,
        pendingIntent: PendingIntent,
        beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption,
        displayName: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon = Icon.createWithResource(context, R.drawable.ic_passkey),
        isAutoSelectAllowed: Boolean = false,
        isDefaultIconPreferredAsSingleProvider: Boolean = false,
    ) : this(
        username,
        displayName,
        context.getString(
            R.string.androidx_credentials_TYPE_PUBLIC_KEY_CREDENTIAL
        ),
        pendingIntent,
        icon,
        lastUsedTime,
        isAutoSelectAllowed,
        beginGetPublicKeyCredentialOption,
        isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider
    )

    /**
     * @constructor constructs an instance of [PublicKeyCredentialEntry]
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param username the username of the account holding the public key credential
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with a unique request code per entry,
     * with flag [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the
     * final request, and NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple
     * times
     * @param beginGetPublicKeyCredentialOption the option from the original
     * [BeginGetCredentialResponse], for which this credential entry is being added
     * @param displayName the displayName of the account holding the public key credential
     * @param lastUsedTime the last used time the credential underlying this entry was
     * used by the user, distinguishable up to the milli second mark only such that if two
     * entries have the same millisecond precision, they will be considered to have been used at
     * the same time
     * @param icon the icon to be displayed with this entry on the selector, if not set, a
     * default icon representing a public key credential type is set by the library
     * @param isAutoSelectAllowed whether this entry is allowed to be auto
     * selected if it is the only one on the UI, only takes effect if the app requesting for
     * credentials also opts for auto select
     *
     * @throws NullPointerException If [context], [username], [pendingIntent], or
     * [beginGetPublicKeyCredentialOption] is null
     * @throws IllegalArgumentException if [username] is empty
     */
    @Deprecated("Use the constructor that allows setting all parameters.",
        replaceWith = ReplaceWith("PublicKeyCredentialEntry(context, username, pendingIntent," +
            "beginGetPublicKeyCredentialOption, displayName, lastUsedTime, icon, " +
            "isAutoSelectAllowed, isDefaultIconPreferredAsSingleProvider)"),
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        context: Context,
        username: CharSequence,
        pendingIntent: PendingIntent,
        beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption,
        displayName: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon = Icon.createWithResource(context, R.drawable.ic_passkey),
        isAutoSelectAllowed: Boolean = false,
    ) : this(
        username,
        displayName,
        context.getString(
            R.string.androidx_credentials_TYPE_PUBLIC_KEY_CREDENTIAL
        ),
        pendingIntent,
        icon,
        lastUsedTime,
        isAutoSelectAllowed,
        beginGetPublicKeyCredentialOption,
        isDefaultIconPreferredAsSingleProvider = false
    )

    @RequiresApi(34)
    private object Api34Impl {

        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            PublicKeyCredentialEntry? {
            val slice = credentialEntry.slice
            return fromSlice(slice)
        }
    }

    @RequiresApi(28)
    private object Api28Impl {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun isDefaultIcon(entry: PublicKeyCredentialEntry): Boolean {
            if (entry.isCreatedFromSlice) {
                return entry.isDefaultIconFromSlice
            }
            return entry.icon.type == Icon.TYPE_RESOURCE &&
                entry.icon.resId == R.drawable.ic_passkey
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(
            entry: PublicKeyCredentialEntry
        ): Slice {
            val type = entry.type
            val title = entry.username
            val subTitle = entry.displayName
            val pendingIntent = entry.pendingIntent
            val typeDisplayName = entry.typeDisplayName
            val lastUsedTime = entry.lastUsedTime
            val icon = entry.icon
            val isAutoSelectAllowed = entry.isAutoSelectAllowed
            val beginGetPublicKeyCredentialOption = entry.beginGetCredentialOption
            val entryGroupId = entry.entryGroupId
            val affiliatedDomain = entry.affiliatedDomain
            val isDefaultIconPreferredAsSingleProvider =
                entry.isDefaultIconPreferredAsSingleProvider

            val autoSelectAllowed = if (isAutoSelectAllowed) {
                TRUE_STRING
            } else {
                FALSE_STRING
            }
            val isUsingDefaultIcon = if (isDefaultIconPreferredAsSingleProvider) {
                TRUE_STRING
            } else {
                FALSE_STRING
            }
            val sliceBuilder = Slice.Builder(
                Uri.EMPTY, SliceSpec(
                    type, REVISION_ID
                )
            )
                .addText(
                    typeDisplayName, /*subType=*/null,
                    listOf(SLICE_HINT_TYPE_DISPLAY_NAME)
                )
                .addText(
                    title, /*subType=*/null,
                    listOf(SLICE_HINT_TITLE)
                )
                .addText(
                    subTitle, /*subType=*/null,
                    listOf(SLICE_HINT_SUBTITLE)
                )
                .addText(
                    autoSelectAllowed, /*subType=*/null,
                    listOf(SLICE_HINT_AUTO_ALLOWED)
                )
                .addText(
                    beginGetPublicKeyCredentialOption.id,
                    /*subType=*/null,
                    listOf(SLICE_HINT_OPTION_ID)
                )
                .addIcon(
                    icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON)
                )
                .addText(
                    entryGroupId, /*subTypes=*/null,
                    listOf(SLICE_HINT_DEDUPLICATION_ID)
                )
                .addText(
                    affiliatedDomain, /*subTypes=*/null,
                    listOf(SLICE_HINT_AFFILIATED_DOMAIN)
                )
                .addText(
                    isUsingDefaultIcon, /*subType=*/null,
                    listOf(SLICE_HINT_IS_DEFAULT_ICON_PREFERRED)
                )
            try {
                if (entry.hasDefaultIcon) {
                    sliceBuilder.addInt(
                        /*true=*/1,
                        /*subType=*/null,
                        listOf(SLICE_HINT_DEFAULT_ICON_RES_ID)
                    )
                }
            } catch (_: IllegalStateException) {
            }

            if (entry.isAutoSelectAllowedFromOption) {
                sliceBuilder.addInt(
                    /*true=*/1,
                    /*subType=*/null,
                    listOf(SLICE_HINT_AUTO_SELECT_FROM_OPTION)
                )
            }
            if (lastUsedTime != null) {
                sliceBuilder.addLong(
                    lastUsedTime.toEpochMilli(),
                    /*subType=*/null,
                    listOf(SLICE_HINT_LAST_USED_TIME_MILLIS)
                )
            }
            sliceBuilder.addAction(
                pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(),
                /*subType=*/null
            )
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [CustomCredentialEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): PublicKeyCredentialEntry? {
            var typeDisplayName: CharSequence? = null
            var title: CharSequence? = null
            var subtitle: CharSequence? = null
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var lastUsedTime: Instant? = null
            var autoSelectAllowed = false
            var beginGetPublicKeyCredentialOptionId: CharSequence? = null
            var autoSelectAllowedFromOption = false
            var isDefaultIconPreferredAsSingleProvider = false
            var isDefaultIcon = false
            var entryGroupId: CharSequence? = null
            var affiliatedDomain: CharSequence? = null

            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_TYPE_DISPLAY_NAME)) {
                    typeDisplayName = it.text
                } else if (it.hasHint(SLICE_HINT_TITLE)) {
                    title = it.text
                } else if (it.hasHint(SLICE_HINT_SUBTITLE)) {
                    subtitle = it.text
                } else if (it.hasHint(SLICE_HINT_ICON)) {
                    icon = it.icon
                } else if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_OPTION_ID)) {
                    beginGetPublicKeyCredentialOptionId = it.text
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTime = Instant.ofEpochMilli(it.long)
                } else if (it.hasHint(SLICE_HINT_AUTO_ALLOWED)) {
                    val autoSelectValue = it.text
                    if (autoSelectValue == TRUE_STRING) {
                        autoSelectAllowed = true
                    }
                } else if (it.hasHint(SLICE_HINT_AUTO_SELECT_FROM_OPTION)) {
                    autoSelectAllowedFromOption = true
                } else if (it.hasHint(SLICE_HINT_IS_DEFAULT_ICON_PREFERRED)) {
                    val defaultIconValue = it.text
                    if (defaultIconValue == TRUE_STRING) {
                        isDefaultIconPreferredAsSingleProvider = true
                    }
                } else if (it.hasHint(SLICE_HINT_DEFAULT_ICON_RES_ID)) {
                    isDefaultIcon = true
                } else if (it.hasHint(SLICE_HINT_DEDUPLICATION_ID)) {
                    entryGroupId = it.text
                } else if (it.hasHint(SLICE_HINT_AFFILIATED_DOMAIN)) {
                    affiliatedDomain = it.text
                }
            }

            return try {
                PublicKeyCredentialEntry(
                    username = title!!,
                    displayName = subtitle,
                    typeDisplayName = typeDisplayName!!,
                    pendingIntent = pendingIntent!!,
                    icon = icon!!,
                    lastUsedTime = lastUsedTime,
                    isAutoSelectAllowed = autoSelectAllowed,
                    beginGetPublicKeyCredentialOption = BeginGetPublicKeyCredentialOption
                        .createFromEntrySlice(
                            Bundle(),
                            beginGetPublicKeyCredentialOptionId!!.toString(),
                        ),
                    entryGroupId = entryGroupId,
                    isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
                    affiliatedDomain = affiliatedDomain,
                    autoSelectAllowedFromOption = autoSelectAllowedFromOption,
                    isCreatedFromSlice = true,
                    isDefaultIconFromSlice = isDefaultIcon,
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }
    }

    companion object {
        private const val TAG = "PublicKeyCredEntry"

        private const val SLICE_HINT_TYPE_DISPLAY_NAME =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_TYPE_DISPLAY_NAME"

        private const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_USER_NAME"

        private const val SLICE_HINT_SUBTITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_CREDENTIAL_TYPE_DISPLAY_NAME"

        private const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"

        private const val SLICE_HINT_ICON =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PROFILE_ICON"

        private const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PENDING_INTENT"

        private const val SLICE_HINT_AUTO_ALLOWED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_ALLOWED"

        private const val SLICE_HINT_IS_DEFAULT_ICON_PREFERRED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_IS_DEFAULT_ICON_PREFERRED"

        private const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_OPTION_ID"

        private const val SLICE_HINT_AUTO_SELECT_FROM_OPTION =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_SELECT_FROM_OPTION"

        private const val SLICE_HINT_DEFAULT_ICON_RES_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEFAULT_ICON_RES_ID"

        private const val SLICE_HINT_AFFILIATED_DOMAIN =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AFFILIATED_DOMAIN"

        private const val SLICE_HINT_DEDUPLICATION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEDUPLICATION_ID"

        private const val TRUE_STRING = "true"

        private const val FALSE_STRING = "false"

        private const val REVISION_ID = 1

        /**
         * Converts an instance of [PublicKeyCredentialEntry] to a [Slice].
         *
         * This method is only expected to be called on an API > 28
         * impl, hence returning null for other levels as the
         * visibility is only restricted to the library.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(
            entry: PublicKeyCredentialEntry
        ): Slice? {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.toSlice(entry)
            }
            return null
        }

        /**
         * Returns an instance of [CustomCredentialEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun fromSlice(slice: Slice): PublicKeyCredentialEntry? {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.fromSlice(slice)
            }
            return null
        }

        /**
         * Converts a framework [android.service.credentials.CredentialEntry] class to a Jetpack
         * [PublicKeyCredentialEntry] class
         *
         * Note that this API is not needed in a general credential retrieval flow that is
         * implemented using this jetpack library, where you are only required to construct
         * an instance of [CredentialEntry] to populate the [BeginGetCredentialResponse].
         *
         * @param credentialEntry the instance of framework class to be converted
         */
        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            PublicKeyCredentialEntry? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromCredentialEntry(credentialEntry)
            }
            return null
        }
    }

    /**
     * Builder for [PublicKeyCredentialEntry]
     */
    class Builder(
        private val context: Context,
        private val username: CharSequence,
        private val pendingIntent: PendingIntent,
        private val beginGetPublicKeyCredentialOption: BeginGetPublicKeyCredentialOption
    ) {
        private var displayName: CharSequence? = null
        private var lastUsedTime: Instant? = null
        private var icon: Icon? = null
        private var autoSelectAllowed: Boolean = false
        private var isDefaultIconPreferredAsSingleProvider: Boolean = false

        /** Sets a displayName to be shown on the UI with this entry */
        fun setDisplayName(displayName: CharSequence?): Builder {
            this.displayName = displayName
            return this
        }

        /** Sets the icon to be shown on the UI with this entry */
        fun setIcon(icon: Icon): Builder {
            this.icon = icon
            return this
        }

        /**
         * Sets whether the entry should be auto-selected.
         * The value is false by default
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setAutoSelectAllowed(autoSelectAllowed: Boolean): Builder {
            this.autoSelectAllowed = autoSelectAllowed
            return this
        }

        /**
         * Sets the last used time of this account
         *
         * This information will be used to sort the entries on the selector.
         */
        fun setLastUsedTime(lastUsedTime: Instant?): Builder {
            this.lastUsedTime = lastUsedTime
            return this
        }

        /**
         * When set to true, the UI prefers to render the default credential type icon when you are
         * the single available provider; false by default.
         */
        fun setDefaultIconPreferredAsSingleProvider(
            isDefaultIconPreferredAsSingleProvider: Boolean
        ): Builder {
            this.isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider
            return this
        }

        /** Builds an instance of [PublicKeyCredentialEntry] */
        fun build(): PublicKeyCredentialEntry {
            if (icon == null && Build.VERSION.SDK_INT >= 23) {
                icon = Icon.createWithResource(context, R.drawable.ic_passkey)
            }
            val typeDisplayName = context.getString(
                R.string.androidx_credentials_TYPE_PUBLIC_KEY_CREDENTIAL
            )
            return PublicKeyCredentialEntry(
                username,
                displayName,
                typeDisplayName,
                pendingIntent,
                icon!!,
                lastUsedTime,
                autoSelectAllowed,
                beginGetPublicKeyCredentialOption,
                isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider
            )
        }
    }
}
