package android.support.v17.leanback.transition;

import android.support.v17.leanback.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.graphics.Path;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.view.View;

/**
 * This class is used by Slide and Explode to create an animator that goes from the start position
 * to the end position. It takes into account the canceled position so that it will not blink out or
 * shift suddenly when the transition is interrupted.
 * @hide
 */
class TranslationAnimationCreator {

    /**
     * Creates an animator that can be used for x and/or y translations. When interrupted, it sets a
     * tag to keep track of the position so that it may be continued from position.
     *
     * @param view The view being moved. This may be in the overlay for onDisappear.
     * @param values The values containing the view in the view hierarchy.
     * @param viewPosX The x screen coordinate of view
     * @param startX The start translation x of view
     * @param endX The end translation x of view
     * @param interpolator The interpolator to use with this animator.
     * @return An animator that moves from (startX, startY) to (endX, endY) unless there was a
     *         previous interruption, in which case it moves from the current position to (endX,
     *         endY).
     */
    static Animator createAnimation(View view, TransitionValues values, int viewPosX, float startX,
            float endX, TimeInterpolator interpolator, Transition transition) {
        float terminalX = view.getTranslationX();
        Integer startPosition = (Integer) values.view.getTag(R.id.transitionPosition);
        if (startPosition != null) {
            startX = startPosition - viewPosX + terminalX;
        }
        // Initial position is at translation startX, startY, so position is offset by that
        // amount
        int startPosX = viewPosX + Math.round(startX - terminalX);

        view.setTranslationX(startX);
        if (startX == endX) {
            return null;
        }
        float y = view.getTranslationY();
        Path path = new Path();
        path.moveTo(startX, y);
        path.lineTo(endX, y);
        ObjectAnimator anim =
                ObjectAnimator.ofFloat(view, View.TRANSLATION_X, View.TRANSLATION_Y, path);

        TransitionPositionListener listener =
                new TransitionPositionListener(view, values.view, startPosX, terminalX);
        transition.addListener(listener);
        anim.addListener(listener);
        anim.addPauseListener(listener);
        anim.setInterpolator(interpolator);
        return anim;
    }

    private static class TransitionPositionListener extends AnimatorListenerAdapter
            implements Transition.TransitionListener {

        private final View mViewInHierarchy;
        private final View mMovingView;
        private final int mStartX;
        private Integer mTransitionPosition;
        private float mPausedX;
        private final float mTerminalX;

        private TransitionPositionListener(View movingView, View viewInHierarchy, int startX,
                float terminalX) {
            mMovingView = movingView;
            mViewInHierarchy = viewInHierarchy;
            mStartX = startX - Math.round(mMovingView.getTranslationX());
            mTerminalX = terminalX;
            mTransitionPosition = (Integer) mViewInHierarchy.getTag(R.id.transitionPosition);
            if (mTransitionPosition != null) {
                mViewInHierarchy.setTag(R.id.transitionPosition, null);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mTransitionPosition = Math.round(mStartX + mMovingView.getTranslationX());
            mViewInHierarchy.setTag(R.id.transitionPosition, mTransitionPosition);
        }

        @Override
        public void onAnimationEnd(Animator animator) {}

        @Override
        public void onAnimationPause(Animator animator) {
            mPausedX = mMovingView.getTranslationX();
            mMovingView.setTranslationX(mTerminalX);
        }

        @Override
        public void onAnimationResume(Animator animator) {
            mMovingView.setTranslationX(mPausedX);
        }

        @Override
        public void onTransitionStart(Transition transition) {}

        @Override
        public void onTransitionEnd(Transition transition) {
            mMovingView.setTranslationX(mTerminalX);
        }

        @Override
        public void onTransitionCancel(Transition transition) {}

        @Override
        public void onTransitionPause(Transition transition) {}

        @Override
        public void onTransitionResume(Transition transition) {}
    }

}

