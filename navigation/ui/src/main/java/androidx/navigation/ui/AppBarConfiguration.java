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

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;

import androidx.navigation.NavGraph;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuration options for {@link NavigationUI} methods that interact with implementations of the
 * app bar pattern such as {@link android.support.v7.widget.Toolbar},
 * {@link android.support.design.widget.CollapsingToolbarLayout}, and
 * {@link android.support.v7.app.ActionBar}.
 */
public final class AppBarConfiguration {

    @NonNull
    private final Set<Integer> mTopLevelDestinations;
    @Nullable
    private final DrawerLayout mDrawerLayout;

    private AppBarConfiguration(@NonNull Set<Integer> topLevelDestinations,
            @Nullable DrawerLayout drawerLayout) {
        mTopLevelDestinations = topLevelDestinations;
        mDrawerLayout = drawerLayout;
    }

    /**
     * The set of destinations by id considered at the top level of your information hierarchy.
     * The Up button will not be displayed when on these destinations.
     *
     * @return The set of top level destinations by id.
     */
    @NonNull
    public Set<Integer> getTopLevelDestinations() {
        return mTopLevelDestinations;
    }

    /**
     * The {@link DrawerLayout} indicating that the Navigation button should be displayed as
     * a drawer symbol when it is not being shown as an Up button.
     * @return The DrawerLayout that should be toggled from the Navigation button
     */
    @Nullable
    public DrawerLayout getDrawerLayout() {
        return mDrawerLayout;
    }

    /**
     * The Builder class for constructing new {@link AppBarConfiguration} instances.
     */
    public static final class Builder {
        @NonNull
        private final Set<Integer> mTopLevelDestinations = new HashSet<>();

        @Nullable
        private DrawerLayout mDrawerLayout;

        /**
         * Create a new Builder whose only top level destination is the start destination
         * of the given {@link NavGraph}. The Up button will not be displayed when on the
         * start destination of the graph.
         *
         * @param navGraph The NavGraph whose start destination should be considered the only
         *                 top level destination. The Up button will not be displayed when on the
         *                 start destination of the graph.
         */
        public Builder(@NonNull NavGraph navGraph) {
            mTopLevelDestinations.add(NavigationUI.findStartDestination(navGraph).getId());
        }

        /**
         * Create a new Builder with a specific set of top level destinations. The Up button will
         * not be displayed when on these destinations.
         *
         * @param topLevelDestinationIds The set of destinations by id considered at the top level
         *                               of your information hierarchy. The Up button will not be
         *                               displayed when on these destinations.
         */
        public Builder(@NonNull int... topLevelDestinationIds) {
            for (int destinationId : topLevelDestinationIds) {
                mTopLevelDestinations.add(destinationId);
            }
        }

        /**
         * Create a new Builder with a specific set of top level destinations. The Up button will
         * not be displayed when on these destinations.
         *
         * @param topLevelDestinationIds The set of destinations by id considered at the top level
         *                               of your information hierarchy. The Up button will not be
         *                               displayed when on these destinations.
         */
        public Builder(@NonNull Set<Integer> topLevelDestinationIds) {
            mTopLevelDestinations.addAll(topLevelDestinationIds);
        }

        /**
         * Display the Navigation button as a drawer symbol when it is not being shown as an
         * Up button.
         * @param drawerLayout The DrawerLayout that should be toggled from the Navigation button
         * @return this {@link Builder}
         */
        @NonNull
        public Builder setDrawerLayout(@Nullable DrawerLayout drawerLayout) {
            mDrawerLayout = drawerLayout;
            return this;
        }

        /**
         * Construct the {@link AppBarConfiguration} instance.
         *
         * @return a valid {@link AppBarConfiguration}
         */
        @SuppressLint("SyntheticAccessor") /* new AppBarConfiguration() must be private to avoid
                                              conflicting with the public AppBarConfiguration.kt */
        @NonNull
        public AppBarConfiguration build() {
            return new AppBarConfiguration(mTopLevelDestinations, mDrawerLayout);
        }
    }
}
