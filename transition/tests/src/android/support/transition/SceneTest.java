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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;

import android.support.transition.test.R;
import android.test.suitebuilder.annotation.SmallTest;
import android.view.ViewGroup;

import org.junit.Test;

public class SceneTest extends BaseTest {

    @Test
    public void testGetSceneRoot() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        Scene scene = new Scene(root);
        assertThat(scene.getSceneRoot(), is(sameInstance(root)));
    }

    @Test
    public void testEnterAction() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        Scene scene = new Scene(root);
        CheckCalledRunnable runnable = new CheckCalledRunnable();
        scene.setEnterAction(runnable);
        scene.enter();
        assertThat(runnable.wasCalled(), is(true));
    }

    @Test
    public void testExitAction() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        Scene scene = new Scene(root);
        scene.enter();
        CheckCalledRunnable runnable = new CheckCalledRunnable();
        scene.setExitAction(runnable);
        scene.exit();
        assertThat(runnable.wasCalled(), is(true));
    }

    @Test
    public void testExitAction_withoutEnter() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        Scene scene = new Scene(root);
        CheckCalledRunnable runnable = new CheckCalledRunnable();
        scene.setExitAction(runnable);
        scene.exit();
        assertThat(runnable.wasCalled(), is(false));
    }

    @Test
    public void testGetSceneForLayout_cache() {
        TransitionActivity activity = rule.getActivity();
        ViewGroup root = activity.getRoot();
        Scene scene = Scene.getSceneForLayout(root, R.layout.scene0, activity);
        assertThat("getSceneForLayout should return the same instance for subsequent calls",
                Scene.getSceneForLayout(root, R.layout.scene0, activity), is(sameInstance(scene)));
    }

}
