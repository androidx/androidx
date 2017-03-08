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

package android.support.navigation.app.nav;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.util.AttributeSet;

import com.android.support.navigation.R;

/**
 * A Navigator built specifically for {@link NavGraph} elements. Handles navigating to the
 * correct destination when the NavGraph is the target of navigation actions.
 */
public class NavGraphNavigator extends Navigator<NavGraphNavigator.Params> {
    public static final String NAME = "navigation";

    private Context mContext;

    /**
     * Construct a Navigator capable of routing incoming navigation requests to the proper
     * destination within a {@link NavGraph}.
     * @param context
     */
    public NavGraphNavigator(Context context) {
        mContext = context;
    }

    /**
     * Creates a new {@link NavGraph} associated with this navigator.
     * @return
     */
    @Override
    public NavDestination createDestination() {
        return new NavGraph(this);
    }

    @Override
    public NavGraphNavigator.Params generateDefaultParams() {
        return new Params();
    }

    @Override
    public NavGraphNavigator.Params inflateParams(Context context, AttributeSet attrs) {
        Params p = generateDefaultParams();
        TypedArray a = context.getResources().obtainAttributes(attrs,
                R.styleable.NavGraphNavigatorParams);
        p.setStartDestination(
                a.getResourceId(R.styleable.NavGraphNavigatorParams_nav_startDestination, 0));
        a.recycle();
        return p;
    }

    @Override
    public boolean navigate(NavDestination destination, Bundle args, NavOptions navOptions) {
        NavGraph navGraph = (NavGraph) destination;
        int startId = ((Params) navGraph.getNavigatorParams()).getStartDestination();
        if (startId == 0) {
            final Resources res = mContext.getResources();
            throw new IllegalStateException("no start destination defined via"
                    + " app:nav_startDestination for "
                    + (navGraph.getId() != 0
                            ? res.getResourceName(navGraph.getId())
                            : "the root navigation"));
        }
        NavDestination startDestination = navGraph.findNode(startId);
        if (startDestination == null) {
            final String dest = mContext.getResources().getResourceName(startId);
            throw new IllegalArgumentException("navigation destination " + dest
                    + " is unknown to this NavGraph");
        }
        return startDestination.navigate(args, navOptions);
    }

    @Override
    public boolean popBackStack() {
        return false;
    }

    /**
     * Params specific to {@link NavGraphNavigator}
     */
    public static class Params extends Navigator.Params {
        private int mStartDestId;

        public Params() {
        }

        @Override
        public void copyFrom(Navigator.Params other) {
            super.copyFrom(other);
            if (other instanceof Params) {
                mStartDestId = ((Params) other).getStartDestination();
            }
        }

        @IdRes
        public int getStartDestination() {
            return mStartDestId;
        }

        public void setStartDestination(@IdRes int startDestId) {
            mStartDestId = startDestId;
        }
    }
}
