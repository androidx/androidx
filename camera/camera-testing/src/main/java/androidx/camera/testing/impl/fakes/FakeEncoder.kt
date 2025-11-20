/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes

import androidx.camera.video.internal.encoder.Encoder
import androidx.camera.video.internal.encoder.EncoderCallback
import androidx.camera.video.internal.encoder.EncoderInfo
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

public class FakeEncoder(
    private val encoderInput: Encoder.EncoderInput,
    private val encoderInfo: EncoderInfo = FakeEncoderInfo(),
    private val configuredBitrate: Int = 0,
    public val releasedFuture: ResolvableFuture<Void?> = ResolvableFuture.create(),
) : Encoder {

    public var isReleaseCalled: Boolean = false

    override fun getInput(): Encoder.EncoderInput = encoderInput

    override fun getEncoderInfo(): EncoderInfo = encoderInfo

    override fun getConfiguredBitrate(): Int = configuredBitrate

    override fun start() {}

    override fun stop() {}

    override fun stop(expectedStopTimeUs: Long) {}

    override fun pause() {}

    override fun release() {
        isReleaseCalled = true
    }

    override fun getReleasedFuture(): ListenableFuture<Void?> = releasedFuture

    override fun setEncoderCallback(encoderCallback: EncoderCallback, executor: Executor) {}

    override fun requestKeyFrame() {}
}
