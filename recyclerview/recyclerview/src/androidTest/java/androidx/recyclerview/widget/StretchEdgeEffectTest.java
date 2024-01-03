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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EdgeEffect;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.InputDeviceCompat;
import androidx.core.view.MotionEventCompat;
import androidx.core.widget.EdgeEffectCompat;
import androidx.core.widget.NestedScrollView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.testutils.AnimationDurationScaleRule;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class StretchEdgeEffectTest extends BaseRecyclerViewInstrumentationTest {
    private static final int NUM_ITEMS = 10;

    private TestRecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private TestEdgeEffectFactory mFactory;
    private TestAdapter mAdapter;

    @Rule
    public final AnimationDurationScaleRule mEnableAnimations =
            AnimationDurationScaleRule.createForAllTests(1f);

    @Before
    public void setup() throws Throwable {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.ensureLayoutState();

        mRecyclerView = new TestRecyclerView(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter = new TestAdapter(NUM_ITEMS) {
            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                TestViewHolder holder = super.onCreateViewHolder(parent, viewType);
                holder.itemView.setMinimumHeight(mRecyclerView.getMeasuredHeight() * 2 / NUM_ITEMS);
                holder.itemView.setMinimumWidth(mRecyclerView.getMeasuredWidth() * 2 / NUM_ITEMS);
                return holder;
            }
        };
        mRecyclerView.setAdapter(mAdapter);
        mFactory = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(mFactory);
        setRecyclerView(mRecyclerView);
        getInstrumentation().waitForIdleSync();
        assertThat("Assumption check", mRecyclerView.getChildCount() > 0, is(true));
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testLeftEdgeEffectRetract() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(-3);
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mLeft) > 0);
        }
        scrollHorizontalBy(4);
        assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mLeft), 0f);
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(mFactory.mLeft.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testTopEdgeEffectRetract() throws Throwable {
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(3);
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mTop) > 0);
        }
        scrollVerticalBy(-4);
        assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mTop), 0f);
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(mFactory.mTop.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testRightEdgeEffectRetract() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollHorizontalBy(3);
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mRight) > 0);
        }
        scrollHorizontalBy(-4);
        assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mRight), 0f);
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(mFactory.mRight.isFinished());
        }
    }

    /**
     * After pulling the edge effect, releasing should return the edge effect to 0.
     */
    @Test
    public void testBottomEdgeEffectRetract() throws Throwable {
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollVerticalBy(-3);
        if (Build.VERSION.SDK_INT >= 31) {
            assertTrue(EdgeEffectCompat.getDistance(mFactory.mBottom) > 0);
        }

        scrollVerticalBy(4);
        if (Build.VERSION.SDK_INT >= 31) {
            assertEquals(0f, EdgeEffectCompat.getDistance(mFactory.mBottom), 0f);
            assertTrue(mFactory.mBottom.isFinished());
        }
    }

    /**
     * A fling should be allowed during pull, but only for S and later.
     */
    @Test
    public void testFlingAfterStretchLeft() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        // test flinging right
        dragHorizontally(1000);

        if (Build.VERSION.SDK_INT >= 31) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mLeft);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(-1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mLeft), 0.01f);
                assertEquals(1000, mFactory.mLeft.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mLeft.finish();
            });

            dragHorizontally(1000);

            // test flinging left
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mLeft);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mLeft), 0.01f);
                assertEquals(-1000, mFactory.mLeft.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mLeft.mAbsorbVelocity);
            });

            dragHorizontally(1000);

            // fling left and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
                assertTrue(mRecyclerView.fling(5000, 0));
                assertEquals(0, mFactory.mLeft.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findFirstVisibleItemPosition() > 0);
            });
        }
    }

    /**
     * A fling should be allowed during pull.
     */
    @Test
    public void testFlingAfterStretchTop() throws Throwable {
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        // test flinging down
        dragVertically(1000);

        if (Build.VERSION.SDK_INT >= 31) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mTop);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, -1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mTop), 0.01f);
                assertEquals(1000, mFactory.mTop.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mTop.finish();
            });

            dragVertically(1000);

            // test flinging up
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mTop);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, 1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mTop), 0.01f);
                assertEquals(-1000, mFactory.mTop.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mTop.mAbsorbVelocity);
            });

            dragVertically(1000);

            // fling up and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mLayoutManager.findFirstVisibleItemPosition());
                assertTrue(mRecyclerView.fling(0, 5000));
                assertEquals(0, mFactory.mTop.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findFirstVisibleItemPosition() > 0);
            });
        }
    }

    /**
     * A fling should be allowed during pull.
     */
    @Test
    public void testFlingAfterStretchRight() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        // test flinging left
        dragHorizontally(-1000);

        if (Build.VERSION.SDK_INT >= 31) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mRight);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mRight), 0.01f);
                assertEquals(1000, mFactory.mRight.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mRight.finish();
            });

            dragHorizontally(-1000);

            // test flinging right
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mRight);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(-1000, 0));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mRight), 0.01f);
                assertEquals(-1000, mFactory.mRight.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mRight.mAbsorbVelocity);
            });

            dragHorizontally(-1000);

            // fling right and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(mRecyclerView.getAdapter().getItemCount() - 1,
                        mLayoutManager.findLastVisibleItemPosition());
                assertTrue(mRecyclerView.fling(-5000, 0));
                assertEquals(0, mFactory.mRight.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findLastVisibleItemPosition()
                        < mRecyclerView.getAdapter().getItemCount() - 1);
            });

        }
    }

    /**
     * A fling should be allowed during pull.
     */
    @Test
    public void testFlingAfterStretchBottom() throws Throwable {
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        // test flinging up
        dragVertically(-1000);

        if (Build.VERSION.SDK_INT >= 31) {
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mBottom);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, 1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mBottom), 0.01f);
                assertEquals(1000, mFactory.mBottom.mAbsorbVelocity);
                // reset the edge effect
                mFactory.mBottom.finish();
            });

            dragVertically(-1000);

            // test flinging down
            mActivityRule.runOnUiThread(() -> {
                float pullDistance = EdgeEffectCompat.getDistance(mFactory.mBottom);
                assertTrue(pullDistance > 0);
                assertFalse(mRecyclerView.fling(0, -1000));
                assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mBottom), 0.01f);
                assertEquals(-1000, mFactory.mBottom.mAbsorbVelocity);
            });
        } else {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(0, mFactory.mBottom.mAbsorbVelocity);
            });

            dragVertically(-1000);

            // fling down and it should just scroll
            mActivityRule.runOnUiThread(() -> {
                assertEquals(mRecyclerView.getAdapter().getItemCount() - 1,
                        mLayoutManager.findLastVisibleItemPosition());
                assertTrue(mRecyclerView.fling(0, -5000));
                assertEquals(0, mFactory.mBottom.mAbsorbVelocity);
            });
            waitForIdleScroll(mRecyclerView);
            mActivityRule.runOnUiThread(() -> {
                assertTrue(mLayoutManager.findLastVisibleItemPosition()
                        < mRecyclerView.getAdapter().getItemCount() - 1);
            });
        }
    }

    /**
     * On S and higher, a large fling back should remove the stretch as well as start flinging
     * the content.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void flingContentAfterStretchOnLeft() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        dragHorizontally(1000);
        float[] startPullDistance = new float[1];
        mActivityRule.runOnUiThread(() -> {
            mFactory.mLeft.mAbsorbVelocity = 0;
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mLeft);
            startPullDistance[0] = pullDistance;
            assertTrue(pullDistance > 0);
            assertTrue(mRecyclerView.fling(10000, 0));
            assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mLeft), 0.01f);
            assertEquals(0, mFactory.mLeft.mAbsorbVelocity);
            assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
        });
        // Wait for at least one animation frame
        CountDownLatch animationLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(() -> mRecyclerView.postOnAnimation(animationLatch::countDown));
        assertTrue(animationLatch.await(1, TimeUnit.SECONDS));

        // Now make sure the stretch is being released:
        mActivityRule.runOnUiThread(() -> {
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mLeft);
            assertTrue(pullDistance < startPullDistance[0]);
        });

        // Wait for the stretch to be fully released:
        float[] currentPullDistance = new float[1];
        long start = SystemClock.uptimeMillis();
        do {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
                currentPullDistance[0] = EdgeEffectCompat.getDistance(mFactory.mLeft);
            });
        } while (currentPullDistance[0] > 0 && SystemClock.uptimeMillis() < start + 1000);

        assertEquals(0f, currentPullDistance[0], 0f);

        // Now wait for the fling to finish:
        waitForIdleScroll(mRecyclerView);
        mActivityRule.runOnUiThread(() -> {
            assertTrue(mLayoutManager.findFirstVisibleItemPosition() > 0);
        });
    }

    /**
     * On S and higher, a large fling back should remove the stretch as well as start flinging
     * the content.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void flingContentAfterStretchOnTop() throws Throwable {
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        dragVertically(1000);
        float[] startPullDistance = new float[1];
        mActivityRule.runOnUiThread(() -> {
            mFactory.mTop.mAbsorbVelocity = 0;
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mTop);
            startPullDistance[0] = pullDistance;
            assertTrue(pullDistance > 0);
            assertTrue(mRecyclerView.fling(0, 10000));
            assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mTop), 0.01f);
            assertEquals(0, mFactory.mTop.mAbsorbVelocity);
            assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
        });
        // Wait for at least one animation frame
        CountDownLatch animationLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(() -> mRecyclerView.postOnAnimation(animationLatch::countDown));
        assertTrue(animationLatch.await(1, TimeUnit.SECONDS));

        // Now make sure the stretch is being released:
        mActivityRule.runOnUiThread(() -> {
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mTop);
            assertTrue(pullDistance < startPullDistance[0]);
        });

        // Wait for the stretch to be fully released:
        float[] currentPullDistance = new float[1];
        long start = SystemClock.uptimeMillis();
        do {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
                currentPullDistance[0] = EdgeEffectCompat.getDistance(mFactory.mTop);
            });
        } while (currentPullDistance[0] > 0 && SystemClock.uptimeMillis() < start + 1000);

        assertEquals(0f, currentPullDistance[0], 0f);

        // Now wait for the fling to finish:
        waitForIdleScroll(mRecyclerView);
        mActivityRule.runOnUiThread(() -> {
            assertTrue(mLayoutManager.findFirstVisibleItemPosition() > 0);
        });
    }

    /**
     * On S and higher, a large fling back should remove the stretch as well as start flinging
     * the content.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void flingContentAfterStretchOnRight() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        dragHorizontally(-1000);
        float[] startPullDistance = new float[1];
        int[] lastItemPosition = new int[1];
        mActivityRule.runOnUiThread(() -> {
            lastItemPosition[0] = mLayoutManager.findLastVisibleItemPosition();
            mFactory.mRight.mAbsorbVelocity = 0;
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mRight);
            startPullDistance[0] = pullDistance;
            assertTrue(pullDistance > 0);
            assertTrue(mRecyclerView.fling(-10000, 0));
            assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mRight), 0.01f);
            assertEquals(0, mFactory.mRight.mAbsorbVelocity);
            assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
        });
        // Wait for at least one animation frame
        CountDownLatch animationLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(() -> mRecyclerView.postOnAnimation(animationLatch::countDown));
        assertTrue(animationLatch.await(1, TimeUnit.SECONDS));

        // Now make sure the stretch is being released:
        mActivityRule.runOnUiThread(() -> {
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mRight);
            assertTrue(pullDistance < startPullDistance[0]);
        });

        // Wait for the stretch to be fully released:
        float[] currentPullDistance = new float[1];
        long start = SystemClock.uptimeMillis();
        do {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
                currentPullDistance[0] = EdgeEffectCompat.getDistance(mFactory.mRight);
            });
        } while (currentPullDistance[0] > 0 && SystemClock.uptimeMillis() < start + 1000);

        assertEquals(0f, currentPullDistance[0], 0f);

        // Now wait for the fling to finish:
        waitForIdleScroll(mRecyclerView);
        mActivityRule.runOnUiThread(() -> {
            assertTrue(lastItemPosition[0] > mLayoutManager.findLastVisibleItemPosition());
        });
    }

    /**
     * On S and higher, a large fling back should remove the stretch as well as start flinging
     * the content.
     */
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void flingContentAfterStretchOnBottom() throws Throwable {
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        dragVertically(-1000);
        float[] startPullDistance = new float[1];
        int[] lastItemPosition = new int[1];
        mActivityRule.runOnUiThread(() -> {
            lastItemPosition[0] = mLayoutManager.findLastVisibleItemPosition();
            mFactory.mBottom.mAbsorbVelocity = 0;
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mBottom);
            startPullDistance[0] = pullDistance;
            assertTrue(pullDistance > 0);
            assertTrue(mRecyclerView.fling(0, -10000));
            assertEquals(pullDistance, EdgeEffectCompat.getDistance(mFactory.mBottom), 0.01f);
            assertEquals(0, mFactory.mBottom.mAbsorbVelocity);
            assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
        });
        // Wait for at least one animation frame
        CountDownLatch animationLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(() -> mRecyclerView.postOnAnimation(animationLatch::countDown));
        assertTrue(animationLatch.await(1, TimeUnit.SECONDS));

        // Now make sure the stretch is being released:
        mActivityRule.runOnUiThread(() -> {
            float pullDistance = EdgeEffectCompat.getDistance(mFactory.mBottom);
            assertTrue(pullDistance < startPullDistance[0]);
        });

        // Wait for the stretch to be fully released:
        float[] currentPullDistance = new float[1];
        long start = SystemClock.uptimeMillis();
        do {
            mActivityRule.runOnUiThread(() -> {
                assertEquals(RecyclerView.SCROLL_STATE_SETTLING, mRecyclerView.getScrollState());
                currentPullDistance[0] = EdgeEffectCompat.getDistance(mFactory.mBottom);
            });
        } while (currentPullDistance[0] > 0 && SystemClock.uptimeMillis() < start + 1000);

        assertEquals(0f, currentPullDistance[0], 0f);

        // Now wait for the fling to finish:
        waitForIdleScroll(mRecyclerView);
        mActivityRule.runOnUiThread(() -> {
            assertTrue(lastItemPosition[0] > mLayoutManager.findLastVisibleItemPosition());
        });
    }

    @Test
    public void testScrollState() throws Throwable {
        // Drag down and it should only activate over scroll
        dragVertically(1000);
        waitForIdleScroll(mRecyclerView);

        mActivityRule.runOnUiThread(() -> {
            List<Integer> scrollStates = mRecyclerView.scrollStates;
            assertTrue(scrollStates.size() >= 2);
            assertEquals(RecyclerView.SCROLL_STATE_DRAGGING, (int) scrollStates.get(0));
            assertEquals(
                    RecyclerView.SCROLL_STATE_IDLE,
                    (int) scrollStates.get(scrollStates.size() - 1)
            );
        });
    }

    @Test
    public void stretchAndAddContentToBottom() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndAddContent(() -> mFactory.mBottom, true, true);
        }
    }

    @Test
    public void doubleStretchBottom() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndStretchMore(() -> mFactory.mBottom, true, true);
        }
    }

    @Test
    public void stretchAndAddContentToTop() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndAddContent(() -> mFactory.mTop, false, true);
        }
    }

    @Test
    public void doubleStretchTop() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndStretchMore(() -> mFactory.mTop, false, true);
        }
    }

    @Test
    public void stretchAndAddContentToRight() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndAddContent(() -> mFactory.mRight, true, false);
        }
    }

    @Test
    public void doubleStretchRight() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndStretchMore(() -> mFactory.mRight, true, false);
        }
    }

    @Test
    public void stretchAndAddContentToLeft() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndAddContent(() -> mFactory.mLeft, false, false);
        }
    }

    @Test
    public void doubleStretchLeft() throws Throwable {
        if (Build.VERSION.SDK_INT >= 31) {
            stretchAndStretchMore(() -> mFactory.mLeft, false, false);
        }
    }

    @Test
    public void testTopStretchEdgeEffectReleasedAfterRotaryEncoderPull() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL));
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        scroll(4, MotionEventCompat.AXIS_SCROLL, InputDeviceCompat.SOURCE_ROTARY_ENCODER);

        assertEquals(Build.VERSION.SDK_INT >= 31, mFactory.mTop.mOnReleaseCalled);
    }

    @Test
    public void testBottomStretchEdgeEffectReleasedAfterRotaryEncoderPull() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL));
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        scroll(-4, MotionEventCompat.AXIS_SCROLL, InputDeviceCompat.SOURCE_ROTARY_ENCODER);

        assertEquals(Build.VERSION.SDK_INT >= 31, mFactory.mBottom.mOnReleaseCalled);
    }

    @Test
    public void testLeftStretchEdgeEffectReleasedAfterRotaryEncoderPull() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        scroll(-4, MotionEventCompat.AXIS_SCROLL, InputDeviceCompat.SOURCE_ROTARY_ENCODER);

        assertEquals(Build.VERSION.SDK_INT >= 31, mFactory.mLeft.mOnReleaseCalled);
    }

    @Test
    public void testRightStretchEdgeEffectReleasedAfterRotaryEncoderPull() throws Throwable {
        mActivityRule.runOnUiThread(
                () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);

        scroll(4, MotionEventCompat.AXIS_SCROLL, InputDeviceCompat.SOURCE_ROTARY_ENCODER);

        assertEquals(Build.VERSION.SDK_INT >= 31, mFactory.mRight.mOnReleaseCalled);
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
    public void flingInNestedScroll() throws Throwable {
        TestActivity activity = mActivityRule.getActivity();
        NestedScrollView scrollView = new NestedScrollView(activity);
        // Give the RecyclerView a parent that can scroll
        mActivityRule.runOnUiThread(() -> {
            ViewGroup parent = (ViewGroup) mRecyclerView.getParent();
            int height = parent.getHeight();
            int width = parent.getWidth();
            parent.removeView(mRecyclerView);
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            View view = new View(activity);
            view.setBackgroundColor(Color.BLUE);
            linearLayout.addView(view, width, height / 2);
            linearLayout.addView(mRecyclerView, width, height);
            scrollView.addView(linearLayout);
            parent.addView(scrollView);
            mRecyclerView.setEdgeEffectFactory(new RecyclerView.EdgeEffectFactory());
        });
        mActivityRule.runOnUiThread(() -> scrollView.scrollTo(0, scrollView.getHeight() / 2));
        mActivityRule.runOnUiThread(() -> mRecyclerView.fling(0, 10000));
        PollingCheck.waitFor(() -> !mRecyclerView.canScrollVertically(1));
        mActivityRule.runOnUiThread(() -> mRecyclerView.fling(0, -10000));
        PollingCheck.waitFor(() -> scrollView.getScrollY() == 0);
    }

    /**
     * When stretching and new items are added, the stretch should be released and
     * a drag from the user should scroll instead of changing the stretch.
     */
    private void stretchAndAddContent(
            @NonNull EdgeFactoryReference edgeEffect,
            boolean increase,
            boolean vertical
    ) throws Throwable {
        if (!vertical) {
            mActivityRule.runOnUiThread(
                    () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        }
        if (increase) {
            scrollToPosition(NUM_ITEMS - 1);
        }
        waitForIdleScroll(mRecyclerView);

        float density =
                mActivityRule.getActivity().getResources().getDisplayMetrics().densityDpi / 160f;

        int distance = (int) (500 * density) * (increase ? -1 : 1);
        int start = increase ? NUM_ITEMS : 0;

        int dragX = vertical ? 0 : distance;
        int dragY = vertical ? distance : 0;

        dragAndExecute(
                dragX,
                dragY,
                null,
                () -> {
                    assertFalse(edgeEffect.invoke().mOnReleaseCalled);
                    try {
                        mAdapter.addAndNotify(start, NUM_ITEMS);
                    } catch (Throwable e) {
                    }
                },
                () -> {
                    assertTrue(edgeEffect.invoke().mOnReleaseCalled);
                }
        );

        float oldDistance = edgeEffect.invoke().mDistance;
        int firstVisible = mLayoutManager.findFirstVisibleItemPosition();

        // There is no animation from the onRelease, so the stretch is still active.
        // Additional drag should not change the stretch distance and should scroll instead.

        dragAndExecute(dragX, dragY, () -> assertFalse(edgeEffect.invoke().mIsHeld), null, null);

        assertEquals(oldDistance, edgeEffect.invoke().mDistance, 0.01f);
        assertNotEquals(firstVisible, mLayoutManager.findFirstVisibleItemPosition());
    }

    /**
     * When stretching and releasing, then drag to stretch again, it should
     * increase the stretch.
     */
    private void stretchAndStretchMore(
            @NonNull EdgeFactoryReference edgeEffect,
            boolean increase,
            boolean vertical
    ) throws Throwable {
        if (!vertical) {
            mActivityRule.runOnUiThread(
                    () -> mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL));
        }
        if (increase) {
            scrollToPosition(NUM_ITEMS - 1);
        }
        waitForIdleScroll(mRecyclerView);

        float density =
                mActivityRule.getActivity().getResources().getDisplayMetrics().densityDpi / 160f;

        int distance = (int) (50 * density) * (increase ? -1 : 1);
        int start = increase ? NUM_ITEMS : 0;

        int dragX = vertical ? 0 : distance;
        int dragY = vertical ? distance : 0;

        drag(dragX, dragY);

        float oldDistance = edgeEffect.invoke().mDistance;
        assertTrue(oldDistance > 0f);
        int firstVisible = mLayoutManager.findFirstVisibleItemPosition();

        // There is no animation from the onRelease, so the stretch is still active.
        // Additional drag should continue the stretch.

        dragAndExecute(dragX, dragY, () -> assertTrue(edgeEffect.invoke().mIsHeld), null, null);

        assertTrue(oldDistance < edgeEffect.invoke().mDistance);
        assertEquals(firstVisible, mLayoutManager.findFirstVisibleItemPosition());
    }

    private void scroll(final int value, final int axis, final int source) throws Throwable {
        mActivityRule.runOnUiThread(() -> TouchUtils.scrollView(axis, value,
                source, mRecyclerView));
    }

    private void scrollVerticalBy(final int value) throws Throwable {
        scroll(value, MotionEvent.AXIS_VSCROLL, InputDeviceCompat.SOURCE_CLASS_POINTER);
    }

    private void scrollHorizontalBy(final int value) throws Throwable {
        scroll(value, MotionEvent.AXIS_HSCROLL, InputDeviceCompat.SOURCE_CLASS_POINTER);
    }

    private void dragVertically(int amount) {
        drag(0, amount);
    }

    private void dragHorizontally(int amount) {
        drag(amount, 0);
    }

    private void drag(int deltaX, int deltaY) {
        dragAndExecute(deltaX, deltaY, null, null, null);
    }

    private void dragAndExecute(
            int deltaX,
            int deltaY,
            @Nullable Runnable afterDown,
            @Nullable Runnable middleDrag,
            @Nullable Runnable endDrag
    ) {
        float centerX = mRecyclerView.getWidth() / 2f;
        float centerY = mRecyclerView.getHeight() / 2f;
        MotionEvent down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN,
                centerX, centerY, 0);
        mActivityRule.runOnUiThread(() -> mRecyclerView.dispatchTouchEvent(down));

        if (afterDown != null) {
            afterDown.run();
        }

        for (int i = 0; i < 10; i++) {
            float x = centerX + (deltaX * (i + 1) / 10f);
            float y = centerY + (deltaY * (i + 1) / 10f);
            MotionEvent move = MotionEvent.obtain(0, (16 * i) + 16, MotionEvent.ACTION_MOVE,
                    x, y, 0);
            mActivityRule.runOnUiThread(() -> mRecyclerView.dispatchTouchEvent(move));
            if (i == 5 && middleDrag != null) {
                middleDrag.run();
            }
        }
        if (endDrag != null) {
            endDrag.run();
        }
        MotionEvent up = MotionEvent.obtain(0, 160, MotionEvent.ACTION_UP,
                centerX + deltaX, centerY + deltaY, 0);
        mActivityRule.runOnUiThread(() -> mRecyclerView.dispatchTouchEvent(up));
    }

    private static class TestEdgeEffectFactory extends RecyclerView.EdgeEffectFactory {
        TestEdgeEffect mTop, mBottom, mLeft, mRight;

        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(RecyclerView view, int direction) {
            TestEdgeEffect effect = new TestEdgeEffect(view.getContext());
            switch (direction) {
                case DIRECTION_LEFT:
                    mLeft = effect;
                    break;
                case DIRECTION_TOP:
                    mTop = effect;
                    break;
                case DIRECTION_RIGHT:
                    mRight = effect;
                    break;
                case DIRECTION_BOTTOM:
                    mBottom = effect;
                    break;
            }
            return effect;
        }
    }

    private static class TestEdgeEffect extends EdgeEffect {

        private float mDistance;
        public int mAbsorbVelocity;
        public boolean mOnReleaseCalled;
        public boolean mIsHeld;

        TestEdgeEffect(Context context) {
            super(context);
        }

        @Override
        public void onPull(float deltaDistance, float displacement) {
            onPull(deltaDistance);
        }

        @Override
        public void onPull(float deltaDistance) {
            mDistance += deltaDistance;
            mIsHeld = true;
        }

        @Override
        public float onPullDistance(float deltaDistance, float displacement) {
            float maxDelta = Math.max(-mDistance, deltaDistance);
            onPull(maxDelta);
            return maxDelta;
        }

        @Override
        public float getDistance() {
            return mDistance;
        }

        @Override
        public void finish() {
            super.finish();
            mDistance = 0;
            mOnReleaseCalled = false;
            mIsHeld = false;
        }

        @Override
        public boolean isFinished() {
            return mDistance == 0;
        }

        @Override
        public void onAbsorb(int velocity) {
            mAbsorbVelocity = velocity;
            mIsHeld = false;
        }

        @Override
        public void onRelease() {
            mOnReleaseCalled = true;
            mIsHeld = false;
        }
    }

    private static class TestRecyclerView extends RecyclerView {
        public List<Integer> scrollStates = new ArrayList<Integer>();

        TestRecyclerView(@NonNull Context context) {
            super(context);
        }

        @Override
        public void onScrollStateChanged(int state) {
            super.onScrollStateChanged(state);
            scrollStates.add(state);
        }
    }

    private interface EdgeFactoryReference {
        TestEdgeEffect invoke();
    }
}
