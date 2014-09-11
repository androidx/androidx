/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.transition;

import android.transition.ChangeTransform;
import android.view.Window;

final class TransitionHelperApi21 {

    TransitionHelperApi21() {
    }

    public Object getSharedElementEnterTransition(Window window) {
        return window.getSharedElementEnterTransition();
    }

    public Object getSharedElementReturnTransition(Window window) {
        return window.getSharedElementReturnTransition();
    }

    public Object getSharedElementExitTransition(Window window) {
        return window.getSharedElementExitTransition();
    }

    public Object getSharedElementReenterTransition(Window window) {
        return window.getSharedElementReenterTransition();
    }

    public Object getEnterTransition(Window window) {
        return window.getEnterTransition();
    }

    public Object getReturnTransition(Window window) {
        return window.getReturnTransition();
    }

    public Object getExitTransition(Window window) {
        return window.getExitTransition();
    }

    public Object getReenterTransition(Window window) {
        return window.getReenterTransition();
    }

    public Object createScale() {
        return new ChangeTransform();
    }
}
