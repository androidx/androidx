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

import androidx.camera.core.impl.CameraCaptureMetaData;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.TagBundle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A fake implementation of {@link CameraCaptureResult} where the values are settable.
 */
@SuppressWarnings("HiddenSuperclass")
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

    public void setAfMode(CameraCaptureMetaData.@NonNull AfMode mode) {
        mAfMode = mode;
    }

    public void setAfState(CameraCaptureMetaData.@NonNull AfState state) {
        mAfState = state;
    }

    public void setAeState(CameraCaptureMetaData.@NonNull AeState state) {
        mAeState = state;
    }

    public void setAwbState(CameraCaptureMetaData.@NonNull AwbState state) {
        mAwbState = state;
    }

    public void setFlashState(CameraCaptureMetaData.@NonNull FlashState state) {
        mFlashState = state;
    }

    public void setAeMode(CameraCaptureMetaData.@NonNull AeMode mode) {
        mAeMode = mode;
    }

    public void setAwbMode(CameraCaptureMetaData.@NonNull AwbMode mode) {
        mAwbMode = mode;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public void setTagBundle(@NonNull TagBundle tag) {
        mTag = tag;
    }

    @Override
    public CameraCaptureMetaData.@NonNull AfMode getAfMode() {
        return mAfMode;
    }

    @Override
    public CameraCaptureMetaData.@NonNull AfState getAfState() {
        return mAfState;
    }

    @Override
    public CameraCaptureMetaData.@NonNull AeState getAeState() {
        return mAeState;
    }

    @Override
    public CameraCaptureMetaData.@NonNull AwbState getAwbState() {
        return mAwbState;
    }

    @Override
    public CameraCaptureMetaData.@NonNull FlashState getFlashState() {
        return mFlashState;
    }

    @Override
    public CameraCaptureMetaData.@NonNull AeMode getAeMode() {
        return mAeMode;
    }

    @Override
    public CameraCaptureMetaData.@NonNull AwbMode getAwbMode() {
        return mAwbMode;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public @NonNull TagBundle getTagBundle() {
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
        public @NonNull FakeCameraCaptureResult build() {
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
        public @NonNull Builder setAfMode(CameraCaptureMetaData.@Nullable AfMode mode) {
            mAfMode = mode;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.AfState} */
        public @NonNull Builder setAfState(CameraCaptureMetaData.@Nullable AfState state) {
            mAfState = state;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.AeState} */
        public @NonNull Builder setAeState(CameraCaptureMetaData.@Nullable AeState state) {
            mAeState = state;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.AwbState} */
        public @NonNull Builder setAwbState(CameraCaptureMetaData.@Nullable AwbState state) {
            mAwbState = state;
            return this;
        }

        /** Sets the {@link CameraCaptureMetaData.FlashState} */
        public @NonNull Builder setFlashState(CameraCaptureMetaData.@Nullable FlashState state) {
            mFlashState = state;
            return this;
        }

        /** Sets the timestamp. */
        public @NonNull Builder setTimestamp(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        /** Sets the {@link TagBundle}. */
        public @NonNull Builder setTagBundle(@NonNull TagBundle tag) {
            mTag = tag;
            return this;
        }
    }
}
