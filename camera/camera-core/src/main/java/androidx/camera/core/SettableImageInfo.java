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

package androidx.camera.core;

import androidx.annotation.Nullable;

class SettableImageInfo implements ImageInfo {

    private Object mTag;
    private long mTimestamp;

    SettableImageInfo(Object tag, long timestamp) {
        mTag = tag;
        mTimestamp = timestamp;
    }

    @Override
    @Nullable
    public Object getTag() {
        return mTag;
    }

    public void setTag(@Nullable Object tag) {
        mTag = tag;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp) {
        mTimestamp = timestamp;
    }

}
