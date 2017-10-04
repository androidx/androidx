/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.graphics.drawable;

import static android.os.Build.VERSION_CODES.M;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

/**
 * Interface that drawables supporting animations and callbacks should extend in support lib.
 */
public interface Animatable2Compat extends Animatable {

    /**
     * Adds a callback to listen to the animation events.
     *
     * @param callback Callback to add.
     */
    void registerAnimationCallback(@NonNull AnimationCallback callback);

    /**
     * Removes the specified animation callback.
     *
     * @param callback Callback to remove.
     * @return {@code false} if callback didn't exist in the call back list, or {@code true} if
     *         callback has been removed successfully.
     */
    boolean unregisterAnimationCallback(@NonNull AnimationCallback callback);

    /**
     * Removes all existing animation callbacks.
     */
    void clearAnimationCallbacks();

    /**
     * Abstract class for animation callback. Used to notify animation events.
     */
    abstract class AnimationCallback {
        /**
         * Called when the animation starts.
         *
         * @param drawable The drawable started the animation.
         */
        public void onAnimationStart(Drawable drawable) {};
        /**
         * Called when the animation ends.
         *
         * @param drawable The drawable finished the animation.
         */
        public void onAnimationEnd(Drawable drawable) {};

        // Only when passing this Animatable2Compat.AnimationCallback to a frameworks' AVD, we need
        // to bridge this compat version callback with the frameworks' callback.
        Animatable2.AnimationCallback mPlatformCallback;

        @RequiresApi(M)
        Animatable2.AnimationCallback getPlatformCallback() {
            if (mPlatformCallback == null) {
                mPlatformCallback = new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationStart(Drawable drawable) {
                        AnimationCallback.this.onAnimationStart(drawable);
                    }

                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        AnimationCallback.this.onAnimationEnd(drawable);
                    }
                };
            }
            return mPlatformCallback;
        }
    }
}
