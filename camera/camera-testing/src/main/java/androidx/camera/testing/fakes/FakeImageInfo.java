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

import androidx.annotation.Nullable;
import androidx.camera.core.ImageInfo;

/**
 * A fake implementation of {@link ImageInfo} where the values are settable.
 */
public final class FakeImageInfo implements ImageInfo {
    private Object mTag;
    private long mTimestamp;
    private int mRotationDegrees;

    public void setTag(Object tag) {
        mTag = tag;
    }
    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }
    public void setRotationDegrees(int rotationDegrees) {
        mRotationDegrees = rotationDegrees;
    }

    @Override
    @Nullable
    public Object getTag() {
        return mTag;
    }
    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    public int getRotationDegrees() {
        return mRotationDegrees;
    }
}
