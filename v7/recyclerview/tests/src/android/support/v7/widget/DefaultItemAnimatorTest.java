/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.widget;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class DefaultItemAnimatorTest extends ActivityInstrumentationTestCase2<TestActivity> {

    DefaultItemAnimator mAnimator;
    Adapter mAdapter;
    ViewGroup mDummyParent;
    CountDownLatch mExpectedItems;

    Set<RecyclerView.ViewHolder> mRemoveFinished = new HashSet<RecyclerView.ViewHolder>();
    Set<RecyclerView.ViewHolder> mAddFinished = new HashSet<RecyclerView.ViewHolder>();
    Set<RecyclerView.ViewHolder> mMoveFinished = new HashSet<RecyclerView.ViewHolder>();
    Set<RecyclerView.ViewHolder> mChangeFinished = new HashSet<RecyclerView.ViewHolder>();

    public DefaultItemAnimatorTest() {
        super("android.support.v7.recyclerview", TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mAnimator = new DefaultItemAnimator();
        mAdapter = new Adapter(20);
        mDummyParent = getActivity().mContainer;
        mAnimator.setListener(new RecyclerView.ItemAnimator.ItemAnimatorListener() {
            @Override
            public void onRemoveFinished(RecyclerView.ViewHolder item) {
                assertTrue(mRemoveFinished.add(item));
                onFinished();
            }

            @Override
            public void onAddFinished(RecyclerView.ViewHolder item) {
                assertTrue(mAddFinished.add(item));
                onFinished();
            }

            @Override
            public void onMoveFinished(RecyclerView.ViewHolder item) {
                assertTrue(mMoveFinished.add(item));
                onFinished();
            }

            @Override
            public void onChangeFinished(RecyclerView.ViewHolder item) {
                assertTrue(mChangeFinished.add(item));
                onFinished();
            }

            private void onFinished() {
                if (mExpectedItems != null) {
                    mExpectedItems.countDown();
                }
            }
        });
    }

    void expectItems(int count) {
        mExpectedItems = new CountDownLatch(count);
    }

    void runAndWait(int seconds) throws Throwable {
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAnimator.runPendingAnimations();
            }
        });
        waitForItems(seconds);
    }

    void waitForItems(int seconds) throws InterruptedException {
        mExpectedItems.await(seconds, TimeUnit.SECONDS);
        assertEquals("all expected finish events should happen", 0, mExpectedItems.getCount());
    }

    public void testAnimateAdd() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(1);
        assertTrue(animateAdd(vh));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    public void testAnimateRemove() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(1);
        assertTrue(animateRemove(vh));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    public void testAnimateMove() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        expectItems(1);
        assertTrue(animateMove(vh, 0, 0, 100, 100));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    public void testAnimateChange() throws Throwable {
        ViewHolder vh = createViewHolder(1);
        ViewHolder vh2 = createViewHolder(2);
        expectItems(2);
        assertTrue(animateChange(vh, vh2, 0, 0, 100, 100));
        assertTrue(mAnimator.isRunning());
        runAndWait(1);
    }

    boolean animateAdd(final RecyclerView.ViewHolder vh) throws Throwable {
        final boolean[] result = new boolean[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateAdd(vh);
            }
        });
        return result[0];
    }

    boolean animateRemove(final RecyclerView.ViewHolder vh) throws Throwable {
        final boolean[] result = new boolean[1];
        runTestOnUiThread(new Runnable() {
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
        runTestOnUiThread(new Runnable() {
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
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = mAnimator.animateChange(oldHolder, newHolder, fromX, fromY, toX, toY);
            }
        });
        return result[0];
    }

    private ViewHolder createViewHolder(final int pos) throws Throwable {
        final ViewHolder vh = mAdapter.createViewHolder(mDummyParent, 1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.bindViewHolder(vh, pos);
                mDummyParent.addView(vh.itemView);
            }
        });

        return vh;
    }


    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        List<String> mItems;

        private Adapter(int count) {
            mItems = new ArrayList<String>();
            for (int i = 0; i < count; i++) {
                mItems.add("item-" + i);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(new TextView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
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
}
