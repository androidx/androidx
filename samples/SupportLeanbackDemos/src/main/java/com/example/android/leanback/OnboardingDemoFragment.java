/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.leanback.app.OnboardingFragment;

import java.util.ArrayList;

public class OnboardingDemoFragment extends OnboardingFragment {
    private static final long ANIMATION_DURATION = 1000;

    private static final int[] CONTENT_BACKGROUNDS = {
            R.drawable.tv_bg,
            R.drawable.gallery_photo_6,
            R.drawable.gallery_photo_8
    };

    private static final int[] CONTENT_ANIMATIONS = {
            R.drawable.tv_content,
            android.R.drawable.stat_sys_download,
            android.R.drawable.ic_popup_sync
    };

    private String[] mTitles;
    private String[] mDescriptions;

    private View mBackgroundView;
    private View mContentView;
    private ImageView mContentBackgroundView;
    private ImageView mContentAnimationView;

    private Animator mContentAnimator;

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(android.app.Activity activity) {
        super.onAttach(activity);
        mTitles = getResources().getStringArray(R.array.onboarding_page_titles);
        mDescriptions = getResources().getStringArray(R.array.onboarding_page_descriptions);
        setLogoResourceId(R.drawable.ic_launcher);
    }

    @Override
    protected int getPageCount() {
        return mTitles.length;
    }

    @Override
    protected CharSequence getPageTitle(int i) {
        return mTitles[i];
    }

    @Override
    protected CharSequence getPageDescription(int i) {
        return mDescriptions[i];
    }

    @Override
    protected View onCreateBackgroundView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        mBackgroundView = layoutInflater.inflate(R.layout.onboarding_image, viewGroup, false);
        return mBackgroundView;
    }

    @Override
    protected View onCreateContentView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        mContentView = layoutInflater.inflate(R.layout.onboarding_content, viewGroup, false);
        mContentBackgroundView = (ImageView) mContentView.findViewById(R.id.background_image);
        mContentAnimationView = (ImageView) mContentView.findViewById(R.id.animation_image);
        return mContentView;
    }

    @Override
    protected View onCreateForegroundView(LayoutInflater layoutInflater, ViewGroup viewGroup) {
        return null;
    }

    @Override
    protected Animator onCreateEnterAnimation() {
        ArrayList<Animator> animators = new ArrayList<>();
        animators.add(createFadeInAnimator(mBackgroundView));
        mContentBackgroundView.setImageResource(CONTENT_BACKGROUNDS[0]);
        mContentAnimationView.setImageResource(CONTENT_ANIMATIONS[0]);
        mContentAnimator = createFadeInAnimator(mContentView);
        animators.add(mContentAnimator);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ((AnimationDrawable) mContentAnimationView.getDrawable()).start();
            }
        });
        return set;
    }

    @Override
    protected void onPageChanged(final int newPage, int previousPage) {
        if (mContentAnimator != null) {
            mContentAnimator.cancel();
        }
        ((AnimationDrawable) mContentAnimationView.getDrawable()).stop();
        ArrayList<Animator> animators = new ArrayList<>();
        Animator fadeOut = createFadeOutAnimator(mContentView);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mContentBackgroundView.setImageResource(CONTENT_BACKGROUNDS[newPage]);
                mContentAnimationView.setImageResource(CONTENT_ANIMATIONS[newPage]);
            }
        });
        Animator fadeIn = createFadeInAnimator(mContentView);
        fadeIn.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ((AnimationDrawable) mContentAnimationView.getDrawable()).start();
            }
        });
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(fadeOut, fadeIn);
        set.start();
        mContentAnimator = set;
    }

    private Animator createFadeInAnimator(View view) {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 0.0f, 1.0f).setDuration(ANIMATION_DURATION);
    }

    private Animator createFadeOutAnimator(View view) {
        return ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.0f).setDuration(ANIMATION_DURATION);
    }

    @Override
    protected void onFinishFragment() {
        getActivity().finish();
    }
}
