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
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.CredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.R
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
 * @property lastUsedTime the last used time of this entry
 * @property icon the icon to be displayed with this entry on the selector. If not set, a
 * default icon representing a password credential type is set by the library
 * @property pendingIntent the [PendingIntent] to be invoked when user selects
 * this entry
 * @property isAutoSelectAllowed whether this entry is allowed to be auto
 * selected if it is the only one on the UI. Note that setting this value
 * to true does not guarantee this behavior. The developer must also set this
 * to true, and the framework must determine that this is the only entry available for the user.
 *
 * @throws IllegalArgumentException if [username] is empty
 *
 * @see CustomCredentialEntry
 */
@RequiresApi(28)
class PasswordCredentialEntry internal constructor(
    val username: CharSequence,
    val displayName: CharSequence?,
    val typeDisplayName: CharSequence,
    val pendingIntent: PendingIntent,
    val lastUsedTime: Instant?,
    val icon: Icon,
    val isAutoSelectAllowed: Boolean,
    beginGetPasswordOption: BeginGetPasswordOption,
    /** @hide */
    val autoSelectAllowedFromOption: Boolean = false,
    /** @hide */
    val isDefaultIcon: Boolean = false
) : CredentialEntry(
    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    beginGetPasswordOption,
    toSlice(
        PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
        username,
        displayName,
        pendingIntent,
        typeDisplayName,
        lastUsedTime,
        icon,
        isAutoSelectAllowed,
        beginGetPasswordOption
    )
) {
    init {
        require(username.isNotEmpty()) { "username must not be empty" }
    }

    constructor(
        context: Context,
        username: CharSequence,
        pendingIntent: PendingIntent,
        beginGetPasswordOption: BeginGetPasswordOption,
        displayName: CharSequence? = null,
        lastUsedTime: Instant? = null,
        icon: Icon = Icon.createWithResource(context, R.drawable.ic_password),
        isAutoSelectAllowed: Boolean = false
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
    )

    /** @hide **/
    @Suppress("AcronymName")
    companion object {
        private const val TAG = "PasswordCredentialEntry"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_TYPE_DISPLAY_NAME =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_TYPE_DISPLAY_NAME"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_USER_NAME"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_SUBTITLE =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_CREDENTIAL_TYPE_DISPLAY_NAME"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_DEFAULT_ICON_RES_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEFAULT_ICON_RES_ID"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ICON =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PROFILE_ICON"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PENDING_INTENT"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_OPTION_ID"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_AUTO_ALLOWED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_ALLOWED"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_AUTO_SELECT_FROM_OPTION =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_SELECT_FROM_OPTION"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val AUTO_SELECT_TRUE_STRING = "true"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val AUTO_SELECT_FALSE_STRING = "false"

        /** @hide */
        @JvmStatic
        fun toSlice(
            type: String,
            title: CharSequence,
            subTitle: CharSequence?,
            pendingIntent: PendingIntent,
            typeDisplayName: CharSequence?,
            lastUsedTime: Instant?,
            icon: Icon,
            isAutoSelectAllowed: Boolean,
            beginGetPasswordCredentialOption: BeginGetPasswordOption
        ): Slice {
            // TODO("Put the right revision value")
            val autoSelectAllowed = if (isAutoSelectAllowed) {
                AUTO_SELECT_TRUE_STRING
            } else {
                AUTO_SELECT_FALSE_STRING
            }
            val sliceBuilder = Slice.Builder(
                Uri.EMPTY, SliceSpec(
                    type, 1
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
                    beginGetPasswordCredentialOption.id,
                    /*subType=*/null,
                    listOf(SLICE_HINT_OPTION_ID)
                )
                .addIcon(
                    icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON)
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
         * @hide
         */
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
                PasswordCredentialEntry(
                    title!!,
                    subTitle,
                    typeDisplayName!!,
                    pendingIntent!!,
                    lastUsedTime,
                    icon!!,
                    autoSelectAllowed,
                    BeginGetPasswordOption.createFromEntrySlice(
                        Bundle(),
                        beginGetPasswordOptionId!!.toString()
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

    /** Builder for [PasswordCredentialEntry] */
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
         * Sets the last used time of this account. This information will be used to sort the
         * entries on the selector.
         */
        fun setLastUsedTime(lastUsedTime: Instant?): Builder {
            this.lastUsedTime = lastUsedTime
            return this
        }

        /** Builds an instance of [PasswordCredentialEntry] */
        fun build(): PasswordCredentialEntry {
            if (icon == null) {
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
                beginGetPasswordOption
            )
        }
    }
}
