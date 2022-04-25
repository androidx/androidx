/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import android.media.CameraProfile;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CamcorderProfileProvider;
import androidx.camera.core.impl.CamcorderProfileProxy;

/**
 * A fake implementation of the {@link CamcorderProfileProvider} and used for test.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FakeCamcorderProfileProvider implements CamcorderProfileProvider {

    private final SparseArray<CamcorderProfileProxy> mQualityToProfileMap;

    FakeCamcorderProfileProvider(@NonNull SparseArray<CamcorderProfileProxy> qualityToProfileMap) {
        mQualityToProfileMap = qualityToProfileMap;
    }

    /** {@inheritDoc} */
    @Override
    @Nullable
    public CamcorderProfileProxy get(int quality) {
        return mQualityToProfileMap.get(quality);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasProfile(int quality) {
        return mQualityToProfileMap.get(quality) != null;
    }

    /**
     * The builder to create a FakeCamcorderProfileProvider instance.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static class Builder {
        private final SparseArray<CamcorderProfileProxy> mQualityToProfileMap = new SparseArray<>();

        /**
         * Sets the camera id and corresponding profiles.
         *
         * <p>In normal case, the {@link CameraProfile#QUALITY_HIGH} and
         * {@link CameraProfile#QUALITY_LOW} should be added in order to follow the contract of
         * CamcorderProfile.
         */
        @NonNull
        public Builder addProfile(@NonNull CamcorderProfileProxy ...camcorderProfiles) {
            for (CamcorderProfileProxy camcorderProfile : camcorderProfiles) {
                mQualityToProfileMap.put(camcorderProfile.getQuality(), camcorderProfile);
            }
            return this;
        }

        /** Builds the FakeCamcorderProfileProvider instance. */
        @NonNull
        public FakeCamcorderProfileProvider build() {
            return new FakeCamcorderProfileProvider(mQualityToProfileMap.clone());
        }
    }
}
