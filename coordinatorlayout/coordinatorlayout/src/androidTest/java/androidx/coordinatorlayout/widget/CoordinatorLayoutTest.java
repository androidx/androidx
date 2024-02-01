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

package androidx.coordinatorlayout.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.test.R;
import androidx.coordinatorlayout.testutils.CoordinatorLayoutUtils;
import androidx.coordinatorlayout.testutils.CoordinatorLayoutUtils.DependentBehavior;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings({"unchecked", "deprecation"})
@LargeTest
@RunWith(AndroidJUnit4.class)
public class CoordinatorLayoutTest {
    @Rule
    public final androidx.test.rule.ActivityTestRule<CoordinatorLayoutActivity> mActivityTestRule;

    private Instrumentation mInstrumentation;

    public CoordinatorLayoutTest() {
        mActivityTestRule = new androidx.test.rule.ActivityTestRule<>(
                CoordinatorLayoutActivity.class);
    }

    @Before
    public void setup() {
        mInstrumentation = getInstrumentation();
    }

    @Test
    @SdkSuppress(minSdkVersion = 21)
    public void testSetFitSystemWindows() throws Throwable {
        // Skip this test on Android TV
        PackageManager manager = mActivityTestRule.getActivity().getPackageManager();
        if (manager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
                || manager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            return;
        }

        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;
        final View view = new View(col.getContext());

        // Create a mock which calls the default impl of onApplyWindowInsets()
        final CoordinatorLayout.Behavior<View> mockBehavior =
                mock(CoordinatorLayout.Behavior.class);
        doCallRealMethod().when(mockBehavior)
                .onApplyWindowInsets(same(col), same(view), any(WindowInsetsCompat.class));

        // Assert that the CoL is currently not set to fitSystemWindows
        assertFalse(col.getFitsSystemWindows());

        // Now add a view with our mocked behavior to the CoordinatorLayout
        view.setFitsSystemWindows(true);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CoordinatorLayout.LayoutParams lp = col.generateDefaultLayoutParams();
                lp.setBehavior(mockBehavior);
                col.addView(view, lp);
            }
        });
        instrumentation.waitForIdleSync();

        // Now request some insets and wait for the pass to happen
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.requestApplyInsets();
            }
        });
        instrumentation.waitForIdleSync();

        // Verify that onApplyWindowInsets() has not been called
        verify(mockBehavior, never())
                .onApplyWindowInsets(same(col), same(view), any(WindowInsetsCompat.class));

        // Now enable fits system windows and wait for a pass to happen
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.setFitsSystemWindows(true);
            }
        });
        instrumentation.waitForIdleSync();

        // Verify that onApplyWindowInsets() has been called with some insets
        verify(mockBehavior, atLeastOnce())
                .onApplyWindowInsets(same(col), same(view), any(WindowInsetsCompat.class));
    }

    @Test
    public void testLayoutChildren() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;
        final View view = new View(col.getContext());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(view, 100, 100);
            }
        });
        instrumentation.waitForIdleSync();
        int horizontallyCentered = (col.getWidth() - view.getWidth()) / 2;
        int end = col.getWidth() - view.getWidth();
        int verticallyCentered = (col.getHeight() - view.getHeight()) / 2;
        int bottom = col.getHeight() - view.getHeight();
        final int[][] testCases = {
                // gravity, expected left, expected top
                {Gravity.NO_GRAVITY, 0, 0},
                {Gravity.LEFT, 0, 0},
                {GravityCompat.START, 0, 0},
                {Gravity.TOP, 0, 0},
                {Gravity.CENTER, horizontallyCentered, verticallyCentered},
                {Gravity.CENTER_HORIZONTAL, horizontallyCentered, 0},
                {Gravity.CENTER_VERTICAL, 0, verticallyCentered},
                {Gravity.RIGHT, end, 0},
                {GravityCompat.END, end, 0},
                {Gravity.BOTTOM, 0, bottom},
                {Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, horizontallyCentered, bottom},
                {Gravity.RIGHT | Gravity.CENTER_VERTICAL, end, verticallyCentered},
        };
        for (final int[] testCase : testCases) {
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final CoordinatorLayout.LayoutParams lp =
                            (CoordinatorLayout.LayoutParams) view.getLayoutParams();
                    lp.gravity = testCase[0];
                    view.setLayoutParams(lp);
                }
            });
            instrumentation.waitForIdleSync();
            mActivityTestRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    assertThat("Gravity: " + testCase[0], view.getLeft(), is(testCase[1]));
                    assertThat("Gravity: " + testCase[0], view.getTop(), is(testCase[2]));
                }
            });
        }
    }

    @Test
    public void testInsetDependency() {
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        final CoordinatorLayout.LayoutParams lpInsetLeft = col.generateDefaultLayoutParams();
        lpInsetLeft.insetEdge = Gravity.LEFT;

        final CoordinatorLayout.LayoutParams lpInsetRight = col.generateDefaultLayoutParams();
        lpInsetRight.insetEdge = Gravity.RIGHT;

        final CoordinatorLayout.LayoutParams lpInsetTop = col.generateDefaultLayoutParams();
        lpInsetTop.insetEdge = Gravity.TOP;

        final CoordinatorLayout.LayoutParams lpInsetBottom = col.generateDefaultLayoutParams();
        lpInsetBottom.insetEdge = Gravity.BOTTOM;

        final CoordinatorLayout.LayoutParams lpDodgeLeft = col.generateDefaultLayoutParams();
        lpDodgeLeft.dodgeInsetEdges = Gravity.LEFT;

        final CoordinatorLayout.LayoutParams lpDodgeLeftAndTop = col.generateDefaultLayoutParams();
        lpDodgeLeftAndTop.dodgeInsetEdges = Gravity.LEFT | Gravity.TOP;

        final CoordinatorLayout.LayoutParams lpDodgeAll = col.generateDefaultLayoutParams();
        lpDodgeAll.dodgeInsetEdges = Gravity.FILL;

        final View a = new View(col.getContext());
        final View b = new View(col.getContext());

        assertThat(dependsOn(lpDodgeLeft, lpInsetLeft, col, a, b), is(true));
        assertThat(dependsOn(lpDodgeLeft, lpInsetRight, col, a, b), is(false));
        assertThat(dependsOn(lpDodgeLeft, lpInsetTop, col, a, b), is(false));
        assertThat(dependsOn(lpDodgeLeft, lpInsetBottom, col, a, b), is(false));

        assertThat(dependsOn(lpDodgeLeftAndTop, lpInsetLeft, col, a, b), is(true));
        assertThat(dependsOn(lpDodgeLeftAndTop, lpInsetRight, col, a, b), is(false));
        assertThat(dependsOn(lpDodgeLeftAndTop, lpInsetTop, col, a, b), is(true));
        assertThat(dependsOn(lpDodgeLeftAndTop, lpInsetBottom, col, a, b), is(false));

        assertThat(dependsOn(lpDodgeAll, lpInsetLeft, col, a, b), is(true));
        assertThat(dependsOn(lpDodgeAll, lpInsetRight, col, a, b), is(true));
        assertThat(dependsOn(lpDodgeAll, lpInsetTop, col, a, b), is(true));
        assertThat(dependsOn(lpDodgeAll, lpInsetBottom, col, a, b), is(true));

        assertThat(dependsOn(lpInsetLeft, lpDodgeLeft, col, a, b), is(false));
    }

    private static boolean dependsOn(CoordinatorLayout.LayoutParams lpChild,
            CoordinatorLayout.LayoutParams lpDependency, CoordinatorLayout col,
            View child, View dependency) {
        child.setLayoutParams(lpChild);
        dependency.setLayoutParams(lpDependency);
        return lpChild.dependsOn(col, child, dependency);
    }

    @Test
    public void testInsetEdge() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        final View insetView = new View(col.getContext());
        final View dodgeInsetView = new View(col.getContext());
        final AtomicInteger originalTop = new AtomicInteger();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CoordinatorLayout.LayoutParams lpInsetView = col.generateDefaultLayoutParams();
                lpInsetView.width = CoordinatorLayout.LayoutParams.MATCH_PARENT;
                lpInsetView.height = 100;
                lpInsetView.gravity = Gravity.TOP | Gravity.LEFT;
                lpInsetView.insetEdge = Gravity.TOP;
                col.addView(insetView, lpInsetView);
                insetView.setBackgroundColor(0xFF0000FF);

                CoordinatorLayout.LayoutParams lpDodgeInsetView = col.generateDefaultLayoutParams();
                lpDodgeInsetView.width = 100;
                lpDodgeInsetView.height = 100;
                lpDodgeInsetView.gravity = Gravity.TOP | Gravity.LEFT;
                lpDodgeInsetView.dodgeInsetEdges = Gravity.TOP;
                col.addView(dodgeInsetView, lpDodgeInsetView);
                dodgeInsetView.setBackgroundColor(0xFFFF0000);
            }
        });
        instrumentation.waitForIdleSync();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<View> dependencies = col.getDependencies(dodgeInsetView);
                assertThat(dependencies.size(), is(1));
                assertThat(dependencies.get(0), is(insetView));

                // Move the insetting view
                originalTop.set(dodgeInsetView.getTop());
                assertThat(originalTop.get(), is(insetView.getBottom()));
                ViewCompat.offsetTopAndBottom(insetView, 123);
            }
        });
        instrumentation.waitForIdleSync();
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Confirm that the dodging view was moved by the same size
                assertThat(dodgeInsetView.getTop() - originalTop.get(), is(123));
            }
        });
    }

    @Test
    public void testDependentViewChanged() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Add two views, A & B, where B depends on A
        final View viewA = new View(col.getContext());
        final CoordinatorLayout.LayoutParams lpA = col.generateDefaultLayoutParams();
        lpA.width = 100;
        lpA.height = 100;

        final View viewB = new View(col.getContext());
        final CoordinatorLayout.LayoutParams lpB = col.generateDefaultLayoutParams();
        lpB.width = 100;
        lpB.height = 100;
        final CoordinatorLayout.Behavior behavior =
                spy(new DependentBehavior(viewA));
        lpB.setBehavior(behavior);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(viewA, lpA);
                col.addView(viewB, lpB);
            }
        });
        instrumentation.waitForIdleSync();

        // Reset the Behavior since onDependentViewChanged may have already been called as part of
        // any layout/draw passes already
        reset(behavior);

        // Now offset view A
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewCompat.offsetLeftAndRight(viewA, 20);
                ViewCompat.offsetTopAndBottom(viewA, 20);
            }
        });
        instrumentation.waitForIdleSync();

        // And assert that view B's Behavior was called appropriately
        verify(behavior, times(1)).onDependentViewChanged(col, viewB, viewA);
    }

    @Test
    public void testDependentViewRemoved() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Add two views, A & B, where B depends on A
        final View viewA = new View(col.getContext());
        final View viewB = new View(col.getContext());
        final CoordinatorLayout.LayoutParams lpB = col.generateDefaultLayoutParams();
        final CoordinatorLayout.Behavior behavior =
                spy(new DependentBehavior(viewA));
        lpB.setBehavior(behavior);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(viewA);
                col.addView(viewB, lpB);
            }
        });
        instrumentation.waitForIdleSync();

        // Now remove view A
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.removeView(viewA);
            }
        });

        // And assert that View B's Behavior was called appropriately
        verify(behavior, times(1)).onDependentViewRemoved(col, viewB, viewA);
    }

    @Test
    public void testGetDependenciesAfterDependentViewRemoved() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Add two views, A & B, where B depends on A
        final View viewA = new View(col.getContext());
        final View viewB = new View(col.getContext());
        final CoordinatorLayout.LayoutParams lpB = col.generateDefaultLayoutParams();
        final CoordinatorLayout.Behavior<View> behavior =
                new CoordinatorLayoutUtils.DependentBehavior(viewA) {
                    @Override
                    public void onDependentViewRemoved(CoordinatorLayout parent,
                            @NonNull View child, @NonNull View dependency) {
                        // Make sure this doesn't crash.
                        parent.getDependencies(child);
                    }
                };
        lpB.setBehavior(behavior);

        // Now add views
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(viewA);
                col.addView(viewB, lpB);
            }
        });

        // Wait for a layout
        instrumentation.waitForIdleSync();

        // Now remove view A, which will trigger onDependentViewRemoved() on view B's behavior
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.removeView(viewA);
            }
        });
    }

    @Test
    public void testGetDependencies() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Add two views, A & B, where B depends on A
        final View viewA = new View(col.getContext());
        final View viewB = new View(col.getContext());
        final CoordinatorLayout.LayoutParams lpB = col.generateDefaultLayoutParams();
        final CoordinatorLayout.Behavior<View> behavior =
                new CoordinatorLayoutUtils.DependentBehavior(viewA);
        lpB.setBehavior(behavior);

        // Now add views
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(viewA);
                col.addView(viewB, lpB);
            }
        });

        // Wait for a layout
        instrumentation.waitForIdleSync();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<View> depsA = col.getDependencies(viewA);
                assertNotNull("getDependencies is non-null", depsA);
                assertThat("A has no dependencies", depsA.isEmpty());

                List<View> depsB = col.getDependencies(viewB);
                assertEquals("B depends only on A", 1, depsB.size());
                assertThat("B depends only on A", depsB.contains(viewA));

                assertThat("getDependencies returns a new list",
                        depsB != col.getDependencies(viewB));
            }
        });
    }

    @Test
    public void testGetDependents() throws Throwable {
        final Instrumentation instrumentation = getInstrumentation();
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Add two views, A & B, where B depends on A
        final View viewA = new View(col.getContext());
        final View viewB = new View(col.getContext());
        final CoordinatorLayout.LayoutParams lpB = col.generateDefaultLayoutParams();
        final CoordinatorLayout.Behavior<View> behavior =
                new CoordinatorLayoutUtils.DependentBehavior(viewA);
        lpB.setBehavior(behavior);

        // Now add views
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(viewA);
                col.addView(viewB, lpB);
            }
        });

        // Wait for a layout
        instrumentation.waitForIdleSync();

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<View> depsA = col.getDependents(viewA);
                assertEquals("A is depended upon only by B", 1, depsA.size());
                assertThat("A is depended upon only by B", depsA.contains(viewB));

                List<View> depsB = col.getDependents(viewB);
                assertNotNull("getDependents is non-null", depsB);
                assertThat("B has no dependents", depsB.isEmpty());

                assertThat("getDependents returns a new list",
                        depsA != col.getDependents(viewA));
            }
        });
    }

    @Test
    public void testDodgeInsetBeforeLayout() throws Throwable {
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Add a no-op view, which will be used to trigger a hierarchy change.
        final View noOpView = new View(col.getContext());

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(noOpView);
            }
        });

        // Wait for a layout.
        mInstrumentation.waitForIdleSync();

        final View dodge = new View(col.getContext());
        final CoordinatorLayout.LayoutParams lpDodge = col.generateDefaultLayoutParams();
        lpDodge.dodgeInsetEdges = Gravity.BOTTOM;
        lpDodge.setBehavior(new CoordinatorLayout.Behavior() {
            @Override
            public boolean getInsetDodgeRect(CoordinatorLayout parent, View child, Rect rect) {
                // Any non-empty rect is fine here.
                rect.set(0, 0, 10, 10);
                return true;
            }
        });

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(dodge, lpDodge);

                // Ensure the new view is in the list of children.
                int heightSpec = MeasureSpec.makeMeasureSpec(col.getHeight(), MeasureSpec.EXACTLY);
                int widthSpec = MeasureSpec.makeMeasureSpec(col.getWidth(), MeasureSpec.EXACTLY);
                col.measure(widthSpec, heightSpec);

                // Force a hierarchy change.
                col.removeView(noOpView);
            }
        });

        // Wait for a layout.
        mInstrumentation.waitForIdleSync();
    }

    @Test
    public void testGoneViewsNotMeasuredLaidOut() throws Throwable {
        final CoordinatorLayoutActivity activity = mActivityTestRule.getActivity();
        final CoordinatorLayout col = activity.mCoordinatorLayout;

        // Now create a GONE view and add it to the CoordinatorLayout
        final View imageView = new View(activity);
        imageView.setVisibility(View.GONE);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                col.addView(imageView, 200, 200);
            }
        });
        // Wait for a layout and measure pass
        mInstrumentation.waitForIdleSync();

        // And assert that it has not been laid out
        assertFalse(imageView.getMeasuredWidth() > 0);
        assertFalse(imageView.getMeasuredHeight() > 0);
        assertFalse(imageView.isLaidOut());

        // Now set the view to INVISIBLE
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setVisibility(View.INVISIBLE);
            }
        });
        // Wait for a layout and measure pass
        mInstrumentation.waitForIdleSync();

        // And assert that it has been laid out
        assertTrue(imageView.getMeasuredWidth() > 0);
        assertTrue(imageView.getMeasuredHeight() > 0);
        assertTrue(imageView.isLaidOut());
    }

    @Test
    @Ignore("b/294608735")
    public void testNestedScrollingDispatchesToBehavior() throws Throwable {
        final CoordinatorLayoutActivity activity = mActivityTestRule.getActivity();
        final CoordinatorLayout col = activity.mCoordinatorLayout;

        // Now create a view and add it to the CoordinatorLayout with the spy behavior,
        // along with a NestedScrollView
        final ImageView imageView = new ImageView(activity);
        final CoordinatorLayout.Behavior behavior = spy(new NestedScrollingBehavior());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater.from(activity).inflate(R.layout.include_nestedscrollview, col, true);

                CoordinatorLayout.LayoutParams clp = new CoordinatorLayout.LayoutParams(200, 200);
                clp.setBehavior(behavior);
                col.addView(imageView, clp);
            }
        });

        // Now vertically swipe up on the NSV, causing nested scrolling to occur
        onView(withId(R.id.nested_scrollview)).perform(swipeUp());

        // Verify that the Behavior's onStartNestedScroll was called once
        verify(behavior, times(1)).onStartNestedScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(View.class), // direct child target
                any(int.class)); // axes

        // Verify that the Behavior's onNestedScrollAccepted was called once
        verify(behavior, times(1)).onNestedScrollAccepted(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(View.class), // direct child target
                any(int.class)); // axes

        // Verify that the Behavior's onNestedPreScroll was called at least once
        verify(behavior, atLeastOnce()).onNestedPreScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(int.class), // dx
                any(int.class), // dy
                any(int[].class)); // consumed

        // Verify that the Behavior's onNestedScroll was called at least once
        verify(behavior, atLeastOnce()).onNestedScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(int.class), // dx consumed
                any(int.class), // dy consumed
                any(int.class), // dx unconsumed
                any(int.class)); // dy unconsumed

        // Verify that the Behavior's onStopNestedScroll was called once
        verify(behavior, times(1)).onStopNestedScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class)); // target
    }

    @Test
    public void testNestedScrollingDispatchingToBehaviorWithGoneView() throws Throwable {
        final CoordinatorLayoutActivity activity = mActivityTestRule.getActivity();
        final CoordinatorLayout col = activity.mCoordinatorLayout;

        // Now create a GONE view and add it to the CoordinatorLayout with the spy behavior,
        // along with a NestedScrollView
        final ImageView imageView = new ImageView(activity);
        imageView.setVisibility(View.GONE);
        final CoordinatorLayout.Behavior behavior = spy(new NestedScrollingBehavior());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LayoutInflater.from(activity).inflate(R.layout.include_nestedscrollview, col, true);

                CoordinatorLayout.LayoutParams clp = new CoordinatorLayout.LayoutParams(200, 200);
                clp.setBehavior(behavior);
                col.addView(imageView, clp);
            }
        });

        // Now vertically swipe up on the NSV, causing nested scrolling to occur
        onView(withId(R.id.nested_scrollview)).perform(swipeUp());

        // Verify that the Behavior's onStartNestedScroll was not called
        verify(behavior, never()).onStartNestedScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(View.class), // direct child target
                any(int.class)); // axes

        // Verify that the Behavior's onNestedScrollAccepted was not called
        verify(behavior, never()).onNestedScrollAccepted(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(View.class), // direct child target
                any(int.class)); // axes

        // Verify that the Behavior's onNestedPreScroll was not called
        verify(behavior, never()).onNestedPreScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(int.class), // dx
                any(int.class), // dy
                any(int[].class)); // consumed

        // Verify that the Behavior's onNestedScroll was not called
        verify(behavior, never()).onNestedScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class), // target
                any(int.class), // dx consumed
                any(int.class), // dy consumed
                any(int.class), // dx unconsumed
                any(int.class)); // dy unconsumed

        // Verify that the Behavior's onStopNestedScroll was not called
        verify(behavior, never()).onStopNestedScroll(
                eq(col), // parent
                eq(imageView), // child
                any(View.class)); // target
    }

    @Test
    public void testNestedScrollingTriggeringDependentViewChanged() throws Throwable {
        final CoordinatorLayoutActivity activity = mActivityTestRule.getActivity();
        final CoordinatorLayout col = activity.mCoordinatorLayout;

        // First a NestedScrollView to trigger nested scrolling
        final View scrollView = LayoutInflater.from(activity).inflate(
                R.layout.include_nestedscrollview, col, false);

        // Now create a View and Behavior which depend on the scrollview
        final ImageView dependentView = new ImageView(activity);
        final CoordinatorLayout.Behavior dependentBehavior = spy(new DependentBehavior(scrollView));

        // Finally a view which accepts nested scrolling in the CoordinatorLayout
        final ImageView nestedScrollAwareView = new ImageView(activity);

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // First add the ScrollView
                col.addView(scrollView);

                // Now add the view which depends on the scrollview
                CoordinatorLayout.LayoutParams clp = new CoordinatorLayout.LayoutParams(200, 200);
                clp.setBehavior(dependentBehavior);
                col.addView(dependentView, clp);

                // Now add the nested scrolling aware view
                clp = new CoordinatorLayout.LayoutParams(200, 200);
                clp.setBehavior(new NestedScrollingBehavior());
                col.addView(nestedScrollAwareView, clp);
            }
        });

        // Wait for any layouts, and reset the Behavior so that the call counts are 0
        getInstrumentation().waitForIdleSync();
        reset(dependentBehavior);

        // Now vertically swipe up on the NSV, causing nested scrolling to occur
        onView(withId(R.id.nested_scrollview)).perform(swipeUp());

        // Verify that the Behavior's onDependentViewChanged is not called due to the
        // nested scroll
        verify(dependentBehavior, never()).onDependentViewChanged(
                eq(col), // parent
                eq(dependentView), // child
                eq(scrollView)); // axes
    }

    @Test
    public void testDodgeInsetViewWithEmptyBounds() throws Throwable {
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Add a view with zero height/width which is set to dodge its bounds
        final View view = new View(col.getContext());
        final CoordinatorLayout.Behavior spyBehavior = spy(new DodgeBoundsBehavior());
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final CoordinatorLayout.LayoutParams lp = col.generateDefaultLayoutParams();
                lp.dodgeInsetEdges = Gravity.BOTTOM;
                lp.gravity = Gravity.BOTTOM;
                lp.height = 0;
                lp.width = 0;
                lp.setBehavior(spyBehavior);
                col.addView(view, lp);
            }
        });

        // Wait for a layout
        mInstrumentation.waitForIdleSync();

        // Now add an non-empty bounds inset view to the bottom of the CoordinatorLayout
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final View dodge = new View(col.getContext());
                final CoordinatorLayout.LayoutParams lp = col.generateDefaultLayoutParams();
                lp.insetEdge = Gravity.BOTTOM;
                lp.gravity = Gravity.BOTTOM;
                lp.height = 60;
                lp.width = CoordinatorLayout.LayoutParams.MATCH_PARENT;
                col.addView(dodge, lp);
            }
        });

        // Verify that the Behavior of the view with empty bounds does not have its
        // getInsetDodgeRect() called
        verify(spyBehavior, never())
                .getInsetDodgeRect(same(col), same(view), any(Rect.class));
    }

    public static class NestedScrollingBehavior extends CoordinatorLayout.Behavior<View> {
        @Override
        public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child,
                View directTargetChild, View target, int nestedScrollAxes) {
            // Return true so that we always accept nested scroll events
            return true;
        }
    }

    public static class DodgeBoundsBehavior extends CoordinatorLayout.Behavior<View> {
        @Override
        public boolean getInsetDodgeRect(CoordinatorLayout parent, View child, Rect rect) {
            rect.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
            return true;
        }
    }

    @UiThreadTest
    @Test
    public void testAnchorDependencyGraph() throws Throwable {
        final CoordinatorLayout col = mActivityTestRule.getActivity().mCoordinatorLayout;

        // Override hashcode because of implementation of SimpleArrayMap used in
        // DirectedAcyclicGraph used for sorting dependencies. Hashcode of anchored view has to be
        // greater than of the one it is anchored to in order to reproduce the error.
        final View anchor = createViewWithHashCode(col.getContext(), 2);
        anchor.setId(R.id.anchor);

        final View ship = createViewWithHashCode(col.getContext(), 3);
        final CoordinatorLayout.LayoutParams lp = col.generateDefaultLayoutParams();
        lp.setAnchorId(R.id.anchor);

        col.addView(anchor);
        col.addView(ship, lp);

        // Get dependencies immediately to avoid possible call to onMeasure(), since error
        // only happens on first computing of sorted dependencies.
        List<View> dependencySortedChildren = col.getDependencySortedChildren();
        assertThat(dependencySortedChildren, is(Arrays.asList(anchor, ship)));
    }

    @NonNull
    private View createViewWithHashCode(final Context context, final int hashCode) {
        return new View(context) {
            @Override
            public int hashCode() {
                return hashCode;
            }
        };
    }
}
