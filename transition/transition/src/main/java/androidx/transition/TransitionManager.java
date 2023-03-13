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

package androidx.transition;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;
import androidx.core.os.BuildCompat;
import androidx.core.view.ViewCompat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * This class manages the set of transitions that fire when there is a
 * change of {@link Scene}. To use the manager, add scenes along with
 * transition objects with calls to {@link #setTransition(Scene, Transition)}
 * or {@link #setTransition(Scene, Scene, Transition)}. Setting specific
 * transitions for scene changes is not required; by default, a Scene change
 * will use {@link AutoTransition} to do something reasonable for most
 * situations. Specifying other transitions for particular scene changes is
 * only necessary if the application wants different transition behavior
 * in these situations.
 *
 * <p>TransitionManagers can be declared in XML resource files inside the
 * <code>res/transition</code> directory. TransitionManager resources consist of
 * the <code>transitionManager</code>tag name, containing one or more
 * <code>transition</code> tags, each of which describe the relationship of
 * that transition to the from/to scene information in that tag.
 * For example, here is a resource file that declares several scene
 * transitions:</p>
 *
 * <pre>
 *     &lt;transitionManager xmlns:android="http://schemas.android.com/apk/res/android"&gt;
 *         &lt;transition android:fromScene="@layout/transition_scene1"
 *                     android:toScene="@layout/transition_scene2"
 *                     android:transition="@transition/changebounds"/&gt;
 *         &lt;transition android:fromScene="@layout/transition_scene2"
 *                     android:toScene="@layout/transition_scene1"
 *                     android:transition="@transition/changebounds"/&gt;
 *         &lt;transition android:toScene="@layout/transition_scene3"
 *                     android:transition="@transition/changebounds_fadein_together"/&gt;
 *         &lt;transition android:fromScene="@layout/transition_scene3"
 *                     android:toScene="@layout/transition_scene1"
 *                     android:transition="@transition/changebounds_fadeout_sequential"/&gt;
 *         &lt;transition android:fromScene="@layout/transition_scene3"
 *                     android:toScene="@layout/transition_scene2"
 *                     android:transition="@transition/changebounds_fadeout_sequential"/&gt;
 *     &lt;/transitionManager&gt;
 * </pre>
 *
 * <p>For each of the <code>fromScene</code> and <code>toScene</code> attributes,
 * there is a reference to a standard XML layout file. This is equivalent to
 * creating a scene from a layout in code by calling
 * {@link Scene#getSceneForLayout(ViewGroup, int, Context)}. For the
 * <code>transition</code> attribute, there is a reference to a resource
 * file in the <code>res/transition</code> directory which describes that
 * transition.</p>
 */
public class TransitionManager {

    private static final String LOG_TAG = "TransitionManager";

    private static Transition sDefaultTransition = new AutoTransition();

    private ArrayMap<Scene, Transition> mSceneTransitions = new ArrayMap<>();
    private ArrayMap<Scene, ArrayMap<Scene, Transition>> mScenePairTransitions = new ArrayMap<>();
    private static ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>>>
            sRunningTransitions = new ThreadLocal<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ArrayList<ViewGroup> sPendingTransitions = new ArrayList<>();

    /**
     * Sets a specific transition to occur when the given scene is entered.
     *
     * @param scene      The scene which, when applied, will cause the given
     *                   transition to run.
     * @param transition The transition that will play when the given scene is
     *                   entered. A value of null will result in the default behavior of
     *                   using the default transition instead.
     */
    public void setTransition(@NonNull Scene scene, @Nullable Transition transition) {
        mSceneTransitions.put(scene, transition);
    }

    /**
     * Sets a specific transition to occur when the given pair of scenes is
     * exited/entered.
     *
     * @param fromScene  The scene being exited when the given transition will
     *                   be run
     * @param toScene    The scene being entered when the given transition will
     *                   be run
     * @param transition The transition that will play when the given scene is
     *                   entered. A value of null will result in the default behavior of
     *                   using the default transition instead.
     */
    public void setTransition(@NonNull Scene fromScene, @NonNull Scene toScene,
            @Nullable Transition transition) {
        ArrayMap<Scene, Transition> sceneTransitionMap = mScenePairTransitions.get(toScene);
        if (sceneTransitionMap == null) {
            sceneTransitionMap = new ArrayMap<>();
            mScenePairTransitions.put(toScene, sceneTransitionMap);
        }
        sceneTransitionMap.put(fromScene, transition);
    }

    /**
     * Returns the Transition for the given scene being entered. The result
     * depends not only on the given scene, but also the scene which the
     * {@link Scene#getSceneRoot() sceneRoot} of the Scene is currently in.
     *
     * @param scene The scene being entered
     * @return The Transition to be used for the given scene change. If no
     * Transition was specified for this scene change, the default transition
     * will be used instead.
     */
    private Transition getTransition(Scene scene) {
        Transition transition;
        ViewGroup sceneRoot = scene.getSceneRoot();
        // TODO: cached in Scene instead? long-term, cache in View itself
        Scene currScene = Scene.getCurrentScene(sceneRoot);
        if (currScene != null) {
            ArrayMap<Scene, Transition> sceneTransitionMap = mScenePairTransitions
                    .get(scene);
            if (sceneTransitionMap != null) {
                transition = sceneTransitionMap.get(currScene);
                if (transition != null) {
                    return transition;
                }
            }
        }
        transition = mSceneTransitions.get(scene);
        return (transition != null) ? transition : sDefaultTransition;
    }

    /**
     * This is where all of the work of a transition/scene-change is
     * orchestrated. This method captures the start values for the given
     * transition, exits the current Scene, enters the new scene, captures
     * the end values for the transition, and finally plays the
     * resulting values-populated transition.
     *
     * @param scene      The scene being entered
     * @param transition The transition to play for this scene change
     */
    private static void changeScene(Scene scene, Transition transition) {
        final ViewGroup sceneRoot = scene.getSceneRoot();

        if (!sPendingTransitions.contains(sceneRoot)) {
            Scene oldScene = Scene.getCurrentScene(sceneRoot);
            if (transition == null) {
                // Notify old scene that it is being exited
                if (oldScene != null) {
                    oldScene.exit();
                }

                scene.enter();
            } else {
                sPendingTransitions.add(sceneRoot);

                Transition transitionClone = transition.clone();

                if (oldScene != null && oldScene.isCreatedFromLayoutResource()) {
                    transitionClone.setCanRemoveViews(true);
                }

                sceneChangeSetup(sceneRoot, transitionClone);

                scene.enter();

                sceneChangeRunTransition(sceneRoot, transitionClone);
            }
        }
    }

    @VisibleForTesting
    static ArrayMap<ViewGroup, ArrayList<Transition>> getRunningTransitions() {
        WeakReference<ArrayMap<ViewGroup, ArrayList<Transition>>> runningTransitions =
                sRunningTransitions.get();
        if (runningTransitions != null) {
            ArrayMap<ViewGroup, ArrayList<Transition>> transitions = runningTransitions.get();
            if (transitions != null) {
                return transitions;
            }
        }
        ArrayMap<ViewGroup, ArrayList<Transition>> transitions = new ArrayMap<>();
        runningTransitions = new WeakReference<>(transitions);
        sRunningTransitions.set(runningTransitions);
        return transitions;
    }

    private static void sceneChangeRunTransition(final ViewGroup sceneRoot,
            final Transition transition) {
        if (transition != null && sceneRoot != null) {
            MultiListener listener = new MultiListener(transition, sceneRoot);
            sceneRoot.addOnAttachStateChangeListener(listener);
            sceneRoot.getViewTreeObserver().addOnPreDrawListener(listener);
        }
    }

    /**
     * This private utility class is used to listen for both OnPreDraw and
     * OnAttachStateChange events. OnPreDraw events are the main ones we care
     * about since that's what triggers the transition to take place.
     * OnAttachStateChange events are also important in case the view is removed
     * from the hierarchy before the OnPreDraw event takes place; it's used to
     * clean up things since the OnPreDraw listener didn't get called in time.
     */
    private static class MultiListener implements ViewTreeObserver.OnPreDrawListener,
            View.OnAttachStateChangeListener {

        Transition mTransition;

        ViewGroup mSceneRoot;

        MultiListener(Transition transition, ViewGroup sceneRoot) {
            mTransition = transition;
            mSceneRoot = sceneRoot;
        }

        private void removeListeners() {
            mSceneRoot.getViewTreeObserver().removeOnPreDrawListener(this);
            mSceneRoot.removeOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewAttachedToWindow(View v) {
        }

        @Override
        public void onViewDetachedFromWindow(View v) {
            removeListeners();

            sPendingTransitions.remove(mSceneRoot);
            ArrayList<Transition> runningTransitions = getRunningTransitions().get(mSceneRoot);
            if (runningTransitions != null && runningTransitions.size() > 0) {
                for (Transition runningTransition : runningTransitions) {
                    runningTransition.resume(mSceneRoot);
                }
            }
            mTransition.clearValues(true);
        }

        @Override
        public boolean onPreDraw() {
            removeListeners();

            // Don't start the transition if it's no longer pending.
            if (!sPendingTransitions.remove(mSceneRoot)) {
                return true;
            }

            // Add to running list, handle end to remove it
            final ArrayMap<ViewGroup, ArrayList<Transition>> runningTransitions =
                    getRunningTransitions();
            ArrayList<Transition> currentTransitions = runningTransitions.get(mSceneRoot);
            ArrayList<Transition> previousRunningTransitions = null;
            if (currentTransitions == null) {
                currentTransitions = new ArrayList<>();
                runningTransitions.put(mSceneRoot, currentTransitions);
            } else if (currentTransitions.size() > 0) {
                previousRunningTransitions = new ArrayList<>(currentTransitions);
            }
            currentTransitions.add(mTransition);
            mTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    ArrayList<Transition> currentTransitions = runningTransitions.get(mSceneRoot);
                    currentTransitions.remove(transition);
                    transition.removeListener(this);
                }
            });
            mTransition.captureValues(mSceneRoot, false);
            if (previousRunningTransitions != null) {
                for (Transition runningTransition : previousRunningTransitions) {
                    runningTransition.resume(mSceneRoot);
                }
            }
            mTransition.playTransition(mSceneRoot);

            return true;
        }
    }

    private static void sceneChangeSetup(ViewGroup sceneRoot, Transition transition) {
        // Capture current values
        ArrayList<Transition> runningTransitions = getRunningTransitions().get(sceneRoot);

        if (runningTransitions != null && runningTransitions.size() > 0) {
            for (Transition runningTransition : runningTransitions) {
                runningTransition.pause(sceneRoot);
            }
        }

        if (transition != null) {
            transition.captureValues(sceneRoot, true);
        }

        // Notify previous scene that it is being exited
        Scene previousScene = Scene.getCurrentScene(sceneRoot);
        if (previousScene != null) {
            previousScene.exit();
        }
    }

    /**
     * Change to the given scene, using the
     * appropriate transition for this particular scene change
     * (as specified to the TransitionManager, or the default
     * if no such transition exists).
     *
     * @param scene The Scene to change to
     */
    public void transitionTo(@NonNull Scene scene) {
        // Auto transition if there is no transition declared for the Scene, but there is
        // a root or parent view
        changeScene(scene, getTransition(scene));
    }

    /**
     * Convenience method to simply change to the given scene using
     * the default transition for TransitionManager.
     *
     * @param scene The Scene to change to
     */
    public static void go(@NonNull Scene scene) {
        changeScene(scene, sDefaultTransition);
    }

    /**
     * Convenience method to simply change to the given scene using
     * the given transition.
     *
     * <p>Passing in <code>null</code> for the transition parameter will
     * result in the scene changing without any transition running, and is
     * equivalent to calling {@link Scene#exit()} on the scene root's
     * current scene, followed by {@link Scene#enter()} on the scene
     * specified by the <code>scene</code> parameter.</p>
     *
     * @param scene      The Scene to change to
     * @param transition The transition to use for this scene change. A
     *                   value of null causes the scene change to happen with no transition.
     */
    public static void go(@NonNull Scene scene, @Nullable Transition transition) {
        changeScene(scene, transition);
    }

    /**
     * Convenience method to animate, using the default transition,
     * to a new scene defined by all changes within the given scene root between
     * calling this method and the next rendering frame.
     * Equivalent to calling {@link #beginDelayedTransition(ViewGroup, Transition)}
     * with a value of <code>null</code> for the <code>transition</code> parameter.
     *
     * @param sceneRoot The root of the View hierarchy to run the transition on.
     */
    public static void beginDelayedTransition(@NonNull final ViewGroup sceneRoot) {
        beginDelayedTransition(sceneRoot, null);
    }

    /**
     * Convenience method to animate to a new scene defined by all changes within
     * the given scene root between calling this method and the next rendering frame.
     * Calling this method causes TransitionManager to capture current values in the
     * scene root and then post a request to run a transition on the next frame.
     * At that time, the new values in the scene root will be captured and changes
     * will be animated. There is no need to create a Scene; it is implied by
     * changes which take place between calling this method and the next frame when
     * the transition begins.
     *
     * <p>Calling this method several times before the next frame (for example, if
     * unrelated code also wants to make dynamic changes and run a transition on
     * the same scene root), only the first call will trigger capturing values
     * and exiting the current scene. Subsequent calls to the method with the
     * same scene root during the same frame will be ignored.</p>
     *
     * <p>Passing in <code>null</code> for the transition parameter will
     * cause the TransitionManager to use its default transition.</p>
     *
     * @param sceneRoot  The root of the View hierarchy to run the transition on.
     * @param transition The transition to use for this change. A
     *                   value of null causes the TransitionManager to use the default transition.
     */
    public static void beginDelayedTransition(@NonNull final ViewGroup sceneRoot,
            @Nullable Transition transition) {
        if (!sPendingTransitions.contains(sceneRoot) && ViewCompat.isLaidOut(sceneRoot)) {
            if (Transition.DBG) {
                Log.d(LOG_TAG, "beginDelayedTransition: root, transition = "
                        + sceneRoot + ", " + transition);
            }
            sPendingTransitions.add(sceneRoot);
            if (transition == null) {
                transition = sDefaultTransition;
            }
            final Transition transitionClone = transition.clone();
            sceneChangeSetup(sceneRoot, transitionClone);
            Scene.setCurrentScene(sceneRoot, null);
            sceneChangeRunTransition(sceneRoot, transitionClone);
        }
    }

    /**
     * Create a {@link TransitionSeekController} to allow seeking an animation to a new
     * scene defined by all changes within the given scene root between calling this method and
     * the next rendered frame. Calling this method causes TransitionManager to capture current
     * values in the scene root and then post a request to run a transition on the next frame.
     * At that time, the new values in the scene root will be captured and changes
     * will be animated. There is no need to create a Scene; it is implied by
     * changes which take place between calling this method and the next frame when
     * the transition begins.
     *
     * <p>Calling this method several times before the next frame (for example, if
     * unrelated code also wants to make dynamic changes and run a transition on
     * the same scene root), only the first call will trigger capturing values
     * and exiting the current scene. Subsequent calls to the method with the
     * same scene root during the same frame will be ignored.</p>
     *
     * @param sceneRoot  The root of the View hierarchy to run the transition on.
     * @param transition The transition to use for this change.
     * @return a {@link TransitionSeekController} that can be used control the animation to the
     * destination scene. {@code null} is returned when seeking is not supported on the scene,
     * either because it is running on {@link android.os.Build.VERSION_CODES.TIRAMISU} or earlier,
     * another Transition is being captured for {@code sceneRoot}, or {@code sceneRoot} hasn't
     * had a layout yet.
     * @throws IllegalArgumentException if {@code transition} returns {@code false} from
     * {@link Transition#isSeekingSupported()}.
     */
    @OptIn(markerClass = BuildCompat.PrereleaseSdkCheck.class)
    @Nullable
    public static TransitionSeekController controlDelayedTransition(
            @NonNull final ViewGroup sceneRoot,
            @NonNull Transition transition
    ) {
        if (sPendingTransitions.contains(sceneRoot) || !ViewCompat.isLaidOut(sceneRoot)
                || !BuildCompat.isAtLeastU()) {
            return null;
        }
        if (!transition.isSeekingSupported()) {
            throw new IllegalArgumentException("The Transition must support seeking.");
        }
        if (Transition.DBG) {
            Log.d(LOG_TAG, "controlDelayedTransition: root, transition = "
                    + sceneRoot + ", " + transition);
        }
        sPendingTransitions.add(sceneRoot);
        final Transition transitionClone = transition.clone();
        final TransitionSet set = new TransitionSet();
        set.addTransition(transitionClone);
        sceneChangeSetup(sceneRoot, set);
        Scene.setCurrentScene(sceneRoot, null);
        sceneChangeRunTransition(sceneRoot, set);
        sceneRoot.invalidate();
        return set.createSeekController();
    }

    /**
     * Ends all pending and ongoing transitions on the specified scene root.
     *
     * @param sceneRoot The root of the View hierarchy to end transitions on.
     */
    public static void endTransitions(final @Nullable ViewGroup sceneRoot) {
        sPendingTransitions.remove(sceneRoot);
        final ArrayList<Transition> runningTransitions = getRunningTransitions().get(sceneRoot);
        if (runningTransitions != null && !runningTransitions.isEmpty()) {
            // Make a copy in case this is called by an onTransitionEnd listener
            ArrayList<Transition> copy = new ArrayList<>(runningTransitions);
            for (int i = copy.size() - 1; i >= 0; i--) {
                final Transition transition = copy.get(i);
                transition.forceToEnd(sceneRoot);
            }
        }
    }
}
