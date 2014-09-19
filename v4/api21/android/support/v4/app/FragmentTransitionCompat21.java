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

    public static Object cloneTransition(Object transition) {
        if (transition != null) {
            transition = ((Transition)transition).clone();
        }
        return transition;
    }

    public static Object captureExitingViews(Object exitTransition, View root,
            ArrayList<View> viewList, Map<String, View> namedViews) {
        if (exitTransition != null) {
            captureTransitioningViews(viewList, root);
            if (namedViews != null) {
                viewList.removeAll(namedViews.values());
            }
            if (viewList.isEmpty()) {
                exitTransition = null;
            } else {
                addTargets((Transition) exitTransition, viewList);
            }
        }
        return exitTransition;
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
     *
     * <p>The shared element transition maps its shared elements immediately prior to
     *  capturing the final state of the Transition.</p>
     */
    public static void addTransitionTargets(Object enterTransitionObject,
            Object sharedElementTransitionObject, final View container,
            final ViewRetriever inFragment, final View nonExistentView,
            EpicenterView epicenterView, final Map<String, String> nameOverrides,
            final ArrayList<View> enteringViews, final Map<String, View> renamedViews,
            final ArrayList<View> sharedElementTargets) {
        if (enterTransitionObject != null || sharedElementTransitionObject != null) {
            final Transition enterTransition = (Transition) enterTransitionObject;
            if (enterTransition != null) {
                enterTransition.addTarget(nonExistentView);
            }
            if (sharedElementTransitionObject != null) {
                Transition sharedElementTransition = (Transition) sharedElementTransitionObject;
                addTargets(sharedElementTransition, sharedElementTargets);
            }

            if (inFragment != null) {
                container.getViewTreeObserver().addOnPreDrawListener(
                        new ViewTreeObserver.OnPreDrawListener() {
                            public boolean onPreDraw() {
                                container.getViewTreeObserver().removeOnPreDrawListener(this);
                                View fragmentView = inFragment.getView();
                                if (fragmentView != null) {
                                    if (!nameOverrides.isEmpty()) {
                                        findNamedViews(renamedViews, fragmentView);
                                        renamedViews.keySet().retainAll(nameOverrides.values());
                                        for (Map.Entry<String, String> entry : nameOverrides.entrySet()) {
                                            String to = entry.getValue();
                                            View view = renamedViews.get(to);
                                            if (view != null) {
                                                String from = entry.getKey();
                                                view.setTransitionName(from);
                                            }
                                        }
                                    }
                                    if (enterTransition != null) {
                                        captureTransitioningViews(enteringViews, fragmentView);
                                        enteringViews.removeAll(renamedViews.values());
                                        addTargets(enterTransition, enteringViews);
                                    }
                                }
                                return true;
                            }
                        });
            }
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

        // Wrap the transitions. Explicit targets like in enter and exit will cause the
        // views to be targeted regardless of excluded views. If that happens, then the
        // excluded fragments views (hidden fragments) will still be in the transition.

        Transition transition;
        if (overlap) {
            // Regular transition -- do it all together
            TransitionSet transitionSet = new TransitionSet();
            if (enterTransition != null) {
                transitionSet.addTransition(enterTransition);
            }
            if (exitTransition != null) {
                transitionSet.addTransition(exitTransition);
            }
            if (sharedElementTransition != null) {
                transitionSet.addTransition(sharedElementTransition);
            }
            transition = transitionSet;
        } else {
            // First do exit, then enter, but allow shared element transition to happen
            // during both.
            Transition staggered = null;
            if (exitTransition != null && enterTransition != null) {
                staggered = new TransitionSet()
                        .addTransition(exitTransition)
                        .addTransition(enterTransition)
                        .setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
            } else if (exitTransition != null) {
                staggered = exitTransition;
            } else if (enterTransition != null) {
                staggered = enterTransition;
            }
            if (sharedElementTransition != null) {
                TransitionSet together = new TransitionSet();
                if (staggered != null) {
                    together.addTransition(staggered);
                }
                together.addTransition(sharedElementTransition);
                transition = together;
            } else {
                transition = staggered;
            }
        }
        return transition;
    }



    private static void setSharedElementEpicenter(Transition transition,
            final EpicenterView epicenterView) {
        if (transition != null) {
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

    public static void cleanupTransitions(final View sceneRoot, final View nonExistentView,
            Object enterTransitionObject, final ArrayList<View> enteringViews,
            Object exitTransitionObject, final ArrayList<View> exitingViews,
            Object sharedElementTransitionObject, final ArrayList<View> sharedElementTargets,
            Object overallTransitionObject, final ArrayList<View> hiddenViews,
            final Map<String, View> renamedViews) {
        final Transition enterTransition = (Transition) enterTransitionObject;
        final Transition exitTransition = (Transition) exitTransitionObject;
        final Transition sharedElementTransition = (Transition) sharedElementTransitionObject;
        final Transition overallTransition = (Transition) overallTransitionObject;
        if (overallTransition != null) {
            sceneRoot.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                public boolean onPreDraw() {
                    sceneRoot.getViewTreeObserver().removeOnPreDrawListener(this);
                    if (enterTransition != null) {
                        enterTransition.removeTarget(nonExistentView);
                        removeTargets(enterTransition, enteringViews);
                    }
                    if (exitTransition != null) {
                        removeTargets(exitTransition, exitingViews);
                    }
                    if (sharedElementTransition != null) {
                        removeTargets(sharedElementTransition, sharedElementTargets);
                    }
                    for (Map.Entry<String, View> entry : renamedViews.entrySet()) {
                        View view = entry.getValue();
                        String name = entry.getKey();
                        view.setTransitionName(name);
                    }
                    int numViews = hiddenViews.size();
                    for (int i = 0; i < numViews; i++) {
                        overallTransition.excludeTarget(hiddenViews.get(i), false);
                    }
                    overallTransition.excludeTarget(nonExistentView, false);
                    return true;
                }
            });
        }
    }

    public static void removeTargets(Object transitionObject, ArrayList<View> views) {
        Transition transition = (Transition) transitionObject;
        int numViews = views.size();
        for (int i = 0; i < numViews; i++) {
            transition.removeTarget(views.get(i));
        }
    }

    public static void addTargets(Object transitionObject, ArrayList<View> views) {
        Transition transition = (Transition) transitionObject;
        int numViews = views.size();
        for (int i = 0; i < numViews; i++) {
            transition.addTarget(views.get(i));
        }
    }

    public interface ViewRetriever {
        View getView();
    }

    public static class EpicenterView {
        public View epicenter;
    }
}
