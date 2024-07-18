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

package androidx.media2.widget;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

class AspectRatioMatcher extends TypeSafeMatcher<View> {
    private static final float EPSILON = .01f;

    private final int mExpectedWidth;
    private final int mExpectedHeight;

    private AspectRatioMatcher(int expectedWidth, int expectedHeight) {
        if (expectedWidth <= 0) {
            throw new IllegalArgumentException("expectedWidth should be greater than zero");
        }
        if (expectedHeight <= 0) {
            throw new IllegalArgumentException("expectedHeight should be greater than zero");
        }
        mExpectedWidth = expectedWidth;
        mExpectedHeight = expectedHeight;
    }

    @Override
    protected boolean matchesSafely(View item) {
        int width = item.getWidth();
        int height = item.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        float expected = (float) mExpectedWidth / mExpectedHeight;
        float actual = (float) width / height;
        return Math.abs(expected - actual) < EPSILON;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with aspect ratio: ");
        description.appendValue(mExpectedWidth + "x" + mExpectedHeight);
    }

    static AspectRatioMatcher withAspectRatio(int width, int height) {
        return new AspectRatioMatcher(width, height);
    }
}
