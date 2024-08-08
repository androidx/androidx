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
 * An actionable entry that is returned as part of the
 * [android.service.credentials.BeginGetCredentialResponse], and then shown on the user selector
 * under a separate category of 'Actions'. An action entry is expected to navigate the user to an
 * activity belonging to the credential provider, and finally result in a
 * [androidx.credentials.GetCredentialResponse].
 *
 * When selected, the associated [PendingIntent] is invoked to launch a provider controlled
 * activity. The activity invoked due to this pending intent will contain the
 * [android.service.credentials.BeginGetCredentialRequest] as part of the intent extras. Providers
 * must use [PendingIntentHandler.retrieveBeginGetCredentialRequest] to get the request.
 *
 * When the user is done interacting with the activity and the provider has a credential to return,
 * provider must call [android.app.Activity.setResult] with the result code as
 * [android.app.Activity.RESULT_OK], and the [android.content.Intent] data that has been prepared by
 * setting [androidx.credentials.GetCredentialResponse] using
 * [PendingIntentHandler.setGetCredentialResponse], or by setting
 * [androidx.credentials.exceptions.GetCredentialException] using
 * [PendingIntentHandler.setGetCredentialException] before ending the activity. If the provider does
 * not have a credential, or an exception to return, provider must call
 * [android.app.Activity.setResult] with the result code as [android.app.Activity.RESULT_CANCELED].
 * Setting the result code to [android.app.Activity.RESULT_CANCELED] will re-surface the selector.
 *
 * Examples of [Action] entries include an entry that is titled 'Add a new Password', and navigates
 * to the 'add password' page of the credential provider app, or an entry that is titled 'Manage
 * Credentials' and navigates to a particular page that lists all credentials, where the user may
 * end up selecting a credential that the provider can then return.
 *
 * @param title the title of the entry
 * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this entry,
 *   must be created with a unique request code per entry, with flag [PendingIntent.FLAG_MUTABLE] to
 *   allow the Android system to attach the final request, and NOT with flag
 *   [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
 * @param subtitle the optional subtitle that is displayed on the entry
 * @constructor constructs an instance of [Action]
 * @throws IllegalArgumentException If [title] is empty
 * @throws NullPointerException If [title] or [pendingIntent] is null
 * @see android.service.credentials.BeginGetCredentialResponse for usage.
 */
class Action(
    val title: CharSequence,
    val pendingIntent: PendingIntent,
    val subtitle: CharSequence? = null,
) {

    init {
        require(title.isNotEmpty()) { "title must not be empty" }
    }

    /**
     * A builder for [Action]
     *
     * @param title the title of this action entry
     * @param pendingIntent the [PendingIntent] that will get invoked when the user selects this
     *   entry, must be created with a unique request code per entry, with flag
     *   [PendingIntent.FLAG_MUTABLE] to allow the Android system to attach the final request, and
     *   NOT with flag [PendingIntent.FLAG_ONE_SHOT] as it can be invoked multiple times
     */
    class Builder
    constructor(private val title: CharSequence, private val pendingIntent: PendingIntent) {
        private var subtitle: CharSequence? = null

        /** Sets a sub title to be shown on the UI with this entry */
        fun setSubtitle(subtitle: CharSequence?): Builder {
            this.subtitle = subtitle
            return this
        }

        /**
         * Builds an instance of [Action]
         *
         * @throws IllegalArgumentException If [title] is empty
         */
        fun build(): Action {
            return Action(title, pendingIntent, subtitle)
        }
    }

    @RequiresApi(34)
    private object Api34Impl {
        @JvmStatic
        fun fromAction(action: android.service.credentials.Action): Action? {
            val slice = action.slice
            return fromSlice(slice)
        }
    }

    companion object {
        private const val TAG = "Action"
        private const val SLICE_SPEC_REVISION = 0
        private const val SLICE_SPEC_TYPE = "Action"

        private const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.action.HINT_ACTION_TITLE"

        private const val SLICE_HINT_SUBTITLE =
            "androidx.credentials.provider.action.HINT_ACTION_SUBTEXT"

        private const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.action.SLICE_HINT_PENDING_INTENT"

        /** Converts to slice */
        @JvmStatic
        @RequiresApi(28)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        fun toSlice(action: Action): Slice {
            val title = action.title
            val subtitle = action.subtitle
            val pendingIntent = action.pendingIntent
            val sliceBuilder =
                Slice.Builder(Uri.EMPTY, SliceSpec(SLICE_SPEC_TYPE, SLICE_SPEC_REVISION))
                    .addText(title, /* subType= */ null, listOf(SLICE_HINT_TITLE))
                    .addText(subtitle, /* subType= */ null, listOf(SLICE_HINT_SUBTITLE))
            sliceBuilder.addAction(
                pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(),
                /*subType=*/ null
            )
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [Action] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         */
        @RequiresApi(28)
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @SuppressLint("WrongConstant") // custom conversion between jetpack and framework
        @JvmStatic
        fun fromSlice(slice: Slice): Action? {
            var title: CharSequence = ""
            var subtitle: CharSequence? = null
            var pendingIntent: PendingIntent? = null

            slice.items.forEach {
                if (it.hasHint(SLICE_HINT_TITLE)) {
                    title = it.text
                } else if (it.hasHint(SLICE_HINT_SUBTITLE)) {
                    subtitle = it.text
                } else if (it.hasHint(SLICE_HINT_PENDING_INTENT)) {
                    pendingIntent = it.action
                }
            }

            return try {
                Action(title, pendingIntent!!, subtitle)
            } catch (e: Exception) {
                Log.i(TAG, "fromSlice failed with: " + e.message)
                null
            }
        }

        /**
         * Converts a framework [android.service.credentials.Action] class to a Jetpack [Action]
         * class
         *
         * Note that this API is not needed in a general credential retrieval flow that is
         * implemented using this jetpack library, where you are only required to construct an
         * instance of [Action] to populate the [BeginGetCredentialResponse], along with setting
         * other entries.
         *
         * @param action the instance of framework action class to be converted
         */
        @JvmStatic
        fun fromAction(action: android.service.credentials.Action): Action? {
            if (Build.VERSION.SDK_INT >= 34) {
                return Api34Impl.fromAction(action)
            }
            return null
        }

        private const val EXTRA_ACTION_SIZE = "androidx.credentials.provider.extra.ACTION_SIZE"
        private const val EXTRA_ACTION_PENDING_INTENT_PREFIX =
            "androidx.credentials.provider.extra.ACTION_PENDING_INTENT_"
        private const val EXTRA_ACTION_TITLE_PREFIX =
            "androidx.credentials.provider.extra.ACTION_TITLE_"
        private const val EXTRA_ACTION_SUBTITLE_PREFIX =
            "androidx.credentials.provider.extra.ACTION_SUBTITLE_"

        /** Marshall a list of action data through an intent. */
        internal fun List<Action>.marshall(bundle: Bundle) {
            bundle.putInt(EXTRA_ACTION_SIZE, this.size)
            for (i in indices) {
                bundle.putParcelable("$EXTRA_ACTION_PENDING_INTENT_PREFIX$i", this[i].pendingIntent)
                bundle.putCharSequence("$EXTRA_ACTION_TITLE_PREFIX$i", this[i].title)
                bundle.putCharSequence("$EXTRA_ACTION_SUBTITLE_PREFIX$i", this[i].subtitle)
            }
        }

        /**
         * Returns a list of [Action]s from an Intent, which was supposed to be injected via
         * [marshall]. Returns an empty list if parsing fails in any way.
         */
        internal fun Bundle.unmarshallActionList(): List<Action> {
            val actions = mutableListOf<Action>()
            val size = this.getInt(EXTRA_ACTION_SIZE, 0)
            for (i in 0 until size) {
                val pendingIntent: PendingIntent? =
                    this.getParcelable("$EXTRA_ACTION_PENDING_INTENT_PREFIX$i")
                val title: CharSequence? = this.getCharSequence("$EXTRA_ACTION_TITLE_PREFIX$i")
                val subtitle: CharSequence? =
                    this.getCharSequence("$EXTRA_ACTION_SUBTITLE_PREFIX$i")
                if (pendingIntent == null || title == null) {
                    return emptyList()
                }
                actions.add(Action(title, pendingIntent, subtitle))
            }
            return actions
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Action) return false
        return this.title == other.title &&
            this.pendingIntent == other.pendingIntent &&
            this.subtitle == other.subtitle
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + pendingIntent.hashCode()
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        return result
    }
}
