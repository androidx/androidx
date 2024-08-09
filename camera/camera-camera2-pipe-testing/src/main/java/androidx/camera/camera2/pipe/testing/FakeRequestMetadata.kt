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

import android.hardware.camera2.CaptureRequest
import android.view.Surface
import androidx.camera.camera2.pipe.Metadata
import androidx.camera.camera2.pipe.Request
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.RequestTemplate
import androidx.camera.camera2.pipe.StreamId
import kotlin.reflect.KClass
import kotlinx.atomicfu.atomic

private val fakeRequestNumbers = atomic(0L)

internal fun nextFakeRequestNumber(): RequestNumber =
    RequestNumber(fakeRequestNumbers.incrementAndGet())

/** Utility class for interacting with objects require specific [CaptureRequest] metadata. */
public class FakeRequestMetadata(
    private val requestParameters: Map<CaptureRequest.Key<*>, Any?> = emptyMap(),
    metadata: Map<Metadata.Key<*>, Any?> = emptyMap(),
    override val template: RequestTemplate = RequestTemplate(0),
    override val streams: Map<StreamId, Surface> = mapOf(),
    override val repeating: Boolean = false,
    override val request: Request = Request(listOf()),
    override val requestNumber: RequestNumber = nextFakeRequestNumber()
) : FakeMetadata(request.extras.plus(metadata)), RequestMetadata {

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(key: CaptureRequest.Key<T>): T? = requestParameters[key] as T?

    override fun <T> getOrDefault(key: CaptureRequest.Key<T>, default: T): T = get(key) ?: default

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = null

    public companion object {
        /** Initialize FakeRequestMetadata based on a specific [Request] object. */
        public fun from(
            request: Request,
            streamToSurfaces: Map<StreamId, Surface>,
            repeating: Boolean = false
        ): FakeRequestMetadata {
            check(streamToSurfaces.keys.containsAll(request.streams))
            return FakeRequestMetadata(
                requestParameters = request.parameters,
                template = request.template ?: RequestTemplate(0),
                streams = request.streams.map { it to streamToSurfaces[it]!! }.toMap(),
                repeating = repeating,
                request = request
            )
        }
    }

    override fun toString(): String =
        "FakeRequestMetadata(requestNumber: ${requestNumber.value}, request: $request)"
}
