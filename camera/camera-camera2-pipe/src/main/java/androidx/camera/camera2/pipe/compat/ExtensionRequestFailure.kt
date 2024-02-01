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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CaptureFailure
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestFailure
import androidx.camera.camera2.pipe.RequestMetadata
import kotlin.reflect.KClass

/**
 * This class implements the [RequestFailure] interface by extracting the fields of
 * the package-private [CaptureFailure] object.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal data class ExtensionRequestFailure(
    override val requestMetadata: RequestMetadata,
    override val wasImageCaptured: Boolean,
    override val frameNumber: FrameNumber,
    override val reason: Int,
) : RequestFailure {
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return null
    }
}
