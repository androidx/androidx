/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.emoji.text;

import androidx.annotation.RequiresApi;

@RequiresApi(19)
public class TestEmojiMetadata extends EmojiMetadata {
    private final int[] mCodePoints;
    private int mId;

    TestEmojiMetadata(int[] codePoints, int id) {
        super(null, 0);
        mCodePoints = codePoints;
        mId = id;
    }

    TestEmojiMetadata(int[] codePoints) {
        this(codePoints, 0);
    }

    @Override
    public int getId() {
        return mId;
    }

    @Override
    public int getCodepointAt(int index) {
        return mCodePoints[index];
    }

    @Override
    public int getCodepointsLength() {
        return mCodePoints.length;
    }
}
