/*
 * Copyright 2020 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.RecyclerListener;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerListenerTest extends BaseRecyclerViewInstrumentationTest {

    private static final boolean DEBUG = false;

    private RecyclerView mRecyclerView;
    private TestAdapter mTestAdapter;
    private TestLayoutManager mLayoutManager;
    private TestRecyclerListener mOgListener;
    private TestRecyclerListener mListener1;
    private TestRecyclerListener mListener2;

    public RecyclerListenerTest() {
        super(DEBUG);
    }

    @Before
    public void setUp() throws Throwable {
        mTestAdapter = new TestAdapter(10);

        mOgListener = new TestRecyclerListener();
        mListener1 = new TestRecyclerListener();
        mListener2 = new TestRecyclerListener();

        mLayoutManager = new InternalTestLayoutManager();

        mRecyclerView = new RecyclerView(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mTestAdapter);

        // Waits until just before we're ready to do the
        // adapter swap that will trigger calls to RecyclerListener.
        mLayoutManager.expectLayouts(1);
        setRecyclerView(mRecyclerView);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        mLayoutManager.expectLayouts(1);
    }

    @Test
    public void listenersNotified() throws Throwable {

        mRecyclerView.setRecyclerListener(mOgListener);
        mRecyclerView.addRecyclerListener(mListener1);
        mRecyclerView.addRecyclerListener(mListener2);

        swapAdapter(new TestAdapter(10), true);

        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        // Once the adapter is swapped out all previous views should be recycled.
        mOgListener.assertCallCount(mTestAdapter.getItemCount());
        mListener1.assertCallCount(mTestAdapter.getItemCount());
        mListener2.assertCallCount(mTestAdapter.getItemCount());
    }

    @Test
    public void removedListenersNotNotified()
            throws Throwable {

        mRecyclerView.setRecyclerListener(mOgListener);
        mRecyclerView.addRecyclerListener(mListener1);
        mRecyclerView.addRecyclerListener(mListener2);

        // ...but nope, later we don't want to listen, so we remove select listeners.
        mRecyclerView.setRecyclerListener(null);
        mRecyclerView.removeRecyclerListener(mListener1);

        swapAdapter(new TestAdapter(10), true);

        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();

        // Once the adapter is swapped out all previous views should be recycled...
        mListener2.assertCallCount(mTestAdapter.getItemCount());

        // ...but the removed/null'd out listeners should obviously not be notified.
        mOgListener.assertCallCount(0);
        mListener1.assertCallCount(0);
    }

    private static class TestRecyclerListener implements RecyclerListener {
        final AtomicInteger mCallCount = new AtomicInteger();

        @Override
        public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
            mCallCount.incrementAndGet();
        }

        void assertCallCount(int expected) {
            assertEquals(String.format("RecyclerListener should have been called %d times, but "
                            + "was called %d times.", expected, mCallCount.get()),
                    expected, mCallCount.get());
        }
    }

    private final class InternalTestLayoutManager extends TestLayoutManager {
        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                layoutRange(recycler, 0, state.getItemCount());
                layoutLatch.countDown();
            } catch (Throwable t) {
                postExceptionToInstrumentation(t);
            } finally {
                layoutLatch.countDown();
            }
        }
    }
}
