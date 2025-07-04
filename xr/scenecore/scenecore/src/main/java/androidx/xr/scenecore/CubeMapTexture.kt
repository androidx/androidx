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

package androidx.xr.scenecore

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.internal.JxrPlatformAdapter
import androidx.xr.runtime.internal.TextureResource as RtTextureResource

/** [CubeMapTexture] represents a cube map texture that can be used with materials. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CubeMapTexture internal constructor(texture: RtTextureResource, session: Session) :
    Texture(texture, TextureSampler.create(), session) {

    public companion object {
        internal fun borrowReflectionTexture(
            platformAdapter: JxrPlatformAdapter,
            session: Session,
        ): CubeMapTexture {
            // TODO(b/396116100): Handle null return from borrow reflection texture.
            return CubeMapTexture(platformAdapter.borrowReflectionTexture()!!, session)
        }

        /**
         * Returns a [CubeMapTexture] which represents the lighting environment as seen by the
         * system. Currently, if the passthrough is enabled, the reflection texture that will be
         * returned will correspond to the environment lighting that is currently set but covered by
         * the passthrough. If an error occurs, this method will throw an exception.
         *
         * This method must be called from the main thread. *
         * https://developer.android.com/guide/components/processes-and-threads
         *
         * @param session The [Session] to use for loading the model.
         * @return a CubeMapTexture.
         */
        @MainThread
        @JvmStatic
        public fun borrowReflectionTexture(session: Session): CubeMapTexture {
            return borrowReflectionTexture(session.platformAdapter, session)
        }
    }
}
