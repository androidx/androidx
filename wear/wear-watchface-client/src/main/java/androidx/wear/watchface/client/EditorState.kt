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

/**
 * The state of the editing session. See [androidx.wear.watchface.editor.EditorSession].
 *
 * @param watchFaceInstanceId Unique ID for the instance of the watch face being edited, only
 *     defined for Android R and beyond, it's `null` on Android P and earlier.
 * @param userStyle The current [UserStyle] encoded as a Map<String, String>.
 * @param previewComplicationData Preview [ComplicationData] needed for taking screenshots without
 *     live complication data.
 * @param commitChanges Whether or not this state should be committed (i.e. the user aborted the
 *     session). If it's not committed then any changes (E.g. complication provider changes)
 *     should be abandoned. There's no need to resend the style to the watchface because the
 *     library will have restored the previous style.
 */
public class EditorState internal constructor(
    public val watchFaceInstanceId: String?,
    public val userStyle: Map<String, String>,
    public val previewComplicationData: Map<Int, ComplicationData>,
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