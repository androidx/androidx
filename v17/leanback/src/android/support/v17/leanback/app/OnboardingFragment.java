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
 * limitations under the License.
 */

package android.support.v17.leanback.app;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.PagingIndicator;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

// TODO: Add Javadoc
/**
 * A base fragment class for onboarding screen.
 * @hide
 */
abstract public class OnboardingFragment extends Fragment {
    private static final long LOGO_SPLASH_PAUSE_DURATION_MS = 1333;
    private static final long START_DELAY_TITLE_MS = 33;
    private static final long START_DELAY_DESCRIPTION_MS = 33;

    private static final long HEADER_ANIMATION_DURATION_MS = 417;
    private static final long DESCRIPTION_START_DELAY_MS = 33;
    private static final long HEADER_APPEAR_DELAY_MS = 500;
    private static final int SLIDE_DISTANCE = 60;

    private static int sSlideDistance;

    private static final TimeInterpolator HEADER_APPEAR_INTERPOLATOR = new DecelerateInterpolator();
    private static final TimeInterpolator HEADER_DISAPPEAR_INTERPOLATOR
            = new AccelerateInterpolator();

    private PagingIndicator mPageIndicator;
    private View mStartButton;
    private ImageView mLogoView;
    private TextView mTitleView;
    private TextView mDescriptionView;

    private boolean mEnterTransitionFinished;
    private int mCurrentPageIndex;

    private AnimatorSet mAnimator;

    /**
     * Called to have the inherited class create its own start animation. The start animation runs
     * after logo splash animation ends.
     */
    abstract protected void runStartAnimation();

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if (!mEnterTransitionFinished) {
                // Do not change page until the enter transition finishes.
                return;
            }
            if (mCurrentPageIndex == getPageCount() - 1) {
                onFinishFragment();
            } else {
                ++mCurrentPageIndex;
                onPageChanged(mCurrentPageIndex - 1);
            }
        }
    };

    private final OnKeyListener mOnKeyListener = new OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (!mEnterTransitionFinished) {
                // Ignore key event until the enter transition finishes.
                return keyCode != KeyEvent.KEYCODE_BACK;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return false;
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                    if (mCurrentPageIndex == 0) {
                        return false;
                    }
                    // pass through
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (mCurrentPageIndex > 0) {
                        --mCurrentPageIndex;
                        onPageChanged(mCurrentPageIndex + 1);
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (mCurrentPageIndex < getPageCount() - 1) {
                        ++mCurrentPageIndex;
                        onPageChanged(mCurrentPageIndex - 1);
                    }
                    return true;
            }
            return false;
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup view = (ViewGroup) inflater.inflate(R.layout.lb_onboarding_fragment, container,
                false);
        mPageIndicator = (PagingIndicator) view.findViewById(R.id.page_indicator);
        mPageIndicator.setPageCount(getPageCount());
        mPageIndicator.setOnClickListener(mOnClickListener);
        mPageIndicator.setOnKeyListener(mOnKeyListener);
        mStartButton = view.findViewById(R.id.button_start);
        mStartButton.setOnClickListener(mOnClickListener);
        mStartButton.setOnKeyListener(mOnKeyListener);
        mLogoView = (ImageView) view.findViewById(R.id.logo);
        mLogoView.setImageResource(getLogoResourceId());
        mTitleView = (TextView) view.findViewById(R.id.title);
        mTitleView.setText(getPageTitle(0));
        mDescriptionView = (TextView) view.findViewById(R.id.description);
        mDescriptionView.setText(getPageDescription(0));
        if (sSlideDistance == 0) {
            sSlideDistance = (int) (SLIDE_DISTANCE * getActivity().getResources()
                    .getDisplayMetrics().scaledDensity);
        }
        mCurrentPageIndex = 0;
        mPageIndicator.onPageSelected(0, false);
        view.requestFocus();
        if (getLogoResourceId() != 0) {
            container.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    container.getViewTreeObserver().removeOnPreDrawListener(this);
                    startLogoAnimation();
                    return true;
                }
            });
        } else {
            onLogoAnimationFinished();
        }
        return view;
    }

    private void startLogoAnimation() {
        mLogoView.setVisibility(View.VISIBLE);
        Animator inAnimator = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.lb_onboarding_logo_enter);
        Animator outAnimator = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.lb_onboarding_logo_exit);
        outAnimator.setStartDelay(LOGO_SPLASH_PAUSE_DURATION_MS);
        AnimatorSet animator = new AnimatorSet();
        animator.playSequentially(inAnimator, outAnimator);
        animator.setTarget(mLogoView);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mEnterTransitionFinished = true;
                if (getActivity() != null) {
                    onLogoAnimationFinished();
                    runStartAnimation();
                }
            }
        });
        animator.start();
    }

    private void onLogoAnimationFinished() {
        mLogoView.setVisibility(View.GONE);
        // Create custom views.
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup backgroundContainer = (ViewGroup) getView().findViewById(
                R.id.background_container);
        View background = onCreateBackgroundView(inflater, backgroundContainer);
        if (background != null) {
            backgroundContainer.setVisibility(View.VISIBLE);
            backgroundContainer.addView(background);
        }
        ViewGroup contentContainer = (ViewGroup) getView().findViewById(R.id.content_container);
        View content = onCreateContentView(inflater, contentContainer);
        if (content != null) {
            contentContainer.setVisibility(View.VISIBLE);
            contentContainer.addView(content);
        }
        ViewGroup foregroundContainer = (ViewGroup) getView().findViewById(
                R.id.foreground_container);
        View foreground = onCreateForegroundView(inflater, foregroundContainer);
        if (foreground != null) {
            foregroundContainer.setVisibility(View.VISIBLE);
            foregroundContainer.addView(foreground);
        }
        // Make views visible which were invisible while logo animation is running.
        getView().findViewById(R.id.page_container).setVisibility(View.VISIBLE);
        getView().findViewById(R.id.content_container).setVisibility(View.VISIBLE);

        List<Animator> animators = new ArrayList<>();
        Animator animator = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.lb_onboarding_page_indicator_enter);
        if (getPageCount() <= 1) {
            // Start button
            mStartButton.setVisibility(View.VISIBLE);
            animator.setTarget(mStartButton);
        } else {
            // Page indicator
            mPageIndicator.setVisibility(View.VISIBLE);
            animator.setTarget(mPageIndicator);
        }
        animators.add(animator);
        // Header title
        View view = getActivity().findViewById(R.id.title);
        view.setAlpha(0);
        animator = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.lb_onboarding_title_enter);
        animator.setStartDelay(START_DELAY_TITLE_MS);
        animator.setTarget(view);
        animators.add(animator);
        // Header description
        view = getActivity().findViewById(R.id.description);
        view.setAlpha(0);
        animator = AnimatorInflater.loadAnimator(getActivity(),
                R.animator.lb_onboarding_description_enter);
        animator.setStartDelay(START_DELAY_DESCRIPTION_MS);
        animator.setTarget(view);
        animators.add(animator);
        mAnimator = new AnimatorSet();
        mAnimator.playTogether(animators);
        mAnimator.start();
        runStartAnimation();
        // Search focus and give the focus to the appropriate child which has become visible.
        getView().requestFocus();
    }

    /**
     * Returns the page count.
     *
     * @return the page count.
     */
    abstract protected int getPageCount();

    /**
     * Returns the title of the given page.
     *
     * @param pageIndex The page index.
     *
     * @return the title of the page.
     */
    abstract protected String getPageTitle(int pageIndex);

    /**
     * Returns the description of the given page.
     *
     * @param pageIndex The page index.
     *
     * @return the description of the page.
     */
    abstract protected String getPageDescription(int pageIndex);

    /**
     * Returns the index of the current page.
     *
     * @return the index of the current page.
     */
    protected final int getCurrentPageIndex() {
        return mCurrentPageIndex;
    }

    /**
     * Returns the resource ID of the splash logo image.
     *
     * @return the resource ID of the splash logo image.
     */
    abstract protected int getLogoResourceId();

    /**
     * Called to have the inherited class create background view. This is optional and the fragment
     * which doesn't have the background view can return {@code null}. This is called inside
     * {@link #onCreateView}.
     *
     * @param inflater The LayoutInflater object that can be used to inflate the views,
     * @param container The parent view that the additional views are attached to.The fragment
     *        should not add the view by itself.
     *
     * @return the background view for the onboarding screen, or null.
     */
    @Nullable
    abstract protected View onCreateBackgroundView(LayoutInflater inflater, ViewGroup container);

    /**
     * Called to have the inherited class create content view. This is optional and the fragment
     * which doesn't have the content view can return {@code null}. This is called inside
     * {@link #onCreateView}.
     *
     * <p>The content view would be located at the center of the screen.
     *
     * @param inflater The LayoutInflater object that can be used to inflate the views,
     * @param container The parent view that the additional views are attached to.The fragment
     *        should not add the view by itself.
     *
     * @return the content view for the onboarding screen, or null.
     */
    @Nullable
    abstract protected View onCreateContentView(LayoutInflater inflater, ViewGroup container);

    /**
     * Called to have the inherited class create foreground view. This is optional and the fragment
     * which doesn't need the foreground view can return {@code null}. This is called inside
     * {@link #onCreateView}.
     *
     * <p>This foreground view would have the highest z-order.
     *
     * @param inflater The LayoutInflater object that can be used to inflate the views,
     * @param container The parent view that the additional views are attached to.The fragment
     *        should not add the view by itself.
     *
     * @return the foreground view for the onboarding screen, or null.
     */
    @Nullable
    abstract protected View onCreateForegroundView(LayoutInflater inflater, ViewGroup container);

    /**
     * Called when the onboarding flow finishes.
     */
    protected void onFinishFragment() {
    }

    /**
     * Called when the page changes.
     */
    private void onPageChanged(int previousPage) {
        if (mAnimator != null) {
            mAnimator.end();
        }
        mPageIndicator.onPageSelected(mCurrentPageIndex, true);

        List<Animator> animators = new ArrayList<>();
        // Header animation
        Animator fadeAnimator = null;
        if (previousPage < getCurrentPageIndex()) {
            // sliding to left
            animators.add(createAnimator(mTitleView, false, Gravity.START, 0));
            animators.add(fadeAnimator = createAnimator(mDescriptionView, false, Gravity.START,
                    DESCRIPTION_START_DELAY_MS));
            animators.add(createAnimator(mTitleView, true, Gravity.END,
                    HEADER_APPEAR_DELAY_MS));
            animators.add(createAnimator(mDescriptionView, true, Gravity.END,
                    HEADER_APPEAR_DELAY_MS + DESCRIPTION_START_DELAY_MS));
        } else {
            // sliding to right
            animators.add(createAnimator(mTitleView, false, Gravity.END, 0));
            animators.add(fadeAnimator = createAnimator(mDescriptionView, false, Gravity.END,
                    DESCRIPTION_START_DELAY_MS));
            animators.add(createAnimator(mTitleView, true, Gravity.START,
                    HEADER_APPEAR_DELAY_MS));
            animators.add(createAnimator(mDescriptionView, true, Gravity.START,
                    HEADER_APPEAR_DELAY_MS + DESCRIPTION_START_DELAY_MS));
        }
        final int currentPageIndex = getCurrentPageIndex();
        fadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mTitleView.setText(getPageTitle(currentPageIndex));
                mDescriptionView.setText(getPageDescription(currentPageIndex));
            }
        });

        // Animator for switching between page indicator and button.
        if (getCurrentPageIndex() == getPageCount() - 1) {
            mStartButton.setVisibility(View.VISIBLE);
            Animator navigatorFadeOutAnimator = AnimatorInflater.loadAnimator(getActivity(),
                    R.animator.lb_onboarding_page_indicator_fade_out);
            navigatorFadeOutAnimator.setTarget(mPageIndicator);
            Animator buttonFadeInAnimator = AnimatorInflater.loadAnimator(getActivity(),
                    R.animator.lb_onboarding_start_button_fade_in);
            buttonFadeInAnimator.setTarget(mStartButton);
            animators.add(navigatorFadeOutAnimator);
            navigatorFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mPageIndicator.setVisibility(View.GONE);
                }
            });
            animators.add(buttonFadeInAnimator);
        } else if (previousPage == getPageCount() - 1) {
            mPageIndicator.setVisibility(View.VISIBLE);
            Animator navigatorFadeInAnimator = AnimatorInflater.loadAnimator(getActivity(),
                    R.animator.lb_onboarding_page_indicator_fade_in);
            navigatorFadeInAnimator.setTarget(mPageIndicator);
            Animator buttonFadeOutAnimator = AnimatorInflater.loadAnimator(getActivity(),
                    R.animator.lb_onboarding_start_button_fade_out);
            buttonFadeOutAnimator.setTarget(mStartButton);
            buttonFadeOutAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mStartButton.setVisibility(View.GONE);
                }
            });
            mAnimator = new AnimatorSet();
            mAnimator.playTogether(navigatorFadeInAnimator, buttonFadeOutAnimator);
            mAnimator.start();
        }
        mAnimator = new AnimatorSet();
        mAnimator.playTogether(animators);
        mAnimator.start();
        runPageChangeAnimation(previousPage);
    }

    private Animator createAnimator(View view, boolean fadeIn, int slideDirection,
            long startDelay) {
        boolean isLtr = getView().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR;
        boolean slideRight = (isLtr && slideDirection == Gravity.END)
                || (!isLtr && slideDirection == Gravity.START)
                || slideDirection == Gravity.RIGHT;
        Animator fadeAnimator;
        Animator slideAnimator;
        if (fadeIn) {
            fadeAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 0.0f, 1.0f);
            slideAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                    slideRight ? sSlideDistance : -sSlideDistance, 0);
            fadeAnimator.setInterpolator(HEADER_APPEAR_INTERPOLATOR);
            slideAnimator.setInterpolator(HEADER_APPEAR_INTERPOLATOR);
        } else {
            fadeAnimator = ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.0f);
            slideAnimator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, 0,
                    slideRight ? sSlideDistance : -sSlideDistance);
            fadeAnimator.setInterpolator(HEADER_DISAPPEAR_INTERPOLATOR);
            slideAnimator.setInterpolator(HEADER_DISAPPEAR_INTERPOLATOR);
        }
        fadeAnimator.setDuration(HEADER_ANIMATION_DURATION_MS);
        fadeAnimator.setTarget(view);
        slideAnimator.setDuration(HEADER_ANIMATION_DURATION_MS);
        slideAnimator.setTarget(view);
        AnimatorSet animator = new AnimatorSet();
        animator.playTogether(fadeAnimator, slideAnimator);
        if (startDelay > 0) {
            animator.setStartDelay(startDelay);
        }
        return animator;
    }

    /**
     * Called to have the inherited class run its own page change animation
     *
     * @param previousPage The previous page.
     */
    abstract protected void runPageChangeAnimation(int previousPage);
}
