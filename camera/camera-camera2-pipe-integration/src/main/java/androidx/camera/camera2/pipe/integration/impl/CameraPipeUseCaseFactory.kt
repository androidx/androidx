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
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.UseCaseConfigFactory

/**
 * CameraPipe implementation of UseCaseFactory.
 * @constructor Creates a CameraPipeUseCaseFactory from the provided [Context].
 */
class CameraPipeUseCaseFactory(context: Context) : UseCaseConfigFactory {
    companion object {
        private const val TAG = "CameraPipeUseCaseFty"
        private val DEBUG = Log.isLoggable(TAG, Log.DEBUG)
    }

    private val cameraPipe: CameraPipe = CameraPipe(CameraPipe.Config(context))

    init {
        if (DEBUG) {
            Log.d(
                TAG,
                "Initialized CameraPipeUseCaseFactory [Context: $context, CameraPipe: $cameraPipe]"
            )
        }
    }

    /**
     * Returns the configuration for the given capture type, or `null` if the
     * configuration cannot be produced.
     */
    override fun getConfig(captureType: UseCaseConfigFactory.CaptureType): Config? {
        return null
    }
}