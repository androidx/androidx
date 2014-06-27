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

package android.support.v4.app;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
import android.transition.Transition;
import android.transition.TransitionSet;
import android.transition.TransitionManager;
import android.graphics.Rect;
import android.util.ArrayMap;
import android.transition.TransitionInflater;

class FragmentTransitionCompat21 {

    public static String getTransitionName(View view) {
        return view.getTransitionName();
    }

    public static Object beginTransition(Activity activity, int transitionId, ViewGroup sceneRoot,
            ArrayList<View> hiddenFragmentViews, ArrayList<String> sourceNames,
            ArrayList<String> targetNames) {
        if (transitionId <= 0 || sceneRoot == null) {
            return null;
        }
        TransitionState state = new TransitionState();
        // get Transition scene root and create Transitions
        state.sceneRoot = sceneRoot;
        captureTransitioningViews(state.transitioningViews, state.sceneRoot);

        state.exitTransition = TransitionInflater.from(activity)
                .inflateTransition(transitionId);
        state.sharedElementTransition = TransitionInflater.from(activity)
                .inflateTransition(transitionId);
        state.enterTransition = TransitionInflater.from(activity)
                .inflateTransition(transitionId);
        // Adding a non-existent target view makes sure that the transitions don't target
        // any views by default. They'll only target the views we tell add. If we don't
        // add any, then no views will be targeted.
        View nonExistentView = new View(activity);
        state.enterTransition.addTarget(nonExistentView);
        state.exitTransition.addTarget(nonExistentView);
        state.sharedElementTransition.addTarget(nonExistentView);

        setSharedElementEpicenter(state.enterTransition, state);

        state.excludingTransition = new TransitionSet()
                .addTransition(state.exitTransition)
                .addTransition(state.enterTransition);
        state.excludingTransition.addListener(new ResetNamesListener(state.namedViews));

        if (sourceNames != null) {
            // Map shared elements.
            findNamedViews(state.namedViews, state.sceneRoot);
            state.namedViews.retainAll(sourceNames);
            View epicenterView = state.namedViews.get(sourceNames.get(0));
            if (epicenterView != null) {
                // The epicenter is only the first shared element.
                setEpicenter(state.exitTransition, epicenterView);
                setEpicenter(state.sharedElementTransition, epicenterView);
            }
            state.transitioningViews.removeAll(state.namedViews.values());
            state.excludingTransition.addTransition(state.sharedElementTransition);
            addTransitioningViews(state.sharedElementTransition, state.namedViews.values());
        }

        // Adds the (maybe) exiting views, not including the shared element.
        // If some stay, that's ok.
        addTransitioningViews(state.exitTransition, state.transitioningViews);

        setNameOverrides(state, sourceNames, targetNames);

        // Don't include any subtree in the views that are hidden when capturing the
        // view hierarchy transitions. They should be as if not there.
        excludeHiddenFragments(activity, hiddenFragmentViews, state, true);

        TransitionManager.beginDelayedTransition(state.sceneRoot, state.excludingTransition);
        return state;
    }

    public static void updateTransitionEndState(Activity activity,
            ArrayList<View> shownFragmentViews, ArrayList<View> hiddenFragmentViews,
            Object stateObj, ArrayList<String> names) {
        if (!(stateObj instanceof TransitionState)) {
            return;
        }
        TransitionState state = (TransitionState) stateObj;
        // Find all views that are entering.
        ArrayList<View> enteringViews = new ArrayList<View>();
        captureTransitioningViews(enteringViews, state.sceneRoot);
        enteringViews.removeAll(state.transitioningViews);

        state.namedViews.clear();

        if (names != null) {
            // find all shared elements.
            findNamedViews(state.namedViews, state.sceneRoot);
            state.namedViews.retainAll(names);
            if (!state.namedViews.isEmpty()) {
                enteringViews.removeAll(state.namedViews.values());
                addTransitioningViews(state.sharedElementTransition, state.namedViews.values());
                // now we know the epicenter of the entering transition.
                state.enteringEpicenterView = state.namedViews.get(names.get(0));

                // Change the names of the shared elements temporarily so that the shared element
                // names can match.
                int count = state.nameOverrides.size();
                for (int i = 0; i < count; i++) {
                    String toName = state.nameOverrides.valueAt(i);
                    View view = state.namedViews.get(toName);
                    if (view != null) {
                        view.setTransitionName(state.nameOverrides.keyAt(i));
                    }
                }
            }
        }

        // Add all entering views to the enter transition.
        addTransitioningViews(state.enterTransition, enteringViews);

        // Don't allow capturing state for the newly-hidden fragments.
        excludeHiddenFragments(activity, hiddenFragmentViews, state, false);

        // Allow capturing state for the newly-shown fragments
        includeVisibleFragments(shownFragmentViews, state.excludingTransition);
    }

    private static void addTransitioningViews(Transition transition, Collection<View> views) {
        for (View view : views) {
            transition.addTarget(view);
        }
    }

    private static void excludeHiddenFragments(Activity activity,
            ArrayList<View> hiddenFragmentViews, TransitionState state, boolean forceExclude) {
        for (int i = hiddenFragmentViews.size() - 1; i >= 0; i--) {
            View view = hiddenFragmentViews.get(i);
            if (forceExclude || !state.hiddenViews.contains(view)) {
                state.excludingTransition.excludeTarget(view, true);
                state.hiddenViews.add(view);
            }
        }
        if (forceExclude && state.hiddenViews.isEmpty()) {
            state.excludingTransition.excludeTarget(new View(activity), true);
        }
    }

    private static void includeVisibleFragments(ArrayList<View> shownFragmentViews,
            Transition transition) {
        for (int i = shownFragmentViews.size() - 1; i >= 0; i--) {
            View view = shownFragmentViews.get(i);
            transition.excludeTarget(view, false);
        }
    }

    private static void setEpicenter(Transition transition, View view) {
        final Rect epicenter = getBoundsOnScreen(view);

        transition.setEpicenterCallback(new Transition.EpicenterCallback() {
            @Override
            public Rect onGetEpicenter(Transition transition) {
                return epicenter;
            }
        });
    }

    private static void setSharedElementEpicenter(Transition transition,
            final TransitionState state) {
        transition.setEpicenterCallback(new Transition.EpicenterCallback() {
            private Rect mEpicenter;

            @Override
            public Rect onGetEpicenter(Transition transition) {
                if (mEpicenter == null && state.enteringEpicenterView != null) {
                    mEpicenter = getBoundsOnScreen(state.enteringEpicenterView);
                }
                return mEpicenter;
            }
        });
    }

    private static Rect getBoundsOnScreen(View view) {
        Rect epicenter = new Rect();
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        // not as good as View.getBoundsOnScreen, but that's not public
        epicenter.set(loc[0], loc[1], loc[0] + view.getWidth(), loc[1] + view.getHeight());
        return epicenter;
    }

    private static void setNameOverride(ArrayMap<String, String> overrides, String source,
            String target) {
        for (int index = 0; index < overrides.size(); index++) {
            if (source.equals(overrides.valueAt(index))) {
                overrides.setValueAt(index, target);
                return;
            }
        }
        overrides.put(source, target);
    }

    public static void setNameOverrides(Object stateObj, ArrayList<String> sourceNames,
            ArrayList<String> targetNames) {
        if (sourceNames != null && stateObj instanceof TransitionState) {
            TransitionState state = (TransitionState) stateObj;
            for (int i = 0; i < sourceNames.size(); i++) {
                String source = sourceNames.get(i);
                String target = targetNames.get(i);
                setNameOverride(state.nameOverrides, source, target);
            }
        }
    }

    private static void captureTransitioningViews(ArrayList<View> transitioningViews, View view) {
        if (view.getVisibility() == View.VISIBLE) {
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                if (viewGroup.isTransitionGroup()) {
                    transitioningViews.add(viewGroup);
                } else {
                    int count = viewGroup.getChildCount();
                    for (int i = 0; i < count; i++) {
                        View child = viewGroup.getChildAt(i);
                        captureTransitioningViews(transitioningViews, child);
                    }
                }
            } else {
                transitioningViews.add(view);
            }
        }
    }

    private static void findNamedViews(ArrayMap<String, View> namedViews, View view) {
        if (view.getVisibility() == View.VISIBLE) {
            String transitionName = view.getTransitionName();
            if (transitionName != null) {
                namedViews.put(transitionName, view);
            }
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                int count = viewGroup.getChildCount();
                for (int i = 0; i < count; i++) {
                    View child = viewGroup.getChildAt(i);
                    findNamedViews(namedViews, child);
                }
            }
        }
    }

    private static class TransitionState {
        public ArrayList<View> hiddenViews = new ArrayList<View>();
        public ArrayList<View> transitioningViews = new ArrayList<View>();
        public ArrayMap<String, View> namedViews = new ArrayMap<String, View>();
        public Transition exitTransition;
        public Transition sharedElementTransition;
        public Transition enterTransition;
        public TransitionSet excludingTransition;
        public ViewGroup sceneRoot;
        public View enteringEpicenterView;
        public ArrayMap<String, String> nameOverrides = new ArrayMap<String, String>();
    }

    private static class ResetNamesListener implements Transition.TransitionListener {
        private ArrayMap<String, View> mRenamedViews;

        public ResetNamesListener(ArrayMap<String, View> renamedViews) {
            mRenamedViews = renamedViews;
        }

        @Override
        public void onTransitionStart(Transition transition) {
            transition.removeListener(this);
            int count = mRenamedViews.size();
            for (int i = 0; i < count; i++) {
                View view = mRenamedViews.valueAt(i);
                String name = mRenamedViews.keyAt(i);
                view.setTransitionName(name);
            }
        }

        @Override
        public void onTransitionCancel(Transition transition) {
        }

        @Override
        public void onTransitionEnd(Transition transition) {
        }

        @Override
        public void onTransitionPause(Transition transition) {
        }

        @Override
        public void onTransitionResume(Transition transition) {
        }
    }
}
