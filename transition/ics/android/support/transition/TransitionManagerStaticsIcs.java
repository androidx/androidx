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

import android.view.ViewGroup;

class TransitionManagerStaticsIcs extends TransitionManagerStaticsImpl {

    @Override
    public void go(SceneImpl scene) {
        TransitionManagerPort.go(((SceneIcs) scene).mScene);
    }

    @Override
    public void go(SceneImpl scene, TransitionImpl transition) {
        TransitionManagerPort.go(((SceneIcs) scene).mScene,
                transition == null ? null : ((TransitionIcs) transition).mTransition);
    }

    @Override
    public void beginDelayedTransition(ViewGroup sceneRoot) {
        TransitionManagerPort.beginDelayedTransition(sceneRoot);
    }

    @Override
    public void beginDelayedTransition(ViewGroup sceneRoot, TransitionImpl transition) {
        TransitionManagerPort.beginDelayedTransition(sceneRoot,
                transition == null ? null : ((TransitionIcs) transition).mTransition);
    }

}
