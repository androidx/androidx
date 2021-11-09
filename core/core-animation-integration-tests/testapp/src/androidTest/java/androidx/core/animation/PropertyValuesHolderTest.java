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

package androidx.core.animation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PropertyValuesHolderTest {

    @Test
    @Ignore("This is a control test to check the behavior of the platform.")
    public void findSetter_platform() {
        Sample sample = new Sample();
        assertThat(sample.getNumber(), is(0));
        android.animation.ObjectAnimator animator = android.animation.ObjectAnimator
                .ofPropertyValuesHolder(sample,
                        android.animation.PropertyValuesHolder.ofInt("number", 0, 10))
                .setDuration(100);
        animator.setCurrentPlayTime(50);
        assertThat(sample.getNumber(), is(5));
    }

    @Test
    public void findSetter_library() {
        Sample sample = new Sample();
        assertThat(sample.getNumber(), is(0));
        ObjectAnimator animator = ObjectAnimator
                .ofPropertyValuesHolder(sample, PropertyValuesHolder.ofInt("number", 0, 10))
                .setDuration(100);
        animator.setCurrentPlayTime(50);
        assertThat(sample.getNumber(), is(5));
    }

    private static class Sample {

        private int mNumber;

        // This is a package-private method. The platform can find it with Class.getMethod, but
        // libraries have to use Class.getDeclaredMethod instead.
        @SuppressWarnings("unused")
        void setNumber(int number) {
            mNumber = number;
        }

        int getNumber() {
            return mNumber;
        }
    }
}
