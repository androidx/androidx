/* This file is auto-generated from OnboardingDemoFragment.java.  DO NOT MODIFY. */

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
import android.support.v17.leanback.app.OnboardingSupportFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;

import java.util.ArrayList;

public class OnboardingDemoSupportFragment extends OnboardingSupportFragment {
    private static final long ANIMATION_DURATION = 1000;

    private static final int[] CONTENT_IMAGES = {
            R.drawable.gallery_photo_1,
            R.drawable.gallery_photo_2,
            R.drawable.gallery_photo_3
    };
    private String[] mTitles;
    private String[] mDescriptions;

    private View mBackgroundView;
    private ImageView mContentView;
    private ImageView mImage1;
    private ImageView mImage2;

    private Animator mContentAnimator;

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
        mContentView = (ImageView) layoutInflater.inflate(R.layout.onboarding_image, viewGroup,
                false);
        MarginLayoutParams layoutParams = ((MarginLayoutParams) mContentView.getLayoutParams());
        layoutParams.topMargin = 30;
        layoutParams.bottomMargin = 60;
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
        mContentView.setImageResource(CONTENT_IMAGES[0]);
        mContentAnimator = createFadeInAnimator(mContentView);
        animators.add(mContentAnimator);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animators);
        return set;
    }

    @Override
    protected void onPageChanged(final int newPage, int previousPage) {
        if (mContentAnimator != null) {
            mContentAnimator.end();
        }
        ArrayList<Animator> animators = new ArrayList<>();
        Animator fadeOut = createFadeOutAnimator(mContentView);
        fadeOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mContentView.setImageResource(CONTENT_IMAGES[newPage]);
            }
        });
        animators.add(fadeOut);
        animators.add(createFadeInAnimator(mContentView));
        AnimatorSet set = new AnimatorSet();
        set.playSequentially(animators);
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
