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

package androidx.navigation.dynamicfeatures;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavGraphNavigator;
import androidx.navigation.NavInflater;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.NavigatorProvider;
import androidx.navigation.dynamicfeatures.core.R;


/**
 * Navigator for `include-dynamic`.
 * <p>
 * Use it for navigating to NavGraphs contained within a dynamic feature module.
 */
@Navigator.Name("include-dynamic")
public final class DynamicIncludeGraphNavigator extends
        Navigator<DynamicIncludeGraphNavigator.DynamicIncludeNavGraph> {
    @NonNull
    private final NavigatorProvider mNavigatorProvider;
    @NonNull
    private final NavInflater mNavInflater;
    @NonNull
    private final DynamicInstallManager mInstallManager;
    @NonNull
    private final Context mContext;

    /**
     * Create a {@link DynamicIncludeGraphNavigator}.
     */
    public DynamicIncludeGraphNavigator(@NonNull Context context,
            @NonNull NavigatorProvider navigatorProvider,
            @NonNull NavInflater navInflater,
            @NonNull DynamicInstallManager installManager) {
        super();
        mContext = context;
        mNavigatorProvider = navigatorProvider;
        mNavInflater = navInflater;
        mInstallManager = installManager;
    }

    @NonNull
    @Override
    public DynamicIncludeNavGraph createDestination() {
        return new DynamicIncludeNavGraph(this);
    }

    @Nullable
    @Override
    public NavDestination navigate(@NonNull DynamicIncludeNavGraph destination,
            @Nullable Bundle args, @Nullable NavOptions navOptions,
            @Nullable Extras navigatorExtras) {
        DynamicExtras extras =
                navigatorExtras instanceof DynamicExtras ? (DynamicExtras) navigatorExtras : null;

        String moduleName = destination.getModuleName();

        if (moduleName != null && mInstallManager.needsInstall(moduleName)) {
            return mInstallManager.performInstall(destination, args, extras,
                    moduleName);
        } else {
            int graphId = mContext.getResources().getIdentifier(
                    destination.getGraphResourceName(), "navigation",
                    destination.getGraphPackage());
            if (graphId == 0) {
                throw new Resources.NotFoundException(
                        destination.getGraphPackage() + ":navigation/"
                                + destination.getGraphResourceName()
                );
            }
            NavGraph includedNav =
                    mNavInflater.inflate(graphId);
            if (includedNav.getId() != 0 && includedNav.getId() != destination.getId()) {
                throw new IllegalStateException("The included <navigation>'s id is different from "
                        + "the destination id. Either remove the <navigation> id or make them "
                        + "match.");
            }
            includedNav.setId(destination.getId());
            NavGraph outerNav = destination.getParent();
            if (outerNav == null) {
                throw new IllegalStateException("The destination " + destination.getId()
                        + " does not have a parent. Make sure it is attached to a NavGraph.");
            }
            // no need to remove previous destination, id is used as key in map
            outerNav.addDestination(includedNav);
            Navigator<NavDestination> navigator = mNavigatorProvider
                    .getNavigator(includedNav.getNavigatorName());
            return navigator.navigate(includedNav, args, navOptions, navigatorExtras);
        }
    }

    @Override
    public boolean popBackStack() {
        return true;
    }

    /**
     * The graph for dynamic-include.
     * <p>
     * This class contains information to navigate to a DynamicNavGraph which is contained
     * within a dynamic feature module.
     */
    public static final class DynamicIncludeNavGraph extends NavDestination {

        private String mGraphResourceName;
        private String mGraphPackage;
        private String mModuleName;

        /**
         * Construct a new {@link DynamicIncludeNavGraph}.
         *
         * @param navGraphNavigator The {@link NavGraphNavigator} which this destination
         *                          will be associated with. Generally retrieved via a
         *                          {@link NavController}'s
         *                          {@link NavigatorProvider#getNavigator(Class)} method.
         */
        DynamicIncludeNavGraph(@NonNull Navigator<? extends NavDestination> navGraphNavigator) {
            super(navGraphNavigator);
        }

        @Override
        public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs) {
            super.onInflate(context, attrs);
            final TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.DynamicIncludeGraphNavigator);
            String graphPackage = a.getString(
                    R.styleable.DynamicIncludeGraphNavigator_graphPackage);
            if (graphPackage == null || graphPackage.equals("")) {
                throw new IllegalArgumentException(
                        "graphPackage must be set for dynamic navigation");
            }
            setGraphPackage(graphPackage);

            String graphResourceName = a.getString(
                    R.styleable.DynamicIncludeGraphNavigator_graphResName);
            if (graphResourceName == null || graphResourceName.equals("")) {
                throw new IllegalArgumentException(
                        "graphResName must be set for dynamic navigation");
            }
            setGraphResourceName(graphResourceName);

            String moduleName = a.getString(R.styleable.DynamicIncludeGraphNavigator_moduleName);
            setModuleName(moduleName);

            a.recycle();
        }

        /**
         * @param moduleName Name of the module containing the included graph.
         */
        public void setModuleName(@Nullable String moduleName) {
            mModuleName = moduleName;
        }

        /**
         * @return Name of the module containing the included graph, if set.
         */
        @Nullable
        public String getModuleName() {
            return mModuleName;
        }

        /**
         * @param graphResourceName The graph resource name to set.
         */
        public void setGraphResourceName(@NonNull String graphResourceName) {
            mGraphResourceName = graphResourceName;
        }

        /**
         * @return Resource name of the graph.
         */
        @NonNull
        public String getGraphResourceName() {
            return mGraphResourceName;
        }

        /**
         * @param graphPackage The graph package to set.
         */
        public void setGraphPackage(@NonNull String graphPackage) {
            mGraphPackage = graphPackage;
        }

        /**
         * @return The graph's package.
         */
        @NonNull
        public String getGraphPackage() {
            return mGraphPackage;
        }
    }
}
