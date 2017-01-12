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

import android.transition.TransitionManager;

import android.annotation.TargetApi;
import android.support.annotation.RequiresApi;

@RequiresApi(19)
@TargetApi(19)
class TransitionManagerKitKat extends TransitionManagerImpl {

    private final android.transition.TransitionManager mTransitionManager = new TransitionManager();

    @Override
    public void setTransition(SceneImpl scene, TransitionImpl transition) {
        mTransitionManager.setTransition(((SceneWrapper) scene).mScene,
                transition == null ? null : ((TransitionKitKat) transition).mTransition);
    }

    @Override
    public void setTransition(SceneImpl fromScene, SceneImpl toScene, TransitionImpl transition) {
        mTransitionManager.setTransition(((SceneWrapper) fromScene).mScene,
                ((SceneWrapper) toScene).mScene,
                transition == null ? null : ((TransitionKitKat) transition).mTransition);
    }

    @Override
    public void transitionTo(SceneImpl scene) {
        mTransitionManager.transitionTo(((SceneWrapper) scene).mScene);
    }

}
