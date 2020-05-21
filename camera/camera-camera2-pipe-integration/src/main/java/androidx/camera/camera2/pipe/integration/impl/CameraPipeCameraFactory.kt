/*
 * Copyright 2020 The Android Open Source Project
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
package androidx.camera.camera2.pipe.integration.impl

import android.content.Context
import android.util.Log
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.core.impl.CameraFactory
import androidx.camera.core.impl.CameraInternal
import androidx.camera.core.impl.CameraThreadConfig

/**
 * CameraPipe implementation of CameraX CameraFactory.
 * @constructor Creates a CameraPipeCameraFactory from the provided [Context] and
 * [CameraThreadConfig].
 */
class CameraPipeCameraFactory(context: Context, threadConfig: CameraThreadConfig) : CameraFactory {
    companion object {
        private const val TAG = "CameraPipeCameraFactory"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }

    private val cameraPipe: CameraPipe = CameraPipe(CameraPipe.Config(context))

    init {
        if (DEBUG) {
            Log.d(
                TAG, "Initialized CameraFactory [Context: $context, " +
                        "ThreadConfig: $threadConfig, CameraPipe: $cameraPipe]"
            )
        }
    }

    override fun getCamera(cameraId: String): CameraInternal {
        TODO("Not implemented.")
    }

    override fun getAvailableCameraIds(): Set<String> {
        return emptySet()
    }
}