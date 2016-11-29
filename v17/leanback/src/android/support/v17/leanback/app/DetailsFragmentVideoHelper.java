/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v17.leanback.app;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Drawable;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v17.leanback.widget.Parallax;
import android.support.v17.leanback.widget.ParallaxRecyclerViewSource;
import android.support.v17.leanback.widget.ParallaxTarget;

/**
 * Helper class responsible for setting up video playback in {@link DetailsFragment}. This
 * takes {@link DetailsFragment} and {@link PlaybackGlue} as input and configures them. This
 * class is also responsible for implementing
 * {@link android.support.v17.leanback.widget.BrowseFrameLayout.OnFocusSearchListener} and
 * {@link android.support.v7.widget.RecyclerView.OnScrollListener} in {@link DetailsFragment}.
 * @hide
 */
public class DetailsFragmentVideoHelper {
    private static final long BACKGROUND_CROSS_FADE_DURATION = 500;
    private static final long CROSSFADE_DELAY = 1000;

    /**
     * Different states {@link DetailsFragment} can be in.
     */
    enum STATE {
        INITIAL,
        PLAY_VIDEO,
        NO_VIDEO
    }

    private final DetailsParallaxManager mParallaxManager;
    private STATE mCurrentState = STATE.INITIAL;

    private ValueAnimator mBackgroundAnimator;
    private Drawable mBackgroundDrawable;
    private PlaybackGlue mPlaybackGlue;

    /**
     * Constructor.
     */
    public DetailsFragmentVideoHelper(
            PlaybackGlue playbackGlue,
            DetailsParallaxManager parallaxManager) {
        this.mPlaybackGlue = playbackGlue;
        this.mParallaxManager = parallaxManager;
        setupParallax();
    }

    void setupParallax() {
        Parallax parallax = mParallaxManager.getParallax();
        ParallaxRecyclerViewSource.ChildPositionProperty frameTop = mParallaxManager.getFrameTop();
        final float maxFrameTop = 1f;
        final float minFrameTop = 0f;
        parallax.addEffect(frameTop.atFraction(maxFrameTop), frameTop.atFraction(minFrameTop))
                .target(new ParallaxTarget() {

                    float mFraction;
                    @Override
                    public void update(float fraction) {
                        if (fraction == maxFrameTop) {
                            updateState(STATE.NO_VIDEO);
                        } else {
                            updateState(STATE.PLAY_VIDEO);
                        }
                        mFraction = fraction;
                    }

                    @Override
                    public float getFraction() {
                        return mFraction;
                    }
                });
    }

    private void updateState(STATE state) {
        if (state == mCurrentState) {
            return;
        }
        mCurrentState = state;
        switch (state) {
            case PLAY_VIDEO:
                if (mPlaybackGlue.isReadyForPlayback()) {
                    internalStartPlayback();
                } else {
                    mPlaybackGlue.setPlayerCallback(new PlaybackControlStateCallback());
                }
                break;
            case NO_VIDEO:
                crossFadeBackgroundToVideo(false);
                mPlaybackGlue.setPlayerCallback(null);
                mPlaybackGlue.pause();
                break;
        }
    }

    private void internalStartPlayback() {
        mPlaybackGlue.play();
        mParallaxManager.getRecyclerView().postDelayed(new Runnable() {
            @Override
            public void run() {
                crossFadeBackgroundToVideo(true);
            }
        }, CROSSFADE_DELAY);
    }

    private void crossFadeBackgroundToVideo(final boolean crossFadeToVideo) {
        if (mBackgroundAnimator != null) {
            mBackgroundAnimator.cancel();
        }

        float startAlpha = crossFadeToVideo ? 1f : 0f;
        float endAlpha = crossFadeToVideo ? 0f : 1f;

        mBackgroundAnimator = ValueAnimator.ofFloat(startAlpha, endAlpha);
        mBackgroundAnimator.setDuration(BACKGROUND_CROSS_FADE_DURATION);
        mBackgroundAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mBackgroundDrawable.setAlpha(
                        (int) ((Float) (valueAnimator.getAnimatedValue()) * 255));
            }
        });

        mBackgroundAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                mBackgroundAnimator = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });

        mBackgroundAnimator.start();
    }

    /**
     * Sets the drawable to be used as background image for {@link DetailsFragment}. If set,
     * we will cross fade from the background drawable to the video.
     */
    public void setBackgroundDrawable(Drawable drawable) {
        this.mBackgroundDrawable = drawable;
    }

    private class PlaybackControlStateCallback extends PlaybackGlue.PlayerCallback {

        @Override
        public void onReadyForPlayback() {
            internalStartPlayback();
        }
    }
}
