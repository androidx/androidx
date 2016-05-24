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

class TransitionSetKitKat extends TransitionKitKat implements TransitionSetImpl {

    private android.transition.TransitionSet mTransitionSet;

    public TransitionSetKitKat(TransitionInterface transition) {
        mTransitionSet = new android.transition.TransitionSet();
        init(transition, mTransitionSet);
    }

    @Override
    public int getOrdering() {
        return mTransitionSet.getOrdering();
    }

    @Override
    public TransitionSetKitKat setOrdering(int ordering) {
        mTransitionSet.setOrdering(ordering);
        return this;
    }

    @Override
    public TransitionSetKitKat addTransition(TransitionImpl transition) {
        mTransitionSet.addTransition(((TransitionKitKat) transition).mTransition);
        return this;
    }

    @Override
    public TransitionSetKitKat removeTransition(TransitionImpl transition) {
        mTransitionSet.removeTransition(((TransitionKitKat) transition).mTransition);
        return this;
    }

}
