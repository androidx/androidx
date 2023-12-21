/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.renderer.dynamicdata;

import android.graphics.drawable.AnimatedVectorDrawable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/* A testable AVD implementation. */
class TestAnimatedVectorDrawable extends AnimatedVectorDrawable {
    public boolean started = false;
    public boolean reset = false;

    // We need to intercept callbacks and save it in this test class as shadow drawable doesn't seem
    // to call onEnd listener, meaning that quota won't be freed and we would get failing test.
    private final List<AnimationCallback> mAnimationCallbacks = new ArrayList<>();

    @Override
    public void start() {
        super.start();
        started = true;
        reset = false;
    }

    @Override
    public void registerAnimationCallback(@NonNull AnimationCallback callback) {
        super.registerAnimationCallback(callback);
        mAnimationCallbacks.add(callback);
    }

    @Override
    public boolean unregisterAnimationCallback(@NonNull AnimationCallback callback) {
        mAnimationCallbacks.remove(callback);
        return super.unregisterAnimationCallback(callback);
    }

    @Override
    public void stop() {
        super.stop();
        started = false;
        mAnimationCallbacks.forEach(c -> c.onAnimationEnd(this));
    }

    @Override
    public void reset() {
        super.reset();
        started = false;
        reset = true;
        mAnimationCallbacks.forEach(c -> c.onAnimationEnd(this));
    }

    @Override
    public boolean isRunning() {
        super.isRunning();
        return started;
    }
}
