/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.core.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.annotation.SuppressLint;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.test.R;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
public class ListViewCompatTest extends BaseInstrumentationTestCase<ListViewTestActivity> {
    private ListView mListView;

    public ListViewCompatTest() {
        super(ListViewTestActivity.class);
    }

    @Before
    public void setUp() throws Throwable {
        mListView = mActivityTestRule.getActivity().findViewById(R.id.content);
        runOnMainAndLayoutSync(mActivityTestRule, mListView, new Runnable() {
            @Override
            public void run() {
                mListView.setAdapter(new BaseAdapter() {
                    @Override
                    public int getCount() {
                        return 500;
                    }

                    @Override
                    public Object getItem(int position) {
                        return null;
                    }

                    @Override
                    public long getItemId(int position) {
                        return position;
                    }

                    @SuppressLint("SetTextI18n")
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        if (convertView == null) {
                            convertView = LayoutInflater.from(mListView.getContext()).inflate(
                                    R.layout.list_view_row, parent, false);
                        }
                        TextView result = (TextView) convertView;
                        result.setText("row #" + (position + 1));
                        return result;
                    }
                });
            }
        }, false);
    }

    private void runOnMainAndLayoutSync(@NonNull final ActivityTestRule activityTestRule,
            @NonNull final View view, @Nullable final Runnable runner, final boolean forceLayout)
            throws Throwable {
        final View rootView = view.getRootView();

        final CountDownLatch latch = new CountDownLatch(1);

        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final OnGlobalLayoutListener listener = new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        // countdown immediately since the layout we were waiting on has happened
                        latch.countDown();
                    }
                };

                rootView.getViewTreeObserver().addOnGlobalLayoutListener(listener);

                if (runner != null) {
                    runner.run();
                }

                if (forceLayout) {
                    rootView.requestLayout();
                }
            }
        });

        try {
            assertTrue("Expected layout pass within 5 seconds",
                    latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCanScroll() throws Throwable {
        final int itemCount = mListView.getAdapter().getCount();

        assertEquals(0, mListView.getFirstVisiblePosition());

        // Verify that when we're at the top of the list, we can't scroll up but we can scroll
        // down.
        assertFalse(ListViewCompat.canScrollList(mListView, -1));
        assertTrue(ListViewCompat.canScrollList(mListView, 1));

        // Scroll down to the very end of the list
        runOnMainAndLayoutSync(mActivityTestRule, mListView,
                new Runnable() {
                    @Override
                    public void run() {
                        mListView.setStackFromBottom(true);
                    }
                }, false);
        assertEquals(itemCount - 1, mListView.getLastVisiblePosition());

        // Verify that when we're at the bottom of the list, we can't scroll down but we can scroll
        // up.
        assertFalse(ListViewCompat.canScrollList(mListView, 1));
        assertTrue(ListViewCompat.canScrollList(mListView, -1));
    }
}
