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

import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.credentials.CredentialManager

/**
 * An entry to be shown on the selector during a create flow initiated when an app calls
 * [CredentialManager.executeCreateCredential]
 *
 * A [CreateEntry] points to a location such as an account, or a group where the credential can be
 * registered. When user selects this entry, the corresponding [PendingIntent] is fired, and the
 * credential creation can be completed.
 *
 * @property accountName the name of the account where the credential
 * will be registered
 * @property pendingIntent the [PendingIntent] to be fired when this
 * [CreateEntry] is selected
 * @property lastUsedTimeMillis the last used time of the account/group underlying this entry
 * @property credentialCountInformation a list of count information per credential type
 * @throws IllegalArgumentException If [accountName] is empty
 *
 * @hide
 */
@RequiresApi(34)
class CreateEntry internal constructor(
    val accountName: CharSequence,
    val pendingIntent: PendingIntent,
    val icon: Icon?,
    val lastUsedTimeMillis: Long,
    val credentialCountInformation: List<CredentialCountInformation>
    ) {

    init {
        require(accountName.isNotEmpty()) { "accountName must not be empty" }
    }

    /**
     * A builder for [CreateEntry]
     *
     * @property accountName the name of the account where the credential will be registered
     * @property pendingIntent the [PendingIntent] that will be fired when the user selects
     * this entry
     *
     * @hide
     */
    class Builder constructor(
        private val accountName: CharSequence,
        private val pendingIntent: PendingIntent
        ) {

        private var credentialCountInformationList: MutableList<CredentialCountInformation> =
            mutableListOf()
        private var icon: Icon? = null
        private var lastUsedTimeMillis: Long = 0

        /** Adds a [CredentialCountInformation] denoting a given credential
         * type and the count of credentials that the provider has stored for that
         * credential type.
         *
         * This information will be displayed on the [CreateEntry] to help the user
         * make a choice.
         */
        fun addCredentialCountInformation(info: CredentialCountInformation): Builder {
            credentialCountInformationList.add(info)
            return this
        }

        /** Sets a list of [CredentialCountInformation]. Each item in the list denotes a given
         * credential type and the count of credentials that the provider has stored of that
         * credential type.
         *
         * This information will be displayed on the [CreateEntry] to help the user
         * make a choice.
         */
        fun setCredentialCountInformationList(infoList: List<CredentialCountInformation>): Builder {
            credentialCountInformationList = infoList as MutableList<CredentialCountInformation>
            return this
        }

        /** Sets an icon to be displayed with the entry on the UI */
        fun setIcon(icon: Icon?): Builder {
            this.icon = icon
            return this
        }

        /** Sets the last time this account was used */
        fun setLastUsedTime(lastUsedTimeMillis: Long): Builder {
            this.lastUsedTimeMillis = lastUsedTimeMillis
            return this
        }

        /**
         * Builds an instance of [CreateEntry]
         *
         * @throws IllegalArgumentException If [accountName] is empty
         */
        fun build(): CreateEntry {
            return CreateEntry(accountName, pendingIntent, icon, lastUsedTimeMillis,
                credentialCountInformationList)
        }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_ACCOUNT_NAME = "HINT_USER_PROVIDER_ACCOUNT_NAME"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_ICON = "HINT_PROFILE_ICON"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_CREDENTIAL_COUNT_INFORMATION =
            "androidx.credentials.provider.SLICE_CREDENTIAL_COUNT_INFORMATION"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_LAST_USED_TIME_MILLIS = "HINT_LAST_USED_TIME_MILLIS"

        @JvmStatic
        fun toSlice(createEntry: CreateEntry): Slice {
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec("type", 1))
                .addText(createEntry.accountName, /*subType=*/null,
                    listOf(SLICE_HINT_ACCOUNT_NAME))
                .addIcon(createEntry.icon, /*subType=*/null, listOf(SLICE_HINT_ICON))
                .addLong(createEntry.lastUsedTimeMillis, /*subType=*/null, listOf(
                    SLICE_HINT_LAST_USED_TIME_MILLIS))

                val credentialCountBundle = convertCredentialCountInfoToBundle(
                    createEntry.credentialCountInformation)
                if (credentialCountBundle != null) {
                    sliceBuilder.addBundle(convertCredentialCountInfoToBundle(
                        createEntry.credentialCountInformation), null, listOf(
                        SLICE_CREDENTIAL_COUNT_INFORMATION))
                }
                return sliceBuilder.build()
        }

        // TODO("Add fromSlice for UI to call")

        @JvmStatic
        internal fun convertCredentialCountInfoToBundle(
            credentialCountInformation: List<CredentialCountInformation>
        ): Bundle? {
            if (credentialCountInformation.isEmpty()) {
                return null
            }
            val bundle = Bundle()
            credentialCountInformation.forEach {
                bundle.putInt(it.type, it.count)
            }
            return bundle
        }

        @JvmStatic
        internal fun toFrameworkClass(createEntry: CreateEntry):
            android.service.credentials.CreateEntry {
            return android.service.credentials.CreateEntry(toSlice(createEntry),
                createEntry.pendingIntent)
        }
    }
}