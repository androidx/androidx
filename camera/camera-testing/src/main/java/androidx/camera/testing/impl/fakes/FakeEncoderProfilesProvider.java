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

package androidx.camera.testing.impl.fakes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.EncoderProfilesProvider;
import androidx.camera.core.impl.EncoderProfilesProxy;

import java.util.HashMap;
import java.util.Map;

/**
 * A fake implementation of the {@link EncoderProfilesProvider} and used for test.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FakeEncoderProfilesProvider implements EncoderProfilesProvider {

    private final Map<Integer, EncoderProfilesProxy> mQualityToProfileMap;

    FakeEncoderProfilesProvider(@NonNull Map<Integer, EncoderProfilesProxy> qualityToProfileMap) {
        mQualityToProfileMap = qualityToProfileMap;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasProfile(int quality) {
        return mQualityToProfileMap.get(quality) != null;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public EncoderProfilesProxy getAll(int quality) {
        return mQualityToProfileMap.get(quality);
    }

    /**
     * The builder to create a FakeEncoderProfilesProvider instance.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    public static class Builder {

        private final Map<Integer, EncoderProfilesProxy> mQualityToProfileMap = new HashMap<>();

        /**
         * Adds a quality and its corresponding profiles.
         */
        @NonNull
        public Builder add(int quality, @NonNull EncoderProfilesProxy profiles) {
            mQualityToProfileMap.put(quality, profiles);
            return this;
        }

        /**
         * Adds qualities and their corresponding profiles.
         */
        @NonNull
        public Builder addAll(@NonNull Map<Integer, EncoderProfilesProxy> qualityToProfileMap) {
            mQualityToProfileMap.putAll(qualityToProfileMap);
            return this;
        }

        /** Builds the FakeEncoderProfilesProvider instance. */
        @NonNull
        public FakeEncoderProfilesProvider build() {
            return new FakeEncoderProfilesProvider(mQualityToProfileMap);
        }
    }
}
