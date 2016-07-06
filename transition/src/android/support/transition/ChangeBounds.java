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

import android.animation.Animator;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

/**
 * This transition captures the layout bounds of target views before and after
 * the scene change and animates those changes during the transition.
 *
 * <p>Unlike the platform version, this does not support use in XML resources.</p>
 */
public class ChangeBounds extends Transition {

    public ChangeBounds() {
        super(true);
        if (Build.VERSION.SDK_INT < 19) {
            mImpl = new ChangeBoundsIcs(this);
        } else {
            mImpl = new ChangeBoundsKitKat(this);
        }
    }

    @Override
    public void captureEndValues(@NonNull TransitionValues transitionValues) {
        mImpl.captureEndValues(transitionValues);
    }

    @Override
    public void captureStartValues(@NonNull TransitionValues transitionValues) {
        mImpl.captureStartValues(transitionValues);
    }

    @Override
    @Nullable
    public Animator createAnimator(@NonNull ViewGroup sceneRoot,
            @NonNull TransitionValues startValues, @NonNull TransitionValues endValues) {
        return mImpl.createAnimator(sceneRoot, startValues, endValues);
    }

    /**
     * When <code>resizeClip</code> is true, ChangeBounds resizes the view using the clipBounds
     * instead of changing the dimensions of the view during the animation. When
     * <code>resizeClip</code> is false, ChangeBounds resizes the View by changing its dimensions.
     *
     * <p>When resizeClip is set to true, the clip bounds is modified by ChangeBounds. Therefore,
     * {@link android.transition.ChangeClipBounds} is not compatible with ChangeBounds
     * in this mode.</p>
     *
     * @param resizeClip Used to indicate whether the view bounds should be modified or the
     *                   clip bounds should be modified by ChangeBounds.
     * @see android.view.View#setClipBounds(android.graphics.Rect)
     */
    public void setResizeClip(boolean resizeClip) {
        ((ChangeBoundsInterface) mImpl).setResizeClip(resizeClip);
    }

}
