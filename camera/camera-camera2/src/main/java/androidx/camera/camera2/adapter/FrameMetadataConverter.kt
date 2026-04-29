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

package androidx.camera.camera2.adapter

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.core.impl.CameraCaptureResult
import java.lang.Class

public object FrameMetadataConverter {
    public fun FrameMetadata.toCameraCaptureResult(): CameraCaptureResult {
        val frameInfo =
            object : FrameInfo {
                private val frameMetadata = this@toCameraCaptureResult
                override val metadata: FrameMetadata = frameMetadata

                override fun get(camera: CameraId): FrameMetadata? = frameMetadata

                override val camera: CameraId = frameMetadata.camera
                override val frameNumber: FrameNumber = frameMetadata.frameNumber
                override val requestMetadata: RequestMetadata = emptyRequestMetadata

                override fun <T : Any> unwrapAs(type: Class<T>): T? = null
            }

        return CaptureResultAdapter(
            emptyRequestMetadata,
            /** RequestMetadata not to be used here */
            frameNumber,
            frameInfo,
        )
    }

    private val emptyRequestMetadata =
        object : RequestMetadata {
            override fun <T> get(key: CaptureRequest.Key<T>): T? = null

            override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T = default

            override val template: RequestTemplate = RequestTemplate(0)
            override val streams: Map<StreamId, Surface> = mapOf()
            override val repeating: Boolean = true
            override val request: Request = Request(listOf())
            override val requestNumber: RequestNumber = RequestNumber(0)

            override fun <T> get(key: Metadata.Key<T>): T? = null

            override fun <T> getOrDefault(key: Metadata.Key<T>, default: T): T = default

            override fun <T : Any> unwrapAs(type: Class<T>): T? = null
        }
}
