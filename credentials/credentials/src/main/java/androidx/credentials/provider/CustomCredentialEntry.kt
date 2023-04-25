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
import androidx.credentials.R
import java.time.Instant
import java.util.Collections

/**
 * Custom credential entry for a custom credential tyoe that is displayed on the account
 * selector UI.
 *
 * Each entry corresponds to an account that can provide a credential.
 *
 * @property title the title shown with this entry on the selector UI
 * @property subtitle the subTitle shown with this entry on the selector UI
 * @property lastUsedTime the last used time the credential underlying this entry was
 * used by the user
 * @property icon the icon to be displayed with this entry on the selector UI. If not set, a
 * default icon representing a custom credential type is set by the library
 * @property pendingIntent the [PendingIntent] to be invoked when this entry
 * is selected by the user
 * @property typeDisplayName the friendly name to be displayed on the UI for
 * the type of the credential
 * @property isAutoSelectAllowed whether this entry is allowed to be auto
 * selected if it is the only one on the UI. Note that setting this value
 * to true does not guarantee this behavior. The developer must also set this
 * to true, and the framework must determine that only one entry is present
 */
@RequiresApi(28)
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
    /** @hide */
    val autoSelectAllowedFromOption: Boolean = false,
    /** @hide */
    val isDefaultIcon: Boolean = false
) : CredentialEntry(
    type,
    beginGetCredentialOption,
    toSlice(
        type,
        title,
        subtitle,
        pendingIntent,
        typeDisplayName,
        lastUsedTime,
        icon,
        isAutoSelectAllowed,
        beginGetCredentialOption
    )
) {
    init {
        require(type.isNotEmpty()) { "type must not be empty" }
        require(title.isNotEmpty()) { "title must not be empty" }
    }

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
        isAutoSelectAllowed: Boolean = false
    ) : this(
        beginGetCredentialOption.type,
        title,
        pendingIntent,
        isAutoSelectAllowed,
        subtitle,
        typeDisplayName,
        icon,
        lastUsedTime,
        beginGetCredentialOption
    )

    /** @hide **/
    @Suppress("AcronymName")
    companion object {
        private const val TAG = "CredentialEntry"

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
        internal const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ICON =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PROFILE_ICON"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_PENDING_INTENT"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_AUTO_ALLOWED =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_ALLOWED"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_OPTION_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_OPTION_ID"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_AUTO_SELECT_FROM_OPTION =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_AUTO_SELECT_FROM_OPTION"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_DEFAULT_ICON_RES_ID =
            "androidx.credentials.provider.credentialEntry.SLICE_HINT_DEFAULT_ICON_RES_ID"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val AUTO_SELECT_TRUE_STRING = "true"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val AUTO_SELECT_FALSE_STRING = "false"

        /** @hide */
        @JvmStatic
        fun toSlice(
            type: String,
            title: CharSequence,
            subtitle: CharSequence?,
            pendingIntent: PendingIntent,
            typeDisplayName: CharSequence?,
            lastUsedTime: Instant?,
            icon: Icon,
            isAutoSelectAllowed: Boolean?,
            beginGetCredentialOption: BeginGetCredentialOption
        ): Slice {
            // TODO("Put the right revision value")
            val autoSelectAllowed = if (isAutoSelectAllowed == true) {
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
                .addIcon(
                    icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON)
                )

            try {
                if (icon.resId == R.drawable.ic_other_sign_in) {
                    sliceBuilder.addInt(
                        /*true=*/1,
                        /*subType=*/null,
                        listOf(SLICE_HINT_DEFAULT_ICON_RES_ID)
                    )
                }
            } catch (_: IllegalStateException) {
            }

            if (CredentialOption.extractAutoSelectValue(
                    beginGetCredentialOption.candidateQueryData
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
                    beginGetCredentialOptionId = it.text
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
                CustomCredentialEntry(
                    type,
                    title!!,
                    pendingIntent!!,
                    autoSelectAllowed,
                    subtitle,
                    typeDisplayName,
                    icon!!,
                    lastUsedTime,
                    BeginGetCustomCredentialOption(
                        beginGetCredentialOptionId!!.toString(),
                        type,
                        Bundle()
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

    /** Builder for [CustomCredentialEntry] */
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

        /** Builds an instance of [CustomCredentialEntry] */
        fun build(): CustomCredentialEntry {
            if (icon == null) {
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
                beginGetCredentialOption
            )
        }
    }
}
