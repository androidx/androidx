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
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import java.util.Collections

/**
 * An actionable entry that is returned as part of the
 * [android.service.credentials.BeginGetCredentialResponse], and then shown on the user selector.
 * When selected, the associated [PendingIntent] is invoked to launch a provider controlled
 * activity.
 *
 * Examples of [Action] entries include and entry that it titled 'Open App', and navigates
 * to the home page of the credential provider app, or an entry that is titled 'Manage Credentials'
 * and navigates to a particular page that lists all credentials.
 *
 * @property title the title of the entry
 * @property pendingIntent the [PendingIntent] that will be invoked when the user selects this entry
 * @property subtitle the optional subtitle that is displayed on the entry
 *
 * @see android.service.credentials.CredentialsResponseContent for usage.
 *
 * @throws IllegalArgumentException If [title] is empty
 * @throws NullPointerException If [title] or [pendingIntent] is null
 */
@RequiresApi(34)
class Action constructor(
    val title: CharSequence,
    val pendingIntent: PendingIntent,
    val subtitle: CharSequence? = null,
    ) : android.service.credentials.Action(
    toSlice(title, subtitle, pendingIntent)) {

    init {
        require(title.isNotEmpty()) { "title must not be empty" }
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(@NonNull dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
    }

    /**
     * A builder for [Action]
     *
     * @param title the title of this action entry
     * @param pendingIntent the [PendingIntent] that will be fired when the user selects
     * this action entry
     */
    class Builder constructor(
        private val title: CharSequence,
        private val pendingIntent: PendingIntent
    ) {
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

    @Suppress("AcronymName")
    companion object {
        private const val TAG = "Action"
        private const val SLICE_SPEC_REVISION = 0
        private const val SLICE_SPEC_TYPE = "Action"

        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_TITLE =
            "androidx.credentials.provider.action.HINT_ACTION_TITLE"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_SUBTITLE =
            "androidx.credentials.provider.action.HINT_ACTION_SUBTEXT"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        internal const val SLICE_HINT_PENDING_INTENT =
            "androidx.credentials.provider.action.SLICE_HINT_PENDING_INTENT"

        @JvmStatic
        internal fun toSlice(
            title: CharSequence,
            subtitle: CharSequence?,
            pendingIntent: PendingIntent
        ): Slice {
            val sliceBuilder = Slice.Builder(Uri.EMPTY, SliceSpec(
                SLICE_SPEC_TYPE, SLICE_SPEC_REVISION))
                .addText(title, /*subType=*/null,
                    listOf(SLICE_HINT_TITLE))
                .addText(subtitle, /*subType=*/null,
                    listOf(SLICE_HINT_SUBTITLE))
            sliceBuilder.addAction(pendingIntent,
                Slice.Builder(sliceBuilder)
                    .addHints(Collections.singletonList(SLICE_HINT_PENDING_INTENT))
                    .build(),
                /*subType=*/null)
            return sliceBuilder.build()
        }

        /**
         * Returns an instance of [Action] derived from a [Slice] object.
         *
         * @param slice the [Slice] object constructed through [toSlice]
         *
         * @hide
         */
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

        @JvmField val CREATOR: Parcelable.Creator<Action> = object :
            Parcelable.Creator<Action> {
            /**
             * This will not be used in any of the credMan flows as Action is constructed
             * in the jetpack library and sent to the framework. UI app will receive the
             * slice and use [fromSlice] to get back the object.
             */
            override fun createFromParcel(p0: Parcel?): Action? {
                val action = android.service.credentials.Action.CREATOR.createFromParcel(p0)
                return fromSlice(action.slice)
            }

            @Suppress("ArrayReturn")
            override fun newArray(size: Int): Array<Action?> {
                return arrayOfNulls<Action>(size)
            }
        }
    }
}