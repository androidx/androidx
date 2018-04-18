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


import static androidx.recyclerview.widget.RecyclerView.HORIZONTAL;
import static androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE;
import static androidx.recyclerview.widget.RecyclerView.VERTICAL;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.test.R;
import androidx.recyclerview.test.RecyclerViewTestActivity;

import org.hamcrest.BaseMatcher;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * This class tests RecyclerView focus search failure handling by using a real LayoutManager.
 */
@LargeTest
@RunWith(Parameterized.class)
public class FocusSearchNavigationTest {
    @Rule
    public ActivityTestRule<RecyclerViewTestActivity> mActivityRule =
            new ActivityTestRule<>(RecyclerViewTestActivity.class);

    private final int mOrientation;
    private final int mLayoutDir;

    public FocusSearchNavigationTest(int orientation, int layoutDir) {
        mOrientation = orientation;
        mLayoutDir = layoutDir;
    }

    @Parameterized.Parameters(name = "orientation:{0},layoutDir:{1}")
    public static List<Object[]> params() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Arrays.asList(
                    new Object[]{VERTICAL, ViewCompat.LAYOUT_DIRECTION_LTR},
                    new Object[]{HORIZONTAL, ViewCompat.LAYOUT_DIRECTION_LTR},
                    new Object[]{HORIZONTAL, ViewCompat.LAYOUT_DIRECTION_RTL}
            );
        } else {
            // Do not test RTL before API 17
            return Arrays.asList(
                    new Object[]{VERTICAL, ViewCompat.LAYOUT_DIRECTION_LTR},
                    new Object[]{HORIZONTAL, ViewCompat.LAYOUT_DIRECTION_LTR}
            );
        }
    }

    private Activity mActivity;
    private RecyclerView mRecyclerView;
    private View mBefore;
    private View mAfter;

    private void setup(final int itemCount) throws Throwable {
        mActivity = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.setContentView(R.layout.focus_search_activity);
                ViewCompat.setLayoutDirection(mActivity.getWindow().getDecorView(), mLayoutDir);
                LinearLayout linearLayout = (LinearLayout) mActivity.findViewById(R.id.root);
                linearLayout.setOrientation(mOrientation);
                mRecyclerView = (RecyclerView) mActivity.findViewById(R.id.recycler_view);
                ViewCompat.setLayoutDirection(mRecyclerView, mLayoutDir);
                LinearLayoutManager layout = new LinearLayoutManager(mActivity.getBaseContext());
                layout.setOrientation(mOrientation);
                mRecyclerView.setLayoutManager(layout);
                mRecyclerView.setAdapter(new FocusSearchAdapter(itemCount, mOrientation));
                if (mOrientation == VERTICAL) {
                    mRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 250));
                } else {
                    mRecyclerView.setLayoutParams(new LinearLayout.LayoutParams(
                            250, ViewGroup.LayoutParams.MATCH_PARENT));
                }

                mBefore = mActivity.findViewById(R.id.before);
                mAfter = mActivity.findViewById(R.id.after);
            }
        });
        waitForIdleSync();
        assertThat("test sanity", mRecyclerView.getLayoutManager().getLayoutDirection(),
                is(mLayoutDir));
        assertThat("test sanity", ViewCompat.getLayoutDirection(mRecyclerView), is(mLayoutDir));
    }

    @Test
    public void focusSearchForward() throws Throwable {
        setup(20);
        requestFocus(mBefore);
        assertThat(mBefore, hasFocus());
        View focused = mBefore;
        for (int i = 0; i < 20; i++) {
            focusSearchAndGive(focused, View.FOCUS_FORWARD);
            RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertThat("vh at " + i, viewHolder, hasFocus());
            focused = viewHolder.itemView;
        }
        focusSearchAndGive(focused, View.FOCUS_FORWARD);
        assertThat(mAfter, hasFocus());
        focusSearchAndGive(mAfter, View.FOCUS_FORWARD);
        assertThat(mBefore, hasFocus());
        focusSearchAndGive(mBefore, View.FOCUS_FORWARD);
        focused = mActivity.getCurrentFocus();
        //noinspection ConstantConditions
        assertThat(focused.getParent(), CoreMatchers.<ViewParent>sameInstance(mRecyclerView));
    }

    @Test
    public void focusSearchBackwards() throws Throwable {
        setup(20);
        requestFocus(mAfter);
        assertThat(mAfter, hasFocus());
        View focused = mAfter;
        RecyclerView.ViewHolder lastViewHolder = null;
        int i = 20;
        while(lastViewHolder == null) {
            lastViewHolder = mRecyclerView.findViewHolderForAdapterPosition(--i);
        }
        assertThat(lastViewHolder, notNullValue());

        while(i >= 0) {
            focusSearchAndGive(focused, View.FOCUS_BACKWARD);
            RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(i);
            assertThat("vh at " + i, viewHolder, hasFocus());
            focused = viewHolder.itemView;
            i--;
        }
        focusSearchAndGive(focused, View.FOCUS_BACKWARD);
        assertThat(mBefore, hasFocus());
        focusSearchAndGive(mBefore, View.FOCUS_BACKWARD);
        assertThat(mAfter, hasFocus());
    }

    private View focusSearchAndGive(final View view, final int focusDir) throws Throwable {
        View next = focusSearch(view, focusDir);
        if (next != null && next != view) {
            requestFocus(next);
            return next;
        }
        return null;
    }

    private View focusSearch(final View view, final int focusDir) throws Throwable {
        final View[] result = new View[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = view.focusSearch(focusDir);
            }
        });
        waitForIdleSync();
        return result[0];
    }

    private void waitForIdleSync() throws Throwable {
        waitForIdleScroll(mRecyclerView);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    private void requestFocus(final View view) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.requestFocus();
            }
        });
        waitForIdleSync();
    }

    public void waitForIdleScroll(final RecyclerView recyclerView) throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecyclerView.OnScrollListener listener = new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                        if (newState == SCROLL_STATE_IDLE) {
                            latch.countDown();
                            recyclerView.removeOnScrollListener(this);
                        }
                    }
                };
                if (recyclerView.getScrollState() == SCROLL_STATE_IDLE) {
                    latch.countDown();
                } else {
                    recyclerView.addOnScrollListener(listener);
                }
            }
        });
        assertTrue("should go idle in 10 seconds", latch.await(10, TimeUnit.SECONDS));
    }

    static class FocusSearchAdapter extends RecyclerView.Adapter {
        private int mItemCount;
        private int mOrientation;
        public FocusSearchAdapter(int itemCount, int orientation) {
            mItemCount = itemCount;
            mOrientation = orientation;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
        int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view,
                    parent, false);
            if (mOrientation == VERTICAL) {
                view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        50));
            } else {
                view.setLayoutParams(new ViewGroup.LayoutParams(50,
                        ViewGroup.LayoutParams.MATCH_PARENT));
            }
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            holder.itemView.setTag("pos " + position);
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }

    static HasFocusMatcher hasFocus() {
        return new HasFocusMatcher();
    }

    static class HasFocusMatcher extends BaseMatcher<Object> {
        @Override
        public boolean matches(Object item) {
            if (item instanceof RecyclerView.ViewHolder) {
                item = ((RecyclerView.ViewHolder) item).itemView;
            }
            return item instanceof View && ((View) item).hasFocus();
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("view has focus");
        }

        private String objectToLog(Object item) {
            if (item instanceof RecyclerView.ViewHolder) {
                RecyclerView.ViewHolder vh = (RecyclerView.ViewHolder) item;
                return vh.toString();
            }
            if (item instanceof View) {
                final Object tag = ((View) item).getTag();
                return tag == null ? item.toString() : tag.toString();
            }
            final String classLog = item == null ? "null" : item.getClass().getSimpleName();
            return classLog;
        }

        @Override
        public void describeMismatch(Object item, Description description) {
            String noun = objectToLog(item);
            description.appendText(noun + " does not have focus");
            Context context = null;
            if (item instanceof RecyclerView.ViewHolder) {
                context = ((RecyclerView.ViewHolder)item).itemView.getContext();
            } else  if (item instanceof View) {
                context = ((View) item).getContext();
            }
            if (context instanceof Activity) {
                View currentFocus = ((Activity) context).getWindow().getCurrentFocus();
                description.appendText(". Current focus is in " + objectToLog(currentFocus));
            }
        }
    }
}
