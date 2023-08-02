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
 * @param icon the icon to be displayed with this entry on the UI, must be created using
 * [Icon.createWithResource] when possible, and especially not with [Icon.createWithBitmap] as
 * the latter consumes more memory and may cause undefined behavior due to memory implications
 * on internal transactions; defaulted to a fallback public key credential icon if not provided
 * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
 * entry, must be created with a unique request code per entry,
 * with flag [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the
 * final request, and NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple
 * times
 * @property isAutoSelectAllowed whether this entry is allowed to be auto
 * selected if it is the only one on the UI. Note that setting this value
 * to true does not guarantee this behavior. The developer must also set this
 * to true, and the framework must determine that it is safe to auto select.
 *
 * @throws IllegalArgumentException if [username] is empty
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
    private val autoSelectAllowedFromOption: Boolean = false,
    private val isDefaultIcon: Boolean = false
) : CredentialEntry(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    beginGetPublicKeyCredentialOption
) {

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
        beginGetPublicKeyCredentialOption
    )

    @RequiresApi(28)
    private object Api28Impl {
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

            val autoSelectAllowed = if (isAutoSelectAllowed) {
                AUTO_SELECT_TRUE_STRING
            } else {
                AUTO_SELECT_FALSE_STRING
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
            try {
                if (icon.resId == R.drawable.ic_passkey) {
                    sliceBuilder.addInt(
                        /*true=*/1,
                        /*subType=*/null,
                        listOf(SLICE_HINT_DEFAULT_ICON_RES_ID)
                    )
                }
            } catch (_: IllegalStateException) {
            }

            if (CredentialOption.extractAutoSelectValue(
                    beginGetPublicKeyCredentialOption.candidateQueryData
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
            var isDefaultIcon = false

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
                    if (autoSelectValue == AUTO_SELECT_TRUE_STRING) {
                        autoSelectAllowed = true
                    }
                } else if (it.hasHint(SLICE_HINT_AUTO_SELECT_FROM_OPTION)) {
                    autoSelectAllowedFromOption = true
                } else if (it.hasHint(SLICE_HINT_DEFAULT_ICON_RES_ID)) {
                    isDefaultIcon = true
                }
            }

            return try {
                PublicKeyCredentialEntry(
                    title!!,
                    subtitle,
                    typeDisplayName!!,
                    pendingIntent!!,
                    icon!!,
                    lastUsedTime,
                    autoSelectAllowed,
                    BeginGetPublicKeyCredentialOption.createFromEntrySlice(
                        Bundle(),
                        beginGetPublicKeyCredentialOptionId!!.toString()
                    ),
                    autoSelectAllowedFromOption,
                    isDefaultIcon
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }
    }

    internal companion object {
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

        private const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_OPTION_ID"

        private const val SLICE_HINT_AUTO_SELECT_FROM_OPTION =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_SELECT_FROM_OPTION"

        private const val SLICE_HINT_DEFAULT_ICON_RES_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEFAULT_ICON_RES_ID"

        private const val AUTO_SELECT_TRUE_STRING = "true"

        private const val AUTO_SELECT_FALSE_STRING = "false"

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
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): PublicKeyCredentialEntry? {
            if (Build.VERSION.SDK_INT >= 28) {
                return Api28Impl.fromSlice(slice)
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
                beginGetPublicKeyCredentialOption
            )
        }
    }
}
