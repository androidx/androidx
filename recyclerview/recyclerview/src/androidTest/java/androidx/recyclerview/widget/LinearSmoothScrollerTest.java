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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.util.DisplayMetrics;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


@RunWith(AndroidJUnit4.class)
@SmallTest
public class LinearSmoothScrollerTest {

    @Test
    public void constructor_doesNotInvokeCalculateSpeedPerPixel() {
        // As this method can be overridden by users of LinearSmoothScroller, it should
        // not be invoked and cached in the constructor. Otherwise it might be called when
        // the object is in a semi-initialized state.
        final AtomicBoolean methodCalled = new AtomicBoolean(false);

        LinearSmoothScroller scroller = new LinearSmoothScroller(
                ApplicationProvider.getApplicationContext()) {

                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    methodCalled.set(true);
                    return super.calculateSpeedPerPixel(displayMetrics);
                }
            };

        assertThat(methodCalled.get(), is(false));
    }

    @Test
    public void calculateTimeForScrolling_cachesResultOfCalculateSpeedPerPixel() {
        final AtomicInteger methodCalls = new AtomicInteger(0);
        LinearSmoothScroller scroller = new LinearSmoothScroller(
                ApplicationProvider.getApplicationContext()) {

                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    methodCalls.getAndIncrement();
                    return super.calculateSpeedPerPixel(displayMetrics);
                }

                // This method is protected by default. Make it public for test purposes.
                @Override
                public int calculateTimeForScrolling(int dx) {
                    return super.calculateTimeForScrolling(dx);
                }
            };

        scroller.calculateTimeForScrolling(/*dx=*/ 10);
        scroller.calculateTimeForScrolling(/*dx=*/ 100);
        scroller.calculateTimeForScrolling(/*dx=*/ 1000);

        assertThat(methodCalls.get(), is(1));
    }
}
