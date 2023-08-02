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

package androidx.camera.video

import android.content.Context
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.Logger
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.CameraUtil
import androidx.camera.video.internal.AudioSource
import androidx.camera.video.internal.FakeBufferProvider
import androidx.camera.video.internal.config.AudioSourceSettingsCamcorderProfileResolver
import androidx.camera.video.internal.encoder.FakeInputBuffer
import androidx.concurrent.futures.await
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class AudioChecker {

    companion object {
        private const val TAG = "AudioChecker"

        fun canAudioSourceBeStarted(
            context: Context,
            cameraSelector: CameraSelector,
            qualitySelector: QualitySelector
        ): Boolean {
            return try {
                checkAudioSourceCanBeStarted(context, cameraSelector, qualitySelector)
                Logger.i(TAG, "Audio source can be started.")
                true
            } catch (t: Throwable) {
                Logger.i(TAG, "Audio source failed to start.", t)
                false
            }
        }

        private fun checkAudioSourceCanBeStarted(
            context: Context,
            cameraSelector: CameraSelector,
            qualitySelector: QualitySelector
        ) = runBlocking {
            // Get audio source settings from CamcorderProfile
            val cameraInfo =
                CameraUtil.createCameraUseCaseAdapter(context, cameraSelector).cameraInfo
            val videoCapabilities = VideoCapabilities.from(cameraInfo)
            val quality = qualitySelector.getPrioritizedQualities(cameraInfo).first()
            // Get a config using the default audio spec.
            val audioSourceSettings =
                AudioSourceSettingsCamcorderProfileResolver(
                    AudioSpec.builder().build(),
                    videoCapabilities.getProfile(quality)!!
                ).get()
            val audioSource = AudioSource(audioSourceSettings, CameraXExecutors.ioExecutor(), null)
            try {
                val completable = CompletableDeferred<Any?>()
                audioSource.setAudioSourceCallback(CameraXExecutors.directExecutor(),
                    object : AudioSource.AudioSourceCallback {
                        override fun onSilenced(silenced: Boolean) {
                            // Ignore
                        }

                        override fun onError(t: Throwable) {
                            completable.completeExceptionally(t)
                        }
                    })

                val fakeBufferProvider = FakeBufferProvider {
                    completable.complete(null)
                    FakeInputBuffer()
                }
                audioSource.setBufferProvider(fakeBufferProvider)
                fakeBufferProvider.setActive(true)
                audioSource.start()
                completable.await()
            } finally {
                audioSource.release().await()
            }
        }
    }
}