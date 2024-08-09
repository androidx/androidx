/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CaptureFailure
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import kotlin.reflect.KClass

/**
 * This class implements the [RequestFailure] interface by passing the package-private
 * [CaptureFailure] object.
 */
public class AndroidCaptureFailure(
    override val requestMetadata: RequestMetadata,
    private val captureFailure: CaptureFailure
) : RequestFailure {
    override val frameNumber: FrameNumber = FrameNumber(captureFailure.frameNumber)
    override val reason: Int = captureFailure.reason
    override val wasImageCaptured: Boolean = captureFailure.wasImageCaptured()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            CaptureFailure::class -> captureFailure as T?
            else -> null
        }
}
