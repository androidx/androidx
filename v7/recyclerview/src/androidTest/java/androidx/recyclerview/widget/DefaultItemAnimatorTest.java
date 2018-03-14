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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class DefaultItemAnimatorTest extends BaseRecyclerViewInstrumentationTest {

    private static final String TAG = "DefaultItemAnimatorTest";
    Throwable mainThreadException;

    DefaultItemAnimator mAnimator;
    Adapter mAdapter;
    ViewGroup mDummyParent;
    List<RecyclerView.ViewHolder> mExpectedItems = new ArrayList<RecyclerView.ViewHolder>();

    Set<RecyclerView.ViewHolder> mRemoveFinished = new HashSet<RecyclerView.ViewHolder>();
    Set<RecyclerView.ViewHolder> mAddFinished = new HashSet<RecyclerView.ViewHolder>();
    Set<RecyclerView.ViewHolder> mMoveFinished = new HashSet<RecyclerView.ViewHolder>();
    Set<RecyclerView.ViewHolder> mChangeFinished = new HashSet<RecyclerView.ViewHolder>();

    Semaphore mExpectedItemCount = new Semaphore(0);

    @Before
    public void setUp() throws Exception {
        mAnimator = new DefaultItemAnimator() {
            @Override
            public void onRemoveFinished(RecyclerView.ViewHolder item) {
                try {
                    assertTrue(mRemoveFinished.add(item));
                    onFinished(item);
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }

            @Override
            public void onAddFinished(RecyclerView.ViewHolder item) {
                try {
                    assertTrue(mAddFinished.add(item));
                    onFinished(item);
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }

            @Override
            public void onMoveFinished(RecyclerView.ViewHolder item) {
                try {
                    assertTrue(mMoveFinished.add(item));
                    onFinished(item);
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }

            @Override
            public void onChangeFinished(RecyclerView.ViewHolder item, boolean oldItem) {
                try {
                    assertTrue(mChangeFinished.add(item));
                    onFinished(item);
                } catch (Throwable t) {
                    postExceptionToInstrumentation(t);
                }
            }

            private void onFinished(RecyclerView.ViewHolder item) {
                assertNotNull(mExpectedItems.remove(item));
                mExpectedItemCount.release(1);
            }
        };
        mAdapter = new Adapter(20);
        mDummyParent = getActivity().getContainer();
    }

    @Override
    void checkForMainThreadException() throws Throwable {
        if (mainThreadException != null) {
            throw mainThreadException;
        }
    }

    @Test
    public void reUseWithPayload() {
        RecyclerView.ViewHolder vh = new ViewHolder(new TextView(getActivity()));
        assertFalse(mAnimator.canReuseUpdatedViewHolder(vh, new ArrayList<>()));
        assertTrue(mAnimator.canReuseUpdatedViewHolder(vh, Arrays.asList((Object) "a")));
    }

    void expectItems(RecyclerView.ViewHolder... viewHolders) {
        mExpectedItems.addAll(Arrays.asList(viewHolders));
    }

    void runAndWait(int itemCount, int seconds) throws Throwable {
        runAndWait(itemCount, seconds, null);
    }

    void runAndWait(int itemCount, int seconds, final ThrowingRunnable postRun) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAnimator.runPendingAnimations();
                if (postRun != null) {
                    try {
                        postRun.run();
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        waitForItems(itemCount, seconds);
        checkForMainThreadException();
    }

    void waitForItems(int itemCount, int seconds) throws InterruptedException {
        assertTrue("all vh animations should end",
                mExpectedItemCount.tryAcquire(itemCount, seconds, TimeUnit.SECONDS));
        assertEquals("all expected finish events should happen", 0, mExpectedItems.size());
        // wait one more second for unwanted
        assertFalse("should not receive any more permits",
                mExpectedItemCount.tryAcquire(1, 2, TimeUnit.SECONDS));
    }

    @Test
    public void animateAdd() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateAdd(vh));
        assertTrue(mAnimator.isRunning());
        runAndWait(1, 1);
    }

    @Test
    public void animateRemove() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateRemove(vh));
        assertTrue(mAnimator.isRunning());
        runAndWait(1, 1);
    }

    @Test
    public void animateMove() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateMove(vh, 0, 0, 100, 100));
        assertTrue(mAnimator.isRunning());
        runAndWait(1, 1);
    }

    @Test
    public void animateChange() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        ViewHolder vh2 = createViewHolder(2);
        expectItems(vh, vh2);
        assertTrue(animateChange(vh, vh2, 0, 0, 100, 100));
        assertTrue(mAnimator.isRunning());
        runAndWait(2, 1);
    }

    public void cancelBefore(int count, final RecyclerView.ViewHolder... toCancel)
            throws Throwable {
        cancelTest(true, count, toCancel);
    }

    public void cancelAfter(int count, final RecyclerView.ViewHolder... toCancel)
            throws Throwable {
        cancelTest(false, count, toCancel);
    }

    public void cancelTest(boolean before, int count, final RecyclerView.ViewHolder... toCancel) throws Throwable {
        if (before) {
            endAnimations(toCancel);
            runAndWait(count, 1);
        } else {
            runAndWait(count, 1, new ThrowingRunnable() {
                @Override
                public void run() throws Throwable {
                    endAnimations(toCancel);
                }
            });
        }
    }

    @Test
    public void cancelAddBefore() throws Throwable {
        final ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateAdd(vh));
        cancelBefore(1, vh);
    }

    @Test
    public void cancelAddAfter() throws Throwable {
        final ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateAdd(vh));
        cancelAfter(1, vh);
    }

    @Test
    public void cancelMoveBefore() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateMove(vh, 10, 10, 100, 100));
        cancelBefore(1, vh);
    }

    @Test
    public void cancelMoveAfter() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateMove(vh, 10, 10, 100, 100));
        cancelAfter(1, vh);
    }

    @Test
    public void cancelRemove() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(vh);
        assertTrue(animateRemove(vh));
        endAnimations(vh);
        runAndWait(1, 1);
    }

    @Test
    public void cancelChangeOldBefore() throws Throwable {
        cancelChangeOldTest(true);
    }
    @Test
    public void cancelChangeOldAfter() throws Throwable {
        cancelChangeOldTest(false);
    }

    public void cancelChangeOldTest(boolean before) throws Throwable {
        ViewHolder vh = createViewHolder(1);
        ViewHolder vh2 = createViewHolder(1);
        expectItems(vh, vh2);
        assertTrue(animateChange(vh, vh2, 20, 20, 100, 100));
        cancelTest(before, 2, vh);
    }

    @Test
    public void cancelChangeNewBefore() throws Throwable {
        cancelChangeNewTest(true);
    }

    @Test
    public void cancelChangeNewAfter() throws Throwable {
        cancelChangeNewTest(false);
    }

    public void cancelChangeNewTest(boolean before) throws Throwable {
        ViewHolder vh = createViewHolder(1);
        ViewHolder vh2 = createViewHolder(1);
        expectItems(vh, vh2);
        assertTrue(animateChange(vh, vh2, 20, 20, 100, 100));
        cancelTest(before, 2, vh2);
    }

    @Test
    public void cancelChangeBothBefore() throws Throwable {
        cancelChangeBothTest(true);
    }

    @Test
    public void cancelChangeBothAfter() throws Throwable {
        cancelChangeBothTest(false);
    }

    public void cancelChangeBothTest(boolean before) throws Throwable {
        ViewHolder vh = createViewHolder(1);
        ViewHolder vh2 = createViewHolder(1);
        expectItems(vh, vh2);
        assertTrue(animateChange(vh, vh2, 20, 20, 100, 100));
        cancelTest(before, 2, vh, vh2);
    }

    void endAnimations(final RecyclerView.ViewHolder... vhs) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (RecyclerView.ViewHolder vh : vhs) {
                    mAnimator.endAnimation(vh);
                }
            }
        });
    }

    boolean animateAdd(final RecyclerView.ViewHolder vh) throws Throwable {
        final boolean[] result = new boolean[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateAdd(vh);
            }
        });
        return result[0];
    }

    boolean animateRemove(final RecyclerView.ViewHolder vh) throws Throwable {
        final boolean[] result = new boolean[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateRemove(vh);
            }
        });
        return result[0];
    }

    boolean animateMove(final RecyclerView.ViewHolder vh, final int fromX, final int fromY,
            final int toX, final int toY) throws Throwable {
        final boolean[] result = new boolean[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateMove(vh, fromX, fromY, toX, toY);
            }
        });
        return result[0];
    }

    boolean animateChange(final RecyclerView.ViewHolder oldHolder,
            final RecyclerView.ViewHolder newHolder,
            final int fromX, final int fromY, final int toX, final int toY) throws Throwable {
        final boolean[] result = new boolean[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
            }
        });
        return result[0];
    }

    private ViewHolder createViewHolder(final int pos) throws Throwable {
        final ViewHolder vh = mAdapter.createViewHolder(mDummyParent, 1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.bindViewHolder(vh, pos);
                mDummyParent.addView(vh.itemView);
            }
        });

        return vh;
    }

    @Override
    void postExceptionToInstrumentation(Throwable t) {
        if (mainThreadException == null) {
            mainThreadException = t;
        } else {
            Log.e(TAG, "skipping secondary main thread exception", t);
        }
    }


    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        List<String> mItems;

        private Adapter(int count) {
            mItems = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                mItems.add("item-" + i);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(mItems.get(position));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }

    private class ViewHolder extends RecyclerView.ViewHolder {

        String mBindedText;

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bind(String text) {
            mBindedText = text;
            ((TextView) itemView).setText(text);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Throwable;
    }
}
