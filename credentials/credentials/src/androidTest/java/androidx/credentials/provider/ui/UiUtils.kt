/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.credentials.provider.Action
import androidx.credentials.provider.AuthenticationAction
import androidx.credentials.provider.BeginGetPasswordOption
import androidx.credentials.provider.CreateEntry
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.RemoteEntry
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress

@SdkSuppress(minSdkVersion = 34, codeName = "UpsideDownCake")
class UiUtils {
    companion object {
        private val sContext = ApplicationProvider.getApplicationContext<Context>()
        private val sIntent = Intent()
        private val sPendingIntent = PendingIntent.getActivity(
            sContext, 0, sIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        private val ACCOUNT_NAME: CharSequence = "account_name"
        private const val DESCRIPTION = "description"
        private const val PASSWORD_COUNT = 10
        private const val PUBLIC_KEY_CREDENTIAL_COUNT = 10
        private const val TOTAL_COUNT = 10
        private const val LAST_USED_TIME = 10L
        private val ICON = Icon.createWithBitmap(
            Bitmap.createBitmap(
                100, 100, Bitmap.Config.ARGB_8888
            )
        )
        private val BEGIN_OPTION = BeginGetPasswordOption(
            setOf(), Bundle.EMPTY, "id"
        )

        /**
         * Generates a default authentication action entry that can be used for tests around the
         * provider objects.
         */
        @JvmStatic
        fun constructAuthenticationActionEntry(title: CharSequence): AuthenticationAction {
            return AuthenticationAction(title, sPendingIntent)
        }

        /**
         * Generates a default action entry that can be used for tests around the provider
         * objects.
         */
        @JvmStatic
        fun constructActionEntry(title: CharSequence, subtitle: CharSequence): Action {
            return Action(title, sPendingIntent, subtitle)
        }

        /**
         * Generates a default password credential entry that can be used for tests around the
         * provider objects.
         */
        @JvmStatic
        fun constructPasswordCredentialEntryDefault(username: CharSequence): CredentialEntry {
            return PasswordCredentialEntry(
                sContext,
                username,
                sPendingIntent,
                BEGIN_OPTION
            )
        }

        /**
         * Generate a default remote entry that can be used for tests around the provider objects.
         */
        @JvmStatic
        fun constructRemoteEntryDefault(): RemoteEntry {
            return RemoteEntry(sPendingIntent)
        }

        /**
         * Generates a create entry with known inputs for accountName and description in order
         * to test proper formation.
         *
         * @param accountName the account name associated with the create entry
         * @param description the description associated with the create entry
         */
        @JvmStatic
        fun constructCreateEntryWithSimpleParams(
            accountName: CharSequence,
            description: CharSequence
        ):
            CreateEntry {
            return CreateEntry.Builder(accountName, sPendingIntent).setDescription(description)
                .build()
        }
    }
}