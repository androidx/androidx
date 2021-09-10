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

import android.graphics.Bitmap
import android.os.Build
import android.support.wearable.watchface.SharedMemoryImage
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.complications.data.ComplicationData
import androidx.wear.complications.data.toApiComplicationData
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData

/**
 * The system is responsible for the management and generation of these ids and they have no
 * context outside of an instance of an EditorState and should not be stored or saved for later
 * use by the WatchFace provider.
 *
 * @param id The system's id for a watch face being edited. This is passed in from
 * [androidx.wear.watchface.EditorRequest.watchFaceId].
 */
public class WatchFaceId(public val id: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WatchFaceId

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

/**
 * The state of the editing session. See [androidx.wear.watchface.editor.EditorSession].
 *
 * @param watchFaceId Unique ID for the instance of the watch face being edited (see
 * [androidx.wear.watchface.editor.EditorRequest.watchFaceId]), only defined for Android R and
 * beyond.
 * @param userStyle The current [UserStyle] encoded as a [UserStyleData].
 * @param previewComplicationsData Preview [ComplicationData] needed for taking screenshots without
 * live complication data.
 * @param shouldCommitChanges Whether or not this state should be committed (i.e. the user aborted
 * the session). If it's not committed then any changes (E.g. complication data source changes)
 * should  be abandoned. There's no need to resend the style to the watchface because the library
 * will have restored the previous style.
 * @param previewImage If `non-null` this [Bitmap] contains a preview image of the watch face
 * rendered with the final style and complications and the
 * [androidx.wear.watchface.editor.PreviewScreenshotParams] specified in the
 * [androidx.wear.watchface.editor.EditorRequest]. If [shouldCommitChanges] is `false` then this
 * will also be `null` (see implementation of [androidx.wear.watchface.editor.EditorSession.close]).
 */
public class EditorState internal constructor(
    @RequiresApi(Build.VERSION_CODES.R)
    public val watchFaceId: WatchFaceId,
    public val userStyle: UserStyleData,
    public val previewComplicationsData: Map<Int, ComplicationData>,
    @get:JvmName("shouldCommitChanges")
    public val shouldCommitChanges: Boolean,
    public val previewImage: Bitmap?
) {
    override fun toString(): String =
        "{watchFaceId: ${watchFaceId.id}, userStyle: $userStyle" +
            ", previewComplicationsData: [" +
            previewComplicationsData.map { "${it.key} -> ${it.value}" }.joinToString() +
            "], shouldCommitChanges: $shouldCommitChanges, previewImage: ${previewImage != null}"
}

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun EditorStateWireFormat.asApiEditorState(): EditorState {
    return EditorState(
        WatchFaceId(watchFaceInstanceId ?: ""),
        UserStyleData(userStyle.mUserStyle),
        previewComplicationData.associateBy(
            { it.id },
            { it.complicationData.toApiComplicationData() }
        ),
        commitChanges,
        previewImageBundle?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                SharedMemoryImage.ashmemReadImageBundle(it)
            } else {
                null
            }
        }
    )
}
