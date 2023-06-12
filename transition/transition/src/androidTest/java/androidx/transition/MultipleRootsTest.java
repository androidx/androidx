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

package androidx.transition;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.transition.test.R;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class MultipleRootsTest {

    static class Views {
        LinearLayout mColumn;
        LinearLayout mRow1;
        LinearLayout mRow2;
        View mRed;
        View mGreen;
        View mBlue;
    }

    private ActivityScenario<TransitionActivity> prepareScenario(final Views views) {
        final ActivityScenario<TransitionActivity> scenario =
                ActivityScenario.launch(TransitionActivity.class);

        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                final ViewGroup root = activity.getRoot();

                final LayoutInflater layoutInflater = activity.getLayoutInflater();
                final View layout = layoutInflater.inflate(R.layout.multiple_roots, root, false);
                root.addView(
                        layout,
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                );
            }
        });

        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                views.mColumn = activity.findViewById(R.id.column);
                views.mRow1 = activity.findViewById(R.id.row_1);
                views.mRow2 = activity.findViewById(R.id.row_2);
                views.mRed = activity.findViewById(R.id.red);
                views.mGreen = activity.findViewById(R.id.green);
                views.mBlue = activity.findViewById(R.id.blue);

                assertThat(views.mColumn).isNotNull();
                assertThat(views.mRow1).isNotNull();
                assertThat(views.mRow2).isNotNull();
                assertThat(views.mRed).isNotNull();
                assertThat(views.mGreen).isNotNull();
                assertThat(views.mBlue).isNotNull();
            }
        });

        return scenario;
    }

    @Test
    public void nested_innerFirst() {
        final Views views = new Views();
        final ActivityScenario<TransitionActivity> scenario = prepareScenario(views);

        final Transition.TransitionListener innerListener =
                spy(new TransitionListenerAdapter());
        final Transition.TransitionListener outerListener =
                spy(new TransitionListenerAdapter());

        final Fade innerTransition = new Fade();
        innerTransition.setDuration(300);
        innerTransition.addListener(innerListener);
        final Fade outerTransition = new Fade();
        outerTransition.setDuration(50);
        outerTransition.addListener(outerListener);

        // Run a transition on the inner ViewGroup.
        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                TransitionManager.beginDelayedTransition(views.mRow1, innerTransition);
                views.mRed.setVisibility(View.INVISIBLE);
            }
        });

        verify(innerListener, timeout(3000)).onTransitionStart(any(Transition.class));

        // Run a transition on the outer ViewGroup.
        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                TransitionManager.beginDelayedTransition(views.mColumn, outerTransition);
                views.mBlue.setVisibility(View.INVISIBLE);
            }
        });

        verify(outerListener, timeout(3000)).onTransitionStart(any(Transition.class));

        verify(innerListener, timeout(3000)).onTransitionEnd(any(Transition.class));
        verify(outerListener, timeout(3000)).onTransitionEnd(any(Transition.class));

        scenario.close();
    }

    @Test
    public void nested_outerFirst() {
        final Views views = new Views();
        final ActivityScenario<TransitionActivity> scenario = prepareScenario(views);

        final Transition.TransitionListener innerListener =
                spy(new TransitionListenerAdapter());
        final Transition.TransitionListener outerListener =
                spy(new TransitionListenerAdapter());

        final Fade outerTransition = new Fade();
        outerTransition.setDuration(300);
        outerTransition.addListener(outerListener);
        final Fade innerTransition = new Fade();
        innerTransition.setDuration(50);
        innerTransition.addListener(innerListener);

        // Run a transition on the outer ViewGroup.
        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                TransitionManager.beginDelayedTransition(views.mColumn, outerTransition);
                views.mBlue.setVisibility(View.INVISIBLE);
            }
        });

        verify(outerListener, timeout(3000)).onTransitionStart(any(Transition.class));

        // Run a transition on the inner ViewGroup.
        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                TransitionManager.beginDelayedTransition(views.mRow1, innerTransition);
                views.mRed.setVisibility(View.INVISIBLE);
            }
        });

        verify(innerListener, timeout(3000)).onTransitionStart(any(Transition.class));

        verify(innerListener, timeout(3000)).onTransitionEnd(any(Transition.class));
        verify(outerListener, timeout(3000)).onTransitionEnd(any(Transition.class));

        scenario.close();
    }

    @Test
    public void adjacent() {
        final Views views = new Views();
        final ActivityScenario<TransitionActivity> scenario = prepareScenario(views);

        final Transition.TransitionListener row1Listener =
                spy(new TransitionListenerAdapter());
        final Transition.TransitionListener row2Listener =
                spy(new TransitionListenerAdapter());

        final Fade row1Transition = new Fade();
        row1Transition.setDuration(300);
        row1Transition.addListener(row1Listener);
        final Fade row2Transition = new Fade();
        row2Transition.setDuration(50);
        row2Transition.addListener(row2Listener);

        // Run a transition on the first ViewGroup.
        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                TransitionManager.beginDelayedTransition(views.mRow1, row1Transition);
                views.mRed.setVisibility(View.INVISIBLE);
            }
        });

        verify(row1Listener, timeout(3000)).onTransitionStart(any(Transition.class));

        // Run a transition on the second ViewGroup.
        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                TransitionManager.beginDelayedTransition(views.mRow2, row2Transition);
                views.mBlue.setVisibility(View.INVISIBLE);
            }
        });

        verify(row2Listener, timeout(3000)).onTransitionStart(any(Transition.class));

        verify(row2Listener, timeout(3000)).onTransitionEnd(any(Transition.class));
        verify(row1Listener, timeout(3000)).onTransitionEnd(any(Transition.class));

        scenario.close();
    }

    @Test
    public void adjacent_subsequence() {
        final Views views = new Views();
        final ActivityScenario<TransitionActivity> scenario = prepareScenario(views);

        // For Row 1, we run a subsequent transition at the end of the first transition.
        final Transition.TransitionListener row1SecondListener =
                spy(new TransitionListenerAdapter());
        final Fade row1SecondTransition = new Fade();
        row1SecondTransition.setDuration(250);
        row1SecondTransition.addListener(row1SecondListener);

        // The first transition for row 1.
        final Fade row1FirstTransition = new Fade();
        row1FirstTransition.setDuration(150);
        final Transition.TransitionListener row2FirstListener = new TransitionListenerAdapter() {
            @Override
            public void onTransitionEnd(@NonNull @NotNull Transition transition) {
                TransitionManager.beginDelayedTransition(views.mRow1, row1SecondTransition);
                views.mRed.setVisibility(View.VISIBLE);
            }
        };
        row1FirstTransition.addListener(row2FirstListener);

        // Only one transition for row 2.
        final Transition.TransitionListener row2Listener =
                spy(new TransitionListenerAdapter());
        final Slide row2Transition = new Slide();
        row2Transition.setDuration(300);
        row2Transition.addListener(row2Listener);

        // Run the transitions at the same time.
        scenario.onActivity(new ActivityScenario.ActivityAction<TransitionActivity>() {
            @Override
            public void perform(TransitionActivity activity) {
                TransitionManager.beginDelayedTransition(views.mRow1, row1FirstTransition);
                views.mRed.setVisibility(View.INVISIBLE);
                TransitionManager.beginDelayedTransition(views.mRow2, row2Transition);
                views.mBlue.setVisibility(View.INVISIBLE);
            }
        });

        verify(row1SecondListener, timeout(3000)).onTransitionStart(any(Transition.class));
        verify(row2Listener, timeout(3000)).onTransitionStart(any(Transition.class));

        verify(row1SecondListener, timeout(3000)).onTransitionEnd(any(Transition.class));
        verify(row2Listener, timeout(3000)).onTransitionEnd(any(Transition.class));

        scenario.close();
    }
}
