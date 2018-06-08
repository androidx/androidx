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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class RecyclerViewSmoothScrollerTest {

    @Test
    public void stop_whileRunning_isRunningIsFalseInOnStop() {
        RecyclerView recyclerView = new RecyclerView(InstrumentationRegistry.getContext());
        RecyclerView.LayoutManager layoutManager = mock(RecyclerView.LayoutManager.class);
        recyclerView.setLayoutManager(layoutManager);
        MockSmoothScroller mockSmoothScroller = spy(new MockSmoothScroller());
        mockSmoothScroller.setTargetPosition(0);
        mockSmoothScroller.start(recyclerView, layoutManager);

        mockSmoothScroller.stop();

        verify(mockSmoothScroller).onStop();
        assertThat(mockSmoothScroller.mWasRunningInOnStop, is(false));
    }

    @Test
    public void stop_whileNotRunning_doesNotCallOnStop() {
        RecyclerView.SmoothScroller mockSmoothScroller = spy(new MockSmoothScroller());
        mockSmoothScroller.stop();
        verify(mockSmoothScroller, never()).onStop();
    }

    public static class MockSmoothScroller extends RecyclerView.SmoothScroller {

        boolean mWasRunningInOnStop;

        @Override
        protected void onStart() {

        }

        @Override
        protected void onStop() {
            mWasRunningInOnStop = isRunning();
        }

        @Override
        protected void onSeekTargetStep(int dx, int dy, RecyclerView.State state,
                Action action) {

        }

        @Override
        protected void onTargetFound(View targetView, RecyclerView.State state,
                Action action) {

        }
    }

}
