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

package androidx.camera.camera2.pipe.testing

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import kotlinx.atomicfu.atomic

/**
 * Utility class for creating fake Surface objects for tests.
 *
 * Close this object to release all surfaces during tests.
 */
public class FakeSurfaces : AutoCloseable {
    private val fakeSurfaces = mutableListOf<Surface>()

    public fun createFakeSurface(size: Size = Size(640, 480)): Surface {
        val surface = create(size)
        synchronized(fakeSurfaces) { fakeSurfaces.add(surface) }
        return surface
    }

    override fun close() {
        synchronized(fakeSurfaces) {
            for (surface in fakeSurfaces) {
                surface.release()
            }
            fakeSurfaces.clear()
        }
    }

    public companion object {
        private val fakeSurfaceTextureNames = atomic(0)

        public fun create(size: Size = Size(640, 480)): Surface {
            return Surface(
                SurfaceTexture(fakeSurfaceTextureNames.getAndIncrement()).also {
                    it.setDefaultBufferSize(size.width, size.height)
                }
            )
        }
    }
}
