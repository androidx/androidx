/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.impl.CameraCaptureMetaData;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.TagBundle;

/**
 * A fake implementation of {@link CameraCaptureResult} where the values are settable.
 *
 * @hide
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeCameraCaptureResult implements CameraCaptureResult {
    private CameraCaptureMetaData.AfMode mAfMode = CameraCaptureMetaData.AfMode.UNKNOWN;
    private CameraCaptureMetaData.AfState mAfState = CameraCaptureMetaData.AfState.UNKNOWN;
    private CameraCaptureMetaData.AeState mAeState = CameraCaptureMetaData.AeState.UNKNOWN;
    private CameraCaptureMetaData.AwbState mAwbState = CameraCaptureMetaData.AwbState.UNKNOWN;
    private CameraCaptureMetaData.FlashState mFlashState = CameraCaptureMetaData.FlashState.UNKNOWN;
    private long mTimestamp = -1L;
    private TagBundle mTag = TagBundle.emptyBundle();

    public void setAfMode(CameraCaptureMetaData.AfMode mode) {
        mAfMode = mode;
    }

    public void setAfState(CameraCaptureMetaData.AfState state) {
        mAfState = state;
    }

    public void setAeState(CameraCaptureMetaData.AeState state) {
        mAeState = state;
    }

    public void setAwbState(CameraCaptureMetaData.AwbState state) {
        mAwbState = state;
    }

    public void setFlashState(CameraCaptureMetaData.FlashState state) {
        mFlashState = state;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

    public void setTag(TagBundle tag) {
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
     * @hide
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static class Builder {
        private CameraCaptureMetaData.AfMode mAfMode = CameraCaptureMetaData.AfMode.UNKNOWN;
        private CameraCaptureMetaData.AfState mAfState = CameraCaptureMetaData.AfState.UNKNOWN;
        private CameraCaptureMetaData.AeState mAeState = CameraCaptureMetaData.AeState.UNKNOWN;
        private CameraCaptureMetaData.AwbState mAwbState = CameraCaptureMetaData.AwbState.UNKNOWN;
        private CameraCaptureMetaData.FlashState mFlashState =
                CameraCaptureMetaData.FlashState.UNKNOWN;
        private long mTimestamp = -1L;
        private TagBundle mTag = TagBundle.emptyBundle();

        /** Construct and return a new instance of a {@link FakeCameraCaptureResult}. */
        @NonNull public FakeCameraCaptureResult build() {
            FakeCameraCaptureResult fakeCameraCaptureResult = new FakeCameraCaptureResult();
            fakeCameraCaptureResult.setAfMode(mAfMode);
            fakeCameraCaptureResult.setAfState(mAfState);
            fakeCameraCaptureResult.setAeState(mAeState);
            fakeCameraCaptureResult.setAwbState(mAwbState);
            fakeCameraCaptureResult.setFlashState(mFlashState);
            fakeCameraCaptureResult.setTimestamp(mTimestamp);
            fakeCameraCaptureResult.setTag(mTag);

            return fakeCameraCaptureResult;
        }

        /** Set the {@link CameraCaptureMetaData.AfMode} **/
        @NonNull
        public Builder setAfMode(@Nullable CameraCaptureMetaData.AfMode mode) {
            mAfMode = mode;
            return this;
        }

        /** Set the {@link CameraCaptureMetaData.AfState} **/
        @NonNull
        public Builder setAfState(@Nullable CameraCaptureMetaData.AfState state) {
            mAfState = state;
            return this;
        }

        /** Set the {@link CameraCaptureMetaData.AeState} **/
        @NonNull
        public Builder setAeState(@Nullable CameraCaptureMetaData.AeState state) {
            mAeState = state;
            return this;
        }

        /** Set the {@link CameraCaptureMetaData.AwbState} **/
        @NonNull
        public Builder setAwbState(@Nullable CameraCaptureMetaData.AwbState state) {
            mAwbState = state;
            return this;
        }

        /** Set the {@link CameraCaptureMetaData.FlashState} **/
        @NonNull
        public Builder setFlashState(@Nullable CameraCaptureMetaData.FlashState state) {
            mFlashState = state;
            return this;
        }

        /** Set the timestamp. */
        @NonNull
        public Builder setTimestamp(long timestamp) {
            mTimestamp = timestamp;
            return this;
        }

        /** Set the tag. */
        @NonNull
        public Builder setTag(@NonNull TagBundle tag) {
            mTag = tag;
            return this;
        }
    }
}
