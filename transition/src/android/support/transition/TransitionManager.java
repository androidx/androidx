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

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

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
 * <p>Unlike the platform version, this does not support declaration by XML resources.</p>
 */
public class TransitionManager {

    private static TransitionManagerStaticsImpl sImpl;

    static {
        if (Build.VERSION.SDK_INT < 19) {
            sImpl = new TransitionManagerStaticsIcs();
        } else {
            sImpl = new TransitionManagerStaticsKitKat();
        }
    }

    private TransitionManagerImpl mImpl;

    public TransitionManager() {
        if (Build.VERSION.SDK_INT < 19) {
            mImpl = new TransitionManagerIcs();
        } else {
            mImpl = new TransitionManagerKitKat();
        }
    }

    /**
     * Convenience method to simply change to the given scene using
     * the default transition for TransitionManager.
     *
     * @param scene The Scene to change to
     */
    public static void go(@NonNull Scene scene) {
        sImpl.go(scene.mImpl);
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
        sImpl.go(scene.mImpl, transition == null ? null : transition.mImpl);
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
        sImpl.beginDelayedTransition(sceneRoot);
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
        sImpl.beginDelayedTransition(sceneRoot, transition == null ? null : transition.mImpl);
    }

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
        mImpl.setTransition(scene.mImpl, transition == null ? null : transition.mImpl);
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
        mImpl.setTransition(fromScene.mImpl, toScene.mImpl,
                transition == null ? null : transition.mImpl);
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
        mImpl.transitionTo(scene.mImpl);
    }

}
