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

package androidx.core.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.test.R;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.ViewCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * So far these tests only cover {@code NestedScrollView}'s implementation of
 * {@link NestedScrollingParent2} and the backwards compatibility of {@code NestedScrollView}'s
 * implementation of {@link androidx.core.view.NestedScrollingParent} for the methods that
 * {@link NestedScrollingParent2} overloads.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NestedScrollViewNestedScrollingParentTest extends
        BaseInstrumentationTestCase<TestContentViewActivity> {

    private NestedScrollView mNestedScrollView;
    private NestedScrollingSpyView mParent;
    private View mChild;

    public NestedScrollViewNestedScrollingParentTest() {
        super(TestContentViewActivity.class);
    }

    @Before
    public void instantiateMembers() {
        mNestedScrollView = new NestedScrollView(mActivityTestRule.getActivity());
        mParent = spy(new NestedScrollingSpyView(mActivityTestRule.getActivity()));
        mChild = new View(mActivityTestRule.getActivity());
    }

    @Test
    public void onStartNestedScroll_scrollAxisIncludesVertical_alwaysReturnsTrue() {
        int vertical = ViewCompat.SCROLL_AXIS_VERTICAL;
        int both = ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL;

        onStartNestedScrollV1(vertical, true);
        onStartNestedScrollV1(both, true);

        onStartNestedScrollV2(vertical, true);
        onStartNestedScrollV2(both, true);
    }

    @Test
    public void onStartNestedScroll_scrollAxisExcludesVertical_alwaysReturnsFalse() {
        int horizontal = ViewCompat.SCROLL_AXIS_HORIZONTAL;
        int neither = ViewCompat.SCROLL_AXIS_NONE;

        onStartNestedScrollV1(horizontal, false);
        onStartNestedScrollV1(neither, false);

        onStartNestedScrollV2(horizontal, false);
        onStartNestedScrollV2(neither, false);
    }

    @Test
    public void onNestedScrollAccepted_callsParentsOnStartNestedScrollWithCorrectParams()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent).onStartNestedScroll(mNestedScrollView, mNestedScrollView,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
            }
        });
    }

    @Test
    public void onNestedScrollAccepted_callsParentsOnNestedScrollAcceptedWithCorrectParams()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

                mNestedScrollView.onNestedScrollAccepted(
                        mChild,
                        mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL,
                        ViewCompat.TYPE_NON_TOUCH);

                    verify(mParent, times(1)).onNestedScrollAccepted(
                            mNestedScrollView,
                            mNestedScrollView,
                            ViewCompat.SCROLL_AXIS_VERTICAL,
                            ViewCompat.TYPE_NON_TOUCH);
                    verify(mParent, times(1)).onNestedScrollAccepted(
                            any(View.class),
                            any(View.class),
                            anyInt(),
                            anyInt());
            }
        });
    }

    @Test
    public void onNestedScrollAccepted_withBothOrientations_pOnNestedScrollAcceptedCalledWithVert()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

                mNestedScrollView.onNestedScrollAccepted(
                        mChild,
                        mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL | ViewCompat.SCROLL_AXIS_HORIZONTAL,
                        ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, times(1)).onNestedScrollAccepted(
                        any(View.class),
                        any(View.class),
                        eq(ViewCompat.SCROLL_AXIS_VERTICAL),
                        anyInt());
                verify(mParent, times(1)).onNestedScrollAccepted(
                        any(View.class),
                        any(View.class),
                        anyInt(),
                        anyInt());

            }
        });
    }

    @Test
    public void onNestedScrollAccepted_parentRejects_parentOnNestedScrollAcceptedNotCalled()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(false)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

                mNestedScrollView.onNestedScrollAccepted(
                        mChild,
                        mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL,
                        ViewCompat.TYPE_TOUCH);

                verify(mParent, never()).onNestedScrollAccepted(
                        any(View.class),
                        any(View.class),
                        anyInt(),
                        anyInt());
            }
        });
    }

    @Test
    public void onNestedScrollAccepted_v1_callsParentWithTypeTouch() throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());

                mNestedScrollView.onNestedScrollAccepted(
                        mChild,
                        mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL);

                verify(mParent, times(1)).onNestedScrollAccepted(
                        any(View.class),
                        any(View.class),
                        anyInt(),
                        eq(ViewCompat.TYPE_TOUCH));
                verify(mParent, times(1)).onNestedScrollAccepted(
                        any(View.class),
                        any(View.class),
                        anyInt(),
                        anyInt());
            }
        });
    }

    @Test
    public void onStopNestedScroll_parentOnStopNestedScrollCalledWithCorrectParams()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                            ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);

                mNestedScrollView.onStopNestedScroll(mChild, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, times(1)).onStopNestedScroll(mNestedScrollView,
                        ViewCompat.TYPE_NON_TOUCH);
                verify(mParent, times(1)).onStopNestedScroll(any(View.class), anyInt());
            }
        });
    }

    // TODO(shepshapard), test with interactions where scroll type changes.

    @Test
    public void onStopNestedScroll_parentRejects_parentOnStopNestedScrollNotCalled()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(false)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL);

                mNestedScrollView.onStopNestedScroll(mChild, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, never()).onStopNestedScroll(any(View.class), anyInt());
            }
        });
    }

    @Test
    public void onStopNestedScroll_calledWithTypeNotYetAccepted_parentOnStopNestedScrollNotCalled()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

                mNestedScrollView.onStopNestedScroll(mChild, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, never()).onStopNestedScroll(any(View.class), anyInt());
            }
        });
    }

    @Test
    public void onStopNestedScroll_v1_parentOnStopNestedScrollCalledWithTypeTouch()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL);

                mNestedScrollView.onStopNestedScroll(mChild);

                verify(mParent, times(1)).onStopNestedScroll(any(View.class),
                        eq(ViewCompat.TYPE_TOUCH));
                verify(mParent, times(1)).onStopNestedScroll(any(View.class), anyInt());
            }
        });
    }

    @Test
    public void onNestedScroll_nsvScrolls() throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50, ViewCompat.TYPE_NON_TOUCH);

                assertThat(mNestedScrollView.getScrollY(), is(50));
            }
        });
    }

    @Test
    public void onNestedScroll_negativeScroll_nsvScrollsNegative() throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNestedScrollView.scrollTo(0, 50);

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, -50, ViewCompat.TYPE_NON_TOUCH);

                assertThat(mNestedScrollView.getScrollY(), is(0));
            }
        });
    }

    @Test
    public void onNestedScroll_nsvConsumesEntireScroll_correctScrollDistancesPastToParent()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, times(1)).onNestedScroll(any(View.class), eq(0), eq(50), eq(0),
                        eq(0), anyInt());
                verify(mParent, times(1)).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), anyInt());
            }
        });
    }

    @Test
    public void onNestedScroll_nsvCanOnlyConsumePartOfScroll_correctScrollDistancesPastToParent()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 75, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, times(1)).onNestedScroll(any(View.class), eq(0), eq(50), eq(0),
                        eq(25), anyInt());
                verify(mParent, times(1)).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), anyInt());
            }
        });
    }

    @Test
    public void onNestedScroll_nsvCanOnlyConsumePartOfScrollNeg_correctScrollDistancesPastToParent()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
                mNestedScrollView.scrollTo(0, 50);

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, -75, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, times(1)).onNestedScroll(any(View.class), eq(0), eq(-50), eq(0),
                        eq(-25), anyInt());
                verify(mParent, times(1)).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), anyInt());
            }
        });
    }

    @Test
    public void onNestedScroll_nsvIsAtEndOfScroll_correctScrollDistancesPastToParent()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
                mNestedScrollView.scrollTo(0, 50);

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, times(1)).onNestedScroll(any(View.class), eq(0), eq(0), eq(0),
                        eq(50), anyInt());
                verify(mParent, times(1)).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), anyInt());
            }
        });
    }

    @Test
    public void onNestedScroll_parentRejects_parentOnNestedScrollNotCalled() throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(false)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, never()).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), anyInt());
            }
        });
    }

    @Test
    public void onNestedScroll_calledWithTypeNotYetAccepted_parentOnStopNestedScrollNotCalled()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50, ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, never()).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), anyInt());
            }
        });
    }

    @Test
    public void onNestedScroll_v1_parentOnNestedScrollCalledWithTypeTouch()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild(50, 100);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL);

                mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50);

                verify(mParent, times(1)).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), eq(ViewCompat.TYPE_TOUCH));
                verify(mParent, times(1)).onNestedScroll(any(View.class), anyInt(), anyInt(),
                        anyInt(), anyInt(), anyInt());
            }
        });
    }

    @Test
    public void onNestedPreScroll_parentOnNestedPreScrollCalledWithCorrectParams()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);

                mNestedScrollView.onNestedPreScroll(mChild, 1, 2, new int[]{0, 0},
                        ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, times(1)).onNestedPreScroll(eq(mNestedScrollView), eq(1), eq(2),
                        eq(new int[]{0, 0}), eq(ViewCompat.TYPE_NON_TOUCH));
                verify(mParent, times(1)).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                        any(int[].class), anyInt());
            }
        });
    }

    @Test
    public void onNestedPreScroll_parentRejects_parentOnNestedPreScrollNotCalled()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(false)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);

                mNestedScrollView.onNestedPreScroll(mChild, 1, 2, new int[2],
                        ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, never()).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                        any(int[].class), anyInt());
            }
        });
    }

    @Test
    public void onNestedPreScroll_calledWithTypeNotYetAccepted_parentOnStopNestedScrollNotCalled()
            throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

                mNestedScrollView.onNestedPreScroll(mChild, 1, 2, new int[2],
                        ViewCompat.TYPE_NON_TOUCH);

                verify(mParent, never()).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                        any(int[].class), anyInt());
            }
        });
    }

    @Test
    public void onNestedPreScroll_v1_parentOnNestedPreScrollCalledWithTypeTouch() throws Throwable {
        setupNestedScrollViewWithParentAndChild();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true)
                        .when(mParent)
                        .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
                mNestedScrollView.onNestedScrollAccepted(mChild, mChild,
                        ViewCompat.SCROLL_AXIS_VERTICAL);
                int[] consumed = new int[2];

                mNestedScrollView.onNestedPreScroll(mChild, 1, 2, consumed);

                verify(mParent, times(1)).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                        any(int[].class), eq(ViewCompat.TYPE_TOUCH));
                verify(mParent, times(1)).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                        any(int[].class), anyInt());
            }
        });
    }

    private void onStartNestedScrollV1(int iScrollAxis, boolean oRetValue) {
        boolean retVal = mNestedScrollView.onStartNestedScroll(mChild, mChild, iScrollAxis);
        assertThat(retVal, is(oRetValue));
    }

    private void onStartNestedScrollV2(int iScrollAxis, boolean oRetValue) {
        boolean retVal = mNestedScrollView.onStartNestedScroll(mChild, mChild, iScrollAxis,
                ViewCompat.TYPE_TOUCH);
        assertThat(retVal, is(oRetValue));
    }

    private void setupNestedScrollViewWithParentAndChild() throws Throwable {
        setupNestedScrollViewWithParentAndChild(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void setupNestedScrollViewWithParentAndChild(int nestedScrollViewHeight,
            int childHeight) throws Throwable {
        final TestContentView testContentView =
                mActivityTestRule.getActivity().findViewById(R.id.testContentView);

        mNestedScrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, nestedScrollViewHeight));
        mNestedScrollView.setMinimumHeight(nestedScrollViewHeight);

        mChild.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, childHeight));
        mChild.setMinimumHeight(childHeight);

        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mNestedScrollView.addView(mChild);
                mParent.addView(mNestedScrollView);
                testContentView.addView(mParent);
            }
        });
        testContentView.awaitLayouts(2);
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingChild2,
            NestedScrollingParent2 {

        public NestedScrollingSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {

        }

        @Override
        public boolean startNestedScroll(int axes, int type) {
            return false;
        }

        @Override
        public void stopNestedScroll(int type) {

        }

        @Override
        public boolean hasNestedScrollingParent(int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public void setNestedScrollingEnabled(boolean enabled) {

        }

        @Override
        public boolean isNestedScrollingEnabled() {
            return false;
        }

        @Override
        public boolean startNestedScroll(int axes) {
            return false;
        }

        @Override
        public void stopNestedScroll() {

        }

        @Override
        public boolean hasNestedScrollingParent() {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed,
                int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onStartNestedScroll(View child, View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(View target) {

        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {

        }

        @Override
        public boolean onNestedFling(View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return 0;
        }
    }

}
