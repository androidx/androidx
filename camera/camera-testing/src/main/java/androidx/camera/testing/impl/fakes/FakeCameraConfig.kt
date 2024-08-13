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

package androidx.camera.testing.impl.fakes

import androidx.annotation.RestrictTo
import androidx.camera.core.impl.CameraConfig
import androidx.camera.core.impl.Config
import androidx.camera.core.impl.Identifier
import androidx.camera.core.impl.OptionsBundle
import androidx.camera.core.impl.SessionProcessor
import androidx.camera.core.impl.UseCaseConfigFactory

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeCameraConfig(
    private val sessionProcessor: SessionProcessor? = null,
    private val postviewSupported: Boolean = false,
    private val captureProcessProgressSupported: Boolean = false
) : CameraConfig {
    private val useCaseConfigFactory = UseCaseConfigFactory { _, _ -> null }
    private val identifier = Identifier.create(Any())

    override fun getUseCaseConfigFactory(): UseCaseConfigFactory {
        return useCaseConfigFactory
    }

    override fun isPostviewSupported(): Boolean {
        return postviewSupported
    }

    override fun isCaptureProcessProgressSupported(): Boolean {
        return captureProcessProgressSupported
    }

    override fun getCompatibilityId(): Identifier {
        return identifier
    }

    override fun getConfig(): Config {
        return OptionsBundle.emptyBundle()
    }

    override fun getSessionProcessor(valueIfMissing: SessionProcessor?): SessionProcessor? {
        return sessionProcessor ?: valueIfMissing
    }

    override fun getSessionProcessor(): SessionProcessor {
        return sessionProcessor!!
    }
}
