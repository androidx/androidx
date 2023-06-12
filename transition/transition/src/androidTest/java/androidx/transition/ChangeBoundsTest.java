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

package androidx.transition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.os.BuildCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.transition.test.R;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@LargeTest
public class ChangeBoundsTest extends BaseTransitionTest {

    @Override
    Transition createTransition() {
        final ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setDuration(400);
        changeBounds.setInterpolator(new LinearInterpolator());
        return changeBounds;
    }

    @Test
    public void testResizeClip() {
        ChangeBounds changeBounds = (ChangeBounds) mTransition;
        assertThat(changeBounds.getResizeClip(), is(false));
        changeBounds.setResizeClip(true);
        assertThat(changeBounds.getResizeClip(), is(true));
    }

    @Test
    public void testBasic() throws Throwable {
        enterScene(R.layout.scene1);
        final ViewHolder startHolder = new ViewHolder(rule.getActivity());
        assertThat(startHolder.red, is(atTop()));
        assertThat(startHolder.green, is(below(startHolder.red)));
        startTransition(R.layout.scene6);
        waitForEnd();
        final ViewHolder endHolder = new ViewHolder(rule.getActivity());
        assertThat(endHolder.green, is(atTop()));
        assertThat(endHolder.red, is(below(endHolder.green)));
    }

    @UiThreadTest
    @Test
    public void testApplyingBounds() {
        View view = new View(rule.getActivity());

        ViewUtils.setLeftTopRightBottom(view, 10, 20, 30, 40);

        assertThat(view.getLeft(), is(10));
        assertThat(view.getTop(), is(20));
        assertThat(view.getRight(), is(30));
        assertThat(view.getBottom(), is(40));
    }

    @Test
    public void testSuppressLayoutWhileAnimating() throws Throwable {
        if (Build.VERSION.SDK_INT < 18) {
            // prior Android 4.3 suppressLayout port has another implementation which is
            // harder to test
            return;
        }
        final TestSuppressLayout suppressLayout = new TestSuppressLayout(rule.getActivity());
        final View testView = new View(rule.getActivity());
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRoot.addView(suppressLayout);
                suppressLayout.addView(testView, new FrameLayout.LayoutParams(1, 1));
            }
        });
        rule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TransitionManager.beginDelayedTransition(suppressLayout, mTransition);
                testView.setLayoutParams(new FrameLayout.LayoutParams(2, 2));
                suppressLayout.expectNewValue(true);
            }
        });
        waitForStart();
        suppressLayout.ensureExpectedValueApplied();

        suppressLayout.expectNewValue(false);
        waitForEnd();
        suppressLayout.ensureExpectedValueApplied();
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingChangeBoundsNoClip() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(100, 100));
        });

        final View view = viewArr[0];
        ViewGroup parent = (ViewGroup) view.getParent();

        rule.runOnUiThread(() -> {
            assertEquals(100, view.getWidth());
            assertEquals(100, view.getHeight());
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 200;
            layoutParams.height = 300;
            view.setLayoutParams(layoutParams);
        });
        final TransitionSeekController seekController = seekControllerArr[0];
        CountDownLatch endLatch = new CountDownLatch(1);

        rule.runOnUiThread(() -> {
            assertEquals(100, view.getWidth());
            assertEquals(100, view.getHeight());
            assertTrue(parent.isLayoutSuppressed());

            // Seek past the always there transition before the change bounds
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(100, view.getWidth());
            assertEquals(100, view.getHeight());
            assertTrue(parent.isLayoutSuppressed());

            // Seek to half through the change bounds
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(150, view.getWidth());
            assertEquals(200, view.getHeight());
            assertTrue(parent.isLayoutSuppressed());

            // Seek past the ChangeBounds
            seekController.setCurrentPlayTimeMillis(800);
            assertEquals(200, view.getWidth());
            assertEquals(300, view.getHeight());
            assertTrue(parent.isLayoutSuppressed());

            // Seek back to half through the change bounds
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(150, view.getWidth());
            assertEquals(200, view.getHeight());
            assertTrue(parent.isLayoutSuppressed());

            // Seek before the change bounds:
            seekController.setCurrentPlayTimeMillis(250);
            assertEquals(100, view.getWidth());
            assertEquals(100, view.getHeight());
            assertTrue(parent.isLayoutSuppressed());

            seekController.setCurrentPlayTimeMillis(450);
            ChangeBounds returnTransition = new ChangeBounds();
            returnTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    endLatch.countDown();
                }
            });
            TransitionManager.beginDelayedTransition(mRoot, returnTransition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 100;
            layoutParams.height = 100;
            view.setLayoutParams(layoutParams);
        });

        rule.runOnUiThread(() -> {
            // It should start from 150x200 through and head toward 100x100
            assertTrue(150 >= view.getWidth());
            assertTrue(200 >= view.getHeight());
        });

        assertTrue(endLatch.await(3, TimeUnit.SECONDS));
        rule.runOnUiThread(() -> {
            assertFalse(parent.isLayoutSuppressed());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekingChangeBoundsWithClip() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setResizeClip(true);
        transition.addTransition(changeBounds);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(400, 300));
        });

        final View view = viewArr[0];
        ViewGroup parent = (ViewGroup) view.getParent();

        rule.runOnUiThread(() -> {
            assertEquals(400, view.getWidth());
            assertEquals(300, view.getHeight());
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 200;
            view.setLayoutParams(layoutParams);
        });
        final TransitionSeekController seekController = seekControllerArr[0];
        CountDownLatch endLatch = new CountDownLatch(1);

        rule.runOnUiThread(() -> {
            assertEquals(400, view.getWidth());
            assertEquals(300, view.getHeight());
            assertEquals(new Rect(0, 0, 400, 300), view.getClipBounds());
            assertTrue(parent.isLayoutSuppressed());

            // Seek past the always there transition before the change bounds
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(400, view.getWidth());
            assertEquals(300, view.getHeight());
            assertEquals(new Rect(0, 0, 400, 300), view.getClipBounds());
            assertTrue(parent.isLayoutSuppressed());

            // Seek to half through the change bounds
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(400, view.getWidth());
            assertEquals(300, view.getHeight());
            assertEquals(new Rect(0, 0, 350, 250), view.getClipBounds());
            assertTrue(parent.isLayoutSuppressed());

            // Seek past the ChangeBounds
            seekController.setCurrentPlayTimeMillis(800);
            assertEquals(300, view.getWidth());
            assertEquals(200, view.getHeight());
            assertNull(view.getClipBounds());
            assertTrue(parent.isLayoutSuppressed());

            // Seek back to half through the change bounds
            seekController.setCurrentPlayTimeMillis(450);
            assertEquals(400, view.getWidth());
            assertEquals(300, view.getHeight());
            assertEquals(new Rect(0, 0, 350, 250), view.getClipBounds());
            assertTrue(parent.isLayoutSuppressed());

            // Seek before the change bounds:
            seekController.setCurrentPlayTimeMillis(250);
            assertEquals(400, view.getWidth());
            assertEquals(300, view.getHeight());
            assertNull(view.getClipBounds());
            assertTrue(parent.isLayoutSuppressed());

            seekController.setCurrentPlayTimeMillis(450);
            ChangeBounds returnTransition = new ChangeBounds();
            returnTransition.setResizeClip(true);
            returnTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    endLatch.countDown();
                }
            });
            TransitionManager.beginDelayedTransition(mRoot, returnTransition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 200;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });

        rule.runOnUiThread(() -> {
            // It should start from 400x500, clipped to 350x250
            assertEquals(400, view.getWidth());
            assertEquals(500, view.getHeight());
            assertTrue(view.getClipBounds().width() <= 350);
            assertTrue(view.getClipBounds().height() >= 250);
        });

        assertTrue(endLatch.await(3, TimeUnit.SECONDS));
        rule.runOnUiThread(() -> {
            assertEquals(200, view.getWidth());
            assertEquals(500, view.getHeight());
            assertFalse(parent.isLayoutSuppressed());
            assertNull(view.getClipBounds());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void interruptedBeforeStartNoClip() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        ChangeBounds changeBounds = new ChangeBounds();
        transition.addTransition(changeBounds);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(400, 300));
        });

        final View view = viewArr[0];

        rule.runOnUiThread(() -> {
            TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });

        rule.runOnUiThread(() -> {
            ChangeBounds change = new ChangeBounds();
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, change);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 100;
            layoutParams.height = 150;
            view.setLayoutParams(layoutParams);
        });

        final TransitionSeekController seekController = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            // It should start from 400x300
            assertEquals(400, view.getWidth());
            assertEquals(300, view.getHeight());

            // go halfway through
            seekController.setCurrentPlayTimeMillis(150);
            assertEquals(250, view.getWidth());
            assertEquals(225, view.getHeight());

            // skip to the end
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(100, view.getWidth());
            assertEquals(150, view.getHeight());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void interruptedBeforeStartWithClip() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setResizeClip(true);
        transition.addTransition(changeBounds);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(400, 300));
        });

        final View view = viewArr[0];

        rule.runOnUiThread(() -> {
            TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });

        rule.runOnUiThread(() -> {
            ChangeBounds change = new ChangeBounds();
            change.setResizeClip(true);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, change);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 600;
            layoutParams.height = 150;
            view.setLayoutParams(layoutParams);
        });

        final TransitionSeekController seekController = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            // It should start from 600x500, clipped to 400x300
            assertEquals(600, view.getWidth());
            assertEquals(500, view.getHeight());
            assertEquals(400, view.getClipBounds().width());
            assertEquals(300, view.getClipBounds().height());

            // go halfway through
            seekController.setCurrentPlayTimeMillis(150);
            assertEquals(500, view.getClipBounds().width());
            assertEquals(225, view.getClipBounds().height());

            // skip to the end
            seekController.setCurrentPlayTimeMillis(300);
            assertEquals(600, view.getWidth());
            assertEquals(150, view.getHeight());
            assertNull(view.getClipBounds());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void interruptedAfterEndNoClip() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        ChangeBounds changeBounds = new ChangeBounds();
        transition.addTransition(changeBounds);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(400, 300));
        });

        final View view = viewArr[0];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });
        TransitionSeekController seekController1 = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            seekController1.setCurrentPlayTimeMillis(800);
            ChangeBounds change = new ChangeBounds();
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, change);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 100;
            layoutParams.height = 150;
            view.setLayoutParams(layoutParams);
        });

        final TransitionSeekController seekController2 = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            // It should start from 300x500
            assertEquals(300, view.getWidth());
            assertEquals(500, view.getHeight());

            // go halfway through
            seekController2.setCurrentPlayTimeMillis(150);
            assertEquals(200, view.getWidth());
            assertEquals(325, view.getHeight());

            // skip to the end
            seekController2.setCurrentPlayTimeMillis(300);
            assertEquals(100, view.getWidth());
            assertEquals(150, view.getHeight());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void interruptedAfterEndWithClip() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }
        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setResizeClip(true);
        transition.addTransition(changeBounds);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(400, 300));
        });

        final View view = viewArr[0];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });
        TransitionSeekController seekController1 = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            seekController1.setCurrentPlayTimeMillis(800);
            ChangeBounds change = new ChangeBounds();
            change.setResizeClip(true);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, change);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 100;
            layoutParams.height = 150;
            view.setLayoutParams(layoutParams);
        });

        final TransitionSeekController seekController2 = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            // It should start from 300x500, clipped to 300x500
            assertEquals(300, view.getWidth());
            assertEquals(500, view.getHeight());
            assertEquals(300, view.getClipBounds().width());
            assertEquals(500, view.getClipBounds().height());

            // go halfway through
            seekController2.setCurrentPlayTimeMillis(150);
            assertEquals(200, view.getClipBounds().width());
            assertEquals(325, view.getClipBounds().height());

            // skip to the end
            seekController2.setCurrentPlayTimeMillis(300);
            assertEquals(100, view.getWidth());
            assertEquals(150, view.getHeight());
            assertNull(view.getClipBounds());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void startTransitionAfterSeeking() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }

        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        ChangeBounds changeBounds = new ChangeBounds();
        changeBounds.setResizeClip(true);
        transition.addTransition(changeBounds);
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(400, 300));
        });

        final View view = viewArr[0];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });
        TransitionSeekController seekController1 = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            seekController1.setCurrentPlayTimeMillis(900);
            seekController1.setCurrentPlayTimeMillis(0);

            ChangeBounds change = new ChangeBounds();
            change.setResizeClip(true);
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, change);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 100;
            layoutParams.height = 150;
            view.setLayoutParams(layoutParams);
        });

        rule.runOnUiThread(() -> {
            assertEquals(400, view.getClipBounds().width());
            assertEquals(300, view.getClipBounds().height());
        });
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    @Test
    public void seekNoChange() throws Throwable {
        if (!BuildCompat.isAtLeastU()) {
            return; // only supported on U+
        }

        final TransitionActivity activity = rule.getActivity();
        TransitionSeekController[] seekControllerArr = new TransitionSeekController[1];

        TransitionSet transition = new TransitionSet();
        transition.addTransition(new AlwaysTransition("before"));
        transition.addTransition(new ChangeBounds());
        transition.addTransition(new AlwaysTransition("after"));
        transition.setOrdering(TransitionSet.ORDERING_SEQUENTIAL);

        View[] viewArr = new View[1];

        rule.runOnUiThread(() -> {
            viewArr[0] = new View(activity);
            mRoot.addView(viewArr[0], new ViewGroup.LayoutParams(400, 300));
        });

        final View view = viewArr[0];

        rule.runOnUiThread(() -> {
            seekControllerArr[0] = TransitionManager.controlDelayedTransition(mRoot, transition);
            TransitionManager.controlDelayedTransition(mRoot, transition);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });
        TransitionSeekController seekController1 = seekControllerArr[0];

        rule.runOnUiThread(() -> {
            seekController1.setCurrentPlayTimeMillis(450);
            seekControllerArr[0] =
                    TransitionManager.controlDelayedTransition(mRoot, new ChangeBounds());
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.width = 300;
            layoutParams.height = 500;
            view.setLayoutParams(layoutParams);
        });

        rule.runOnUiThread(() -> {
            assertEquals(0, seekControllerArr[0].getDurationMillis());
            assertEquals(350, view.getWidth());
            assertEquals(400, view.getHeight());

            // First seek controls the transition
            seekController1.setCurrentPlayTimeMillis(900);
            assertEquals(300, view.getWidth());
            assertEquals(500, view.getHeight());
        });
    }

    private static class TestSuppressLayout extends FrameLayout {

        private boolean mExpectedSuppressLayout;
        private Boolean mActualSuppressLayout;

        private TestSuppressLayout(@NonNull Context context) {
            super(context);
        }

        void expectNewValue(boolean frozen) {
            mExpectedSuppressLayout = frozen;
        }

        void ensureExpectedValueApplied() {
            assertNotNull(mActualSuppressLayout);
            assertEquals(mExpectedSuppressLayout, mActualSuppressLayout);
            mActualSuppressLayout = null;
        }

        // Called via reflection
        public void suppressLayout(boolean suppress) {
            mActualSuppressLayout = suppress;
        }
    }

    private static TypeSafeMatcher<View> atTop() {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                return view.getTop() == 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is placed at the top of its parent");
            }
        };
    }

    private static TypeSafeMatcher<View> below(final View other) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return other.getBottom() == item.getTop();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is placed below the specified view");
            }
        };
    }

    private static class ViewHolder {

        public final View red;
        public final View green;

        ViewHolder(TransitionActivity activity) {
            red = activity.findViewById(R.id.redSquare);
            green = activity.findViewById(R.id.greenSquare);
        }
    }

}
