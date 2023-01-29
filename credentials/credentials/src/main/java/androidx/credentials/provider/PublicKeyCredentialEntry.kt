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
import android.os.Parcel
import android.os.Parcelable
import android.service.credentials.CredentialEntry
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.PublicKeyCredential
import androidx.credentials.R
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
 * @property lastUsedTime the last used time of this entry
 * @property icon the icon to be displayed with this entry on the selector
 * @param pendingIntent the [PendingIntent] to be invoked when the user
 * selects this entry
 * @property isAutoSelectAllowed whether this entry is allowed to be auto
 * selected if it is the only one on the UI. Note that setting this value
 * to true does not guarantee this behavior. The developer must also set this
 * to true, and the framework must determine that it is safe to auto select.
 *
 * @throws IllegalArgumentException if [username] is empty
 */
@RequiresApi(34)
class PublicKeyCredentialEntry internal constructor(
    val username: CharSequence,
    val displayName: CharSequence?,
    val typeDisplayName: CharSequence,
    val pendingIntent: PendingIntent,
    val icon: Icon,
    val lastUsedTime: Instant?,
    val isAutoSelectAllowed: Boolean
) : CredentialEntry(
    PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
    toSlice(
        PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL,
        username,
        displayName,
        pendingIntent,
        typeDisplayName,
        lastUsedTime,
        icon,
        isAutoSelectAllowed
    )
) {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }

    @Suppress("AcronymName")
    companion object CREATOR {

        private const val TAG = "PublicKeyCredEntry"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_TYPE_DISPLAY_NAME =
            "androidx.credentials.provider.publicKeyCredEntry.SLICE_HINT_TYPE_DISPLAY_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.publicKeyCredEntry.SLICE_HINT_USER_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_SUBTITLE =
            "androidx.credentials.provider.publicKeyCredEntry.SLICE_HINT_TYPE_DISPLAY_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.publicKeyCredEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ICON =
            "androidx.credentials.provider.publicKeyCredEntry.SLICE_HINT_PROFILE_ICON"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.publicKeyCredEntry.SLICE_HINT_PENDING_INTENT"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_AUTO_ALLOWED =
            "androidx.credentials.provider.publicKeyCredEntry.SLICE_HINT_AUTO_ALLOWED"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val AUTO_SELECT_TRUE_STRING = "true"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val AUTO_SELECT_FALSE_STRING = "false"

        /** @hide */
        @JvmStatic
        internal fun toSlice(
            type: String,
            title: CharSequence,
            subTitle: CharSequence?,
            pendingIntent: PendingIntent,
            typeDisplayName: CharSequence?,
            lastUsedTime: Instant?,
            icon: Icon?,
            isAutoSelectAllowed: Boolean
        ): Slice {
            // TODO("Put the right revision value")
            val autoSelectAllowed = if (isAutoSelectAllowed) {
                AUTO_SELECT_TRUE_STRING
            } else {
                AUTO_SELECT_FALSE_STRING
            }
            val sliceBuilder = Slice.Builder(
                Uri.EMPTY, SliceSpec(
                    type, 1)
            )
                .addText(typeDisplayName, /*subType=*/null,
                    listOf(SLICE_HINT_TYPE_DISPLAY_NAME))
                .addText(title, /*subType=*/null,
                    listOf(SLICE_HINT_TITLE))
                .addText(subTitle, /*subType=*/null,
                    listOf(SLICE_HINT_SUBTITLE))
                .addText(autoSelectAllowed, /*subType=*/null,
                    listOf(SLICE_HINT_AUTO_ALLOWED))
            if (lastUsedTime != null) {
                sliceBuilder.addLong(lastUsedTime.toEpochMilli(),
                    /*subType=*/null,
                    listOf(SLICE_HINT_LAST_USED_TIME_MILLIS))
            }
            if (icon != null) {
                sliceBuilder.addIcon(icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON))
            }
            sliceBuilder.addAction(pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(),
                /*subType=*/null)
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
        fun fromSlice(slice: Slice): PublicKeyCredentialEntry? {
            var typeDisplayName: CharSequence? = null
            var title: CharSequence? = null
            var subTitle: CharSequence? = null
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var lastUsedTime: Instant? = null
            var autoSelectAllowed = false

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
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTime = Instant.ofEpochMilli(it.long)
                } else if (it.hasHint(SLICE_HINT_AUTO_ALLOWED)) {
                    val autoSelectValue = it.text
                    if (autoSelectValue == AUTO_SELECT_TRUE_STRING) {
                        autoSelectAllowed = true
                    }
                }
            }

            return try {
                PublicKeyCredentialEntry(
                    title!!,
                    subTitle,
                    typeDisplayName!!,
                    pendingIntent!!,
                    icon!!,
                    lastUsedTime,
                    autoSelectAllowed)
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        @JvmField val CREATOR: Parcelable.Creator<PublicKeyCredentialEntry> = object :
            Parcelable.Creator<PublicKeyCredentialEntry> {
            override fun createFromParcel(p0: Parcel?): PublicKeyCredentialEntry? {
                val credentialEntry = CredentialEntry.CREATOR.createFromParcel(p0)
                return fromSlice(credentialEntry.slice)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<PublicKeyCredentialEntry?> {
                return arrayOfNulls(size)
            }
        }
    }

    /**
     * Builder for [PublicKeyCredentialEntry]
     */
    class Builder(
        private val context: Context,
        private val username: CharSequence,
        private val pendingIntent: PendingIntent
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
        fun setIcon(icon: Icon?): Builder {
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
            if (icon == null) {
                icon = Icon.createWithResource(context, R.drawable.ic_passkey)
            }
            val typeDisplayName = context.getString(
                R.string.androidx_credentials_TYPE_PUBLIC_KEY_CREDENTIAL)
            return PublicKeyCredentialEntry(
                username, displayName, typeDisplayName, pendingIntent, icon!!,
                lastUsedTime, autoSelectAllowed)
        }
    }
}
