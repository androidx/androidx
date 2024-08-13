/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import kotlin.reflect.KClass

/** Utility class for testing code that depends on [RequestFailure] with reasonable defaults. */
public class FakeRequestFailure(
    override val requestMetadata: RequestMetadata,
    override val frameNumber: FrameNumber,
    override val reason: Int = 0,
    override val wasImageCaptured: Boolean = false,
) : RequestFailure {
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        // Fake objects cannot be unwrapped.
        return null
    }
}
