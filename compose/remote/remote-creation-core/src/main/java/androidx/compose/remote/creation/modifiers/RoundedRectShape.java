/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation.modifiers;
import androidx.annotation.RestrictTo;

/** Round rectangle shape */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RoundedRectShape extends Shape {
    float mTopStart;
    float mTopEnd;
    float mBottomStart;
    float mBottomEnd;

    public RoundedRectShape(float topStart, float topEnd, float bottomStart, float bottomEnd) {
        mTopStart = topStart;
        mTopEnd = topEnd;
        mBottomStart = bottomStart;
        mBottomEnd = bottomEnd;
    }

    public float getTopStart() {
        return mTopStart;
    }

    public float getTopEnd() {
        return mTopEnd;
    }

    public float getBottomStart() {
        return mBottomStart;
    }

    public float getBottomEnd() {
        return mBottomEnd;
    }
}
