/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.core.Log.debug
import androidx.camera.core.UseCase
import androidx.camera.core.impl.DeferrableSurface
import androidx.camera.core.impl.SessionConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Aggregate the SessionConfig from a List of [UseCase]s, and provide a validated SessionConfig for
 * operation.
 */
class SessionConfigAdapter(
    private val useCases: Collection<UseCase>,
) {
    private val validatingBuilder: SessionConfig.ValidatingBuilder by lazy {
        val validatingBuilder = SessionConfig.ValidatingBuilder()
        useCases.forEach {
            validatingBuilder.add(it.sessionConfig)
        }
        validatingBuilder
    }

    private val sessionConfig: SessionConfig by lazy {
        check(validatingBuilder.isValid)

        validatingBuilder.build()
    }

    val deferrableSurfaces: List<DeferrableSurface> by lazy {
        check(validatingBuilder.isValid)

        sessionConfig.surfaces
    }

    fun getValidSessionConfigOrNull(): SessionConfig? {
        return if (isSessionConfigValid()) sessionConfig else null
    }

    fun isSessionConfigValid(): Boolean {
        return validatingBuilder.isValid
    }

    fun reportSurfaceInvalid(deferrableSurface: DeferrableSurface) {
        debug { "Unavailable $deferrableSurface, notify SessionConfig invalid" }

        // Only report error to one SessionConfig, CameraInternal#onUseCaseReset()
        // will handle the other failed Surfaces if there are any.
        val sessionConfig = useCases.firstOrNull { useCase ->
            useCase.sessionConfig.surfaces.contains(deferrableSurface)
        }?.sessionConfig

        CoroutineScope(Dispatchers.Main.immediate).launch {
            // The error listener is used to notify the UseCase to recreate the pipeline,
            // and the create pipeline task would be executed on the main thread.
            sessionConfig?.errorListeners?.forEach {
                it.onError(
                    sessionConfig,
                    SessionConfig.SessionError.SESSION_ERROR_SURFACE_NEEDS_RESET
                )
            }
        }
    }
}