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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;

/**
 * Configuration options for {@link NavigationUI} methods that interact with implementations of the
 * app bar pattern such as {@link android.support.v7.widget.Toolbar},
 * {@link android.support.design.widget.CollapsingToolbarLayout}, and
 * {@link android.support.v7.app.ActionBar}.
 */
public class AppBarConfiguration {

    @Nullable
    private final DrawerLayout mDrawerLayout;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    AppBarConfiguration(@Nullable DrawerLayout drawerLayout) {
        mDrawerLayout = drawerLayout;
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
    public static class Builder {
        @Nullable
        private DrawerLayout mDrawerLayout;

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
        @NonNull
        public AppBarConfiguration build() {
            return new AppBarConfiguration(mDrawerLayout);
        }
    }
}
