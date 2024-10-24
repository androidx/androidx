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
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;

import androidx.annotation.AnimRes;
import androidx.core.view.OneShotPreDrawListener;
import androidx.fragment.R;

import org.jspecify.annotations.NonNull;

class FragmentAnim {
    /**
     * Static util classes shouldn't be instantiated.
     */
    private FragmentAnim() {
    }

    @SuppressLint("ResourceType")
    static AnimationOrAnimator loadAnimation(@NonNull Context context,
            @NonNull Fragment fragment, boolean enter, boolean isPop) {
        int transit = fragment.getNextTransition();
        int nextAnim = getNextAnim(fragment, enter, isPop);
        // Clear the Fragment animations
        fragment.setAnimations(0, 0, 0, 0);
        // We do not need to keep up with the removing Fragment after we get its next animation.
        // If transactions do not allow reordering, this will always be true and the visible
        // removing fragment will be cleared. If reordering is allowed, this will only be true
        // after all records in a transaction have been executed and the visible removing
        // fragment has the correct animation, so it is time to clear it.
        if (fragment.mContainer != null
                && fragment.mContainer.getTag(R.id.visible_removing_fragment_view_tag) != null) {
            fragment.mContainer.setTag(R.id.visible_removing_fragment_view_tag, null);
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

        if (nextAnim == 0 && transit != 0) {
            nextAnim = transitToAnimResourceId(context, transit, enter);
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
        return null;
    }

    @AnimRes
    private static int getNextAnim(Fragment fragment, boolean enter, boolean isPop) {
        if (isPop) {
            if (enter) {
                return fragment.getPopEnterAnim();
            } else {
                return fragment.getPopExitAnim();
            }
        } else {
            if (enter) {
                return fragment.getEnterAnim();
            } else {
                return fragment.getExitAnim();
            }
        }
    }

    @AnimRes
    private static int transitToAnimResourceId(@NonNull Context context, int transit,
            boolean enter) {
        int animAttr = -1;
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                animAttr = enter ? R.animator.fragment_open_enter : R.animator.fragment_open_exit;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_CLOSE:
                animAttr = enter ? R.animator.fragment_close_enter : R.animator.fragment_close_exit;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                animAttr = enter ? R.animator.fragment_fade_enter : R.animator.fragment_fade_exit;
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_OPEN:
                animAttr = enter
                        ? toActivityTransitResId(context, android.R.attr.activityOpenEnterAnimation)
                        : toActivityTransitResId(context, android.R.attr.activityOpenExitAnimation);
                break;
            case FragmentTransaction.TRANSIT_FRAGMENT_MATCH_ACTIVITY_CLOSE:
                animAttr = enter
                        ? toActivityTransitResId(context,
                        android.R.attr.activityCloseEnterAnimation)
                        : toActivityTransitResId(context,
                                android.R.attr.activityCloseExitAnimation);
                break;
        }
        return animAttr;
    }

    @AnimRes
    private static int toActivityTransitResId(@NonNull Context context, int attrInt) {
        int resId;
        TypedArray typedArray = context.obtainStyledAttributes(
                android.R.style.Animation_Activity, new int[]{attrInt});
        resId = typedArray.getResourceId(0, View.NO_ID);
        typedArray.recycle();
        return resId;
    }

    /**
     * Contains either an animator or animation. One of these should be null.
     */
    static class AnimationOrAnimator {
        public final Animation animation;
        public final AnimatorSet animator;

        AnimationOrAnimator(Animation animation) {
            this.animation = animation;
            this.animator = null;
            if (animation == null) {
                throw new IllegalStateException("Animation cannot be null");
            }
        }

        AnimationOrAnimator(Animator animator) {
            this.animation = null;
            this.animator = new AnimatorSet();
            this.animator.play(animator);
            if (animator == null) {
                throw new IllegalStateException("Animator cannot be null");
            }
        }
    }

    /**
     * We must call endViewTransition() before the animation ends or else the parent doesn't
     * get nulled out. We use both startViewTransition() and startAnimation() to solve a problem
     * with Views remaining in the hierarchy as disappearing children after the view has been
     * removed in some edge cases.
     */
    static class EndViewTransitionAnimation extends AnimationSet implements Runnable {
        private final ViewGroup mParent;
        private final View mChild;
        private boolean mEnded;
        private boolean mTransitionEnded;
        private boolean mAnimating = true;

        EndViewTransitionAnimation(@NonNull Animation animation,
                @NonNull ViewGroup parent, @NonNull View child) {
            super(false);
            mParent = parent;
            mChild = child;
            addAnimation(animation);
            // We must call endViewTransition() even if the animation was never run or it
            // is interrupted in a way that can't be detected easily (app put in background)
            mParent.post(this);
        }

        @Override
        public boolean getTransformation(long currentTime, @NonNull Transformation t) {
            mAnimating = true;
            if (mEnded) {
                return !mTransitionEnded;
            }
            boolean more = super.getTransformation(currentTime, t);
            if (!more) {
                mEnded = true;
                OneShotPreDrawListener.add(mParent, this);
            }
            return true;
        }

        @Override
        public boolean getTransformation(long currentTime,
                @NonNull Transformation outTransformation, float scale) {
            mAnimating = true;
            if (mEnded) {
                return !mTransitionEnded;
            }
            boolean more = super.getTransformation(currentTime, outTransformation, scale);
            if (!more) {
                mEnded = true;
                OneShotPreDrawListener.add(mParent, this);
            }
            return true;
        }

        @Override
        public void run() {
            if (!mEnded && mAnimating) {
                mAnimating = false;
                // Called while animating, so we'll check again on next cycle
                mParent.post(this);
            } else {
                mParent.endViewTransition(mChild);
                mTransitionEnded = true;
            }
        }
    }
}
