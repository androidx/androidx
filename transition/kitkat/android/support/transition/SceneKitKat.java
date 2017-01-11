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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.support.annotation.RequiresApi;

@RequiresApi(19)
@TargetApi(19)
class SceneKitKat extends SceneWrapper {

    private static Field sEnterAction;
    private static Method sSetCurrentScene;

    private View mLayout; // alternative to layoutId

    @Override
    public void init(ViewGroup sceneRoot) {
        mScene = new android.transition.Scene(sceneRoot);
    }

    @Override
    public void init(ViewGroup sceneRoot, View layout) {
        if (layout instanceof ViewGroup) {
            mScene = new android.transition.Scene(sceneRoot, (ViewGroup) layout);
        } else {
            mScene = new android.transition.Scene(sceneRoot);
            mLayout = layout;
        }
    }

    @Override
    public void enter() {
        if (mLayout != null) {
            // empty out parent container before adding to it
            final ViewGroup root = getSceneRoot();
            root.removeAllViews();
            root.addView(mLayout);
            invokeEnterAction();
            updateCurrentScene(root);
        } else {
            mScene.enter();
        }
    }

    private void invokeEnterAction() {
        if (sEnterAction == null) {
            try {
                sEnterAction = android.transition.Scene.class.getDeclaredField("mEnterAction");
                sEnterAction.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            final Runnable enterAction = (Runnable) sEnterAction.get(mScene);
            if (enterAction != null) {
                enterAction.run();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /** Sets this Scene as the current scene of the View. */
    private void updateCurrentScene(View view) {
        if (sSetCurrentScene == null) {
            try {
                sSetCurrentScene = android.transition.Scene.class.getDeclaredMethod(
                        "setCurrentScene", View.class, android.transition.Scene.class);
                sSetCurrentScene.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            sSetCurrentScene.invoke(null, view, mScene);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
