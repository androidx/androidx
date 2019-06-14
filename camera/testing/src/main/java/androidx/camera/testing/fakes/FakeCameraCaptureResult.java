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
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.camera.core.CameraCaptureMetaData;
import androidx.camera.core.CameraCaptureResult;

/**
 * A fake implementation of {@link CameraCaptureResult} where the values are settable.
 *
 * @hide
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public final class FakeCameraCaptureResult implements CameraCaptureResult {
    private CameraCaptureMetaData.AfMode mAfMode = CameraCaptureMetaData.AfMode.UNKNOWN;
    private CameraCaptureMetaData.AfState mAfState = CameraCaptureMetaData.AfState.UNKNOWN;
    private CameraCaptureMetaData.AeState mAeState = CameraCaptureMetaData.AeState.UNKNOWN;
    private CameraCaptureMetaData.AwbState mAwbState = CameraCaptureMetaData.AwbState.UNKNOWN;
    private CameraCaptureMetaData.FlashState mFlashState = CameraCaptureMetaData.FlashState.UNKNOWN;
    private long mTimestamp = -1L;
    private Object mTag = null;

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

    public void setTag(Object tag) {
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
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public Object getTag() {
        return mTag;
    }

    /**
     * Builder for fake implementation of {@link CameraCaptureResult} where the values are settable.
     *
     * @hide
     */
    @RestrictTo(Scope.LIBRARY_GROUP)
    public static class Builder {
        private CameraCaptureMetaData.AfMode mAfMode = CameraCaptureMetaData.AfMode.UNKNOWN;
        private CameraCaptureMetaData.AfState mAfState = CameraCaptureMetaData.AfState.UNKNOWN;
        private CameraCaptureMetaData.AeState mAeState = CameraCaptureMetaData.AeState.UNKNOWN;
        private CameraCaptureMetaData.AwbState mAwbState = CameraCaptureMetaData.AwbState.UNKNOWN;
        private CameraCaptureMetaData.FlashState mFlashState =
                CameraCaptureMetaData.FlashState.UNKNOWN;
        private long mTimestamp = -1L;
        private Object mTag = null;

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

        /** Set the {@link androidx.camera.core.CameraCaptureMetaData.AfMode} **/
        public void setAfMode(@Nullable CameraCaptureMetaData.AfMode mode) {
            mAfMode = mode;
        }

        /** Set the {@link androidx.camera.core.CameraCaptureMetaData.AfState} **/
        public void setAfState(@Nullable CameraCaptureMetaData.AfState state) {
            mAfState = state;
        }

        /** Set the {@link androidx.camera.core.CameraCaptureMetaData.AeState} **/
        public void setAeState(@Nullable CameraCaptureMetaData.AeState state) {
            mAeState = state;
        }

        /** Set the {@link androidx.camera.core.CameraCaptureMetaData.AwbState} **/
        public void setAwbState(@Nullable CameraCaptureMetaData.AwbState state) {
            mAwbState = state;
        }

        /** Set the {@link androidx.camera.core.CameraCaptureMetaData.FlashState} **/
        public void setFlashState(@Nullable CameraCaptureMetaData.FlashState state) {
            mFlashState = state;
        }

        /** Set the timestamp. */
        public void setTimestamp(long timestamp) {
            mTimestamp = timestamp;
        }

        /** Set the tag. */
        public void setTag(@Nullable Object tag) {
            mTag = tag;
        }
    }
}
