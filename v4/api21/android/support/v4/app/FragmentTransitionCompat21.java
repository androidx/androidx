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

import android.graphics.Rect;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.ArrayList;
import java.util.List;
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
            ArrayList<View> viewList, Map<String, View> namedViews, View nonExistentView) {
        if (exitTransition != null) {
            captureTransitioningViews(viewList, root);
            if (namedViews != null) {
                viewList.removeAll(namedViews.values());
            }
            if (viewList.isEmpty()) {
                exitTransition = null;
            } else {
                viewList.add(nonExistentView);
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

    public static Object wrapSharedElementTransition(Object transitionObj) {
        if (transitionObj == null) {
            return null;
        }
        Transition transition = (Transition) transitionObj;
        if (transition == null) {
            return null;
        }
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition(transition);
        return transitionSet;
    }

    private static void excludeViews(Transition transition, Transition fromTransition,
            ArrayList<View> views, boolean exclude) {
        if (transition != null) {
            final int viewCount = fromTransition == null ? 0 : views.size();
            for (int i = 0; i < viewCount; i++) {
                transition.excludeTarget(views.get(i), exclude);
            }
        }
    }

    /**
     * Exclude (or remove the exclude) of shared element views from the enter and exit transitions.
     *
     * @param enterTransitionObj The enter transition
     * @param exitTransitionObj The exit transition
     * @param sharedElementTransitionObj The shared element transition
     * @param views The shared element target views.
     * @param exclude <code>true</code> to exclude or <code>false</code> to remove the excluded
     *                views.
     */
    public static void excludeSharedElementViews(Object enterTransitionObj,
            Object exitTransitionObj, Object sharedElementTransitionObj, ArrayList<View> views,
            boolean exclude) {
        Transition enterTransition = (Transition) enterTransitionObj;
        Transition exitTransition = (Transition) exitTransitionObj;
        Transition sharedElementTransition = (Transition) sharedElementTransitionObj;
        excludeViews(enterTransition, sharedElementTransition, views, exclude);
        excludeViews(exitTransition, sharedElementTransition, views, exclude);
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
            Object sharedElementTransitionObject, Object exitTransitionObject, final View container,
            final ViewRetriever inFragment, final View nonExistentView,
            EpicenterView epicenterView, final Map<String, String> nameOverrides,
            final ArrayList<View> enteringViews, final ArrayList<View> exitingViews,
            final Map<String, View> namedViews, final Map<String, View> renamedViews,
            final ArrayList<View> sharedElementTargets) {
        final Transition enterTransition = (Transition) enterTransitionObject;
        final Transition exitTransition = (Transition) exitTransitionObject;
        final Transition sharedElementTransition = (Transition) sharedElementTransitionObject;
        excludeViews(enterTransition, exitTransition, exitingViews, true);
        if (enterTransitionObject != null || sharedElementTransitionObject != null) {
            if (enterTransition != null) {
                enterTransition.addTarget(nonExistentView);
            }
            if (sharedElementTransitionObject != null) {
                setSharedElementTargets(sharedElementTransition, nonExistentView,
                        namedViews, sharedElementTargets);
                excludeViews(enterTransition, sharedElementTransition, sharedElementTargets, true);
                excludeViews(exitTransition, sharedElementTransition, sharedElementTargets, true);
            }

            container.getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        public boolean onPreDraw() {
                            container.getViewTreeObserver().removeOnPreDrawListener(this);
                            if (enterTransition != null) {
                                enterTransition.removeTarget(nonExistentView);
                            }
                            if (inFragment != null) {
                                View fragmentView = inFragment.getView();
                                if (fragmentView != null) {
                                    if (!nameOverrides.isEmpty()) {
                                        findNamedViews(renamedViews, fragmentView);
                                        renamedViews.keySet().retainAll(nameOverrides.values());
                                        for (Map.Entry<String, String> entry : nameOverrides
                                                .entrySet()) {
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
                                        enteringViews.add(nonExistentView);
                                        addTargets(enterTransition, enteringViews);
                                    }
                                }
                            }
                            excludeViews(exitTransition, enterTransition, enteringViews, true);

                            return true;
                        }
                    });
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

    /**
     * Finds all children of the shared elements and sets the wrapping TransitionSet
     * targets to point to those. It also limits transitions that have no targets to the
     * specific shared elements. This allows developers to target child views of the
     * shared elements specifically, but this doesn't happen by default.
     */
    public static void setSharedElementTargets(Object transitionObj,
            View nonExistentView, Map<String, View> namedViews,
            ArrayList<View> sharedElementTargets) {
        TransitionSet transition = (TransitionSet) transitionObj;
        sharedElementTargets.clear();
        sharedElementTargets.addAll(namedViews.values());

        final List<View> views = transition.getTargets();
        views.clear();
        final int count = sharedElementTargets.size();
        for (int i = 0; i < count; i++) {
            final View view = sharedElementTargets.get(i);
            bfsAddViewChildren(views, view);
        }
        sharedElementTargets.add(nonExistentView);
        addTargets(transition, sharedElementTargets);
    }

    /**
     * Uses a breadth-first scheme to add startView and all of its children to views.
     * It won't add a child if it is already in views.
     */
    private static void bfsAddViewChildren(final List<View> views, final View startView) {
        final int startIndex = views.size();
        if (containedBeforeIndex(views, startView, startIndex)) {
            return; // This child is already in the list, so all its children are also.
        }
        views.add(startView);
        for (int index = startIndex; index < views.size(); index++) {
            final View view = views.get(index);
            if (view instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) view;
                final int childCount =  viewGroup.getChildCount();
                for (int childIndex = 0; childIndex < childCount; childIndex++) {
                    final View child = viewGroup.getChildAt(childIndex);
                    if (!containedBeforeIndex(views, child, startIndex)) {
                        views.add(child);
                    }
                }
            }
        }
    }

    /**
     * Does a linear search through views for view, limited to maxIndex.
     */
    private static boolean containedBeforeIndex(final List<View> views, final View view,
            final int maxIndex) {
        for (int i = 0; i < maxIndex; i++) {
            if (views.get(i) == view) {
                return true;
            }
        }
        return false;
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
                        removeTargets(enterTransition, enteringViews);
                        excludeViews(enterTransition, exitTransition, exitingViews, false);
                        excludeViews(enterTransition, sharedElementTransition, sharedElementTargets,
                                false);
                    }
                    if (exitTransition != null) {
                        removeTargets(exitTransition, exitingViews);
                        excludeViews(exitTransition, enterTransition, enteringViews, false);
                        excludeViews(exitTransition, sharedElementTransition, sharedElementTargets,
                                false);
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

    /**
     * This method removes the views from transitions that target ONLY those views.
     * The views list should match those added in addTargets and should contain
     * one view that is not in the view hierarchy (state.nonExistentView).
     */
    public static void removeTargets(Object transitionObject, ArrayList<View> views) {
        Transition transition = (Transition) transitionObject;
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                removeTargets(child, views);
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (targets != null && targets.size() == views.size() &&
                    targets.containsAll(views)) {
                // We have an exact match. We must have added these earlier in addTargets
                for (int i = views.size() - 1; i >= 0; i--) {
                    transition.removeTarget(views.get(i));
                }
            }
        }
    }

    /**
     * This method adds views as targets to the transition, but only if the transition
     * doesn't already have a target. It is best for views to contain one View object
     * that does not exist in the view hierarchy (state.nonExistentView) so that
     * when they are removed later, a list match will suffice to remove the targets.
     * Otherwise, if you happened to have targeted the exact views for the transition,
     * the removeTargets call will remove them unexpectedly.
     */
    public static void addTargets(Object transitionObject, ArrayList<View> views) {
        Transition transition = (Transition) transitionObject;
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                addTargets(child, views);
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (isNullOrEmpty(targets)) {
                // We can just add the target views
                int numViews = views.size();
                for (int i = 0; i < numViews; i++) {
                    transition.addTarget(views.get(i));
                }
            }
        }
    }

    private static boolean hasSimpleTarget(Transition transition) {
        return !isNullOrEmpty(transition.getTargetIds()) ||
                !isNullOrEmpty(transition.getTargetNames()) ||
                !isNullOrEmpty(transition.getTargetTypes());
    }

    private static boolean isNullOrEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public interface ViewRetriever {
        View getView();
    }

    public static class EpicenterView {
        public View epicenter;
    }
}
