/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.painting

/**
 * Records a [Picture] containing a sequence of graphical operations.
 *
 * To begin recording, construct a [Canvas] to record the commands.
 * To end recording, use the [PictureRecorder.endRecording] method.
 */

/**
 * Creates a new idle PictureRecorder. To associate it with a
 * [Canvas] and begin recording, pass this [PictureRecorder] to the
 * [Canvas] constructor.
 */
class PictureRecorder {

    internal val frameworkPicture = android.graphics.Picture()

    /**
     * Whether this object is currently recording commands.
     *
     * Specifically, this returns true if a [Canvas] object has been
     * created to record commands and recording has not yet ended via a
     * call to [endRecording], and false if either this
     * [PictureRecorder] has not yet been associated with a [Canvas],
     * or the [endRecording] method has already been called.
     */
    val isRecording: Boolean
    get() {
        TODO()
//        native 'PictureRecorder_isRecording';
    }

    /**
     * Finishes recording graphical operations.
     *
     * Returns a picture containing the graphical operations that have been
     * recorded thus far. After calling this function, both the picture recorder
     * and the canvas objects are invalid and cannot be used further.
     *
     * Returns null if the PictureRecorder is not associated with a canvas.
     */
    fun endRecording(): Picture {
        return Picture(frameworkPicture)
    }
}