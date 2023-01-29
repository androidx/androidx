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
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.CredentialManager
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
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
 * @throws IllegalArgumentException If [accountName] is empty
 */
@RequiresApi(34)
class CreateEntry internal constructor(
    val accountName: CharSequence,
    val pendingIntent: PendingIntent,
    val icon: Icon?,
    val description: CharSequence?,
    val lastUsedTime: Instant?,
    private val credentialCountInformationMap: Map<String, Int>
    ) : android.service.credentials.CreateEntry(
    toSlice(
        accountName,
        icon,
        description,
        lastUsedTime,
        credentialCountInformationMap,
        pendingIntent)
) {

    init {
        require(accountName.isNotEmpty()) { "accountName must not be empty" }
    }

    /** Returns the no. of password type credentials that the provider with this entry has. */
    @Suppress("AutoBoxing")
    fun getPasswordCredentialCount(): Int? {
        return credentialCountInformationMap[PasswordCredential.TYPE_PASSWORD_CREDENTIAL]
    }

    /** Returns the no. of public key type credentials that the provider with this entry has. */
    @Suppress("AutoBoxing")
    fun getPublicKeyCredentialCount(): Int? {
        return credentialCountInformationMap[PasswordCredential.TYPE_PASSWORD_CREDENTIAL]
    }

    /** Returns the no. of total credentials that the provider with this entry has.
     *
     * This total count is not necessarily equal to the sum of [getPasswordCredentialCount]
     * and [getPublicKeyCredentialCount].
     *
     */
    @Suppress("AutoBoxing")
    fun getTotalCredentialCount(): Int? {
        return credentialCountInformationMap[TYPE_TOTAL_CREDENTIAL]
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }

    /**
     * A builder for [CreateEntry]
     *
     * @param accountName the name of the account where the credential will be registered
     * @param pendingIntent the [PendingIntent] that will be fired when the user selects
     * this entry
     */
    class Builder constructor(
        private val accountName: CharSequence,
        private val pendingIntent: PendingIntent
    ) {

        private var credentialCountInformationMap: MutableMap<String, Int> =
            mutableMapOf()
        private var icon: Icon? = null
        private var description: CharSequence? = null
        private var lastUsedTime: Instant? = null

        /** Sets the password credential count, denoting how many credentials of type
         * [PasswordCredential.TYPE_PASSWORD_CREDENTIAL] does the provider have stored.
         *
         * This information will be displayed on the [CreateEntry] to help the user
         * make a choice.
         */
        fun setPasswordCredentialCount(count: Int): Builder {
            credentialCountInformationMap[PasswordCredential.TYPE_PASSWORD_CREDENTIAL] = count
            return this
        }

        /** Sets the password credential count, denoting how many credentials of type
         * [PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL] does the provider have stored.
         *
         * This information will be displayed on the [CreateEntry] to help the user
         * make a choice.
         */
        fun setPublicKeyCredentialCount(count: Int): Builder {
            credentialCountInformationMap[PublicKeyCredential.TYPE_PUBLIC_KEY_CREDENTIAL] = count
            return this
        }

        /** Sets the total credential count, denoting how many credentials in total
         * does the provider have stored.
         *
         * This total count no. does not need to be a total of the counts set through
         * [setPasswordCredentialCount] and [setPublicKeyCredentialCount].
         *
         * This information will be displayed on the [CreateEntry] to help the user
         * make a choice.
         */
        fun setTotalCredentialCount(count: Int): Builder {
            credentialCountInformationMap[TYPE_TOTAL_CREDENTIAL] = count
            return this
        }

        /** Sets an icon to be displayed with the entry on the UI */
        fun setIcon(icon: Icon?): Builder {
            this.icon = icon
            return this
        }

        /**
         * Sets a description to be displayed on the UI at the time of credential creation.
         *
         * Typically this description should contain information informing the user of the
         * credential being created, and where it is being stored. Providers are free
         * to phrase this however they see fit.
         *
         * This description must be no more than 150 characters long. Any characters beyond
         * that limit will be trimmed off.
         */
        fun setDescription(description: CharSequence?): Builder {
            this.description = description
            return this
        }

        /** Sets the last time this account was used */
        fun setLastUsedTime(lastUsedTime: Instant?): Builder {
            this.lastUsedTime = lastUsedTime
            return this
        }

        /**
         * Builds an instance of [CreateEntry]
         *
         * @throws IllegalArgumentException If [accountName] is empty
         */
        fun build(): CreateEntry {
            return CreateEntry(
                accountName, pendingIntent, icon, description, lastUsedTime,
                credentialCountInformationMap
            )
        }
    }

    @Suppress("AcronymName")
    companion object {
        private const val TAG = "CreateEntry"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val TYPE_TOTAL_CREDENTIAL = "TOTAL_CREDENTIAL_COUNT_TYPE"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ACCOUNT_NAME =
            "androidx.credentials.provider.createEntry.SLICE_HINT_USER_PROVIDER_ACCOUNT_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_NOTE =
            "androidx.credentials.provider.createEntry.SLICE_HINT_NOTE"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_ICON =
            "androidx.credentials.provider.createEntry.SLICE_HINT_PROFILE_ICON"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_CREDENTIAL_COUNT_INFORMATION =
            "androidx.credentials.provider.createEntry.SLICE_HINT_CREDENTIAL_COUNT_INFORMATION"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_LAST_USED_TIME_MILLIS =
            "androidx.credentials.provider.createEntry.SLICE_HINT_LAST_USED_TIME_MILLIS"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.createEntry.SLICE_HINT_PENDING_INTENT"

        /** @hide **/
        @JvmStatic
        fun toSlice(
            accountName: CharSequence,
            icon: Icon?,
            description: CharSequence?,
            lastUsedTime: Instant?,
            credentialCountInformationMap: Map<String, Int>,
            pendingIntent: PendingIntent
        ): Slice {
            // TODO("Use the right type and revision")
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec("type", 1))
            sliceBuilder.addText(
                accountName, /*subType=*/null,
                listOf(SLICE_HINT_ACCOUNT_NAME)
            )

            if (lastUsedTime != null) {
                sliceBuilder.addLong(
                    lastUsedTime.toEpochMilli(),
                    /*subType=*/null,
                    listOf(SLICE_HINT_LAST_USED_TIME_MILLIS)
                )
            }

            if (description != null) {
                sliceBuilder.addText(description, null,
                    listOf(SLICE_HINT_NOTE))
            }

            if (icon != null) {
                sliceBuilder.addIcon(
                    icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON)
                )
            }
            val credentialCountBundle = convertCredentialCountInfoToBundle(
                credentialCountInformationMap
            )
            if (credentialCountBundle != null) {
                sliceBuilder.addBundle(
                    convertCredentialCountInfoToBundle(
                        credentialCountInformationMap
                    ), null, listOf(
                        SLICE_HINT_CREDENTIAL_COUNT_INFORMATION
                    )
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
         * Returns an instance of [CreateEntry] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         *
         * @hide
         */
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): CreateEntry? {
            // TODO("Put the right spec and version value")
            var accountName: CharSequence = ""
            var icon: Icon? = null
            var pendingIntent: PendingIntent? = null
            var credentialCountInfo: Map<String, Int> = mapOf()
            var description: CharSequence? = null
            var lastUsedTime: Instant? = null
            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_ACCOUNT_NAME)) {
                    accountName = it.text
                } else if (it.hasHint(SLICE_HINT_ICON)) {
                    icon = it.icon
                } else if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_CREDENTIAL_COUNT_INFORMATION)) {
                    credentialCountInfo = convertBundleToCredentialCountInfo(it.bundle)
                } else if (it.hasHint(SLICE_HINT_LAST_USED_TIME_MILLIS)) {
                    lastUsedTime = Instant.ofEpochMilli(it.long)
                } else if (it.hasHint(SLICE_HINT_NOTE)) {
                    description = it.text
                }
            }
            return try {
                CreateEntry(

                    accountName, pendingIntent!!, icon, description,
                    lastUsedTime, credentialCountInfo
                )
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        /** @hide **/
        @JvmStatic
        internal fun convertBundleToCredentialCountInfo(bundle: Bundle?):
            Map<String, Int> {
            val credentialCountMap = HashMap<String, Int>()
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

        /** @hide **/
        @JvmStatic
        internal fun convertCredentialCountInfoToBundle(
            credentialCountInformationMap: Map<String, Int>
        ): Bundle? {
            if (credentialCountInformationMap.isEmpty()) {
                return null
            }
            val bundle = Bundle()
            credentialCountInformationMap.forEach {
                bundle.putInt(it.key, it.value)
            }
            return bundle
        }

        @JvmField val CREATOR: Parcelable.Creator<CreateEntry> =
            object : Parcelable.Creator<CreateEntry> {
                override fun createFromParcel(p0: Parcel?): CreateEntry? {
                    val createEntry = android.service.credentials.CreateEntry
                        .CREATOR.createFromParcel(p0)
                    return fromSlice(createEntry.slice)
                }

                @Suppress("ArrayReturn")
                override fun newArray(size: Int): Array<CreateEntry?> {
                    return arrayOfNulls(size)
                }
            }
    }
    }
