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
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import androidx.annotation.NonNull;
import androidx.core.os.CancellationSignal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * A SpecialEffectsController that hooks into the existing Fragment APIs to run
 * animations and transitions.
 */
class DefaultSpecialEffectsController extends SpecialEffectsController {

    private final HashMap<Operation, HashSet<CancellationSignal>>
            mRunningOperations = new HashMap<>();

    DefaultSpecialEffectsController(@NonNull ViewGroup container) {
        super(container);
    }

    /**
     * Add new {@link CancellationSignal} for special effects
     */
    private void addCancellationSignal(@NonNull Operation operation,
            @NonNull CancellationSignal signal) {
        if (mRunningOperations.get(operation) == null) {
            mRunningOperations.put(operation, new HashSet<CancellationSignal>());
        }
        mRunningOperations.get(operation).add(signal);
    }

    /**
     * Remove a {@link CancellationSignal} that was previously added with
     * {@link #addCancellationSignal(Operation, CancellationSignal)}.
     *
     * This calls through to {@link Operation#complete()} when the last special effect is complete.
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void removeCancellationSignal(@NonNull Operation operation,
            @NonNull CancellationSignal signal) {
        HashSet<CancellationSignal> signals = mRunningOperations.get(operation);
        if (signals != null && signals.remove(signal) && signals.isEmpty()) {
            mRunningOperations.remove(operation);
            operation.complete();
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    void cancelAllSpecialEffects(@NonNull Operation operation) {
        HashSet<CancellationSignal> signals = mRunningOperations.remove(operation);
        if (signals != null) {
            for (CancellationSignal signal : signals) {
                signal.cancel();
            }
        }
    }

    @Override
    void executeOperations(@NonNull List<Operation> operations) {
        for (final Operation operation : operations) {
            // Create the animation CancellationSignal
            CancellationSignal animCancellationSignal = new CancellationSignal();
            addCancellationSignal(operation, animCancellationSignal);
            // TODO Add the transition CancellationSignal

            // Ensure that when the Operation is cancelled, we cancel all special effects
            operation.getCancellationSignal().setOnCancelListener(
                    new CancellationSignal.OnCancelListener() {
                        @Override
                        public void onCancel() {
                            cancelAllSpecialEffects(operation);
                        }
                    });

            // Start animation special effects
            startAnimation(operation, animCancellationSignal);
        }

        // TODO start transition special effects
    }

    private void startAnimation(final @NonNull Operation operation,
            final @NonNull CancellationSignal signal) {
        final ViewGroup container = getContainer();
        final Context context = container.getContext();
        final Fragment fragment = operation.getFragment();
        final View viewToAnimate = fragment.mView;
        FragmentAnim.AnimationOrAnimator anim = FragmentAnim.loadAnimation(context,
                fragment, operation.getType() == Operation.Type.ADD);
        if (anim == null) {
            // No animation, so we can immediately remove the CancellationSignal
            removeCancellationSignal(operation, signal);
            return;
        }
        // We have an animation to run!
        container.startViewTransition(viewToAnimate);
        // Kick off the respective type of animation
        if (anim.animation != null) {
            final Animation animation = operation.getType() == Operation.Type.ADD
                    ? anim.animation
                    : new FragmentAnim.EndViewTransitionAnimation(anim.animation, container,
                            viewToAnimate);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    // onAnimationEnd() comes during draw(), so there can still be some
                    // draw events happening after this call. We don't want to remove the
                    // CancellationSignal until after the onAnimationEnd()
                    container.post(new Runnable() {
                        @Override
                        public void run() {
                            container.endViewTransition(viewToAnimate);
                            removeCancellationSignal(operation, signal);
                        }
                    });
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            viewToAnimate.startAnimation(animation);
        } else { // anim.animator != null
            anim.animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator anim) {
                    container.endViewTransition(viewToAnimate);
                    removeCancellationSignal(operation, signal);
                }
            });
            anim.animator.setTarget(viewToAnimate);
            anim.animator.start();
        }

        // Listen for cancellation and use that to cancel any running animations
        signal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                viewToAnimate.clearAnimation();
            }
        });
    }
}
