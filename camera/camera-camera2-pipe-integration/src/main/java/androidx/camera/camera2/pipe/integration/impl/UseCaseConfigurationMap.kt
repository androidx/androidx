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
import androidx.camera.camera2.pipe.impl.Log.debug
import androidx.camera.camera2.pipe.impl.Log.info
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.UseCaseConfigFactory

/**
 * This class builds [Config] objects for a given [UseCaseConfigFactory.CaptureType].
 *
 * This includes things like default template and session parameters, as well as maximum resolution
 * and aspect ratios for the display.
 */
class UseCaseConfigurationMap(context: Context) : UseCaseConfigFactory {
    init {
        if (context === context.applicationContext) {
            info {
                "The provided context ($context) is application scoped and will be used to infer " +
                    "the default display for computing the default preview size, orientation, " +
                    "and default aspect ratio for UseCase outputs."
            }
        }
        debug { "Created UseCaseConfigurationMap" }
    }

    /**
     * Returns the configuration for the given capture type, or `null` if the configuration
     * cannot be produced.
     */
    override fun getConfig(captureType: UseCaseConfigFactory.CaptureType): Config? {
        TODO("Not Implemented")
    }
}