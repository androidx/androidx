/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.graphics.drawable;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DrawableContainerTest {

    /**
     * Regression test for b/171913944 where DrawableContainer fails to copy when there are no
     * children in the constant state.
     */
    @Test
    public void testDrawableContainerCopyNoChildren() {
        MyDrawableContainerState orig = new MyDrawableContainerState();

        // This shouldn't crash.
        new MyDrawableContainerState(orig);
    }

    class MyDrawableContainerState extends DrawableContainer.DrawableContainerState {
        MyDrawableContainerState() {
            super(null, null, null);
        }
        MyDrawableContainerState(DrawableContainer.DrawableContainerState orig) {
            super(orig, null, null);
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new DrawableContainer();
        }
    }
}
