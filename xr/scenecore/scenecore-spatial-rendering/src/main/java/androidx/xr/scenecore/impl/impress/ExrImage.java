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
import androidx.xr.scenecore.runtime.ExrImageResource;

import org.jspecify.annotations.NonNull;

/** Wrapper class for the native EXR image. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class ExrImage extends BindingsResource implements ExrImageResource {
    private final ImpressApi mImpressApi;

    private ExrImage(Builder builder) {
        super(builder.mImpressapi.getBindingsResourceManager(), builder.mNativeExrImage);
        mImpressApi = builder.mImpressapi;
    }

    @Override
    protected void releaseBindingsResource(long nativeHandle) {
        mImpressApi.releaseImageBasedLightingAsset(nativeHandle);
    }

    /** Use Builder to construct a ExrImage object instance. */
    public static class Builder {
        private ImpressApi mImpressapi;
        private long mNativeExrImage = -1;

        /** Sets the Impress API. */
        @NonNull
        public Builder setImpressApi(@NonNull ImpressApi impressApi) {
            mImpressapi = impressApi;
            return this;
        }

        /** Sets the native EXR image. */
        @NonNull
        public Builder setNativeExrImage(long nativeExrImage) {
            mNativeExrImage = nativeExrImage;
            return this;
        }

        /** Builds the ExrImage. */
        @NonNull
        public ExrImage build() {
            if (mImpressapi == null || mNativeExrImage == -1) {
                throw new IllegalStateException("ExrImage not built properly.");
            }
            return new ExrImage(this);
        }
    }
}
