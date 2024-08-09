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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.CaptureSequence
import androidx.camera.camera2.pipe.CaptureSequence.CaptureSequenceListener

/**
 * Fake [CaptureSequenceListener] instance that can be used to check to see if
 * [onCaptureSequenceComplete] has been invoked.
 */
public class FakeCaptureSequenceListener : CaptureSequenceListener {
    public var isComplete: Boolean = false
        private set

    override fun onCaptureSequenceComplete(captureSequence: CaptureSequence<*>) {
        isComplete = true
    }
}
