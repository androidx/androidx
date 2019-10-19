/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures.activity;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.ActivityNavigator;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.NavigatorProvider;
import androidx.navigation.dynamicfeatures.DynamicExtras;
import androidx.navigation.dynamicfeatures.DynamicInstallManager;

/**
 * Dynamic feature navigator for Activity destinations.
 */
@Navigator.Name("activity")
public final class DynamicActivityNavigator extends ActivityNavigator {

    @NonNull
    private final DynamicInstallManager mInstallManager;

    /**
     * Create a {@link DynamicActivityNavigator}.
     */
    public DynamicActivityNavigator(@NonNull final Activity activity,
            @NonNull DynamicInstallManager installManager) {
        super(activity);
        mInstallManager = installManager;
    }

    @Nullable
    @Override
    public NavDestination navigate(@NonNull ActivityNavigator.Destination destination,
            @Nullable Bundle args,
            @Nullable NavOptions navOptions,
            @Nullable Navigator.Extras navigatorExtras) {
        DynamicExtras extras =
                navigatorExtras instanceof DynamicExtras ? (DynamicExtras) navigatorExtras : null;
        if (destination instanceof Destination) {
            String moduleName = ((Destination) destination).getModuleName();
            if (moduleName != null && mInstallManager.needsInstall(moduleName)) {
                return mInstallManager.performInstall(destination, args, extras, moduleName);
            }
        }
        return super.navigate(destination,
                args,
                navOptions,
                extras != null ? extras.getDestinationExtras() : navigatorExtras);
    }

    @NonNull
    @Override
    public Destination createDestination() {
        return new Destination(this);
    }

    /**
     * Destination for {@link DynamicActivityNavigator}.
     */
    public static final class Destination extends ActivityNavigator.Destination {
        @Nullable
        private String mModuleName;

        /**
         * Create a new {@link Destination} with a {@link NavigatorProvider}.
         * @see ActivityNavigator.Destination
         */
        public Destination(@NonNull NavigatorProvider navigatorProvider) {
            super(navigatorProvider);
        }

        /**
         * Create a new {@link Destination} with an {@link ActivityNavigator.Destination}.
         * @param activityNavigator The Navigator to use for this {@link Destination}.
         */
        public Destination(
                @NonNull Navigator<? extends ActivityNavigator.Destination> activityNavigator) {
            super(activityNavigator);
        }

        @Override
        public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs) {
            super.onInflate(context, attrs);
            TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.DynamicActivityNavigator);
            String moduleName = a.getString(R.styleable.DynamicActivityNavigator_moduleName);
            setModuleName(moduleName);
            a.recycle();
        }

        /**
         * Set the name of the dynamic feature module which contains the Destination.
         * @param moduleName The module name. This has to be the same as defined in the dynamic
         *                   feature module's AndroidManifest.xml file.
         */
        public void setModuleName(@Nullable String moduleName) {
            this.mModuleName = moduleName;
        }

        /**
         * @return The module name of this {@link Destination}'s dynamic feature module.
         */
        @Nullable
        public String getModuleName() {
            return mModuleName;
        }
    }
}
