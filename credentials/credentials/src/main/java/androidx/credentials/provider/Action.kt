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
 * An actionable entry that is shown on the user selector. When selected,
 * the associated [PendingIntent] is invoked.
 *
 * See [CredentialsResponseContent] for usage.
 *
 * @property title the title to be displayed on the UI with this
 * action entry
 * @property subTitle the subTitle to be displayed on the UI with this
 * action entry
 * @property pendingIntent the [PendingIntent] to be invoked when user
 * selects this action entry
 * @property icon the icon to be displayed on the UI with this action entry
 *
 * @hide
 */
@RequiresApi(34)
class Action constructor(
    val title: CharSequence,
    val subTitle: CharSequence?,
    val pendingIntent: PendingIntent,
    val icon: Icon?
    ) {

    init {
        require(title.isNotEmpty()) { "title must not be empty" }
    }

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_TITLE = "HINT_ACTION_TITLE"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_SUBTITLE = "HINT_ACTION_SUBTEXT"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val SLICE_HINT_ICON = "HINT_ACTION_ICON"

        @JvmStatic
        fun toSlice(action: Action): Slice {
            // TODO("Put the right spec and version value")
            return Slice.Builder(Uri.EMPTY, SliceSpec("type", 1))
                .addText(action.title, /*subType=*/null,
                    listOf(SLICE_HINT_TITLE))
                .addText(action.subTitle, /*subType=*/null,
                    listOf(SLICE_HINT_SUBTITLE))
                .addIcon(action.icon, /*subType=*/null,
                    listOf(SLICE_HINT_ICON))
                .build()
        }

        @JvmStatic
        internal fun toFrameworkClass(action: Action): android.service.credentials.Action {
            return android.service.credentials.Action(toSlice(action), action.pendingIntent)
        }
    }
}