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

package androidx.navigation.dynamicfeatures.fragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.NavigatorProvider;
import androidx.navigation.dynamicfeatures.DynamicExtras;
import androidx.navigation.dynamicfeatures.DynamicInstallManager;
import androidx.navigation.fragment.FragmentNavigator;

/**
 * Dynamic feature navigator for Fragment destinations.
 */
@Navigator.Name("fragment")
public final class DynamicFragmentNavigator extends FragmentNavigator {

    @NonNull
    private final DynamicInstallManager mInstallManager;

    public DynamicFragmentNavigator(
            @NonNull Context context,
            @NonNull FragmentManager manager,
            int containerId,
            @NonNull DynamicInstallManager installManager) {
        super(context, manager, containerId);
        this.mInstallManager = installManager;
    }

    @NonNull
    @Override
    public FragmentNavigator.Destination createDestination() {
        return new Destination(this);
    }

    @Nullable
    @Override
    public NavDestination navigate(
            @NonNull FragmentNavigator.Destination destination,
            @Nullable Bundle args,
            @Nullable NavOptions navOptions,
            @Nullable Navigator.Extras navigatorExtras) {
        DynamicExtras extras = navigatorExtras instanceof DynamicExtras
                ? (DynamicExtras) navigatorExtras : null;
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

    /**
     * Destination for dynamic feature navigator.
     */
    public static final class Destination extends FragmentNavigator.Destination {
        @Nullable
        private String mModuleName;

        public Destination(@NonNull NavigatorProvider navigatorProvider) {
            super(navigatorProvider);
        }

        public Destination(
                @NonNull Navigator<? extends FragmentNavigator.Destination> fragmentNavigator) {
            super(fragmentNavigator);
        }

        @Override
        public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs) {
            super.onInflate(context, attrs);
            TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.DynamicFragmentNavigator);
            String moduleName = a.getString(R.styleable.DynamicFragmentNavigator_moduleName);
            setModuleName(moduleName);
            a.recycle();
        }

        public void setModuleName(@Nullable String moduleName) {
            mModuleName = moduleName;
        }

        @Nullable
        public String getModuleName() {
            return mModuleName;
        }
    }

}
