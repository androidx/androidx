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
import android.net.Uri
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.util.Collections

/**
 * An entry on the selector, denoting that the provider service is locked and authentication
 * is needed to proceed.
 *
 * Providers should set this entry when the provider app is locked, and no credentials can
 * be returned.
 * Providers must set the [PendingIntent] that leads to their unlock activity. When the user
 * selects this entry, the corresponding [PendingIntent] is fired and the unlock activity is
 * invoked. Once the provider authentication flow is complete, providers must set
 * the [android.service.credentials.BeginGetCredentialResponse] containing the unlocked credential
 * entries, through the [PendingIntentHandler.setBeginGetCredentialResponse] method, before
 * finishing the activity.
 * If providers fail to set the [android.service.credentials.BeginGetCredentialResponse], the
 * system will assume that there are no credentials available and the this entry will be removed
 * from the selector.
 *
 * @property pendingIntent the [PendingIntent] to be invoked if the user selects
 * this authentication entry on the UI
 * @property title the title to be shown with this entry on the account selector UI
 *
 * @see android.service.credentials.BeginGetCredentialResponse
 * for usage details.
 *
 * @throws NullPointerException If the [pendingIntent] is null
 */
class AuthenticationAction constructor(
    val title: CharSequence,
    val pendingIntent: PendingIntent,
) {
    /**
     * A builder for [AuthenticationAction]
     *
     * @param title the title to be displayed with this authentication action entry
     * @param pendingIntent the [PendingIntent] that will be fired when the user selects
     * this entry
     */
    class Builder constructor(
        private val title: CharSequence,
        private val pendingIntent: PendingIntent
    ) {
        /**
         * Builds an instance of [AuthenticationAction]
         */
        fun build(): AuthenticationAction {
            return AuthenticationAction(title, pendingIntent)
        }
    }

    @Suppress("AcronymName")
    internal companion object {
        private const val TAG = "AuthenticationAction"
        private const val SLICE_SPEC_REVISION = 0
        private const val SLICE_SPEC_TYPE = "AuthenticationAction"

        private const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.authenticationAction.SLICE_HINT_TITLE"

        private const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.authenticationAction.SLICE_HINT_PENDING_INTENT"

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        @JvmStatic
        fun toSlice(authenticationAction: AuthenticationAction): Slice {
            val title = authenticationAction.title
            val pendingIntent = authenticationAction.pendingIntent
            val sliceBuilder = Slice.Builder(
                Uri.EMPTY, SliceSpec(
                    SLICE_SPEC_TYPE,
                    SLICE_SPEC_REVISION
                )
            )
            sliceBuilder
                .addAction(
                    pendingIntent,
                    Slice.Builder(sliceBuilder)
                        .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                        .build(),
                    /*subType=*/null
                )
                .addText(title, /*subType=*/null, listOf(SLICE_HINT_TITLE))
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [AuthenticationAction] derived from a [Slice] object.
         *
         * @param slice the [Slice] object that contains the information required for
         * constructing an instance of this class.
         *
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @RequiresApi(28)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): AuthenticationAction? {
            var title: CharSequence? = null
            var pendingIntent: PendingIntent? = null

            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                } else if (it.hasHint(SLICE_HINT_TITLE)) {
                    title = it.text
                }
            }
            return try {
                AuthenticationAction(title!!, pendingIntent!!)
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }
    }
}
