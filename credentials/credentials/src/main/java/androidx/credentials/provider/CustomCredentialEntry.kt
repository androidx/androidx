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
import androidx.credentials.R
import java.time.Instant
import java.util.Collections

/**
 * Custom credential entry for a custom credential type that is displayed on the account
 * selector UI.
 *
 * Each entry corresponds to an account that can provide a credential.
 *
 * @property title the title shown with this entry on the selector UI
 * @property subtitle the subTitle shown with this entry on the selector UI
 * @property lastUsedTime the last used time the credential underlying this entry was
 * used by the user. Note that this value will only be distinguishable up to the milli
 * second mark. If two entries have the same millisecond precision, they will be considered to
 * have been used at the same time
 * @property icon the icon to be displayed with this entry on the UI, must be created using
 * [Icon.createWithResource] when possible, and especially not with [Icon.createWithBitmap] as
 * the latter consumes more memory and may cause undefined behavior due to memory implications
 * on internal transactions; defaulted to a fallback custom credential icon if not provided
 * @property pendingIntent the [PendingIntent] that will get invoked when the user selects this
 * entry, must be created with flag [PendingIntent.FLAG_MUTABLE] to allow the Android
 * system to attach the final request
 * @property typeDisplayName the friendly name to be displayed on the UI for
 * the type of the credential
 * @property isAutoSelectAllowed whether this entry is allowed to be auto
 * selected if it is the only one on the UI. Note that setting this value
 * to true does not guarantee this behavior. The developer must also set this
 * to true, and the framework must determine that only one entry is present
 * @property affiliatedDomain the user visible affiliated domain, a CharSequence
 * representation of a web domain or an app package name that the given credential in this
 * entry is associated with when it is different from the requesting entity, default null
 * @property entryGroupId an ID used for deduplication or grouping entries during display, by
 * default set to [title]; for more info on this id, see [CredentialEntry]
 * @param isDefaultIconPreferredAsSingleProvider when set to true, the UI prefers to render the
 * default credential type icon (see the default value of [icon]) when you are
 * the only available provider; false by default
 * @property isAutoSelectAllowedFromOption whether the [beginGetCredentialOption] request
 * for which this entry was created allows this entry to be auto-selected
 * @property hasDefaultIcon whether this entry was created without a custom icon and hence
 * contains a default icon set by the library, only to be used in Android API levels >= 28
 *
 * @throws IllegalArgumentException If [type] or [title] are empty
 *
 * @see CredentialEntry
 */
@RequiresApi(26)
class CustomCredentialEntry internal constructor(
    override val type: String,
    val title: CharSequence,
    val pendingIntent: PendingIntent,
    @get:Suppress("AutoBoxing")
    val isAutoSelectAllowed: Boolean,
    val subtitle: CharSequence?,
    val typeDisplayName: CharSequence?,
    val icon: Icon,
    val lastUsedTime: Instant?,
    beginGetCredentialOption: BeginGetCredentialOption,
    isDefaultIconPreferredAsSingleProvider: Boolean,
    entryGroupId: CharSequence? = title,
    affiliatedDomain: CharSequence? = null,
    autoSelectAllowedFromOption: Boolean = CredentialOption.extractAutoSelectValue(
        beginGetCredentialOption.candidateQueryData),
    private var isCreatedFromSlice: Boolean = false,
    private var isDefaultIconFromSlice: Boolean = false,
) : CredentialEntry(
    type,
    beginGetCredentialOption,
    entryGroupId ?: title,
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
        require(type.isNotEmpty()) { "type must not be empty" }
        require(title.isNotEmpty()) { "title must not be empty" }
    }

    /**
     * @constructor constructs an instance of [CustomCredentialEntry]
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param title the title shown with this entry on the selector UI
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with flag [PendingIntent.FLAG_MUTABLE] to allow the Android
     * system to attach the final request
     * @param beginGetCredentialOption the option from the original [BeginGetCredentialResponse],
     * for which this credential entry is being added
     * @param subtitle the subTitle shown with this entry on the selector UI
     * @param lastUsedTime the last used time the credential underlying this entry was
     * used by the user, distinguishable up to the milli second mark only such that if two
     * entries have the same millisecond precision, they will be considered to have been used at
     * the same time
     * @param typeDisplayName the friendly name to be displayed on the UI for
     * the type of the credential
     * @param icon the icon to be displayed with this entry on the selector UI, if not set a
     * default icon representing a custom credential type is set by the library
     * @param isAutoSelectAllowed whether this entry is allowed to be auto
     * selected if it is the only one on the UI, only takes effect if the app requesting for
     * credentials also opts for auto select
     *
     * @throws IllegalArgumentException if [type] or [title] are empty
     */
    @Deprecated("Use the constructor that allows setting all parameters.",
        replaceWith = ReplaceWith("CustomCredentialEntry(context, title, pendingIntent," +
            "beginGetCredentialOption, subtitle, typeDisplayName, lastUsedTime, icon, " +
            "isAutoSelectAllowed, entryGroupId, isDefaultIconPreferredAsSingleProvider)"),
        level = DeprecationLevel.HIDDEN
    )
    constructor(
        context: Context,
        title: CharSequence,
        pendingIntent: PendingIntent,
        beginGetCredentialOption: BeginGetCredentialOption,
        subtitle: CharSequence? = null,
        typeDisplayName: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon = Icon.createWithResource(context, R.drawable.ic_other_sign_in),
        @Suppress("AutoBoxing")
        isAutoSelectAllowed: Boolean = false,
    ) : this(
        beginGetCredentialOption.type,
        title,
        pendingIntent,
        isAutoSelectAllowed,
        subtitle,
        typeDisplayName,
        icon,
        lastUsedTime,
        beginGetCredentialOption,
        isDefaultIconPreferredAsSingleProvider = false
    )

    /**
     * @constructor constructs an instance of [CustomCredentialEntry]
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param title the title shown with this entry on the selector UI
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with flag [PendingIntent.FLAG_MUTABLE] to allow the Android
     * system to attach the final request
     * @param beginGetCredentialOption the option from the original [BeginGetCredentialRequest],
     * for which this credential entry is being added
     * @param subtitle the subTitle shown with this entry on the selector UI
     * @param lastUsedTime the last used time the credential underlying this entry was
     * used by the user, distinguishable up to the milli second mark only such that if two
     * entries have the same millisecond precision, they will be considered to have been used at
     * the same time
     * @param typeDisplayName the friendly name to be displayed on the UI for
     * the type of the credential
     * @param icon the icon to be displayed with this entry on the selector UI, if not set a
     * default icon representing a custom credential type is set by the library
     * @param isAutoSelectAllowed whether this entry is allowed to be auto
     * selected if it is the only one on the UI, only takes effect if the app requesting for
     * credentials also opts for auto select
     * @param entryGroupId an ID to uniquely mark this entry for deduplication or to group entries
     * during display, set to [title] by default
     * @param isDefaultIconPreferredAsSingleProvider when set to true, the UI prefers to render the
     * default credential type icon (see the default value of [icon]) when you are
     * the only available provider; false by default
     *
     * @throws IllegalArgumentException If [type] or [title] are empty
     */
    constructor(
        context: Context,
        title: CharSequence,
        pendingIntent: PendingIntent,
        beginGetCredentialOption: BeginGetCredentialOption,
        subtitle: CharSequence? = null,
        typeDisplayName: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon = Icon.createWithResource(context, R.drawable.ic_other_sign_in),
        @Suppress("AutoBoxing")
        isAutoSelectAllowed: Boolean = false,
        entryGroupId: CharSequence = title,
        isDefaultIconPreferredAsSingleProvider: Boolean = false
    ) : this(
        beginGetCredentialOption.type,
        title,
        pendingIntent,
        isAutoSelectAllowed,
        subtitle,
        typeDisplayName,
        icon,
        lastUsedTime,
        beginGetCredentialOption,
        isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
        entryGroupId.ifEmpty { title },
    )

    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            CustomCredentialEntry? {
            val slice = credentialEntry.slice
            return fromSlice(slice)
        }
    }

    @RequiresApi(28)
    private object Api28Impl {
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun isDefaultIcon(entry: CustomCredentialEntry): Boolean {
            if (entry.isCreatedFromSlice) {
                return entry.isDefaultIconFromSlice
            }
            return entry.icon.type == Icon.TYPE_RESOURCE &&
                entry.icon.resId == R.drawable.ic_other_sign_in
        }
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(
            entry: CustomCredentialEntry
        ): Slice {
            val type = entry.type
            val title = entry.title
            val subtitle = entry.subtitle
            val pendingIntent = entry.pendingIntent
            val typeDisplayName = entry.typeDisplayName
            val lastUsedTime = entry.lastUsedTime
            val icon = entry.icon
            val isAutoSelectAllowed = entry.isAutoSelectAllowed
            val beginGetCredentialOption = entry.beginGetCredentialOption
            val entryGroupId = entry.entryGroupId
            val affiliatedDomain = entry.affiliatedDomain
            val isDefaultIconPreferredAsSingleProvider =
                entry.isDefaultIconPreferredAsSingleProvider

            val autoSelectAllowed = if (isAutoSelectAllowed) {
                TRUE_STRING
            } else {
                FALSE_STRING
            }

            val isUsingDefaultIconPreferred = if (isDefaultIconPreferredAsSingleProvider) {
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
                    subtitle, /*subType=*/null,
                    listOf(SLICE_HINT_SUBTITLE)
                )
                .addText(
                    autoSelectAllowed, /*subType=*/null,
                    listOf(SLICE_HINT_AUTO_ALLOWED)
                )
                .addText(
                    beginGetCredentialOption.id,
                    /*subType=*/null,
                    listOf(SLICE_HINT_OPTION_ID)
                )
                .addText(
                    entryGroupId, /*subTypes=*/null,
                    listOf(SLICE_HINT_DEDUPLICATION_ID)
                )
                .addText(
                    affiliatedDomain, /*subTypes=*/null,
                    listOf(SLICE_HINT_AFFILIATED_DOMAIN)
                )
                .addIcon(
                    icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON)
                )
                .addText(
                    isUsingDefaultIconPreferred, /*subType=*/null,
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
         *
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY) // used from java tests
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CustomCredentialEntry? {
            val type: String = slice.spec!!.type
            var typeDisplayName: CharSequence? = null
            var title: CharSequence? = null
            var subtitle: CharSequence? = null
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var lastUsedTime: Instant? = null
            var autoSelectAllowed = false
            var beginGetCredentialOptionId: CharSequence? = null
            var entryGroupId: CharSequence? = null
            var autoSelectAllowedFromOption = false
            var isDefaultIconPreferredAsSingleProvider = false
            var isDefaultIcon = false
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
                    beginGetCredentialOptionId = it.text
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTime = Instant.ofEpochMilli(it.long)
                } else if (it.hasHint(SLICE_HINT_AUTO_ALLOWED)) {
                    val autoSelectValue = it.text
                    if (autoSelectValue == TRUE_STRING) {
                        autoSelectAllowed = true
                    }
                } else if (it.hasHint(SLICE_HINT_DEDUPLICATION_ID)) {
                    entryGroupId = it.text
                } else if (it.hasHint(SLICE_HINT_AUTO_SELECT_FROM_OPTION)) {
                    autoSelectAllowedFromOption = true
                } else if (it.hasHint(SLICE_HINT_IS_DEFAULT_ICON_PREFERRED)) {
                    val defaultIconValue = it.text
                    if (defaultIconValue == TRUE_STRING) {
                        isDefaultIconPreferredAsSingleProvider = true
                    }
                } else if (it.hasHint(SLICE_HINT_DEFAULT_ICON_RES_ID)) {
                    isDefaultIcon = true
                } else if (it.hasHint(SLICE_HINT_AFFILIATED_DOMAIN)) {
                    affiliatedDomain = it.text
                }
            }

            return try {
                CustomCredentialEntry(
                    type = type,
                    title = title!!,
                    pendingIntent = pendingIntent!!,
                    isAutoSelectAllowed = autoSelectAllowed,
                    subtitle = subtitle,
                    typeDisplayName = typeDisplayName,
                    icon = icon!!,
                    lastUsedTime = lastUsedTime,
                    beginGetCredentialOption = BeginGetCustomCredentialOption(
                        beginGetCredentialOptionId!!.toString(),
                        type,
                        Bundle()
                    ),
                    isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
                    entryGroupId = entryGroupId,
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
        private const val TAG = "CredentialEntry"

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

        private const val SLICE_HINT_DEDUPLICATION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEDUPLICATION_ID"

        private const val SLICE_HINT_AFFILIATED_DOMAIN =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AFFILIATED_DOMAIN"

        private const val SLICE_HINT_DEFAULT_ICON_RES_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEFAULT_ICON_RES_ID"

        private const val TRUE_STRING = "true"

        private const val FALSE_STRING = "false"

        private const val REVISION_ID = 1

        /**
         * Converts an instance of [CustomCredentialEntry] to a [Slice].
         *
         * This method is only expected to be called on an API > 28
         * impl, hence returning null for other levels as the
         * visibility is only restricted to the library.
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(
            entry: CustomCredentialEntry
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
         *
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun fromSlice(slice: Slice): CustomCredentialEntry? {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.fromSlice(slice)
            }
            return null
        }

        /**
         * Converts a framework [android.service.credentials.CredentialEntry] class to a Jetpack
         * [CustomCredentialEntry] class
         *
         * @param credentialEntry the instance of framework class to be converted
         */
        @JvmStatic
        fun fromCredentialEntry(credentialEntry: android.service.credentials.CredentialEntry):
            CustomCredentialEntry? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromCredentialEntry(credentialEntry)
            }
            return null
        }
    }

    /**
     * Builder for [CustomCredentialEntry]
     *
     * @constructor constructs an instance of [CustomCredentialEntry.Builder]
     *
     * @param context the context of the calling app, required to retrieve fallback resources
     * @param type the type string that defines this custom credential
     * @param title the title shown with this entry on the selector UI
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     * entry, must be created with flag [PendingIntent.FLAG_MUTABLE] to allow the Android
     * system to attach the final request
     * @param beginGetCredentialOption the option from the original [BeginGetCredentialResponse],
     * for which this credential entry is being added
     *
     * @throws NullPointerException If [context], [type], [title], [pendingIntent], or
     * [beginGetCredentialOption] is null
     */
    class Builder(
        private val context: Context,
        private val type: String,
        private val title: CharSequence,
        private val pendingIntent: PendingIntent,
        private val beginGetCredentialOption: BeginGetCredentialOption
    ) {
        private var subtitle: CharSequence? = null
        private var lastUsedTime: Instant? = null
        private var typeDisplayName: CharSequence? = null
        private var icon: Icon? = null
        private var autoSelectAllowed = false
        private var entryGroupId: CharSequence = title
        private var isDefaultIconPreferredAsSingleProvider = false

        /** Sets a displayName to be shown on the UI with this entry. */
        fun setSubtitle(subtitle: CharSequence?): Builder {
            this.subtitle = subtitle
            return this
        }

        /** Sets the display name of this credential type, to be shown on the UI with this entry. */
        fun setTypeDisplayName(typeDisplayName: CharSequence?): Builder {
            this.typeDisplayName = typeDisplayName
            return this
        }

        /**
         * Sets the icon to be show on the UI.
         * If no icon is set, a default icon representing a custom credential will be set.
         */
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
         * Sets an ID to uniquely mark this entry for deduplication or for grouping entries during
         * display; if not set, will default to [title].
         *
         * @throws IllegalArgumentException If the entryGroupId is empty
         */
        fun setEntryGroupId(entryGroupId: CharSequence): Builder {
            require(entryGroupId.isNotEmpty()) { "entryGroupId must not be empty" }
            this.entryGroupId = entryGroupId
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

        /** Builds an instance of [CustomCredentialEntry] */
        fun build(): CustomCredentialEntry {
            if (icon == null && Build.VERSION.SDK_INT >= 23) {
                icon = Icon.createWithResource(context, R.drawable.ic_other_sign_in)
            }
            return CustomCredentialEntry(
                type,
                title,
                pendingIntent,
                autoSelectAllowed,
                subtitle,
                typeDisplayName,
                icon!!,
                lastUsedTime,
                beginGetCredentialOption,
                isDefaultIconPreferredAsSingleProvider = isDefaultIconPreferredAsSingleProvider,
                entryGroupId = entryGroupId,
            )
        }
    }
}
