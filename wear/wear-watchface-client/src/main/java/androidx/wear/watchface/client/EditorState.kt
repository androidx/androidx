/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.watchface.client

import androidx.annotation.RestrictTo
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.asApiComplicationData
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.UserStyle

/** The state of the editing session. See [androidx.wear.watchface.editor.EditorSession]. */
public class EditorState internal constructor(
    /**
     * Unique ID for the instance of the watch face being edited, only defined for Android R and
     * beyond, it's `null` on Android P and earlier.
     */
    public val watchFaceInstanceId: String?,

    /** The current [UserStyle] encoded as a Map<String, String>. */
    public val userStyle: Map<String, String>,

    /** Preview [ComplicationData] needed for taking screenshots without live complication data. */
    public val previewComplicationData: Map<Int, ComplicationData>,

    /** Whether or not this state should be committed. */
    @get:JvmName("hasCommitChanges")
    public val commitChanges: Boolean
)

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun EditorStateWireFormat.asApiEditorState(): EditorState {
    return EditorState(
        watchFaceInstanceId,
        userStyle.mUserStyle,
        previewComplicationData.associateBy(
            { it.id },
            { it.complicationData.asApiComplicationData() }
        ),
        commitChanges
    )
}