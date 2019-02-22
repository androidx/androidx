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
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The abstract OnDestinationChangedListener for keeping any type of app bar updated.
 * This handles both updating the title and updating the Up Indicator, transitioning between
 * the drawer icon and up arrow as needed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class AbstractAppBarOnDestinationChangedListener
        implements NavController.OnDestinationChangedListener {
    private final Context mContext;
    private final Set<Integer> mTopLevelDestinations;
    @Nullable
    private final WeakReference<DrawerLayout> mDrawerLayoutWeakReference;
    private DrawerArrowDrawable mArrowDrawable;
    private ValueAnimator mAnimator;

    AbstractAppBarOnDestinationChangedListener(@NonNull Context context,
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

    protected abstract void setNavigationIcon(Drawable icon, @StringRes int contentDescription);

    @Override
    public void onDestinationChanged(@NonNull NavController controller,
            @NonNull NavDestination destination, @Nullable Bundle arguments) {
        DrawerLayout drawerLayout = mDrawerLayoutWeakReference != null
                ? mDrawerLayoutWeakReference.get()
                : null;
        if (mDrawerLayoutWeakReference != null && drawerLayout == null) {
            controller.removeOnDestinationChangedListener(this);
            return;
        }
        CharSequence label = destination.getLabel();
        if (!TextUtils.isEmpty(label)) {
            // Fill in the data pattern with the args to build a valid URI
            StringBuffer title = new StringBuffer();
            Pattern fillInPattern = Pattern.compile("\\{(.+?)\\}");
            Matcher matcher = fillInPattern.matcher(label);
            while (matcher.find()) {
                String argName = matcher.group(1);
                if (arguments != null && arguments.containsKey(argName)) {
                    matcher.appendReplacement(title, "");
                    //noinspection ConstantConditions
                    title.append(arguments.get(argName).toString());
                } else {
                    throw new IllegalArgumentException("Could not find " + argName + " in "
                            + arguments + " to fill label " + label);
                }
            }
            matcher.appendTail(title);
            setTitle(title);
        }
        boolean isTopLevelDestination = NavigationUI.matchDestinations(destination,
                mTopLevelDestinations);
        if (drawerLayout == null && isTopLevelDestination) {
            setNavigationIcon(null, 0);
        } else {
            setActionBarUpIndicator(drawerLayout != null && isTopLevelDestination);
        }
    }

    private void setActionBarUpIndicator(boolean showAsDrawerIndicator) {
        boolean animate = true;
        if (mArrowDrawable == null) {
            mArrowDrawable = new DrawerArrowDrawable(mContext);
            // We're setting the initial state, so skip the animation
            animate = false;
        }
        setNavigationIcon(mArrowDrawable, showAsDrawerIndicator
                ? R.string.nav_app_bar_open_drawer_description
                : R.string.nav_app_bar_navigate_up_description);
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
