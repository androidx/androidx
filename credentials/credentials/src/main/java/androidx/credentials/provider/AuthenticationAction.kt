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
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

/**
 * An entry on the selector, denoting that authentication is needed to proceed.
 *
 * Providers should set this entry when the provider app is locked, and no credentials can
 * be returned. Providers must set the [PendingIntent] that leads to their authentication flow.
 *
 * @property title the title to be displayed on the UI with this authentication entry. If not
 * provided, the label from the provider service is displayed
 * @property pendingIntent the [PendingIntent] to be invoked when the user selects
 * this authentication entry
 * @property icon the icon to be displayed on the UI with this authentication entry. If not
 * provided, the label from the provider service is displayed
 *
 * See [CredentialsResponseContent] for usage details.
 *
 * @hide
 */
@RequiresApi(34)
class AuthenticationAction constructor(
    val title: String?,
    val pendingIntent: PendingIntent,
    val icon: Icon?
    ) {
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_TITLE = "androidx.credentials.provider.auth.SLICE_HINT_TITLE"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_ICON = "androidx.credentials.provider.auth.SLICE_HINT_ICON"
        @JvmStatic
        fun toSlice(authenticationAction: AuthenticationAction): Slice {
            // TODO("Put the right spec and version value")
            return Slice.Builder(Uri.EMPTY, SliceSpec("type", 1))
                .addText(authenticationAction.title, /*subType=*/null,
                    listOf(SLICE_HINT_TITLE))
                .addIcon(authenticationAction.icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON))
                .build()
        }

        internal fun toFrameworkClass(authenticationAction: AuthenticationAction):
            android.service.credentials.Action {
            return android.service.credentials.Action(toSlice(authenticationAction),
                authenticationAction.pendingIntent)
        }
    }
}