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
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.toApiComplicationData
import androidx.wear.watchface.editor.data.EditorStateWireFormat
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleData

/**
 * The system is responsible for the management and generation of these ids. Some systems support
 * multiple instances of a watchface and hence there can be multiple ids, but only if
 * androidx.wear.watchface.MULTIPLE_INSTANCES_ALLOWED meta-data is in the watch face's manifest.
 *
 * Some systems don't support multiple instances at all, for those there'll only be one id, however
 * watch faces and editors should not need to do anything special because of this.
 *
 * @param id The system's id for a watch face being edited. This is passed in from
 * [androidx.wear.watchface.EditorRequest.watchFaceId] and matches the value passed to
 * [androidx.wear.watchface.WatchState.watchFaceInstanceId].
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

    override fun toString(): String {
        return "WatchFaceId(id='$id')"
    }
}

/**
 * The state of the editing session. See [androidx.wear.watchface.editor.EditorSession].
 *
 * @param watchFaceId The system's watch face instance ID. See [WatchFaceId] for details.
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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EditorState

        if (watchFaceId != other.watchFaceId) return false
        if (userStyle != other.userStyle) return false
        if (previewComplicationsData != other.previewComplicationsData) return false
        if (shouldCommitChanges != other.shouldCommitChanges) return false
        if (previewImage != other.previewImage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = watchFaceId.hashCode()
        result = 31 * result + userStyle.hashCode()
        result = 31 * result + previewComplicationsData.hashCode()
        result = 31 * result + shouldCommitChanges.hashCode()
        result = 31 * result + (previewImage?.hashCode() ?: 0)
        return result
    }
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
