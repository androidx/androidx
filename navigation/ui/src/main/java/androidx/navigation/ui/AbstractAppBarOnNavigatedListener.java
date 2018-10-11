/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.text.TextUtils;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import java.lang.ref.WeakReference;
import java.util.Set;

/**
 * The abstract OnNavigatedListener for keeping any type of app bar updated. This handles both
 * updating the title and updating the Up Indicator, transitioning between the drawer icon and
 * up arrow as needed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class AbstractAppBarOnNavigatedListener
        implements NavController.OnNavigatedListener {
    private final Context mContext;
    private final Set<Integer> mTopLevelDestinations;
    @Nullable
    private final WeakReference<DrawerLayout> mDrawerLayoutWeakReference;
    private DrawerArrowDrawable mArrowDrawable;
    private ValueAnimator mAnimator;

    AbstractAppBarOnNavigatedListener(@NonNull Context context,
            @NonNull AppBarConfiguration configuration) {
        mContext = context;
        mTopLevelDestinations = configuration.getTopLevelDestinations();
        DrawerLayout drawerLayout = configuration.getDrawerLayout();
        if (drawerLayout != null) {
            mDrawerLayoutWeakReference = new WeakReference<>(drawerLayout);
        } else {
            mDrawerLayoutWeakReference = null;
        }
    }

    protected abstract void setTitle(CharSequence title);

    protected abstract void setNavigationIcon(Drawable icon);

    @Override
    public void onNavigated(@NonNull NavController controller,
            @NonNull NavDestination destination) {
        DrawerLayout drawerLayout = mDrawerLayoutWeakReference != null
                ? mDrawerLayoutWeakReference.get()
                : null;
        if (mDrawerLayoutWeakReference != null && drawerLayout == null) {
            controller.removeOnNavigatedListener(this);
            return;
        }
        CharSequence title = destination.getLabel();
        if (!TextUtils.isEmpty(title)) {
            setTitle(title);
        }
        boolean isTopLevelDestination = NavigationUI.matchDestinations(destination,
                mTopLevelDestinations);
        if (drawerLayout == null && isTopLevelDestination) {
            setNavigationIcon(null);
        } else {
            setActionBarUpIndicator(drawerLayout != null && isTopLevelDestination);
        }
    }

    void setActionBarUpIndicator(boolean showAsDrawerIndicator) {
        boolean animate = true;
        if (mArrowDrawable == null) {
            mArrowDrawable = new DrawerArrowDrawable(mContext);
            // We're setting the initial state, so skip the animation
            animate = false;
        }
        setNavigationIcon(mArrowDrawable);
        float endValue = showAsDrawerIndicator ? 0f : 1f;
        if (animate) {
            float startValue = mArrowDrawable.getProgress();
            if (mAnimator != null) {
                mAnimator.cancel();
            }
            mAnimator = ObjectAnimator.ofFloat(mArrowDrawable, "progress",
                    startValue, endValue);
            mAnimator.start();
        } else {
            mArrowDrawable.setProgress(endValue);
        }
    }
}
