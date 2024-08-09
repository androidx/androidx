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
@file:Suppress("deprecation") // For usage of Slice

package androidx.credentials.provider

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.slice.Slice
import android.app.slice.SliceSpec
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import java.util.Collections

/**
 * An entry on the selector, denoting that the provider service is locked and authentication is
 * needed to proceed.
 *
 * Providers should set this entry when the provider app is locked, and no credentials can be
 * returned. Providers must set the [PendingIntent] that leads to their unlock activity. When the
 * user selects this entry, the corresponding [PendingIntent] is fired and the unlock activity is
 * invoked.
 *
 * When the user is done with the authentication flow and the provider has credential entries to
 * return, provider must call [android.app.Activity.setResult] with the result code as
 * [android.app.Activity.RESULT_OK], and the [android.content.Intent] data that has been prepared by
 * setting [BeginGetCredentialResponse] using [PendingIntentHandler.setBeginGetCredentialResponse],
 * or by setting [androidx.credentials.exceptions.GetCredentialException] using
 * [PendingIntentHandler.setGetCredentialException] before ending the activity. If the provider does
 * not have a credential, or an exception to return, provider must call
 * [android.app.Activity.setResult] with the result code as [android.app.Activity.RESULT_CANCELED].
 * Setting the result code to [android.app.Activity.RESULT_CANCELED] will re-surface the selector,
 * with this authentication action labeled as having no valid credentials.
 *
 * @param title the title to be shown with this entry on the account selector UI
 * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this entry,
 *   must be created with a unique request code per entry, with flag [PendingIntent.FLAG_MUTABLE] to
 *   allow the Android system to attach the final request, and NOT with flag
 *   [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
 * @constructor constructs an instance of [AuthenticationAction]
 * @throws NullPointerException If the [pendingIntent] or [title] is null
 * @throws IllegalArgumentException If the [title] is empty
 * @see android.service.credentials.BeginGetCredentialResponse for more usage details.
 */
class AuthenticationAction(
    val title: CharSequence,
    val pendingIntent: PendingIntent,
) {
    init {
        require(title.isNotEmpty()) { "title must not be empty" }
    }

    /**
     * A builder for [AuthenticationAction]
     *
     * @param title the title to be displayed with this authentication action entry
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     *   entry, must be created with a unique request code per entry, with flag
     *   [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the final request, and
     *   NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
     */
    class Builder
    constructor(private val title: CharSequence, private val pendingIntent: PendingIntent) {
        /** Builds an instance of [AuthenticationAction] */
        fun build(): AuthenticationAction {
            return AuthenticationAction(title, pendingIntent)
        }
    }

    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromAction(
            authenticationAction: android.service.credentials.Action
        ): AuthenticationAction? {
            val slice = authenticationAction.slice
            return fromSlice(slice)
        }
    }

    companion object {
        private const val TAG = "AuthenticationAction"
        private const val SLICE_SPEC_REVISION = 0
        private const val SLICE_SPEC_TYPE = "AuthenticationAction"

        private const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.authenticationAction.SLICE_HINT_TITLE"

        private const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.authenticationAction.SLICE_HINT_PENDING_INTENT"

        @RequiresApi(28)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmStatic
        fun toSlice(authenticationAction: AuthenticationAction): Slice {
            val title = authenticationAction.title
            val pendingIntent = authenticationAction.pendingIntent
            val sliceBuilder =
                Slice.Builder(Uri.EMPTY, SliceSpec(SLICE_SPEC_TYPE, SLICE_SPEC_REVISION))
            sliceBuilder
                .addAction(
                    pendingIntent,
                    Slice.Builder(sliceBuilder)
                        .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                        .build(),
                    /*subType=*/ null
                )
                .addText(title, /* subType= */ null, listOf(SLICE_HINT_TITLE))
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [AuthenticationAction] derived from a [Slice] object.
         *
         * @param slice the [Slice] object that contains the information required for constructing
         *   an instance of this class.
         */
        @RequiresApi(28)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @RestrictTo(RestrictTo.Scope.LIBRARY)
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

        /**
         * Converts a framework [android.service.credentials.Action] class to a Jetpack
         * [AuthenticationAction] class
         *
         * Note that this API is not needed in a general credential retrieval flow that is
         * implemented using this jetpack library, where you are only required to construct an
         * instance of [AuthenticationAction] to populate the [BeginGetCredentialResponse], along
         * with setting other entries.
         *
         * @param authenticationAction the instance of framework action class to be converted
         */
        @JvmStatic
        @RequiresApi(34)
        fun fromAction(
            authenticationAction: android.service.credentials.Action
        ): AuthenticationAction? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromAction(authenticationAction)
            }
            return null
        }

        private const val EXTRA_AUTH_ACTION_SIZE =
            "androidx.credentials.provider.extra.AUTH_ACTION_SIZE"
        private const val EXTRA_AUTH_ACTION_PENDING_INTENT_PREFIX =
            "androidx.credentials.provider.extra.AUTH_ACTION_PENDING_INTENT_"
        private const val EXTRA_AUTH_ACTION_TITLE_PREFIX =
            "androidx.credentials.provider.extra.AUTH_ACTION_TITLE_"

        /** Marshall a list of auth action data through an intent. */
        internal fun List<AuthenticationAction>.marshall(bundle: Bundle) {
            bundle.putInt(EXTRA_AUTH_ACTION_SIZE, this.size)
            for (i in indices) {
                bundle.putParcelable(
                    "$EXTRA_AUTH_ACTION_PENDING_INTENT_PREFIX$i",
                    this[i].pendingIntent
                )
                bundle.putCharSequence("$EXTRA_AUTH_ACTION_TITLE_PREFIX$i", this[i].title)
            }
        }

        /**
         * Returns a list of [AuthenticationAction]s from an Intent, which was supposed to be
         * injected via [marshall]. Returns an empty list if parsing fails in any way.
         */
        internal fun Bundle.unmarshallAuthActionList(): List<AuthenticationAction> {
            val authActions = mutableListOf<AuthenticationAction>()
            val size = this.getInt(EXTRA_AUTH_ACTION_SIZE, 0)
            for (i in 0 until size) {
                val pendingIntent: PendingIntent? =
                    this.getParcelable("$EXTRA_AUTH_ACTION_PENDING_INTENT_PREFIX$i")
                val title: CharSequence? = this.getCharSequence("$EXTRA_AUTH_ACTION_TITLE_PREFIX$i")
                if (pendingIntent == null || title == null) {
                    return emptyList()
                }
                authActions.add(AuthenticationAction(title, pendingIntent))
            }
            return authActions
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AuthenticationAction) return false
        return this.title == other.title && this.pendingIntent == other.pendingIntent
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + pendingIntent.hashCode()
        return result
    }
}
