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

package android.support.transition;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;

@RequiresApi(14)
class AnimatorUtilsApi14 implements AnimatorUtilsImpl {

    @Override
    public void addPauseListener(@NonNull Animator animator,
            @NonNull AnimatorListenerAdapter listener) {
        // Do nothing
    }

    @Override
    public void pause(@NonNull Animator animator) {
        final ArrayList<Animator.AnimatorListener> listeners = animator.getListeners();
        if (listeners != null) {
            for (int i = 0, size = listeners.size(); i < size; i++) {
                final Animator.AnimatorListener listener = listeners.get(i);
                if (listener instanceof AnimatorPauseListenerCompat) {
                    ((AnimatorPauseListenerCompat) listener).onAnimationPause(animator);
                }
            }
        }
    }

    @Override
    public void resume(@NonNull Animator animator) {
        final ArrayList<Animator.AnimatorListener> listeners = animator.getListeners();
        if (listeners != null) {
            for (int i = 0, size = listeners.size(); i < size; i++) {
                final Animator.AnimatorListener listener = listeners.get(i);
                if (listener instanceof AnimatorPauseListenerCompat) {
                    ((AnimatorPauseListenerCompat) listener).onAnimationResume(animator);
                }
            }
        }
    }

    /**
     * Listeners can implement this interface in addition to the platform AnimatorPauseListener to
     * make them compatible with API level 18 and below. Animators will not be paused or resumed,
     * but the callbacks here are invoked.
     */
    interface AnimatorPauseListenerCompat {

        void onAnimationPause(Animator animation);

        void onAnimationResume(Animator animation);

    }

}
