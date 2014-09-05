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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.Map;

class FragmentTransitionCompat21 {
    public static String getTransitionName(View view) {
        return view.getTransitionName();
    }

    private static void addTransitioningViews(Object transitionObject, final ArrayList<View> views) {
        Transition transition = (Transition) transitionObject;
        int numViews = views.size();
        for (int i = 0; i < numViews; i++) {
            View view = views.get(i);
            transition.addTarget(view);
        }

        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                transition.removeListener(this);
                int numViews = views.size();
                for (int i = 0; i < numViews; i++) {
                    View view = views.get(i);
                    transition.removeTarget(view);
                }
            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
    }

    public static Object captureExitingViews(Object exitTransition, View root) {
        if (exitTransition != null) {
            ArrayList<View> viewList = new ArrayList<>();
            captureTransitioningViews(viewList, root);
            if (viewList.isEmpty()) {
                exitTransition = null;
            } else {
                addTransitioningViews(exitTransition, viewList);
            }
        }
        return exitTransition;
    }

    public static void cleanupHiddenFragments(Object transitionObject,
            final ArrayList<View> hiddenViews) {
        Transition transition = (Transition) transitionObject;
        transition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                transition.removeListener(this);
                int numViews = hiddenViews.size();
                for (int i = 0; i < numViews; i++) {
                    transition.excludeTarget(hiddenViews.get(i), false);
                }
            }

            @Override
            public void onTransitionEnd(Transition transition) {
            }

            @Override
            public void onTransitionCancel(Transition transition) {
            }

            @Override
            public void onTransitionPause(Transition transition) {
            }

            @Override
            public void onTransitionResume(Transition transition) {
            }
        });
    }

    public static void excludeTarget(Object transitionObject, View view, boolean exclude) {
        Transition transition = (Transition) transitionObject;
        transition.excludeTarget(view, exclude);
    }

    public static void beginDelayedTransition(ViewGroup sceneRoot, Object transitionObject) {
        Transition transition = (Transition) transitionObject;
        TransitionManager.beginDelayedTransition(sceneRoot, transition);
    }

    public static void setEpicenter(Object transitionObject, View view) {
        Transition transition = (Transition) transitionObject;
        final Rect epicenter = getBoundsOnScreen(view);

        transition.setEpicenterCallback(new Transition.EpicenterCallback() {
            @Override
            public Rect onGetEpicenter(Transition transition) {
                return epicenter;
            }
        });
    }

    /**
     * Prepares the enter transition by adding a non-existent view to the transition's target list
     * and setting it epicenter callback. By adding a non-existent view to the target list,
     * we can prevent any view from being targeted at the beginning of the transition.
     * We will add to the views before the end state of the transition is captured so that the
     * views will appear. At the start of the transition, we clear the list of targets so that
     * we can restore the state of the transition and use it again.
     */
    public static void prepareEnterTransition(Object enterTransitionObject,
            final View container, final ViewRetriever inFragment, final View nonExistentView,
            EpicenterView epicenterView, Map<String, String> nameOverrides) {
        if (enterTransitionObject != null) {
            final Transition enterTransition = (Transition) enterTransitionObject;
            SetupEnterTransition setup = new SetupEnterTransition(enterTransition, container,
                    inFragment, nameOverrides, nonExistentView);
            enterTransition.addListener(setup);
            setSharedElementEpicenter(enterTransition, epicenterView);
        }
    }

    public static Object mergeTransitions(Object enterTransitionObject,
            Object exitTransitionObject, Object sharedElementTransitionObject,
            boolean allowOverlap) {
        boolean overlap = true;
        Transition enterTransition = (Transition) enterTransitionObject;
        Transition exitTransition = (Transition) exitTransitionObject;
        Transition sharedElementTransition = (Transition) sharedElementTransitionObject;

        if (enterTransition != null && exitTransition != null) {
            overlap = allowOverlap;
        }

        Transition transition;
        if (overlap) {
            transition = mergeTransitions(enterTransition, exitTransition,
                    sharedElementTransition);
        } else {
            TransitionSet staggered = new TransitionSet()
                    .addTransition(exitTransition)
                    .addTransition(enterTransition)
                    .setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
            transition = mergeTransitions(staggered, sharedElementTransition);
        }
        return transition;
    }



    private static void setSharedElementEpicenter(Transition transition,
            final EpicenterView epicenterView) {
        transition.setEpicenterCallback(new Transition.EpicenterCallback() {
            private Rect mEpicenter;

            @Override
            public Rect onGetEpicenter(Transition transition) {
                if (mEpicenter == null && epicenterView.epicenter != null) {
                    mEpicenter = getBoundsOnScreen(epicenterView.epicenter);
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

    public static void findNamedViews(Map<String, View> namedViews, View view) {
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

    /**
     * Handles the renaming of views to handle name overrides and setting the targets for
     * the enter transition.
     */
    public static class SetupEnterTransition implements Transition.TransitionListener,
            ViewTreeObserver.OnPreDrawListener {
        final private ArrayMap<String, View> mRenamedViews = new ArrayMap<>();
        final private ArrayList<View> mEnteringViews = new ArrayList<View>();
        private Map<String, String> mNameOverrides;
        private ViewRetriever mFragmentView;
        private View mSceneRoot;
        private View mNonExistentView;
        private Transition mEnterTransition;

        public SetupEnterTransition(Transition enterTransition, View sceneRoot,
                ViewRetriever fragmentView, Map<String, String> nameOverrides,
                View nonExistentView) {
            mSceneRoot = sceneRoot;
            mNameOverrides = nameOverrides;
            mFragmentView = fragmentView;
            mNonExistentView = nonExistentView;
            mEnterTransition = enterTransition;
            mEnterTransition.addTarget(nonExistentView);
            mSceneRoot.getViewTreeObserver().addOnPreDrawListener(this);
        }

        @Override
        public void onTransitionStart(Transition transition) {
            transition.removeListener(this);
            transition.removeTarget(mNonExistentView);
            int numViews = mEnteringViews.size();
            for (int i = 0; i < numViews; i++) {
                transition.removeTarget(mEnteringViews.get(i));
            }
            transition.removeTarget(mNonExistentView);
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

        @Override
        public boolean onPreDraw() {
            mSceneRoot.getViewTreeObserver().removeOnPreDrawListener(this);
            View fragmentView = mFragmentView.getView();
            if (fragmentView != null) {
                captureTransitioningViews(mEnteringViews, fragmentView);
                int numEnteringViews = mEnteringViews.size();
                for (int i = 0; i < numEnteringViews; i++) {
                    mEnterTransition.addTarget(mEnteringViews.get(i));
                }
                if (!mNameOverrides.isEmpty()) {
                    findNamedViews(mRenamedViews, fragmentView);
                    mRenamedViews.retainAll(mNameOverrides.values());
                    for (Map.Entry<String, String> entry : mNameOverrides.entrySet()) {
                        String to = entry.getValue();
                        View view = mRenamedViews.get(to);
                        if (view != null) {
                            String from = entry.getKey();
                            view.setTransitionName(from);
                        }
                    }
                }
            }
            return true;
        }
    }

    public interface ViewRetriever {
        View getView();
    }

    public static class EpicenterView {
        public View epicenter;
    }

    public static Transition mergeTransitions(Transition... transitions) {
        int count = 0;
        int nonNullIndex = -1;
        for (int i = 0; i < transitions.length; i++) {
            if (transitions[i] != null) {
                count++;
                nonNullIndex = i;
            }
        }

        if (count == 0) {
            return null;
        }

        if (count == 1) {
            return transitions[nonNullIndex];
        }

        TransitionSet transitionSet = new TransitionSet();
        for (int i = 0; i < transitions.length; i++) {
            if (transitions[i] != null) {
                transitionSet.addTransition(transitions[i]);
            }
        }
        return transitionSet;
    }
}
