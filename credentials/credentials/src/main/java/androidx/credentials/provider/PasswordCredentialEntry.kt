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
import androidx.credentials.PasswordCredential
import androidx.credentials.R
import androidx.credentials.provider.PasswordCredentialEntry.Companion.toSlice
import java.time.Instant
import java.util.Collections

/**
 * A password credential entry that is displayed on the account selector UI. This
 * entry denotes that a credential of type [PasswordCredential.TYPE_PASSWORD_CREDENTIAL]
 * is available for the user to select.
 *
 * Once this entry is selected, the corresponding [pendingIntent] will be invoked. The provider
 * can then show any activity they wish to. Before finishing the activity, provider must
 * set the final [androidx.credentials.GetCredentialResponse] through the
 * [PendingIntentHandler.setGetCredentialResponse] helper API.
 *
 * @property username the username of the account holding the password credential
 * @property displayName the displayName of the account holding the password credential
 * @property lastUsedTime the last used time of this entry, distinguishable up to the milli
 * second mark, such that if two entries have the same millisecond precision,
 * they will be considered to have been used at the same time
 * @property icon the icon to be displayed with this entry on the UI, must be created using
 * [Icon.createWithResource] when possible, and especially not with [Icon.createWithBitmap] as
 * the latter consumes more memory and may cause undefined behavior due to memory implications
 * on internal transactions; defaulted to a fallback password credential icon if not provided
 * @property pendingIntent the [PendingIntent] that will get invoked when the user selects this
 * entry, must be created with a unique request code per entry,
 * with flag [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the
 * final request, and NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple
 * times
 * @property isAutoSelectAllowed whether this entry is allowed to be auto
 * selected if it is the only one on the UI. Note that setting this value
 * to true does not guarantee this behavior. The developer must also set this to true, and the
 * framework must determine that this is the only entry available for the user.
 * @property entryGroupId an ID used for deduplication or grouping entries during display, always
 * set to [username]; for more info on this id, see [CredentialEntry]
 *
 * @throws IllegalArgumentException If [username] is empty
 *
 * @see CustomCredentialEntry
 * @see CredentialEntry
 */
@RequiresApi(26)
class PasswordCredentialEntry internal constructor(
    val username: CharSequence,
    val displayName: CharSequence?,
    val typeDisplayName: CharSequence,
    val pendingIntent: PendingIntent,
    val lastUsedTime: Instant?,
    val icon: Icon,
    val isAutoSelectAllowed: Boolean,
    beginGetPasswordOption: BeginGetPasswordOption,
    entryGroupId: CharSequence? = username,
    isDefaultIconPreferredAsSingleProvider: Boolean,
    affiliatedDomain: CharSequence? = null,
    private val autoSelectAllowedFromOption: Boolean = false,
    private val isDefaultIcon: Boolean = false
) : CredentialEntry(
    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    beginGetPasswordOption,
    entryGroupId ?: username,
    affiliatedDomain,
    isDefaultIconPreferredAsSingleProvider
) {
    init {
        require(username.isNotEmpty()) { "username must not be empty" }
    }

    /**
     * @constructor constructs an instance of [PasswordCredentialEntry]
     *
     * The [affiliatedDomain] parameter is filled if you provide a credential
     * that is not directly associated with the requesting entity, but rather originates from an
     * entity that is determined as being associated with the requesting entity through mechanisms
     * such as digital asset links.
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param username the username of the account holding the password credential
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with flag [PendingIntent.FLAG_MUTABLE] to allow the Android
     * system to attach the final request
     * @param beginGetPasswordOption the option from the original [BeginGetCredentialRequest],
     * for which this credential entry is being added
     * @param displayName the displayName of the account holding the password credential
     * @param lastUsedTime the last used time the credential underlying this entry was
     * used by the user, distinguishable up to the milli second mark only such that if two
     * entries have the same millisecond precision, they will be considered to have been used at
     * the same time
     * @param icon the icon to be displayed with this entry on the selector, if not set, a
     * default icon representing a password credential type is set by the library
     * @param isAutoSelectAllowed whether this entry is allowed to be auto
     * selected if it is the only one on the UI, only takes effect if the app requesting for
     * credentials also opts for auto select
     * @param affiliatedDomain the user visible affiliated domain, a CharSequence
     * representation of a web domain or an app package name that the given credential in this
     * entry is associated with when it is different from the requesting entity, default null
     * @param isDefaultIconPreferredAsSingleProvider when set to true, the UI prefers to render the
     * default credential type icon (see the default value of [icon]) when you are the
     * only available provider; false by default
     *
     * @throws IllegalArgumentException If [username] is empty
     * @throws NullPointerException If [context], [username], [pendingIntent], or
     * [beginGetPasswordOption] is null
     */
    constructor(
        context: Context,
        username: CharSequence,
        pendingIntent: PendingIntent,
        beginGetPasswordOption: BeginGetPasswordOption,
        displayName: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon = Icon.createWithResource(context, R.drawable.ic_password),
        isAutoSelectAllowed: Boolean = false,
        affiliatedDomain: CharSequence? = null,
        isDefaultIconPreferredAsSingleProvider: Boolean = false,
    ) : this(
        username,
        displayName,
        typeDisplayName = context.getString(
            R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL
        ),
        pendingIntent,
        lastUsedTime,
        icon,
        isAutoSelectAllowed,
        beginGetPasswordOption,
        affiliatedDomain = affiliatedDomain,
        isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
    )

    /**
     * @constructor constructs an instance of [PasswordCredentialEntry]
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param username the username of the account holding the password credential
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with flag [PendingIntent.FLAG_MUTABLE] to allow the Android
     * system to attach the final request
     * @param beginGetPasswordOption the option from the original [BeginGetCredentialRequest],
     * for which this credential entry is being added
     * @param displayName the displayName of the account holding the password credential
     * @param lastUsedTime the last used time the credential underlying this entry was
     * used by the user, distinguishable up to the milli second mark only such that if two
     * entries have the same millisecond precision, they will be considered to have been used at
     * the same time
     * @param icon the icon to be displayed with this entry on the selector, if not set, a
     * default icon representing a password credential type is set by the library
     * @param isAutoSelectAllowed whether this entry is allowed to be auto
     * selected if it is the only one on the UI, only takes effect if the app requesting for
     * credentials also opts for auto select
     *
     * @throws IllegalArgumentException If [username] is empty
     * @throws NullPointerException If [context], [username], [pendingIntent], or
     * [beginGetPasswordOption] is null
     */
    @Deprecated(
        "Use the constructor that allows setting all parameters.",
        replaceWith = ReplaceWith(
            "PasswordCredentialEntry(context, username, " +
                "pendingIntent, beginGetPasswordOption, displayName, lastUsedTime, icon, " +
                "isAutoSelectAllowed, affiliatedDomain, isDefaultIconPreferredAsSingleProvider)"
        ),
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        context: Context,
        username: CharSequence,
        pendingIntent: PendingIntent,
        beginGetPasswordOption: BeginGetPasswordOption,
        displayName: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon = Icon.createWithResource(context, R.drawable.ic_password),
        isAutoSelectAllowed: Boolean = false,
    ) : this(
        username,
        displayName,
        typeDisplayName = context.getString(
            R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL
        ),
        pendingIntent,
        lastUsedTime,
        icon,
        isAutoSelectAllowed,
        beginGetPasswordOption,
        isDefaultIconPreferredAsSingleProvider = false
    )

    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            PasswordCredentialEntry? {
            val slice = credentialEntry.slice
            return fromSlice(slice)
        }
    }

    @RequiresApi(28)
    private object Api28Impl {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(
            entry: PasswordCredentialEntry
        ): Slice {
            val type = entry.type
            val title = entry.username
            val subtitle = entry.displayName
            val pendingIntent = entry.pendingIntent
            val typeDisplayName = entry.typeDisplayName
            val lastUsedTime = entry.lastUsedTime
            val icon = entry.icon
            val isAutoSelectAllowed = entry.isAutoSelectAllowed
            val beginGetPasswordCredentialOption = entry.beginGetCredentialOption
            val affiliatedDomain = entry.affiliatedDomain
            val entryGroupId = entry.entryGroupId
            var isDefaultIconPreferredAsSingleProvider =
                entry.isDefaultIconPreferredAsSingleProvider

            val autoSelectAllowed = if (isAutoSelectAllowed) {
                TRUE_STRING
            } else {
                FALSE_STRING
            }
            val isUsingDefaultIcon =
                if (isDefaultIconPreferredAsSingleProvider) TRUE_STRING else FALSE_STRING
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
                    subtitle, /*subType=*/null,
                    listOf(SLICE_HINT_SUBTITLE)
                )
                .addText(
                    autoSelectAllowed, /*subType=*/null,
                    listOf(SLICE_HINT_AUTO_ALLOWED)
                )
                .addText(
                    beginGetPasswordCredentialOption.id,
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
                if (icon.resId == R.drawable.ic_password) {
                    sliceBuilder.addInt(
                        /*true=*/1,
                        /*subType=*/null,
                        listOf(SLICE_HINT_DEFAULT_ICON_RES_ID)
                    )
                }
            } catch (_: IllegalStateException) {
            }

            if (CredentialOption.extractAutoSelectValue(
                    beginGetPasswordCredentialOption.candidateQueryData
                )
            ) {
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
         *
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): PasswordCredentialEntry? {
            var typeDisplayName: CharSequence? = null
            var title: CharSequence? = null
            var subTitle: CharSequence? = null
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var lastUsedTime: Instant? = null
            var autoSelectAllowed = false
            var autoSelectAllowedFromOption = false
            var beginGetPasswordOptionId: CharSequence? = null
            var isDefaultIconPreferredAsSingleProvider = false
            var affiliatedDomain: CharSequence? = null
            var entryGroupId: CharSequence? = null
            var isDefaultIcon = false

            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_TYPE_DISPLAY_NAME)) {
                    typeDisplayName = it.text
                } else if (it.hasHint(SLICE_HINT_TITLE)) {
                    title = it.text
                } else if (it.hasHint(SLICE_HINT_SUBTITLE)) {
                    subTitle = it.text
                } else if (it.hasHint(SLICE_HINT_ICON)) {
                    icon = it.icon
                } else if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_OPTION_ID)) {
                    beginGetPasswordOptionId = it.text
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTime = Instant.ofEpochMilli(it.long)
                } else if (it.hasHint(SLICE_HINT_AUTO_ALLOWED)) {
                    val autoSelectValue = it.text
                    if (autoSelectValue == TRUE_STRING) {
                        autoSelectAllowed = true
                    }
                } else if (it.hasHint(SLICE_HINT_AUTO_SELECT_FROM_OPTION)) {
                    autoSelectAllowedFromOption = true
                } else if (it.hasHint(SLICE_HINT_AFFILIATED_DOMAIN)) {
                    affiliatedDomain = it.text
                } else if (it.hasHint(SLICE_HINT_DEDUPLICATION_ID)) {
                    entryGroupId = it.text
                } else if (it.hasHint(SLICE_HINT_IS_DEFAULT_ICON_PREFERRED)) {
                    val defaultIconValue = it.text
                    if (defaultIconValue == TRUE_STRING) {
                        isDefaultIconPreferredAsSingleProvider = true
                    }
                } else if (it.hasHint(SLICE_HINT_DEFAULT_ICON_RES_ID)) {
                    isDefaultIcon = true
                }
            }

            return try {
                PasswordCredentialEntry(
                    title!!,
                    subTitle,
                    typeDisplayName!!,
                    pendingIntent!!,
                    lastUsedTime,
                    icon!!,
                    autoSelectAllowed,
                    BeginGetPasswordOption.createFrom(
                        Bundle(),
                        beginGetPasswordOptionId!!.toString()
                    ),
                    entryGroupId = entryGroupId,
                    isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
                    affiliatedDomain = affiliatedDomain,
                    autoSelectAllowedFromOption = autoSelectAllowedFromOption,
                    isDefaultIcon = isDefaultIcon,
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }
    }

    companion object {
        private const val TAG = "PasswordCredentialEntry"

        private const val SLICE_HINT_TYPE_DISPLAY_NAME =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_TYPE_DISPLAY_NAME"

        private const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_USER_NAME"

        private const val SLICE_HINT_SUBTITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_CREDENTIAL_TYPE_DISPLAY_NAME"

        private const val SLICE_HINT_DEFAULT_ICON_RES_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEFAULT_ICON_RES_ID"

        private const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"

        private const val SLICE_HINT_ICON =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PROFILE_ICON"

        private const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PENDING_INTENT"

        private const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_OPTION_ID"

        private const val SLICE_HINT_AUTO_ALLOWED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_ALLOWED"

        private const val SLICE_HINT_IS_DEFAULT_ICON_PREFERRED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_IS_DEFAULT_ICON_PREFERRED"

        private const val SLICE_HINT_AUTO_SELECT_FROM_OPTION =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_SELECT_FROM_OPTION"

        private const val SLICE_HINT_DEDUPLICATION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEDUPLICATION_ID"

        private const val SLICE_HINT_AFFILIATED_DOMAIN =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AFFILIATED_DOMAIN"

        private const val TRUE_STRING = "true"

        private const val FALSE_STRING = "false"

        private const val REVISION_ID = 1

        /**
         * Converts an instance of [PasswordCredentialEntry] to a [Slice].
         *
         * This method is only expected to be called on an API > 28
         * impl, hence returning null for other levels as the
         * visibility is only restricted to the library.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(
            entry: PasswordCredentialEntry
        ): Slice? {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.toSlice(entry)
            }
            return null
        }

        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromSlice(
            slice: Slice
        ): PasswordCredentialEntry? {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.fromSlice(slice)
            }
            return null
        }

        /**
         * Converts a framework [android.service.credentials.CredentialEntry] class to a Jetpack
         * [PasswordCredentialEntry] class
         *
         * Note that this API is not needed in a general credential retrieval flow that is
         * implemented using this jetpack library, where you are only required to construct
         * an instance of [CredentialEntry] to populate the [BeginGetCredentialResponse].
         *
         * @param credentialEntry the instance of framework class to be converted
         */
        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            PasswordCredentialEntry? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromCredentialEntry(credentialEntry)
            }
            return null
        }
    }

    /**
     * Builder for [PasswordCredentialEntry]
     *
     * @constructor constructs an instance of [PasswordCredentialEntry.Builder]
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param username the username of the account holding the password credential
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with a unique request code per entry,
     * with flag [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the
     * final request, and NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple
     * times
     * @param beginGetPasswordOption the option from the original [BeginGetCredentialResponse],
     * for which this credential entry is being added
     *
     * @throws NullPointerException If [context], [username], [pendingIntent], or
     * [beginGetPasswordOption] is null
     * @throws IllegalArgumentException If [username] is empty
     */
    class Builder(
        private val context: Context,
        private val username: CharSequence,
        private val pendingIntent: PendingIntent,
        private val beginGetPasswordOption: BeginGetPasswordOption
    ) {
        private var displayName: CharSequence? = null
        private var lastUsedTime: Instant? = null
        private var icon: Icon? = null
        private var autoSelectAllowed = false
        private var affiliatedDomain: CharSequence? = null
        private var isDefaultIconPreferredAsSingleProvider: Boolean = false

        /** Sets a displayName to be shown on the UI with this entry. */
        fun setDisplayName(displayName: CharSequence?): Builder {
            this.displayName = displayName
            return this
        }

        /** Sets the icon to be shown on the UI with this entry. */
        fun setIcon(icon: Icon): Builder {
            this.icon = icon
            return this
        }

        /**
         * Sets whether the entry should be auto-selected.
         * The value is false by default.
         */
        @Suppress("MissingGetterMatchingBuilder")
        fun setAutoSelectAllowed(autoSelectAllowed: Boolean): Builder {
            this.autoSelectAllowed = autoSelectAllowed
            return this
        }

        /**
         * Sets whether the entry should have an affiliated domain, a CharSequence
         * representation of some larger entity that may be used to bind multiple entries together
         * (e.g. app_one, and app_two may be bound by 'super_app' as the larger affiliation
         * domain) without length limit, default null.
         */
        fun setAffiliatedDomain(affiliatedDomain: CharSequence?): Builder {
            this.affiliatedDomain = affiliatedDomain
            return this
        }

        /**
         * Sets the last used time of this account. This information will be used to sort the
         * entries on the selector.
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

        /** Builds an instance of [PasswordCredentialEntry] */
        fun build(): PasswordCredentialEntry {
            if (icon == null && Build.VERSION.SDK_INT >= 23) {
                icon = Icon.createWithResource(context, R.drawable.ic_password)
            }
            val typeDisplayName = context.getString(
                R.string.android_credentials_TYPE_PASSWORD_CREDENTIAL
            )
            return PasswordCredentialEntry(
                username,
                displayName,
                typeDisplayName,
                pendingIntent,
                lastUsedTime,
                icon!!,
                autoSelectAllowed,
                beginGetPasswordOption,
                affiliatedDomain = affiliatedDomain,
                isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
            )
        }
    }
}
