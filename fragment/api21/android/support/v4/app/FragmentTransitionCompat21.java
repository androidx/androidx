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

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.support.annotation.RequiresApi;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiresApi(21)
@TargetApi(21)
class FragmentTransitionCompat21 {

    /**
     * Returns a clone of a transition or null if it is null
     */
    public static Object cloneTransition(Object transition) {
        Transition copy = null;
        if (transition != null) {
            copy = ((Transition) transition).clone();
        }
        return copy;
    }

    /**
     * Wraps a transition in a TransitionSet and returns the set. If transition is null, null is
     * returned.
     */
    public static Object wrapTransitionInSet(Object transition) {
        if (transition == null) {
            return null;
        }
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition((Transition) transition);
        return transitionSet;
    }

    /**
     * Finds all children of the shared elements and sets the wrapping TransitionSet
     * targets to point to those. It also limits transitions that have no targets to the
     * specific shared elements. This allows developers to target child views of the
     * shared elements specifically, but this doesn't happen by default.
     */
    public static void setSharedElementTargets(Object transitionObj,
            View nonExistentView, ArrayList<View> sharedViews) {
        TransitionSet transition = (TransitionSet) transitionObj;
        final List<View> views = transition.getTargets();
        views.clear();
        final int count = sharedViews.size();
        for (int i = 0; i < count; i++) {
            final View view = sharedViews.get(i);
            bfsAddViewChildren(views, view);
        }
        views.add(nonExistentView);
        sharedViews.add(nonExistentView);
        addTargets(transition, sharedViews);
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

    /**
     * Sets a transition epicenter to the rectangle of a given View.
     */
    public static void setEpicenter(Object transitionObj, View view) {
        if (view != null) {
            Transition transition = (Transition) transitionObj;
            final Rect epicenter = new Rect();
            getBoundsOnScreen(view, epicenter);

            transition.setEpicenterCallback(new Transition.EpicenterCallback() {
                @Override
                public Rect onGetEpicenter(Transition transition) {
                    return epicenter;
                }
            });
        }
    }

    /**
     * Replacement for view.getBoundsOnScreen because that is not public. This returns a rect
     * containing the bounds relative to the screen that the view is in.
     */
    public static void getBoundsOnScreen(View view, Rect epicenter) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        epicenter.set(loc[0], loc[1], loc[0] + view.getWidth(), loc[1] + view.getHeight());
    }

    /**
     * This method adds views as targets to the transition, but only if the transition
     * doesn't already have a target. It is best for views to contain one View object
     * that does not exist in the view hierarchy (state.nonExistentView) so that
     * when they are removed later, a list match will suffice to remove the targets.
     * Otherwise, if you happened to have targeted the exact views for the transition,
     * the replaceTargets call will remove them unexpectedly.
     */
    public static void addTargets(Object transitionObj, ArrayList<View> views) {
        Transition transition = (Transition) transitionObj;
        if (transition == null) {
            return;
        }
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

    /**
     * Returns true if there are any targets based on ID, transition or type.
     */
    private static boolean hasSimpleTarget(Transition transition) {
        return !isNullOrEmpty(transition.getTargetIds())
                || !isNullOrEmpty(transition.getTargetNames())
                || !isNullOrEmpty(transition.getTargetTypes());
    }

    /**
     * Simple utility to detect if a list is null or has no elements.
     */
    private static boolean isNullOrEmpty(List list) {
        return list == null || list.isEmpty();
    }

    /**
     * Creates a TransitionSet that plays all passed transitions together. Any null
     * transitions passed will not be added to the set. If all are null, then an empty
     * TransitionSet will be returned.
     */
    public static Object mergeTransitionsTogether(Object transition1, Object transition2,
            Object transition3) {
        TransitionSet transitionSet = new TransitionSet();
        if (transition1 != null) {
            transitionSet.addTransition((Transition) transition1);
        }
        if (transition2 != null) {
            transitionSet.addTransition((Transition) transition2);
        }
        if (transition3 != null) {
            transitionSet.addTransition((Transition) transition3);
        }
        return transitionSet;
    }

    /**
     * After the transition completes, the fragment's view is set to GONE and the exiting
     * views are set to VISIBLE.
     */
    public static void scheduleHideFragmentView(Object exitTransitionObj, final View fragmentView,
            final ArrayList<View> exitingViews) {
        Transition exitTransition = (Transition) exitTransitionObj;
        exitTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
            }

            @Override
            public void onTransitionEnd(Transition transition) {
                transition.removeListener(this);
                fragmentView.setVisibility(View.GONE);
                final int numViews = exitingViews.size();
                for (int i = 0; i < numViews; i++) {
                    exitingViews.get(i).setVisibility(View.VISIBLE);
                }
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

    /**
     * Combines enter, exit, and shared element transition so that they play in the proper
     * sequence. First the exit transition plays along with the shared element transition.
     * When the exit transition completes, the enter transition starts. The shared element
     * transition can continue running while the enter transition plays.
     *
     * @return A TransitionSet with all of enter, exit, and shared element transitions in
     * it (modulo null values), ordered such that they play in the proper sequence.
     */
    public static Object mergeTransitionsInSequence(Object exitTransitionObj,
            Object enterTransitionObj, Object sharedElementTransitionObj) {
        // First do exit, then enter, but allow shared element transition to happen
        // during both.
        Transition staggered = null;
        final Transition exitTransition = (Transition) exitTransitionObj;
        final Transition enterTransition = (Transition) enterTransitionObj;
        final Transition sharedElementTransition = (Transition) sharedElementTransitionObj;
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
            return together;
        } else {
            return staggered;
        }
    }

    /**
     * Calls {@link TransitionManager#beginDelayedTransition(ViewGroup, Transition)}.
     */
    public static void beginDelayedTransition(ViewGroup sceneRoot, Object transition) {
        TransitionManager.beginDelayedTransition(sceneRoot, (Transition) transition);
    }

    /**
     * Prepares for setting the shared element names by gathering the names of the incoming
     * shared elements and clearing them. {@link #setNameOverridesOptimized(View, ArrayList,
     * ArrayList, ArrayList, Map)} must be called after this to complete setting the shared element
     * name overrides. This must be called before
     * {@link #beginDelayedTransition(ViewGroup, Object)}.
     */
    public static ArrayList<String> prepareSetNameOverridesOptimized(
            final ArrayList<View> sharedElementsIn) {
        final ArrayList<String> names = new ArrayList<>();
        final int numSharedElements = sharedElementsIn.size();
        for (int i = 0; i < numSharedElements; i++) {
            final View view = sharedElementsIn.get(i);
            names.add(view.getTransitionName());
            view.setTransitionName(null);
        }
        return names;
    }

    /**
     * Changes the shared element names for the incoming shared eleemnts to match those of the
     * outgoing shared elements. This also temporarily clears the shared element names of the
     * outgoing shared elements. Must be called after
     * {@link #beginDelayedTransition(ViewGroup, Object)}.
     */
    public static void setNameOverridesOptimized(final View sceneRoot,
            final ArrayList<View> sharedElementsOut, final ArrayList<View> sharedElementsIn,
            final ArrayList<String> inNames, final Map<String, String> nameOverrides) {
        final int numSharedElements = sharedElementsIn.size();
        final ArrayList<String> outNames = new ArrayList<>();

        for (int i = 0; i < numSharedElements; i++) {
            final View view = sharedElementsOut.get(i);
            final String name = view.getTransitionName();
            outNames.add(name);
            if (name == null) {
                continue;
            }
            view.setTransitionName(null);
            final String inName = nameOverrides.get(name);
            for (int j = 0; j < numSharedElements; j++) {
                if (inName.equals(inNames.get(j))) {
                    sharedElementsIn.get(j).setTransitionName(name);
                    break;
                }
            }
        }

        OneShotPreDrawListener.add(sceneRoot, new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < numSharedElements; i++) {
                    sharedElementsIn.get(i).setTransitionName(inNames.get(i));
                    sharedElementsOut.get(i).setTransitionName(outNames.get(i));
                }
            }
        });
    }

    /**
     * Gets the Views in the hierarchy affected by entering and exiting Activity Scene transitions.
     * @param transitioningViews This View will be added to transitioningViews if it is VISIBLE and
     *                           a normal View or a ViewGroup with
     *                           {@link android.view.ViewGroup#isTransitionGroup()} true.
     * @param view The base of the view hierarchy to look in.
     */
    public static void captureTransitioningViews(ArrayList<View> transitioningViews, View view) {
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

    /**
     * Finds all views that have transition names in the hierarchy under the given view and
     * stores them in {@code namedViews} map with the name as the key.
     */
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

    public static void setNameOverridesUnoptimized(final View sceneRoot,
            final ArrayList<View> sharedElementsIn, final Map<String, String> nameOverrides) {
        OneShotPreDrawListener.add(sceneRoot, new Runnable() {
            @Override
            public void run() {
                final int numSharedElements = sharedElementsIn.size();
                for (int i = 0; i < numSharedElements; i++) {
                    View view = sharedElementsIn.get(i);
                    String name = view.getTransitionName();
                    if (name != null) {
                        String inName = findKeyForValue(nameOverrides, name);
                        view.setTransitionName(inName);
                    }
                }
            }
        });
    }

    /**
     * Utility to find the String key in {@code map} that maps to {@code value}.
     */
    private static String findKeyForValue(Map<String, String> map, String value) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * After the transition has started, remove all targets that we added to the transitions
     * so that the transitions are left in a clean state.
     */
    public static void scheduleRemoveTargets(final Object overallTransitionObj,
            final Object enterTransition, final ArrayList<View> enteringViews,
            final Object exitTransition, final ArrayList<View> exitingViews,
            final Object sharedElementTransition, final ArrayList<View> sharedElementsIn) {
        final Transition overallTransition = (Transition) overallTransitionObj;
        overallTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(Transition transition) {
                if (enterTransition != null) {
                    replaceTargets(enterTransition, enteringViews, null);
                }
                if (exitTransition != null) {
                    replaceTargets(exitTransition, exitingViews, null);
                }
                if (sharedElementTransition != null) {
                    replaceTargets(sharedElementTransition, sharedElementsIn, null);
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

    /**
     * Swap the targets for the shared element transition from those Views in sharedElementsOut
     * to those in sharedElementsIn
     */
    public static void swapSharedElementTargets(Object sharedElementTransitionObj,
            ArrayList<View> sharedElementsOut, ArrayList<View> sharedElementsIn) {
        TransitionSet sharedElementTransition = (TransitionSet) sharedElementTransitionObj;
        if (sharedElementTransition != null) {
            sharedElementTransition.getTargets().clear();
            sharedElementTransition.getTargets().addAll(sharedElementsIn);
            replaceTargets(sharedElementTransition, sharedElementsOut, sharedElementsIn);
        }
    }


    /**
     * This method removes the views from transitions that target ONLY those views and
     * replaces them with the new targets list.
     * The views list should match those added in addTargets and should contain
     * one view that is not in the view hierarchy (state.nonExistentView).
     */
    public static void replaceTargets(Object transitionObj, ArrayList<View> oldTargets,
            ArrayList<View> newTargets) {
        Transition transition = (Transition) transitionObj;
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                replaceTargets(child, oldTargets, newTargets);
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (targets != null && targets.size() == oldTargets.size()
                    && targets.containsAll(oldTargets)) {
                // We have an exact match. We must have added these earlier in addTargets
                final int targetCount = newTargets == null ? 0 : newTargets.size();
                for (int i = 0; i < targetCount; i++) {
                    transition.addTarget(newTargets.get(i));
                }
                for (int i = oldTargets.size() - 1; i >= 0; i--) {
                    transition.removeTarget(oldTargets.get(i));
                }
            }
        }
    }

    /**
     * Adds a View target to a transition. If transitionObj is null, nothing is done.
     */
    public static void addTarget(Object transitionObj, View view) {
        if (transitionObj != null) {
            Transition transition = (Transition) transitionObj;
            transition.addTarget(view);
        }
    }

    /**
     * Remove a View target to a transition. If transitionObj is null, nothing is done.
     */
    public static void removeTarget(Object transitionObj, View view) {
        if (transitionObj != null) {
            Transition transition = (Transition) transitionObj;
            transition.removeTarget(view);
        }
    }

    /**
     * Sets the epicenter of a transition to a rect object. The object can be modified until
     * the transition runs.
     */
    public static void setEpicenter(Object transitionObj, final Rect epicenter) {
        if (transitionObj != null) {
            Transition transition = (Transition) transitionObj;
            transition.setEpicenterCallback(new Transition.EpicenterCallback() {
                @Override
                public Rect onGetEpicenter(Transition transition) {
                    if (epicenter == null || epicenter.isEmpty()) {
                        return null;
                    }
                    return epicenter;
                }
            });
        }
    }

    public static void scheduleNameReset(final ViewGroup sceneRoot,
            final ArrayList<View> sharedElementsIn, final Map<String, String> nameOverrides) {
        OneShotPreDrawListener.add(sceneRoot, new Runnable() {
            @Override
            public void run() {
                final int numSharedElements = sharedElementsIn.size();
                for (int i = 0; i < numSharedElements; i++) {
                    final View view = sharedElementsIn.get(i);
                    final String name = view.getTransitionName();
                    final String inName = nameOverrides.get(name);
                    view.setTransitionName(inName);
                }
            }
        });
    }
}
