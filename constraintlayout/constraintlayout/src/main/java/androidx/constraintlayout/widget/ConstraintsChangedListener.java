/*
 * Copyright (C) 2018 The Android Open Source Project
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
package androidx.constraintlayout.widget;

/**
 * <b>Added in 2.0</b>
 * <p>
 * Callbacks on state change
 * </p>
 */
public abstract class ConstraintsChangedListener {

    /**
     * called before layout happens
     * @param stateId -1 if state unknown, otherwise the state we will transition to
     * @param constraintId the constraintSet id that we will transition to
     */
    public void preLayoutChange(int stateId, int constraintId){
        // nothing
    }

    /**
     * called after layout happens
     * @param stateId -1 if state unknown, otherwise the current state
     * @param constraintId the current constraintSet id we transitioned to
     */
    public void postLayoutChange(int stateId, int constraintId) {
        // nothing
    }
}
