/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.antelope

/** Contains all the different timing values for a test run */
class CameraTimer {
    internal var testStart: Long = 0
    internal var testEnd: Long = 0

    internal var openStart: Long = 0
    internal var openEnd: Long = 0

    internal var cameraCloseStart: Long = 0
    internal var cameraCloseEnd: Long = 0

    internal var previewFillStart: Long = 0
    internal var previewFillEnd: Long = 0

    internal var captureStart: Long = 0
    internal var captureEnd: Long = 0

    internal var autofocusStart: Long = 0
    internal var autofocusEnd: Long = 0

    internal var imageReaderStart: Long = 0
    internal var imageReaderEnd: Long = 0

    internal var imageSaveStart: Long = 0
    internal var imageSaveEnd: Long = 0

    internal var previewStart: Long = 0
    internal var previewEnd: Long = 0

    internal var previewCloseStart: Long = 0
    internal var previewCloseEnd: Long = 0

    internal var switchToSecondStart: Long = 0
    internal var switchToSecondEnd: Long = 0
    internal var switchToFirstStart: Long = 0
    internal var switchToFirstEnd: Long = 0

    internal var isFirstPhoto: Boolean = true
    internal var isHDRPlus: Boolean = false

    /** Reset timers related to an individual capture */
    fun clearImageTimers() {
        captureStart = 0L
        captureEnd = 0L
        autofocusStart = 0L
        autofocusEnd = 0L
        imageReaderStart = 0L
        imageReaderEnd = 0L
        imageSaveStart = 0L
        imageSaveEnd = 0L
        isHDRPlus = false
    }
}