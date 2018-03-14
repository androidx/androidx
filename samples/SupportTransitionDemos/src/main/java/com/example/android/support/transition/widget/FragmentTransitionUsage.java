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

package com.example.android.support.transition.widget;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.AutoTransition;
import androidx.transition.Fade;
import androidx.transition.Transition;

import com.example.android.support.transition.R;

/**
 * Demonstrates usage of shared element Transition between Fragments.
 */
public class FragmentTransitionUsage extends TransitionUsageBase {

    private static final String SHARED = "red";

    private static final Transition SHARED_TRANSITION = new AutoTransition();
    private static final Transition NON_SHARED_TRANSITION = new Fade();

    static {
        SHARED_TRANSITION.setDuration(1000);
        SHARED_TRANSITION.setInterpolator(new FastOutSlowInInterpolator());
        NON_SHARED_TRANSITION.setDuration(1000);
        NON_SHARED_TRANSITION.setInterpolator(new FastOutSlowInInterpolator());
    }

    private static final String FRAGMENT_FIRST = "first";
    private static final String FRAGMENT_SECOND = "second";

    @Override
    int getLayoutResId() {
        return R.layout.fragment_transition;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new FirstFragment(), FRAGMENT_FIRST)
                    .setReorderingAllowed(true)
                    .commitNow();
        }
    }

    void showSecond(View sharedElement) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        final FirstFragment first =
                (FirstFragment) fragmentManager.findFragmentByTag(FRAGMENT_FIRST);
        if (first == null) {
            return;
        }
        final SecondFragment second = new SecondFragment();

        fragmentManager.beginTransaction()
                .replace(R.id.container, second, FRAGMENT_SECOND)
                .addToBackStack(null)
                .setReorderingAllowed(true)
                .addSharedElement(sharedElement, SHARED)
                .commit();
    }

    private abstract static class TransitionFragment extends Fragment {

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setSharedElementEnterTransition(SHARED_TRANSITION);
            setSharedElementReturnTransition(SHARED_TRANSITION);
            setExitTransition(NON_SHARED_TRANSITION);
            setEnterTransition(NON_SHARED_TRANSITION);
            setReenterTransition(NON_SHARED_TRANSITION);
            setReturnTransition(NON_SHARED_TRANSITION);
            setAllowEnterTransitionOverlap(true);
            setAllowReturnTransitionOverlap(true);
        }

    }

    /**
     * A {@link Fragment} with red and yellow squares.
     */
    public static class FirstFragment extends TransitionFragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_transition_first, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            final View red = view.findViewById(R.id.red);
            ViewCompat.setTransitionName(red, SHARED);
            view.findViewById(R.id.move).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FragmentTransitionUsage activity = (FragmentTransitionUsage) getActivity();
                    if (activity != null) {
                        activity.showSecond(red);
                    }
                }
            });
        }

    }

    /**
     * A {@link Fragment} with red and blue squares.
     */
    public static class SecondFragment extends TransitionFragment {

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_transition_second, container, false);
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            ViewCompat.setTransitionName(view.findViewById(R.id.red), SHARED);
        }

    }

}
