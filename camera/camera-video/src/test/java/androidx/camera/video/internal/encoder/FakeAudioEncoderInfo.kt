/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.video.internal.encoder

import android.util.Range

/** A fake [AudioEncoderInfo]. */
class FakeAudioEncoderInfo(
    override val bitrateRange: Range<Int> = Range(1, Int.MAX_VALUE),
    override val mime: String = "audio/fake",
    private val name: String = "fake.audio.encoder",
) : AudioEncoderInfo {

    override fun getName(): String = name
}
