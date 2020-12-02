/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.leanback.widget;

public class PlaybackSeekProviderSample extends PlaybackSeekDataProvider {

    protected long[] mSeekPositions;

    public PlaybackSeekProviderSample(long duration, int numSeekPositions) {
        this(0, duration, numSeekPositions);
    }

    public PlaybackSeekProviderSample(long first, long last, int numSeekPositions) {
        mSeekPositions = new long[numSeekPositions];
        for (int i = 0; i < mSeekPositions.length; i++) {
            mSeekPositions[i] = first + i * (last - first) / (numSeekPositions - 1);
        }
    }

    @Override
    public long[] getSeekPositions() {
        return mSeekPositions;
    }
}
