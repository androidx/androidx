/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.support.annotation.RestrictTo;
import android.support.v4.util.ArrayMap;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import static android.support.annotation.RestrictTo.Scope.GROUP_ID;

abstract class TransitionPort implements Cloneable {

    static final boolean DBG = false;

    private static final String LOG_TAG = "Transition";

    // Per-animator information used for later canceling when future transitions overlap
    private static ThreadLocal<ArrayMap<Animator, AnimationInfo>> sRunningAnimators =
            new ThreadLocal<>();

    long mStartDelay = -1;

    long mDuration = -1;

    TimeInterpolator mInterpolator = null;

    ArrayList<Integer> mTargetIds = new ArrayList<>();

    ArrayList<View> mTargets = new ArrayList<>();

    ArrayList<Integer> mTargetIdExcludes = null;

    ArrayList<View> mTargetExcludes = null;

    ArrayList<Class> mTargetTypeExcludes = null;

    ArrayList<Integer> mTargetIdChildExcludes = null;

    ArrayList<View> mTargetChildExcludes = null;

    ArrayList<Class> mTargetTypeChildExcludes = null;

    TransitionSetPort mParent = null;

    // Scene Root is set at createAnimator() time in the cloned Transition
    ViewGroup mSceneRoot = null;

    // Whether removing views from their parent is possible. This is only for views
    // in the start scene, which are no longer in the view hierarchy. This property
    // is determined by whether the previous Scene was created from a layout
    // resource, and thus the views from the exited scene are going away anyway
    // and can be removed as necessary to achieve a particular effect, such as
    // removing them from parents to add them to overlays.
    boolean mCanRemoveViews = false;

    // Number of per-target instances of this Transition currently running. This count is
    // determined by calls to start() and end()
    int mNumInstances = 0;

    // Whether this transition is currently paused, due to a call to pause()
    boolean mPaused = false;

    // The set of listeners to be sent transition lifecycle events.
    ArrayList<TransitionListener> mListeners = null;

    // The set of animators collected from calls to createAnimator(),
    // to be run in runAnimators()
    ArrayList<Animator> mAnimators = new ArrayList<>();

    private String mName = getClass().getName();

    private android.support.transition.TransitionValuesMaps mStartValues
            = new android.support.transition.TransitionValuesMaps();

    private android.support.transition.TransitionValuesMaps mEndValues
            = new android.support.transition.TransitionValuesMaps();

    // Track all animators in use in case the transition gets canceled and needs to
    // cancel running animators
    ArrayList<Animator> mCurrentAnimators = new ArrayList<>();

    // Whether this transition has ended. Used to avoid pause/resume on transitions
    // that have completed
    private boolean mEnded = false;

    /**
     * Constructs a Transition object with no target objects. A transition with
     * no targets defaults to running on all target objects in the scene hierarchy
     * (if the transition is not contained in a TransitionSet), or all target
     * objects passed down from its parent (if it is in a TransitionSet).
     */
    public TransitionPort() {
    }

    private static ArrayMap<Animator, AnimationInfo> getRunningAnimators() {
        ArrayMap<Animator, AnimationInfo> runningAnimators = sRunningAnimators.get();
        if (runningAnimators == null) {
            runningAnimators = new ArrayMap<>();
            sRunningAnimators.set(runningAnimators);
        }
        return runningAnimators;
    }

    public long getDuration() {
        return mDuration;
    }

    public TransitionPort setDuration(long duration) {
        mDuration = duration;
        return this;
    }

    public long getStartDelay() {
        return mStartDelay;
    }

    public TransitionPort setStartDelay(long startDelay) {
        mStartDelay = startDelay;
        return this;
    }

    public TimeInterpolator getInterpolator() {
        return mInterpolator;
    }

    public TransitionPort setInterpolator(TimeInterpolator interpolator) {
        mInterpolator = interpolator;
        return this;
    }

    public String[] getTransitionProperties() {
        return null;
    }


    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
            TransitionValues endValues) {
        return null;
    }

    /**
     * This method, essentially a wrapper around all calls to createAnimator for all
     * possible target views, is called with the entire set of start/end
     * values. The implementation in Transition iterates through these lists
     * and calls {@link #createAnimator(ViewGroup, TransitionValues, TransitionValues)}
     * with each set of start/end values on this transition. The
     * TransitionSet subclass overrides this method and delegates it to
     * each of its children in succession.
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    protected void createAnimators(ViewGroup sceneRoot, TransitionValuesMaps startValues,
            TransitionValuesMaps endValues) {
        if (DBG) {
            Log.d(LOG_TAG, "createAnimators() for " + this);
        }
        ArrayMap<View, TransitionValues> endCopy =
                new ArrayMap<>(endValues.viewValues);
        SparseArray<TransitionValues> endIdCopy =
                new SparseArray<>(endValues.idValues.size());
        for (int i = 0; i < endValues.idValues.size(); ++i) {
            int id = endValues.idValues.keyAt(i);
            endIdCopy.put(id, endValues.idValues.valueAt(i));
        }
        LongSparseArray<TransitionValues> endItemIdCopy =
                new LongSparseArray<>(endValues.itemIdValues.size());
        for (int i = 0; i < endValues.itemIdValues.size(); ++i) {
            long id = endValues.itemIdValues.keyAt(i);
            endItemIdCopy.put(id, endValues.itemIdValues.valueAt(i));
        }
        // Walk through the start values, playing everything we find
        // Remove from the end set as we go
        ArrayList<TransitionValues> startValuesList = new ArrayList<>();
        ArrayList<TransitionValues> endValuesList = new ArrayList<>();
        for (View view : startValues.viewValues.keySet()) {
            TransitionValues start;
            TransitionValues end = null;
            boolean isInListView = false;
            if (view.getParent() instanceof ListView) {
                isInListView = true;
            }
            if (!isInListView) {
                int id = view.getId();
                start = startValues.viewValues.get(view) != null ?
                        startValues.viewValues.get(view) : startValues.idValues.get(id);
                if (endValues.viewValues.get(view) != null) {
                    end = endValues.viewValues.get(view);
                    endCopy.remove(view);
                } else if (id != View.NO_ID) {
                    end = endValues.idValues.get(id);
                    View removeView = null;
                    for (View viewToRemove : endCopy.keySet()) {
                        if (viewToRemove.getId() == id) {
                            removeView = viewToRemove;
                        }
                    }
                    if (removeView != null) {
                        endCopy.remove(removeView);
                    }
                }
                endIdCopy.remove(id);
                if (isValidTarget(view, id)) {
                    startValuesList.add(start);
                    endValuesList.add(end);
                }
            } else {
                ListView parent = (ListView) view.getParent();
                if (parent.getAdapter().hasStableIds()) {
                    int position = parent.getPositionForView(view);
                    long itemId = parent.getItemIdAtPosition(position);
                    start = startValues.itemIdValues.get(itemId);
                    endItemIdCopy.remove(itemId);
                    // TODO: deal with targetIDs for itemIDs for ListView items
                    startValuesList.add(start);
                    endValuesList.add(end);
                }
            }
        }
        int startItemIdCopySize = startValues.itemIdValues.size();
        for (int i = 0; i < startItemIdCopySize; ++i) {
            long id = startValues.itemIdValues.keyAt(i);
            if (isValidTarget(null, id)) {
                TransitionValues start = startValues.itemIdValues.get(id);
                TransitionValues end = endValues.itemIdValues.get(id);
                endItemIdCopy.remove(id);
                startValuesList.add(start);
                endValuesList.add(end);
            }
        }
        // Now walk through the remains of the end set
        for (View view : endCopy.keySet()) {
            int id = view.getId();
            if (isValidTarget(view, id)) {
                TransitionValues start = startValues.viewValues.get(view) != null ?
                        startValues.viewValues.get(view) : startValues.idValues.get(id);
                TransitionValues end = endCopy.get(view);
                endIdCopy.remove(id);
                startValuesList.add(start);
                endValuesList.add(end);
            }
        }
        int endIdCopySize = endIdCopy.size();
        for (int i = 0; i < endIdCopySize; ++i) {
            int id = endIdCopy.keyAt(i);
            if (isValidTarget(null, id)) {
                TransitionValues start = startValues.idValues.get(id);
                TransitionValues end = endIdCopy.get(id);
                startValuesList.add(start);
                endValuesList.add(end);
            }
        }
        int endItemIdCopySize = endItemIdCopy.size();
        for (int i = 0; i < endItemIdCopySize; ++i) {
            long id = endItemIdCopy.keyAt(i);
            // TODO: Deal with targetIDs and itemIDs
            TransitionValues start = startValues.itemIdValues.get(id);
            TransitionValues end = endItemIdCopy.get(id);
            startValuesList.add(start);
            endValuesList.add(end);
        }
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        for (int i = 0; i < startValuesList.size(); ++i) {
            TransitionValues start = startValuesList.get(i);
            TransitionValues end = endValuesList.get(i);
            // Only bother trying to animate with values that differ between start/end
            if (start != null || end != null) {
                if (start == null || !start.equals(end)) {
                    if (DBG) {
                        View view = (end != null) ? end.view : start.view;
                        Log.d(LOG_TAG, "  differing start/end values for view " +
                                view);
                        if (start == null || end == null) {
                            Log.d(LOG_TAG, "    " + ((start == null) ?
                                    "start null, end non-null" : "start non-null, end null"));
                        } else {
                            for (String key : start.values.keySet()) {
                                Object startValue = start.values.get(key);
                                Object endValue = end.values.get(key);
                                if (startValue != endValue && !startValue.equals(endValue)) {
                                    Log.d(LOG_TAG, "    " + key + ": start(" + startValue +
                                            "), end(" + endValue + ")");
                                }
                            }
                        }
                    }
                    // TODO: what to do about targetIds and itemIds?
                    Animator animator = createAnimator(sceneRoot, start, end);
                    if (animator != null) {
                        // Save animation info for future cancellation purposes
                        View view;
                        TransitionValues infoValues = null;
                        if (end != null) {
                            view = end.view;
                            String[] properties = getTransitionProperties();
                            if (view != null && properties != null && properties.length > 0) {
                                infoValues = new TransitionValues();
                                infoValues.view = view;
                                TransitionValues newValues = endValues.viewValues.get(view);
                                if (newValues != null) {
                                    for (int j = 0; j < properties.length; ++j) {
                                        infoValues.values.put(properties[j],
                                                newValues.values.get(properties[j]));
                                    }
                                }
                                int numExistingAnims = runningAnimators.size();
                                for (int j = 0; j < numExistingAnims; ++j) {
                                    Animator anim = runningAnimators.keyAt(j);
                                    AnimationInfo info = runningAnimators.get(anim);
                                    if (info.values != null && info.view == view &&
                                            ((info.name == null && getName() == null) ||
                                                    info.name.equals(getName()))) {
                                        if (info.values.equals(infoValues)) {
                                            // Favor the old animator
                                            animator = null;
                                            break;
                                        }
                                    }
                                }
                            }
                        } else {
                            view = start.view;
                        }
                        if (animator != null) {
                            AnimationInfo info = new AnimationInfo(view, getName(),
                                    WindowIdPort.getWindowId(sceneRoot), infoValues);
                            runningAnimators.put(animator, info);
                            mAnimators.add(animator);
                        }
                    }
                }
            }
        }
    }

    /**
     * Internal utility method for checking whether a given view/id
     * is valid for this transition, where "valid" means that either
     * the Transition has no target/targetId list (the default, in which
     * cause the transition should act on all views in the hiearchy), or
     * the given view is in the target list or the view id is in the
     * targetId list. If the target parameter is null, then the target list
     * is not checked (this is in the case of ListView items, where the
     * views are ignored and only the ids are used).
     */
    boolean isValidTarget(View target, long targetId) {
        if (mTargetIdExcludes != null && mTargetIdExcludes.contains((int)targetId)) {
            return false;
        }
        if (mTargetExcludes != null && mTargetExcludes.contains(target)) {
            return false;
        }
        if (mTargetTypeExcludes != null && target != null) {
            int numTypes = mTargetTypeExcludes.size();
            for (int i = 0; i < numTypes; ++i) {
                Class type = mTargetTypeExcludes.get(i);
                if (type.isInstance(target)) {
                    return false;
                }
            }
        }
        if (mTargetIds.size() == 0 && mTargets.size() == 0) {
            return true;
        }
        if (mTargetIds.size() > 0) {
            for (int i = 0; i < mTargetIds.size(); ++i) {
                if (mTargetIds.get(i) == targetId) {
                    return true;
                }
            }
        }
        if (target != null && mTargets.size() > 0) {
            for (int i = 0; i < mTargets.size(); ++i) {
                if (mTargets.get(i) == target) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This is called internally once all animations have been set up by the
     * transition hierarchy. \
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    protected void runAnimators() {
        if (DBG) {
            Log.d(LOG_TAG, "runAnimators() on " + this);
        }
        start();
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        // Now start every Animator that was previously created for this transition
        for (Animator anim : mAnimators) {
            if (DBG) {
                Log.d(LOG_TAG, "  anim: " + anim);
            }
            if (runningAnimators.containsKey(anim)) {
                start();
                runAnimator(anim, runningAnimators);
            }
        }
        mAnimators.clear();
        end();
    }

    private void runAnimator(Animator animator,
            final ArrayMap<Animator, AnimationInfo> runningAnimators) {
        if (animator != null) {
            // TODO: could be a single listener instance for all of them since it uses the param
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    mCurrentAnimators.add(animation);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    runningAnimators.remove(animation);
                    mCurrentAnimators.remove(animation);
                }
            });
            animate(animator);
        }
    }

    public abstract void captureStartValues(TransitionValues transitionValues);

    public abstract void captureEndValues(TransitionValues transitionValues);

    public TransitionPort addTarget(int targetId) {
        if (targetId > 0) {
            mTargetIds.add(targetId);
        }
        return this;
    }

    public TransitionPort removeTarget(int targetId) {
        if (targetId > 0) {
            mTargetIds.remove((Integer) targetId);
        }
        return this;
    }

    public TransitionPort excludeTarget(int targetId, boolean exclude) {
        mTargetIdExcludes = excludeId(mTargetIdExcludes, targetId, exclude);
        return this;
    }


    public TransitionPort excludeChildren(int targetId, boolean exclude) {
        mTargetIdChildExcludes = excludeId(mTargetIdChildExcludes, targetId, exclude);
        return this;
    }

    /**
     * Utility method to manage the boilerplate code that is the same whether we
     * are excluding targets or their children.
     */
    private ArrayList<Integer> excludeId(ArrayList<Integer> list, int targetId, boolean exclude) {
        if (targetId > 0) {
            if (exclude) {
                list = ArrayListManager.add(list, targetId);
            } else {
                list = ArrayListManager.remove(list, targetId);
            }
        }
        return list;
    }

    public TransitionPort excludeTarget(View target, boolean exclude) {
        mTargetExcludes = excludeView(mTargetExcludes, target, exclude);
        return this;
    }

    public TransitionPort excludeChildren(View target, boolean exclude) {
        mTargetChildExcludes = excludeView(mTargetChildExcludes, target, exclude);
        return this;
    }

    /**
     * Utility method to manage the boilerplate code that is the same whether we
     * are excluding targets or their children.
     */
    private ArrayList<View> excludeView(ArrayList<View> list, View target, boolean exclude) {
        if (target != null) {
            if (exclude) {
                list = ArrayListManager.add(list, target);
            } else {
                list = ArrayListManager.remove(list, target);
            }
        }
        return list;
    }

    public TransitionPort excludeTarget(Class type, boolean exclude) {
        mTargetTypeExcludes = excludeType(mTargetTypeExcludes, type, exclude);
        return this;
    }

    public TransitionPort excludeChildren(Class type, boolean exclude) {
        mTargetTypeChildExcludes = excludeType(mTargetTypeChildExcludes, type, exclude);
        return this;
    }

    /**
     * Utility method to manage the boilerplate code that is the same whether we
     * are excluding targets or their children.
     */
    private ArrayList<Class> excludeType(ArrayList<Class> list, Class type, boolean exclude) {
        if (type != null) {
            if (exclude) {
                list = ArrayListManager.add(list, type);
            } else {
                list = ArrayListManager.remove(list, type);
            }
        }
        return list;
    }

    /**
     * Sets the target view instances that this Transition is interested in
     * animating. By default, there are no targets, and a Transition will
     * listen for changes on every view in the hierarchy below the sceneRoot
     * of the Scene being transitioned into. Setting targets constrains
     * the Transition to only listen for, and act on, these views.
     * All other views will be ignored.
     *
     * <p>The target list is like the {@link #addTarget(int) targetId}
     * list except this list specifies the actual View instances, not the ids
     * of the views. This is an important distinction when scene changes involve
     * view hierarchies which have been inflated separately; different views may
     * share the same id but not actually be the same instance. If the transition
     * should treat those views as the same, then {@link #addTarget(int)} should be used
     * instead of {@link #addTarget(View)}. If, on the other hand, scene changes involve
     * changes all within the same view hierarchy, among views which do not
     * necessarily have ids set on them, then the target list of views may be more
     * convenient.</p>
     *
     * @param target A View on which the Transition will act, must be non-null.
     * @return The Transition to which the target is added.
     * Returning the same object makes it easier to chain calls during
     * construction, such as
     * <code>transitionSet.addTransitions(new Fade()).addTarget(someView);</code>
     * @see #addTarget(int)
     */
    public TransitionPort addTarget(View target) {
        mTargets.add(target);
        return this;
    }

    public TransitionPort removeTarget(View target) {
        if (target != null) {
            mTargets.remove(target);
        }
        return this;
    }

    public List<Integer> getTargetIds() {
        return mTargetIds;
    }

    public List<View> getTargets() {
        return mTargets;
    }

    /**
     * Recursive method that captures values for the given view and the
     * hierarchy underneath it.
     *
     * @param sceneRoot The root of the view hierarchy being captured
     * @param start     true if this capture is happening before the scene change,
     *                  false otherwise
     */
    void captureValues(ViewGroup sceneRoot, boolean start) {
        clearValues(start);
        if (mTargetIds.size() > 0 || mTargets.size() > 0) {
            if (mTargetIds.size() > 0) {
                for (int i = 0; i < mTargetIds.size(); ++i) {
                    int id = mTargetIds.get(i);
                    View view = sceneRoot.findViewById(id);
                    if (view != null) {
                        TransitionValues values = new TransitionValues();
                        values.view = view;
                        if (start) {
                            captureStartValues(values);
                        } else {
                            captureEndValues(values);
                        }
                        if (start) {
                            mStartValues.viewValues.put(view, values);
                            if (id >= 0) {
                                mStartValues.idValues.put(id, values);
                            }
                        } else {
                            mEndValues.viewValues.put(view, values);
                            if (id >= 0) {
                                mEndValues.idValues.put(id, values);
                            }
                        }
                    }
                }
            }
            if (mTargets.size() > 0) {
                for (int i = 0; i < mTargets.size(); ++i) {
                    View view = mTargets.get(i);
                    if (view != null) {
                        TransitionValues values = new TransitionValues();
                        values.view = view;
                        if (start) {
                            captureStartValues(values);
                        } else {
                            captureEndValues(values);
                        }
                        if (start) {
                            mStartValues.viewValues.put(view, values);
                        } else {
                            mEndValues.viewValues.put(view, values);
                        }
                    }
                }
            }
        } else {
            captureHierarchy(sceneRoot, start);
        }
    }

    /**
     * Clear valuesMaps for specified start/end state
     *
     * @param start true if the start values should be cleared, false otherwise
     */
    void clearValues(boolean start) {
        if (start) {
            mStartValues.viewValues.clear();
            mStartValues.idValues.clear();
            mStartValues.itemIdValues.clear();
        } else {
            mEndValues.viewValues.clear();
            mEndValues.idValues.clear();
            mEndValues.itemIdValues.clear();
        }
    }

    /**
     * Recursive method which captures values for an entire view hierarchy,
     * starting at some root view. Transitions without targetIDs will use this
     * method to capture values for all possible views.
     *
     * @param view  The view for which to capture values. Children of this View
     *              will also be captured, recursively down to the leaf nodes.
     * @param start true if values are being captured in the start scene, false
     *              otherwise.
     */
    private void captureHierarchy(View view, boolean start) {
        if (view == null) {
            return;
        }
        boolean isListViewItem = false;
        if (view.getParent() instanceof ListView) {
            isListViewItem = true;
        }
        if (isListViewItem && !((ListView) view.getParent()).getAdapter().hasStableIds()) {
            // ignore listview children unless we can track them with stable IDs
            return;
        }
        int id = View.NO_ID;
        long itemId = View.NO_ID;
        if (!isListViewItem) {
            id = view.getId();
        } else {
            ListView listview = (ListView) view.getParent();
            int position = listview.getPositionForView(view);
            itemId = listview.getItemIdAtPosition(position);
//            view.setHasTransientState(true);
        }
        if (mTargetIdExcludes != null && mTargetIdExcludes.contains(id)) {
            return;
        }
        if (mTargetExcludes != null && mTargetExcludes.contains(view)) {
            return;
        }
        if (mTargetTypeExcludes != null && view != null) {
            int numTypes = mTargetTypeExcludes.size();
            for (int i = 0; i < numTypes; ++i) {
                if (mTargetTypeExcludes.get(i).isInstance(view)) {
                    return;
                }
            }
        }
        TransitionValues values = new TransitionValues();
        values.view = view;
        if (start) {
            captureStartValues(values);
        } else {
            captureEndValues(values);
        }
        if (start) {
            if (!isListViewItem) {
                mStartValues.viewValues.put(view, values);
                if (id >= 0) {
                    mStartValues.idValues.put((int) id, values);
                }
            } else {
                mStartValues.itemIdValues.put(itemId, values);
            }
        } else {
            if (!isListViewItem) {
                mEndValues.viewValues.put(view, values);
                if (id >= 0) {
                    mEndValues.idValues.put((int) id, values);
                }
            } else {
                mEndValues.itemIdValues.put(itemId, values);
            }
        }
        if (view instanceof ViewGroup) {
            // Don't traverse child hierarchy if there are any child-excludes on this view
            if (mTargetIdChildExcludes != null && mTargetIdChildExcludes.contains(id)) {
                return;
            }
            if (mTargetChildExcludes != null && mTargetChildExcludes.contains(view)) {
                return;
            }
            if (mTargetTypeChildExcludes != null && view != null) {
                int numTypes = mTargetTypeChildExcludes.size();
                for (int i = 0; i < numTypes; ++i) {
                    if (mTargetTypeChildExcludes.get(i).isInstance(view)) {
                        return;
                    }
                }
            }
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); ++i) {
                captureHierarchy(parent.getChildAt(i), start);
            }
        }
    }

    public TransitionValues getTransitionValues(View view, boolean start) {
        if (mParent != null) {
            return mParent.getTransitionValues(view, start);
        }
        TransitionValuesMaps valuesMaps = start ? mStartValues : mEndValues;
        TransitionValues values = valuesMaps.viewValues.get(view);
        if (values == null) {
            int id = view.getId();
            if (id >= 0) {
                values = valuesMaps.idValues.get(id);
            }
            if (values == null && view.getParent() instanceof ListView) {
                ListView listview = (ListView) view.getParent();
                int position = listview.getPositionForView(view);
                long itemId = listview.getItemIdAtPosition(position);
                values = valuesMaps.itemIdValues.get(itemId);
            }
            // TODO: Doesn't handle the case where a view was parented to a
            // ListView (with an itemId), but no longer is
        }
        return values;
    }

    /**
     * Pauses this transition, sending out calls to {@link
     * TransitionListener#onTransitionPause(TransitionPort)} to all listeners
     * and pausing all running animators started by this transition.
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    public void pause(View sceneRoot) {
        if (!mEnded) {
            ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
            int numOldAnims = runningAnimators.size();
            WindowIdPort windowId = WindowIdPort.getWindowId(sceneRoot);
            for (int i = numOldAnims - 1; i >= 0; i--) {
                AnimationInfo info = runningAnimators.valueAt(i);
                if (info.view != null && windowId.equals(info.windowId)) {
                    Animator anim = runningAnimators.keyAt(i);
                    anim.cancel(); // pause() is API Level 19
                }
            }
            if (mListeners != null && mListeners.size() > 0) {
                ArrayList<TransitionListener> tmpListeners =
                        (ArrayList<TransitionListener>) mListeners.clone();
                int numListeners = tmpListeners.size();
                for (int i = 0; i < numListeners; ++i) {
                    tmpListeners.get(i).onTransitionPause(this);
                }
            }
            mPaused = true;
        }
    }

    /**
     * Resumes this transition, sending out calls to {@link
     * TransitionListener#onTransitionPause(TransitionPort)} to all listeners
     * and pausing all running animators started by this transition.
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    public void resume(View sceneRoot) {
        if (mPaused) {
            if (!mEnded) {
                ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
                int numOldAnims = runningAnimators.size();
                WindowIdPort windowId = WindowIdPort.getWindowId(sceneRoot);
                for (int i = numOldAnims - 1; i >= 0; i--) {
                    AnimationInfo info = runningAnimators.valueAt(i);
                    if (info.view != null && windowId.equals(info.windowId)) {
                        Animator anim = runningAnimators.keyAt(i);
                        anim.end(); // resume() is API Level 19
                    }
                }
                if (mListeners != null && mListeners.size() > 0) {
                    ArrayList<TransitionListener> tmpListeners =
                            (ArrayList<TransitionListener>) mListeners.clone();
                    int numListeners = tmpListeners.size();
                    for (int i = 0; i < numListeners; ++i) {
                        tmpListeners.get(i).onTransitionResume(this);
                    }
                }
            }
            mPaused = false;
        }
    }

    /**
     * Called by TransitionManager to play the transition. This calls
     * createAnimators() to set things up and create all of the animations and then
     * runAnimations() to actually start the animations.
     */
    void playTransition(ViewGroup sceneRoot) {
        ArrayMap<Animator, AnimationInfo> runningAnimators = getRunningAnimators();
        int numOldAnims = runningAnimators.size();
        for (int i = numOldAnims - 1; i >= 0; i--) {
            Animator anim = runningAnimators.keyAt(i);
            if (anim != null) {
                AnimationInfo oldInfo = runningAnimators.get(anim);
                if (oldInfo != null && oldInfo.view != null &&
                        oldInfo.view.getContext() == sceneRoot.getContext()) {
                    boolean cancel = false;
                    TransitionValues oldValues = oldInfo.values;
                    View oldView = oldInfo.view;
                    TransitionValues newValues = mEndValues.viewValues != null ?
                            mEndValues.viewValues.get(oldView) : null;
                    if (newValues == null) {
                        newValues = mEndValues.idValues.get(oldView.getId());
                    }
                    if (oldValues != null) {
                        // if oldValues null, then transition didn't care to stash values,
                        // and won't get canceled
                        if (newValues != null) {
                            for (String key : oldValues.values.keySet()) {
                                Object oldValue = oldValues.values.get(key);
                                Object newValue = newValues.values.get(key);
                                if (oldValue != null && newValue != null &&
                                        !oldValue.equals(newValue)) {
                                    cancel = true;
                                    if (DBG) {
                                        Log.d(LOG_TAG, "Transition.playTransition: " +
                                                "oldValue != newValue for " + key +
                                                ": old, new = " + oldValue + ", " + newValue);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                    if (cancel) {
                        if (anim.isRunning() || anim.isStarted()) {
                            if (DBG) {
                                Log.d(LOG_TAG, "Canceling anim " + anim);
                            }
                            anim.cancel();
                        } else {
                            if (DBG) {
                                Log.d(LOG_TAG, "removing anim from info list: " + anim);
                            }
                            runningAnimators.remove(anim);
                        }
                    }
                }
            }
        }

        createAnimators(sceneRoot, mStartValues, mEndValues);
        runAnimators();
    }

    /**
     * This is a utility method used by subclasses to handle standard parts of
     * setting up and running an Animator: it sets the {@link #getDuration()
     * duration} and the {@link #getStartDelay() startDelay}, starts the
     * animation, and, when the animator ends, calls {@link #end()}.
     *
     * @param animator The Animator to be run during this transition.
     * @hide
     */
    @RestrictTo(GROUP_ID)
    protected void animate(Animator animator) {
        // TODO: maybe pass auto-end as a boolean parameter?
        if (animator == null) {
            end();
        } else {
            if (getDuration() >= 0) {
                animator.setDuration(getDuration());
            }
            if (getStartDelay() >= 0) {
                animator.setStartDelay(getStartDelay());
            }
            if (getInterpolator() != null) {
                animator.setInterpolator(getInterpolator());
            }
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    end();
                    animation.removeListener(this);
                }
            });
            animator.start();
        }
    }

    /**
     * This method is called automatically by the transition and
     * TransitionSet classes prior to a Transition subclass starting;
     * subclasses should not need to call it directly.
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    protected void start() {
        if (mNumInstances == 0) {
            if (mListeners != null && mListeners.size() > 0) {
                ArrayList<TransitionListener> tmpListeners =
                        (ArrayList<TransitionListener>) mListeners.clone();
                int numListeners = tmpListeners.size();
                for (int i = 0; i < numListeners; ++i) {
                    tmpListeners.get(i).onTransitionStart(this);
                }
            }
            mEnded = false;
        }
        mNumInstances++;
    }

    /**
     * This method is called automatically by the Transition and
     * TransitionSet classes when a transition finishes, either because
     * a transition did nothing (returned a null Animator from
     * {@link TransitionPort#createAnimator(ViewGroup, TransitionValues,
     * TransitionValues)}) or because the transition returned a valid
     * Animator and end() was called in the onAnimationEnd()
     * callback of the AnimatorListener.
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    protected void end() {
        --mNumInstances;
        if (mNumInstances == 0) {
            if (mListeners != null && mListeners.size() > 0) {
                ArrayList<TransitionListener> tmpListeners =
                        (ArrayList<TransitionListener>) mListeners.clone();
                int numListeners = tmpListeners.size();
                for (int i = 0; i < numListeners; ++i) {
                    tmpListeners.get(i).onTransitionEnd(this);
                }
            }
            for (int i = 0; i < mStartValues.itemIdValues.size(); ++i) {
                TransitionValues tv = mStartValues.itemIdValues.valueAt(i);
                View v = tv.view;
//                if (v.hasTransientState()) {
//                    v.setHasTransientState(false);
//                }
            }
            for (int i = 0; i < mEndValues.itemIdValues.size(); ++i) {
                TransitionValues tv = mEndValues.itemIdValues.valueAt(i);
                View v = tv.view;
//                if (v.hasTransientState()) {
//                    v.setHasTransientState(false);
//                }
            }
            mEnded = true;
        }
    }

    /**
     * This method cancels a transition that is currently running.
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    protected void cancel() {
        int numAnimators = mCurrentAnimators.size();
        for (int i = numAnimators - 1; i >= 0; i--) {
            Animator animator = mCurrentAnimators.get(i);
            animator.cancel();
        }
        if (mListeners != null && mListeners.size() > 0) {
            ArrayList<TransitionListener> tmpListeners =
                    (ArrayList<TransitionListener>) mListeners.clone();
            int numListeners = tmpListeners.size();
            for (int i = 0; i < numListeners; ++i) {
                tmpListeners.get(i).onTransitionCancel(this);
            }
        }
    }

    /**
     * Adds a listener to the set of listeners that are sent events through the
     * life of an animation, such as start, repeat, and end.
     *
     * @param listener the listener to be added to the current set of listeners
     *                 for this animation.
     * @return This transition object.
     */
    public TransitionPort addListener(TransitionListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<>();
        }
        mListeners.add(listener);
        return this;
    }

    public TransitionPort removeListener(TransitionListener listener) {
        if (mListeners == null) {
            return this;
        }
        mListeners.remove(listener);
        if (mListeners.size() == 0) {
            mListeners = null;
        }
        return this;
    }

    TransitionPort setSceneRoot(ViewGroup sceneRoot) {
        mSceneRoot = sceneRoot;
        return this;
    }

    void setCanRemoveViews(boolean canRemoveViews) {
        mCanRemoveViews = canRemoveViews;
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    public TransitionPort clone() {
        TransitionPort clone = null;
        try {
            clone = (TransitionPort) super.clone();
            clone.mAnimators = new ArrayList<Animator>();
            clone.mStartValues = new TransitionValuesMaps();
            clone.mEndValues = new TransitionValuesMaps();
        } catch (CloneNotSupportedException e) {
        }

        return clone;
    }

    public String getName() {
        return mName;
    }

    String toString(String indent) {
        String result = indent + getClass().getSimpleName() + "@" +
                Integer.toHexString(hashCode()) + ": ";
        if (mDuration != -1) {
            result += "dur(" + mDuration + ") ";
        }
        if (mStartDelay != -1) {
            result += "dly(" + mStartDelay + ") ";
        }
        if (mInterpolator != null) {
            result += "interp(" + mInterpolator + ") ";
        }
        if (mTargetIds.size() > 0 || mTargets.size() > 0) {
            result += "tgts(";
            if (mTargetIds.size() > 0) {
                for (int i = 0; i < mTargetIds.size(); ++i) {
                    if (i > 0) {
                        result += ", ";
                    }
                    result += mTargetIds.get(i);
                }
            }
            if (mTargets.size() > 0) {
                for (int i = 0; i < mTargets.size(); ++i) {
                    if (i > 0) {
                        result += ", ";
                    }
                    result += mTargets.get(i);
                }
            }
            result += ")";
        }
        return result;
    }

    public interface TransitionListener {

        /**
         * Notification about the start of the transition.
         *
         * @param transition The started transition.
         */
        void onTransitionStart(TransitionPort transition);

        /**
         * Notification about the end of the transition. Canceled transitions
         * will always notify listeners of both the cancellation and end
         * events. That is, {@link #onTransitionEnd(TransitionPort)} is always called,
         * regardless of whether the transition was canceled or played
         * through to completion.
         *
         * @param transition The transition which reached its end.
         */
        void onTransitionEnd(TransitionPort transition);

        /**
         * Notification about the cancellation of the transition.
         * Note that cancel may be called by a parent {@link TransitionSetPort} on
         * a child transition which has not yet started. This allows the child
         * transition to restore state on target objects which was set at
         * {@link #createAnimator(ViewGroup, TransitionValues, TransitionValues)
         * createAnimator()} time.
         *
         * @param transition The transition which was canceled.
         */
        void onTransitionCancel(TransitionPort transition);

        /**
         * Notification when a transition is paused.
         * Note that createAnimator() may be called by a parent {@link TransitionSetPort} on
         * a child transition which has not yet started. This allows the child
         * transition to restore state on target objects which was set at
         * {@link #createAnimator(ViewGroup, TransitionValues, TransitionValues)
         * createAnimator()} time.
         *
         * @param transition The transition which was paused.
         */
        void onTransitionPause(TransitionPort transition);

        /**
         * Notification when a transition is resumed.
         * Note that resume() may be called by a parent {@link TransitionSetPort} on
         * a child transition which has not yet started. This allows the child
         * transition to restore state which may have changed in an earlier call
         * to {@link #onTransitionPause(TransitionPort)}.
         *
         * @param transition The transition which was resumed.
         */
        void onTransitionResume(TransitionPort transition);
    }

    /**
     * Utility adapter class to avoid having to override all three methods
     * whenever someone just wants to listen for a single event.
     *
     * @hide
     */
    @RestrictTo(GROUP_ID)
    public static class TransitionListenerAdapter implements TransitionListener {

        @Override
        public void onTransitionStart(TransitionPort transition) {
        }

        @Override
        public void onTransitionEnd(TransitionPort transition) {
        }

        @Override
        public void onTransitionCancel(TransitionPort transition) {
        }

        @Override
        public void onTransitionPause(TransitionPort transition) {
        }

        @Override
        public void onTransitionResume(TransitionPort transition) {
        }
    }

    /**
     * Holds information about each animator used when a new transition starts
     * while other transitions are still running to determine whether a running
     * animation should be canceled or a new animation noop'd. The structure holds
     * information about the state that an animation is going to, to be compared to
     * end state of a new animation.
     */
    private static class AnimationInfo {

        View view;

        String name;

        TransitionValues values;

        WindowIdPort windowId;

        AnimationInfo(View view, String name, WindowIdPort windowId, TransitionValues values) {
            this.view = view;
            this.name = name;
            this.values = values;
            this.windowId = windowId;
        }
    }

    /**
     * Utility class for managing typed ArrayLists efficiently. In particular, this
     * can be useful for lists that we don't expect to be used often (eg, the exclude
     * lists), so we'd like to keep them nulled out by default. This causes the code to
     * become tedious, with constant null checks, code to allocate when necessary,
     * and code to null out the reference when the list is empty. This class encapsulates
     * all of that functionality into simple add()/remove() methods which perform the
     * necessary checks, allocation/null-out as appropriate, and return the
     * resulting list.
     */
    private static class ArrayListManager {

        /**
         * Add the specified item to the list, returning the resulting list.
         * The returned list can either the be same list passed in or, if that
         * list was null, the new list that was created.
         *
         * Note that the list holds unique items; if the item already exists in the
         * list, the list is not modified.
         */
        static <T> ArrayList<T> add(ArrayList<T> list, T item) {
            if (list == null) {
                list = new ArrayList<T>();
            }
            if (!list.contains(item)) {
                list.add(item);
            }
            return list;
        }

        /**
         * Remove the specified item from the list, returning the resulting list.
         * The returned list can either the be same list passed in or, if that
         * list becomes empty as a result of the remove(), the new list was created.
         */
        static <T> ArrayList<T> remove(ArrayList<T> list, T item) {
            if (list != null) {
                list.remove(item);
                if (list.isEmpty()) {
                    list = null;
                }
            }
            return list;
        }
    }

}
