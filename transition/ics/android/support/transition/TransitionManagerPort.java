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

import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

class TransitionManagerPort {
    // TODO: how to handle enter/exit?

    private static final String[] EMPTY_STRINGS = new String[0];

    private static String LOG_TAG = "TransitionManager";

    private static TransitionPort sDefaultTransition = new AutoTransitionPort();

    private static ThreadLocal<WeakReference<ArrayMap<ViewGroup, ArrayList<TransitionPort>>>>
            sRunningTransitions = new ThreadLocal<>();

    private static ArrayList<ViewGroup> sPendingTransitions = new ArrayList<>();

    ArrayMap<ScenePort, TransitionPort> mSceneTransitions = new ArrayMap<>();

    ArrayMap<ScenePort, ArrayMap<ScenePort, TransitionPort>> mScenePairTransitions =
            new ArrayMap<>();

    ArrayMap<ScenePort, ArrayMap<String, TransitionPort>> mSceneNameTransitions = new ArrayMap<>();

    ArrayMap<String, ArrayMap<ScenePort, TransitionPort>> mNameSceneTransitions = new ArrayMap<>();

    /**
     * Gets the current default transition. The initial value is an {@link
     * AutoTransition} instance.
     *
     * @return The current default transition.
     * @hide pending later changes
     * @see #setDefaultTransition(TransitionPort)
     */
    public static TransitionPort getDefaultTransition() {
        return sDefaultTransition;
    }

    /**
     * Sets the transition to be used for any scene change for which no
     * other transition is explicitly set. The initial value is
     * an {@link AutoTransition} instance.
     *
     * @param transition The default transition to be used for scene changes.
     * @hide pending later changes
     */
    public void setDefaultTransition(TransitionPort transition) {
        sDefaultTransition = transition;
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
    private static void changeScene(ScenePort scene, TransitionPort transition) {

        final ViewGroup sceneRoot = scene.getSceneRoot();

        TransitionPort transitionClone = null;
        if (transition != null) {
            transitionClone = transition.clone();
            transitionClone.setSceneRoot(sceneRoot);
        }

        ScenePort oldScene = ScenePort.getCurrentScene(sceneRoot);
        if (oldScene != null && oldScene.isCreatedFromLayoutResource()) {
            transitionClone.setCanRemoveViews(true);
        }

        sceneChangeSetup(sceneRoot, transitionClone);

        scene.enter();

        sceneChangeRunTransition(sceneRoot, transitionClone);
    }

    private static ArrayMap<ViewGroup, ArrayList<TransitionPort>> getRunningTransitions() {
        WeakReference<ArrayMap<ViewGroup, ArrayList<TransitionPort>>> runningTransitions =
                sRunningTransitions.get();
        if (runningTransitions == null || runningTransitions.get() == null) {
            ArrayMap<ViewGroup, ArrayList<TransitionPort>> transitions = new ArrayMap<>();
            runningTransitions = new WeakReference<>(transitions);
            sRunningTransitions.set(runningTransitions);
        }
        return runningTransitions.get();
    }

    private static void sceneChangeRunTransition(final ViewGroup sceneRoot,
            final TransitionPort transition) {
        if (transition != null && sceneRoot != null) {
            MultiListener listener = new MultiListener(transition, sceneRoot);
            sceneRoot.addOnAttachStateChangeListener(listener);
            sceneRoot.getViewTreeObserver().addOnPreDrawListener(listener);
        }
    }

    private static void sceneChangeSetup(ViewGroup sceneRoot, TransitionPort transition) {

        // Capture current values
        ArrayList<TransitionPort> runningTransitions = getRunningTransitions().get(sceneRoot);

        if (runningTransitions != null && runningTransitions.size() > 0) {
            for (TransitionPort runningTransition : runningTransitions) {
                runningTransition.pause(sceneRoot);
            }
        }

        if (transition != null) {
            transition.captureValues(sceneRoot, true);
        }

        // Notify previous scene that it is being exited
        ScenePort previousScene = ScenePort.getCurrentScene(sceneRoot);
        if (previousScene != null) {
            previousScene.exit();
        }
    }

    public static void go(ScenePort scene) {
        changeScene(scene, sDefaultTransition);
    }

    public static void go(ScenePort scene, TransitionPort transition) {
        changeScene(scene, transition);
    }

    public static void beginDelayedTransition(final ViewGroup sceneRoot) {
        beginDelayedTransition(sceneRoot, null);
    }

    public static void beginDelayedTransition(final ViewGroup sceneRoot,
            TransitionPort transition) {
        if (!sPendingTransitions.contains(sceneRoot) && ViewCompat.isLaidOut(sceneRoot)) {
            if (TransitionPort.DBG) {
                Log.d(LOG_TAG, "beginDelayedTransition: root, transition = " +
                        sceneRoot + ", " + transition);
            }
            sPendingTransitions.add(sceneRoot);
            if (transition == null) {
                transition = sDefaultTransition;
            }
            final TransitionPort transitionClone = transition.clone();
            sceneChangeSetup(sceneRoot, transitionClone);
            ScenePort.setCurrentScene(sceneRoot, null);
            sceneChangeRunTransition(sceneRoot, transitionClone);
        }
    }

    public void setTransition(ScenePort scene, TransitionPort transition) {
        mSceneTransitions.put(scene, transition);
    }

    public void setTransition(ScenePort fromScene, ScenePort toScene, TransitionPort transition) {
        ArrayMap<ScenePort, TransitionPort> sceneTransitionMap = mScenePairTransitions.get(toScene);
        if (sceneTransitionMap == null) {
            sceneTransitionMap = new ArrayMap<>();
            mScenePairTransitions.put(toScene, sceneTransitionMap);
        }
        sceneTransitionMap.put(fromScene, transition);
    }

    /**
     * Returns the Transition for the given scene being entered. The result
     * depends not only on the given scene, but also the scene which the
     * {@link ScenePort#getSceneRoot() sceneRoot} of the Scene is currently in.
     *
     * @param scene The scene being entered
     * @return The Transition to be used for the given scene change. If no
     * Transition was specified for this scene change, the default transition
     * will be used instead.
     */
    private TransitionPort getTransition(ScenePort scene) {
        TransitionPort transition;
        ViewGroup sceneRoot = scene.getSceneRoot();
        if (sceneRoot != null) {
            // TODO: cached in Scene instead? long-term, cache in View itself
            ScenePort currScene = ScenePort.getCurrentScene(sceneRoot);
            if (currScene != null) {
                ArrayMap<ScenePort, TransitionPort> sceneTransitionMap = mScenePairTransitions
                        .get(scene);
                if (sceneTransitionMap != null) {
                    transition = sceneTransitionMap.get(currScene);
                    if (transition != null) {
                        return transition;
                    }
                }
            }
        }
        transition = mSceneTransitions.get(scene);
        return (transition != null) ? transition : sDefaultTransition;
    }

    /**
     * Retrieve the transition from a named scene to a target defined scene if one has been
     * associated with this TransitionManager.
     *
     * <p>A named scene is an indirect link for a transition. Fundamentally a named
     * scene represents a potentially arbitrary intersection point of two otherwise independent
     * transitions. Activity A may define a transition from scene X to "com.example.scene.FOO"
     * while activity B may define a transition from scene "com.example.scene.FOO" to scene Y.
     * In this way applications may define an API for more sophisticated transitions between
     * caller and called activities very similar to the way that <code>Intent</code> extras
     * define APIs for arguments and data propagation between activities.</p>
     *
     * @param fromName Named scene that this transition corresponds to
     * @param toScene  Target scene that this transition will move to
     * @return Transition corresponding to the given fromName and toScene or null
     * if no association exists in this TransitionManager
     * @see #setTransition(String, ScenePort, TransitionPort)
     */
    public TransitionPort getNamedTransition(String fromName, ScenePort toScene) {
        ArrayMap<ScenePort, TransitionPort> m = mNameSceneTransitions.get(fromName);
        if (m != null) {
            return m.get(toScene);
        }
        return null;
    }

    ;

    /**
     * Retrieve the transition from a defined scene to a target named scene if one has been
     * associated with this TransitionManager.
     *
     * <p>A named scene is an indirect link for a transition. Fundamentally a named
     * scene represents a potentially arbitrary intersection point of two otherwise independent
     * transitions. Activity A may define a transition from scene X to "com.example.scene.FOO"
     * while activity B may define a transition from scene "com.example.scene.FOO" to scene Y.
     * In this way applications may define an API for more sophisticated transitions between
     * caller and called activities very similar to the way that <code>Intent</code> extras
     * define APIs for arguments and data propagation between activities.</p>
     *
     * @param fromScene Scene that this transition starts from
     * @param toName    Name of the target scene
     * @return Transition corresponding to the given fromScene and toName or null
     * if no association exists in this TransitionManager
     */
    public TransitionPort getNamedTransition(ScenePort fromScene, String toName) {
        ArrayMap<String, TransitionPort> m = mSceneNameTransitions.get(fromScene);
        if (m != null) {
            return m.get(toName);
        }
        return null;
    }

    /**
     * Retrieve the supported target named scenes when transitioning away from the given scene.
     *
     * <p>A named scene is an indirect link for a transition. Fundamentally a named
     * scene represents a potentially arbitrary intersection point of two otherwise independent
     * transitions. Activity A may define a transition from scene X to "com.example.scene.FOO"
     * while activity B may define a transition from scene "com.example.scene.FOO" to scene Y.
     * In this way applications may define an API for more sophisticated transitions between
     * caller and called activities very similar to the way that <code>Intent</code> extras
     * define APIs for arguments and data propagation between activities.</p>
     *
     * @param fromScene Scene to transition from
     * @return An array of Strings naming each supported transition starting from
     * <code>fromScene</code>. If no transitions to a named scene from the given
     * scene are supported this function will return a String[] of length 0.
     * @see #setTransition(ScenePort, String, TransitionPort)
     */
    public String[] getTargetSceneNames(ScenePort fromScene) {
        final ArrayMap<String, TransitionPort> m = mSceneNameTransitions.get(fromScene);
        if (m == null) {
            return EMPTY_STRINGS;
        }
        final int count = m.size();
        final String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = m.keyAt(i);
        }
        return result;
    }

    /**
     * Set a transition from a specific scene to a named scene.
     *
     * <p>A named scene is an indirect link for a transition. Fundamentally a named
     * scene represents a potentially arbitrary intersection point of two otherwise independent
     * transitions. Activity A may define a transition from scene X to "com.example.scene.FOO"
     * while activity B may define a transition from scene "com.example.scene.FOO" to scene Y.
     * In this way applications may define an API for more sophisticated transitions between
     * caller and called activities very similar to the way that <code>Intent</code> extras
     * define APIs for arguments and data propagation between activities.</p>
     *
     * @param fromScene  Scene to transition from
     * @param toName     Named scene to transition to
     * @param transition Transition to use
     * @see #getTargetSceneNames(ScenePort)
     */
    public void setTransition(ScenePort fromScene, String toName, TransitionPort transition) {
        ArrayMap<String, TransitionPort> m = mSceneNameTransitions.get(fromScene);
        if (m == null) {
            m = new ArrayMap<>();
            mSceneNameTransitions.put(fromScene, m);
        }
        m.put(toName, transition);
    }

    /**
     * Set a transition from a named scene to a concrete scene.
     *
     * <p>A named scene is an indirect link for a transition. Fundamentally a named
     * scene represents a potentially arbitrary intersection point of two otherwise independent
     * transitions. Activity A may define a transition from scene X to "com.example.scene.FOO"
     * while activity B may define a transition from scene "com.example.scene.FOO" to scene Y.
     * In this way applications may define an API for more sophisticated transitions between
     * caller and called activities very similar to the way that <code>Intent</code> extras
     * define APIs for arguments and data propagation between activities.</p>
     *
     * @param fromName   Named scene to transition from
     * @param toScene    Scene to transition to
     * @param transition Transition to use
     * @see #getNamedTransition(String, ScenePort)
     */
    public void setTransition(String fromName, ScenePort toScene, TransitionPort transition) {
        ArrayMap<ScenePort, TransitionPort> m = mNameSceneTransitions.get(fromName);
        if (m == null) {
            m = new ArrayMap<>();
            mNameSceneTransitions.put(fromName, m);
        }
        m.put(toScene, transition);
    }

    public void transitionTo(ScenePort scene) {
        // Auto transition if there is no transition declared for the Scene, but there is
        // a root or parent view
        changeScene(scene, getTransition(scene));
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

        TransitionPort mTransition;

        ViewGroup mSceneRoot;

        MultiListener(TransitionPort transition, ViewGroup sceneRoot) {
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
            ArrayList<TransitionPort> runningTransitions = getRunningTransitions().get(mSceneRoot);
            if (runningTransitions != null && runningTransitions.size() > 0) {
                for (TransitionPort runningTransition : runningTransitions) {
                    runningTransition.resume(mSceneRoot);
                }
            }
            mTransition.clearValues(true);
        }

        @Override
        public boolean onPreDraw() {
            removeListeners();
            sPendingTransitions.remove(mSceneRoot);
            // Add to running list, handle end to remove it
            final ArrayMap<ViewGroup, ArrayList<TransitionPort>> runningTransitions =
                    getRunningTransitions();
            ArrayList<TransitionPort> currentTransitions = runningTransitions.get(mSceneRoot);
            ArrayList<TransitionPort> previousRunningTransitions = null;
            if (currentTransitions == null) {
                currentTransitions = new ArrayList<>();
                runningTransitions.put(mSceneRoot, currentTransitions);
            } else if (currentTransitions.size() > 0) {
                previousRunningTransitions = new ArrayList<>(currentTransitions);
            }
            currentTransitions.add(mTransition);
            mTransition.addListener(new TransitionPort.TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(TransitionPort transition) {
                    ArrayList<TransitionPort> currentTransitions =
                            runningTransitions.get(mSceneRoot);
                    currentTransitions.remove(transition);
                }
            });
            mTransition.captureValues(mSceneRoot, false);
            if (previousRunningTransitions != null) {
                for (TransitionPort runningTransition : previousRunningTransitions) {
                    runningTransition.resume(mSceneRoot);
                }
            }
            mTransition.playTransition(mSceneRoot);

            return true;
        }
    }
}
