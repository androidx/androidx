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

package androidx.transition;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.os.CancellationSignal;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransitionImpl;

import java.util.ArrayList;
import java.util.List;


/**
 */
// This is instantiated in androidx.fragment.app.FragmentTransition
@SuppressWarnings("unused")
@RestrictTo(LIBRARY_GROUP_PREFIX)
@SuppressLint("RestrictedApi") // remove once fragment lib would be released with the new
// LIBRARY_GROUP_PREFIX restriction. tracking in b/127286008
public class FragmentTransitionSupport extends FragmentTransitionImpl {

    @Override
    public boolean canHandle(@NonNull Object transition) {
        return transition instanceof Transition;
    }

    @Nullable
    @Override
    public Object cloneTransition(@Nullable Object transition) {
        Transition copy = null;
        if (transition != null) {
            copy = ((Transition) transition).clone();
        }
        return copy;
    }

    @Nullable
    @Override
    public Object wrapTransitionInSet(@Nullable Object transition) {
        if (transition == null) {
            return null;
        }
        TransitionSet transitionSet = new TransitionSet();
        transitionSet.addTransition((Transition) transition);
        return transitionSet;
    }

    @Override
    public void setSharedElementTargets(@NonNull Object transitionObj,
            @NonNull View nonExistentView, @NonNull ArrayList<View> sharedViews) {
        TransitionSet transition = (TransitionSet) transitionObj;
        final List<View> views = transition.getTargets();
        views.clear();
        final int count = sharedViews.size();
        for (int i = 0; i < count; i++) {
            final View view = sharedViews.get(i);
            bfsAddViewChildren(views, view);
        }
        views.add(nonExistentView);
        sharedViews.add(nonExistentView);
        addTargets(transition, sharedViews);
    }

    @Override
    public void setEpicenter(@NonNull Object transitionObj, @Nullable View view) {
        if (view != null) {
            Transition transition = (Transition) transitionObj;
            final Rect epicenter = new Rect();
            getBoundsOnScreen(view, epicenter);

            transition.setEpicenterCallback(new Transition.EpicenterCallback() {
                @Override
                public Rect onGetEpicenter(@NonNull Transition transition) {
                    return epicenter;
                }
            });
        }
    }

    @Override
    public void addTargets(@NonNull Object transitionObj, @NonNull ArrayList<View> views) {
        Transition transition = (Transition) transitionObj;
        if (transition == null) {
            return;
        }
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                addTargets(child, views);
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (isNullOrEmpty(targets)) {
                // We can just add the target views
                int numViews = views.size();
                for (int i = 0; i < numViews; i++) {
                    transition.addTarget(views.get(i));
                }
            }
        }
    }

    private static boolean hasSimpleTarget(Transition transition) {
        return !isNullOrEmpty(transition.getTargetIds())
                || !isNullOrEmpty(transition.getTargetNames())
                || !isNullOrEmpty(transition.getTargetTypes());
    }

    @NonNull
    @Override
    public Object mergeTransitionsTogether(@Nullable Object transition1,
            @Nullable Object transition2, @Nullable Object transition3) {
        TransitionSet transitionSet = new TransitionSet();
        if (transition1 != null) {
            transitionSet.addTransition((Transition) transition1);
        }
        if (transition2 != null) {
            transitionSet.addTransition((Transition) transition2);
        }
        if (transition3 != null) {
            transitionSet.addTransition((Transition) transition3);
        }
        return transitionSet;
    }

    @Override
    public void scheduleHideFragmentView(@NonNull Object exitTransitionObj,
            final @NonNull View fragmentView, final @NonNull ArrayList<View> exitingViews) {
        Transition exitTransition = (Transition) exitTransitionObj;
        exitTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {
                // If any of the exiting views are not shared elements, the TransitionManager
                // adds additional listeners to the this transition. If those listeners are
                // DisappearListeners for a view that is going away, they can change the state of
                // views after our onTransitionEnd callback.
                // We need to make sure this listener gets the onTransitionEnd callback last to
                // ensure that exiting views are made visible once the Transition is complete.
                transition.removeListener(this);
                transition.addListener(this);
            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                transition.removeListener(this);
                fragmentView.setVisibility(View.GONE);
                final int numViews = exitingViews.size();
                for (int i = 0; i < numViews; i++) {
                    exitingViews.get(i).setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) {
            }

            @Override
            public void onTransitionPause(@NonNull Transition transition) {
            }

            @Override
            public void onTransitionResume(@NonNull Transition transition) {
            }
        });
    }

    @Nullable
    @Override
    public Object mergeTransitionsInSequence(@Nullable Object exitTransitionObj,
            @Nullable Object enterTransitionObj, @Nullable Object sharedElementTransitionObj) {
        // First do exit, then enter, but allow shared element transition to happen
        // during both.
        Transition staggered = null;
        final Transition exitTransition = (Transition) exitTransitionObj;
        final Transition enterTransition = (Transition) enterTransitionObj;
        final Transition sharedElementTransition = (Transition) sharedElementTransitionObj;
        if (exitTransition != null && enterTransition != null) {
            staggered = new TransitionSet()
                    .addTransition(exitTransition)
                    .addTransition(enterTransition)
                    .setOrdering(TransitionSet.ORDERING_SEQUENTIAL);
        } else if (exitTransition != null) {
            staggered = exitTransition;
        } else if (enterTransition != null) {
            staggered = enterTransition;
        }
        if (sharedElementTransition != null) {
            TransitionSet together = new TransitionSet();
            if (staggered != null) {
                together.addTransition(staggered);
            }
            together.addTransition(sharedElementTransition);
            return together;
        } else {
            return staggered;
        }
    }

    @Override
    public void beginDelayedTransition(@NonNull ViewGroup sceneRoot, @Nullable Object transition) {
        TransitionManager.beginDelayedTransition(sceneRoot, (Transition) transition);
    }

    @Override
    public boolean isSeekingSupported() {
        return true;
    }

    @Override
    public boolean isSeekingSupported(@NonNull Object transition) {
        boolean supported = ((Transition) transition).isSeekingSupported();
        if (!supported) {
            Log.v("FragmentManager",
                    "Predictive back not available for AndroidX Transition "
                            + transition + ". Please enable seeking support for the designated "
                            + "transition by overriding isSeekingSupported().");
        }
        return supported;
    }

    @Override
    @Nullable
    public Object controlDelayedTransition(@NonNull ViewGroup sceneRoot,
            @NonNull Object transition) {
        return TransitionManager.controlDelayedTransition(sceneRoot, (Transition) transition);
    }

    @Override
    public void setCurrentPlayTime(@NonNull Object transitionController, float progress) {
        TransitionSeekController controller = (TransitionSeekController) transitionController;
        if (controller.isReady()) {
            long time = (long) (progress * controller.getDurationMillis());
            // We cannot let the time get to 0 or the totalDuration to avoid
            // completing the operation accidentally.
            if (time == 0L) {
                time = 1L;
            }
            if (time == controller.getDurationMillis()) {
                time = controller.getDurationMillis() - 1;
            }
            controller.setCurrentPlayTimeMillis(time);
        }
    }

    @Override
    public void animateToEnd(@NonNull Object transitionController) {
        TransitionSeekController controller = (TransitionSeekController) transitionController;
        controller.animateToEnd();
    }

    @NonNull
    @Override
    public Runnable animateToStart(@NonNull Object transitionController,
            @NonNull ViewGroup sceneRoot, @NonNull Runnable completeRunnable) {
        TransitionSeekController controller = (TransitionSeekController) transitionController;
        controller.animateToStart();
        return () -> {
            final Fade zeroDurationTransition = new Fade();
            zeroDurationTransition.setDuration(0);
            zeroDurationTransition.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(@NonNull Transition transition) {
                    completeRunnable.run();
                }
            });
            TransitionManager.beginDelayedTransition(sceneRoot, zeroDurationTransition);
        };
    }

    @Override
    public void scheduleRemoveTargets(final @NonNull Object overallTransitionObj,
            final @Nullable Object enterTransition, final @Nullable ArrayList<View> enteringViews,
            final @Nullable Object exitTransition, final @Nullable ArrayList<View> exitingViews,
            final @Nullable Object sharedElementTransition,
            final @Nullable ArrayList<View> sharedElementsIn) {
        final Transition overallTransition = (Transition) overallTransitionObj;
        overallTransition.addListener(new TransitionListenerAdapter() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) {
                if (enterTransition != null) {
                    replaceTargets(enterTransition, enteringViews, null);
                }
                if (exitTransition != null) {
                    replaceTargets(exitTransition, exitingViews, null);
                }
                if (sharedElementTransition != null) {
                    replaceTargets(sharedElementTransition, sharedElementsIn, null);
                }
            }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                transition.removeListener(this);
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * If either exitingViews or SharedElementsOut contain a view, an
     * {@link Transition.TransitionListener#onTransitionEnd} listener is added that calls
     * {@link Runnable#run()} once the Transition ends.
     *
     * If {@link CancellationSignal#cancel()} is called on the given signal, the transition calls
     * {@link Transition#cancel()}.
     */
    @Override
    public void setListenerForTransitionEnd(@NonNull final Fragment outFragment,
            @NonNull final Object transition, @NonNull final CancellationSignal signal,
            @NonNull final Runnable transitionCompleteRunnable) {
        setListenerForTransitionEnd(outFragment, transition, signal,
                null, transitionCompleteRunnable);
    }

    @Override
    public void setListenerForTransitionEnd(@NonNull Fragment outFragment,
            @NonNull Object transition, @NonNull CancellationSignal signal,
            @Nullable Runnable cancelRunnable, @NonNull Runnable transitionCompleteRunnable) {
        final Transition realTransition = ((Transition) transition);
        signal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
            @Override
            public void onCancel() {
                if (cancelRunnable == null) {
                    realTransition.cancel();
                } else {
                    cancelRunnable.run();
                }
            }
        });
        realTransition.addListener(new Transition.TransitionListener() {
            @Override
            public void onTransitionStart(@NonNull Transition transition) { }

            @Override
            public void onTransitionEnd(@NonNull Transition transition) {
                transitionCompleteRunnable.run();
            }

            @Override
            public void onTransitionCancel(@NonNull Transition transition) { }

            @Override
            public void onTransitionPause(@NonNull Transition transition) { }

            @Override
            public void onTransitionResume(@NonNull Transition transition) { }
        });
    }

    @Override
    public void swapSharedElementTargets(@Nullable Object sharedElementTransitionObj,
            @Nullable ArrayList<View> sharedElementsOut,
            @Nullable ArrayList<View> sharedElementsIn) {
        TransitionSet sharedElementTransition = (TransitionSet) sharedElementTransitionObj;
        if (sharedElementTransition != null) {
            sharedElementTransition.getTargets().clear();
            sharedElementTransition.getTargets().addAll(sharedElementsIn);
            replaceTargets(sharedElementTransition, sharedElementsOut, sharedElementsIn);
        }
    }

    @Override
    public void replaceTargets(@NonNull Object transitionObj,
            @SuppressLint("UnknownNullness") ArrayList<View> oldTargets,
            @SuppressLint("UnknownNullness") ArrayList<View> newTargets) {
        Transition transition = (Transition) transitionObj;
        if (transition instanceof TransitionSet) {
            TransitionSet set = (TransitionSet) transition;
            int numTransitions = set.getTransitionCount();
            for (int i = 0; i < numTransitions; i++) {
                Transition child = set.getTransitionAt(i);
                replaceTargets(child, oldTargets, newTargets);
            }
        } else if (!hasSimpleTarget(transition)) {
            List<View> targets = transition.getTargets();
            if (targets.size() == oldTargets.size()
                    && targets.containsAll(oldTargets)) {
                // We have an exact match. We must have added these earlier in addTargets
                final int targetCount = newTargets == null ? 0 : newTargets.size();
                for (int i = 0; i < targetCount; i++) {
                    transition.addTarget(newTargets.get(i));
                }
                for (int i = oldTargets.size() - 1; i >= 0; i--) {
                    transition.removeTarget(oldTargets.get(i));
                }
            }
        }
    }

    @Override
    public void addTarget(@NonNull Object transitionObj, @NonNull View view) {
        if (transitionObj != null) {
            Transition transition = (Transition) transitionObj;
            transition.addTarget(view);
        }
    }

    @Override
    public void removeTarget(@NonNull Object transitionObj, @NonNull View view) {
        if (transitionObj != null) {
            Transition transition = (Transition) transitionObj;
            transition.removeTarget(view);
        }
    }

    @Override
    public void setEpicenter(@NonNull Object transitionObj, final @NonNull Rect epicenter) {
        if (transitionObj != null) {
            Transition transition = (Transition) transitionObj;
            transition.setEpicenterCallback(new Transition.EpicenterCallback() {
                @Override
                public Rect onGetEpicenter(@NonNull Transition transition) {
                    if (epicenter == null || epicenter.isEmpty()) {
                        return null;
                    }
                    return epicenter;
                }
            });
        }
    }

}
