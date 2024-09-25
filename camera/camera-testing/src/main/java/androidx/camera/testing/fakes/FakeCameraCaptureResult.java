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

package androidx.camera.testing.fakes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraCaptureMetaData;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.TagBundle;

/**
 * A fake implementation of {@link CameraCaptureResult} where the values are settable.
 */
public final class FakeCameraCaptureResult implements CameraCaptureResult {
    private CameraCaptureMetaData.AfMode mAfMode = CameraCaptureMetaData.AfMode.UNKNOWN;
    private CameraCaptureMetaData.AfState mAfState = CameraCaptureMetaData.AfState.UNKNOWN;
    private CameraCaptureMetaData.AeState mAeState = CameraCaptureMetaData.AeState.UNKNOWN;
    private CameraCaptureMetaData.AwbState mAwbState = CameraCaptureMetaData.AwbState.UNKNOWN;
    private CameraCaptureMetaData.FlashState mFlashState = CameraCaptureMetaData.FlashState.UNKNOWN;
    private CameraCaptureMetaData.AeMode mAeMode = CameraCaptureMetaData.AeMode.UNKNOWN;
    private CameraCaptureMetaData.AwbMode mAwbMode = CameraCaptureMetaData.AwbMode.UNKNOWN;
    private long mTimestamp = -1L;
    private TagBundle mTag = TagBundle.emptyBundle();

    public void setAfMode(@NonNull CameraCaptureMetaData.AfMode mode) {
        mAfMode = mode;
    }

    public void setAfState(@NonNull CameraCaptureMetaData.AfState state) {
        mAfState = state;
    }

    public void setAeState(@NonNull CameraCaptureMetaData.AeState state) {
        mAeState = state;
    }

    public void setAwbState(@NonNull CameraCaptureMetaData.AwbState state) {
        mAwbState = state;
    }

    public void setFlashState(@NonNull CameraCaptureMetaData.FlashState state) {
        mFlashState = state;
    }

    public void setAeMode(@NonNull CameraCaptureMetaData.AeMode mode) {
        mAeMode = mode;
    }

    public void setAwbMode(@NonNull CameraCaptureMetaData.AwbMode mode) {
        mAwbMode = mode;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public void setTagBundle(@NonNull TagBundle tag) {
        mTag = tag;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AfMode getAfMode() {
        return mAfMode;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AfState getAfState() {
        return mAfState;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AeState getAeState() {
        return mAeState;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AwbState getAwbState() {
        return mAwbState;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.FlashState getFlashState() {
        return mFlashState;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AeMode getAeMode() {
        return mAeMode;
    }

    @NonNull
    @Override
    public CameraCaptureMetaData.AwbMode getAwbMode() {
        return mAwbMode;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @NonNull
    @Override
    public TagBundle getTagBundle() {
        return mTag;
    }

    /**
     * Builder for fake implementation of {@link CameraCaptureResult} where the values are settable.
     *
     */
    @SuppressWarnings("unused")
    public static final class Builder {
        private CameraCaptureMetaData.AfMode mAfMode = CameraCaptureMetaData.AfMode.UNKNOWN;
        private CameraCaptureMetaData.AfState mAfState = CameraCaptureMetaData.AfState.UNKNOWN;
        private CameraCaptureMetaData.AeState mAeState = CameraCaptureMetaData.AeState.UNKNOWN;
        private CameraCaptureMetaData.AwbState mAwbState = CameraCaptureMetaData.AwbState.UNKNOWN;
        private CameraCaptureMetaData.FlashState mFlashState =
                CameraCaptureMetaData.FlashState.UNKNOWN;
        private long mTimestamp = -1L;
        private TagBundle mTag = TagBundle.emptyBundle();

        /** Constructs and returns a new instance of a {@link FakeCameraCaptureResult}. */
        @NonNull public FakeCameraCaptureResult build() {
            FakeCameraCaptureResult fakeCameraCaptureResult = new FakeCameraCaptureResult();
            fakeCameraCaptureResult.setAfMode(mAfMode);
            fakeCameraCaptureResult.setAfState(mAfState);
            fakeCameraCaptureResult.setAeState(mAeState);
            fakeCameraCaptureResult.setAwbState(mAwbState);
            fakeCameraCaptureResult.setFlashState(mFlashState);
            fakeCameraCaptureResult.setTimestamp(mTimestamp);
            fakeCameraCaptureResult.setTagBundle(mTag);

            return fakeCameraCaptureResult;
        }

        /** Sets the {@link CameraCaptureMetaData.AfMode} */
        @NonNull
        public Builder setAfMode(@Nullable CameraCaptureMetaData.AfMode mode) {
            mAfMode = mode;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.AfState} */
        @NonNull
        public Builder setAfState(@Nullable CameraCaptureMetaData.AfState state) {
            mAfState = state;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.AeState} */
        @NonNull
        public Builder setAeState(@Nullable CameraCaptureMetaData.AeState state) {
            mAeState = state;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.AwbState} */
        @NonNull
        public Builder setAwbState(@Nullable CameraCaptureMetaData.AwbState state) {
            mAwbState = state;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.FlashState} */
        @NonNull
        public Builder setFlashState(@Nullable CameraCaptureMetaData.FlashState state) {
            mFlashState = state;
            return this;
        }

        /** Sets the timestamp. */
        @NonNull
        public Builder setTimestamp(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        /** Sets the {@link TagBundle}. */
        @NonNull
        public Builder setTagBundle(@NonNull TagBundle tag) {
            mTag = tag;
            return this;
        }
    }
}
