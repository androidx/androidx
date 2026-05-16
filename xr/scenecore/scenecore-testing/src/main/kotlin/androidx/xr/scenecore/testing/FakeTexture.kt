/*
 * Copyright 2026 The Android Open Source Project
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

@file:Suppress("DEPRECATION")

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.runtime.TextureResource
import androidx.xr.scenecore.testing.internal.FakeTexture as InternalFakeTexture
import java.util.Collections
import java.util.WeakHashMap

@Deprecated("Use SceneCoreTestRule instead.")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
/** Test-only implementation of [androidx.xr.scenecore.runtime.TextureResource] */
public class FakeTexture internal constructor(internal var fakeInternal: InternalFakeTexture) :
    TextureResource {

    init {
        textureMap[fakeInternal] = this
    }

    public constructor() : this(InternalFakeTexture())

    /** The name of the texture file to load or the URL of the remote texture. */
    public var assetName: String
        get() = fakeInternal.assetName
        set(value) {
            fakeInternal.assetName = value
        }

    /** Whether this texture has been destroyed. */
    public var isDestroyed: Boolean
        get() = fakeInternal.isDestroyed
        set(value) {
            fakeInternal.isDestroyed = value
        }

    internal companion object {
        /**
         * A map that maintains the 1:1 relationship between an [InternalFakeTexture] and its
         * corresponding [FakeTexture] proxy.
         */
        internal val textureMap: MutableMap<InternalFakeTexture, FakeTexture> =
            Collections.synchronizedMap(WeakHashMap())

        /**
         * Unwraps a [FakeTexture] proxy to its underlying [InternalFakeTexture].
         *
         * If the provided [texture] is not a [FakeTexture], it returns the texture as is.
         */
        internal fun unwrap(texture: TextureResource): TextureResource {
            return (texture as? FakeTexture)?.fakeInternal ?: texture
        }

        /**
         * Wraps an [InternalFakeTexture] back to its corresponding [FakeTexture] proxy.
         *
         * This ensures that when the runtime returns a texture, we provide the user with the same
         * proxy instance they originally created or interacted with.
         */
        internal fun wrap(texture: TextureResource): TextureResource {
            return textureMap[texture] ?: texture
        }
    }
}
