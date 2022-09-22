/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.constraintlayout.motion.widget;

import androidx.constraintlayout.motion.widget.MotionScene.Transition;
import androidx.constraintlayout.widget.ConstraintSet;

/**
 * Builder class for creating {@link Transition} programmatically.
 */
public class TransitionBuilder {
    private static final String TAG = "TransitionBuilder";

    /**
     * It validates if the motion layout is setup correctly or not. Use this for debugging purposes.
     */
    public static void validate(MotionLayout layout) {
        if (layout.mScene == null) {
            throw new RuntimeException("Invalid motion layout. Layout missing Motion Scene.");
        }

        MotionScene scene = layout.mScene;
        if (!scene.validateLayout(layout)) {
            throw new RuntimeException("MotionLayout doesn't have the right motion scene.");
        }

        if (scene.mCurrentTransition == null || scene.getDefinedTransitions().isEmpty()) {
            throw new RuntimeException("Invalid motion layout. "
                    + "Motion Scene doesn't have any transition.");
        }
    }

    /**
     * Builder for a basic transition that transition from the startConstraintSet to
     * the endConstraintSet.
     * @param scene
     * @param transitionId a unique id to represent the created transition
     * @param startConstraintSetId
     * @param startConstraintSet
     * @param endConstraintSetId
     * @param endConstraintSet
     * @return
     */
    public static Transition buildTransition(
            MotionScene scene,
            int transitionId,
            int startConstraintSetId,
            ConstraintSet startConstraintSet,
            int endConstraintSetId,
            ConstraintSet endConstraintSet) {
        Transition transition = new Transition(
                transitionId,
                scene,
                startConstraintSetId,
                endConstraintSetId);

        updateConstraintSetInMotionScene(scene, transition, startConstraintSet, endConstraintSet);
        return transition;
    }

    /**
     * Ensure that motion scene understands the constraint set and its respective ids.
     */
    private static void updateConstraintSetInMotionScene(
            MotionScene scene,
            Transition transition,
            ConstraintSet startConstraintSet,
            ConstraintSet endConstraintSet) {
        int startId = transition.getStartConstraintSetId();
        int endId = transition.getEndConstraintSetId();

        scene.setConstraintSet(startId, startConstraintSet);
        scene.setConstraintSet(endId, endConstraintSet);
    }

}
