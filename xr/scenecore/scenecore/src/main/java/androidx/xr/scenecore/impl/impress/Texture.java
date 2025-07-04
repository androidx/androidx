/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.scenecore.impl.impress;

import androidx.annotation.RestrictTo;
import androidx.xr.runtime.internal.TextureSampler;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Texture class for the native Impress texture wrapper struct. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class Texture {
    private final ImpressApi impressApi;
    private final long nativeTexture;
    private final TextureSampler sampler;

    private Texture(Builder builder) {
        this.impressApi = builder.impressApi;
        this.nativeTexture = builder.nativeTexture;
        this.sampler = builder.sampler;
    }

    /** Returns the native texture handle of the Impress texture. */
    public long getNativeHandle() {
        return nativeTexture;
    }

    /** Returns the sampler used to load the texture. */
    @Nullable
    public TextureSampler getTextureSampler() {
        return sampler;
    }

    /** Destroys the native texture. */
    public void destroyNativeObject() {
        impressApi.destroyNativeObject(nativeTexture);
    }

    /** Use Builder to construct a Texture object instance. */
    public static class Builder {
        private ImpressApi impressApi;
        private long nativeTexture = -1;
        private TextureSampler sampler;

        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            this.impressApi = impressApi;
            return this;
        }

        @NonNull
        public Builder setNativeTexture(long nativeTexture) {
            this.nativeTexture = nativeTexture;
            return this;
        }

        @NonNull
        public Builder setTextureSampler(@NonNull TextureSampler sampler) {
            this.sampler = sampler;
            return this;
        }

        @NonNull
        public Texture build() {
            if (impressApi == null || nativeTexture == -1) {
                throw new IllegalStateException("Texture not built properly.");
            }
            return new Texture(this);
        }
    }
}
