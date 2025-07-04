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

package androidx.xr.scenecore.impl;

import androidx.xr.runtime.internal.TextureResource;

/**
 * Implementation of a SceneCore TextureResource.
 *
 * <p>This is used to create to load a texture that can later be used when overriding a material.
 */
final class TextureResourceImpl implements TextureResource {
    private final long mToken;

    TextureResourceImpl(long token) {
        mToken = token;
    }

    public long getTextureToken() {
        return mToken;
    }
}
