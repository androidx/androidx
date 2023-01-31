/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.state;

/**
 * This defines the interface to motionScene functionality
 */
public interface CoreMotionScene {

    /**
     * set the Transitions string onto the MotionScene
     *
     * @param elementName the name of the element
     */
    void setTransitionContent(String elementName, String toJSON);

    /**
     * Get the ConstraintSet as a string
     */
    String getConstraintSet(String ext);

    /**
     * set the constraintSet json string
     *
     * @param csName the name of the constraint set
     * @param toJSON the json string of the constraintset
     */
    void setConstraintSetContent(String csName, String toJSON);

    /**
     * set the debug name for remote access
     *
     * @param name name to call this motion scene
     */
    void setDebugName(String name);

    /**
     * get a transition give the name
     *
     * @param str the name of the transition
     * @return the json of the transition
     */
    String getTransition(String str);

    /**
     * get a constraintset
     *
     * @param index of the constraintset
     */
    String getConstraintSet(int index);
}
