/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.app;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.fragment.R;

class FragmentAnim {
    /**
     * Static util classes shouldn't be instantiated.
     */
    private FragmentAnim() {
    }

    static AnimationOrAnimator loadAnimation(@NonNull Context context,
            @NonNull FragmentContainer fragmentContainer, @NonNull Fragment fragment,
            boolean enter) {
        int transit = fragment.getNextTransition();
        int nextAnim = fragment.getNextAnim();
        // Clear the Fragment animation
        fragment.setNextAnim(0);
        // We do not need to keep up with the removing Fragment after we get its next animation.
        // If transactions do not allow reordering, this will always be true and the visible
        // removing fragment will be cleared. If reordering is allowed, this will only be true
        // after all records in a transaction have been executed and the visible removing
        // fragment has the correct animation, so it is time to clear it.
        View container = fragmentContainer.onFindViewById(fragment.mContainerId);
        if (container != null
                && container.getTag(R.id.visible_removing_fragment_view_tag) != null) {
            container.setTag(R.id.visible_removing_fragment_view_tag, null);
        }
        // If there is a transition on the container, clear those set on the fragment
        if (fragment.mContainer != null && fragment.mContainer.getLayoutTransition() != null) {
            return null;
        }
        Animation animation = fragment.onCreateAnimation(transit, enter, nextAnim);
        if (animation != null) {
            return new AnimationOrAnimator(animation);
        }

        Animator animator = fragment.onCreateAnimator(transit, enter, nextAnim);
        if (animator != null) {
            return new AnimationOrAnimator(animator);
        }

        if (nextAnim != 0) {
            String dir = context.getResources().getResourceTypeName(nextAnim);
            boolean isAnim = "anim".equals(dir);
            boolean successfulLoad = false;
            if (isAnim) {
                // try AnimationUtils first
                try {
                    animation = AnimationUtils.loadAnimation(context, nextAnim);
                    if (animation != null) {
                        return new AnimationOrAnimator(animation);
                    }
                    // A null animation may be returned and that is acceptable
                    successfulLoad = true; // succeeded in loading animation, but it is null
                } catch (Resources.NotFoundException e) {
                    throw e; // Rethrow it -- the resource should be found if it is provided.
                } catch (RuntimeException e) {
                    // Other exceptions can occur when loading an Animator from AnimationUtils.
                }
            }
            if (!successfulLoad) {
                // try Animator
                try {
                    animator = AnimatorInflater.loadAnimator(context, nextAnim);
                    if (animator != null) {
                        return new AnimationOrAnimator(animator);
                    }
                } catch (RuntimeException e) {
                    if (isAnim) {
                        // Rethrow it -- we already tried AnimationUtils and it failed.
                        throw e;
                    }
                    // Otherwise, it is probably an animation resource
                    animation = AnimationUtils.loadAnimation(context, nextAnim);
                    if (animation != null) {
                        return new AnimationOrAnimator(animation);
                    }
                }
            }
        }

        if (transit == 0) {
            return null;
        }

        int animResourceId = transitToAnimResourceId(transit, enter);
        if (animResourceId < 0) {
            return null;
        }

        return new AnimationOrAnimator(AnimationUtils.loadAnimation(
                context,
                animResourceId
        ));
    }

    @AnimRes
    private static int transitToAnimResourceId(int transit, boolean enter) {
        int animAttr = -1;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                animAttr = enter ? R.anim.fragment_open_enter : R.anim.fragment_open_exit;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                animAttr = enter ? R.anim.fragment_close_enter : R.anim.fragment_close_exit;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                animAttr = enter ? R.anim.fragment_fade_enter : R.anim.fragment_fade_exit;
                break;
        }
        return animAttr;
    }

    /**
     * Contains either an animator or animation. One of these should be null.
     */
    static class AnimationOrAnimator {
        public final Animation animation;
        public final Animator animator;

        AnimationOrAnimator(Animation animation) {
            this.animation = animation;
            this.animator = null;
            if (animation == null) {
                throw new IllegalStateException("Animation cannot be null");
            }
        }

        AnimationOrAnimator(Animator animator) {
            this.animation = null;
            this.animator = animator;
            if (animator == null) {
                throw new IllegalStateException("Animator cannot be null");
            }
        }
    }
}
