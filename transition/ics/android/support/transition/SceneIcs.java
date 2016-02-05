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

import android.view.View;
import android.view.ViewGroup;

class SceneIcs extends SceneImpl {

    /* package */ ScenePort mScene;

    @Override
    public void init(ViewGroup sceneRoot) {
        mScene = new ScenePort(sceneRoot);
    }

    @Override
    public void init(ViewGroup sceneRoot, View layout) {
        mScene = new ScenePort(sceneRoot, layout);
    }

    @Override
    public void enter() {
        mScene.enter();
    }

    @Override
    public void exit() {
        mScene.exit();
    }


    @Override
    public ViewGroup getSceneRoot() {
        return mScene.getSceneRoot();
    }

    @Override
    public void setEnterAction(Runnable action) {
        mScene.setEnterAction(action);
    }

    @Override
    public void setExitAction(Runnable action) {
        mScene.setExitAction(action);
    }

}
