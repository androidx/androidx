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
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavGraphNavigator;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.NavigatorProvider;
import androidx.navigation.dynamicfeatures.core.R;


/**
 * Navigator for graphs in dynamic feature modules.
 */
@Navigator.Name("navigation")
public final class DynamicGraphNavigator extends NavGraphNavigator {

    @NonNull
    private final NavigatorProvider mNavigatorProvider;

    @NonNull
    private final DynamicInstallManager mInstallManager;

    @Nullable
    private ProgressDestinationSupplier mProgressDestinationSupplier;

    /**
     * Construct a Navigator capable of routing incoming navigation requests to the proper
     * destination within a {@link androidx.navigation.NavGraph}.
     *
     * @param navigatorProvider NavigatorProvider used to retrieve the correct
     *                          {@link Navigator} to navigate to the start destination
     */
    public DynamicGraphNavigator(@NonNull NavigatorProvider navigatorProvider,
            @NonNull DynamicInstallManager installManager) {
        super(navigatorProvider);
        mNavigatorProvider = navigatorProvider;
        mInstallManager = installManager;
    }

    /**
     * Navigate to a destination.
     *
     * In case the destination module is installed the navigation will trigger directly.
     * Otherwise the dynamic feature module is requested and navigation is postponed until the
     * module has successfully been installed.
     */
    @Nullable
    @Override
    public NavDestination navigate(@NonNull NavGraph destination, @Nullable Bundle args,
            @Nullable NavOptions navOptions, @Nullable Extras navigatorExtras) {
        DynamicExtras extras =
                navigatorExtras instanceof DynamicExtras ? (DynamicExtras) navigatorExtras : null;
        if (destination instanceof DynamicNavGraph) {
            String moduleName = ((DynamicNavGraph) destination).getModuleName();
            if (moduleName != null && mInstallManager.needsInstall(moduleName)) {
                return mInstallManager.performInstall(destination, args, extras, moduleName);
            }
        }
        return super.navigate(destination, args, navOptions,
                extras != null ? extras.getDestinationExtras() : navigatorExtras);
    }

    /**
     * Create a destination for the {@link DynamicNavGraph}.
     *
     * @return The created graph.
     */
    @NonNull
    @Override
    public DynamicNavGraph createDestination() {
        return new DynamicNavGraph(this, mNavigatorProvider);
    }

    /**
     * Installs the default progress destination to this graph via [ProgressDestinationSupplier].
     * This supplies a [NavDestination] to use when the actual destination is not installed at
     * navigation time.
     *
     * @param defaultProgress The default progress destination supplier.
     */
    public void installDefaultProgressDestination(
            @Nullable ProgressDestinationSupplier defaultProgress) {
        this.mProgressDestinationSupplier = defaultProgress;
    }

    /**
     * @return The {@link ProgressDestinationSupplier} if any is set.
     */
    @Nullable
    ProgressDestinationSupplier getProgressDestinationSupplier() {
        return mProgressDestinationSupplier;
    }

    /**
     * Navigates to a destination after progress is done.
     *
     * @return The destination to navigate to if any.
     */
    @Nullable
    NavDestination navigateToProgressDestination(@NonNull DynamicNavGraph dynamicNavGraph,
            @Nullable Bundle progressArgs) {
        int progressDestinationId = dynamicNavGraph.getProgressDestination();
        if (progressDestinationId == 0) {
            if (mProgressDestinationSupplier == null) {
                throw new IllegalStateException("You must set a default progress destination "
                        + "using DynamicNavGraphNavigator.installDefaultProgressDestination or "
                        + "pass in an DynamicInstallMonitor in the DynamicExtras.\n"
                        + "Alternatively, when using NavHostFragment make sure to swap it with "
                        + "DynamicNavHostFragment. This will take care of setting the default "
                        + "progress destination for you.");
            }
            NavDestination progressDestination =
                    mProgressDestinationSupplier.getProgressDestination();
            dynamicNavGraph.addDestination(progressDestination);
            dynamicNavGraph.setProgressDestination(progressDestination.getId());
            progressDestinationId = progressDestination.getId();
        }

        NavDestination progressDestination = dynamicNavGraph.findNode(progressDestinationId);
        if (progressDestination == null) {
            throw new IllegalStateException("The progress destination id must be set and "
                    + "accessible to the module of this navigator.");
        }
        Navigator<NavDestination> navigator = mNavigatorProvider.getNavigator(
                progressDestination.getNavigatorName());
        return navigator.navigate(progressDestination, progressArgs,
                null, null);
    }

    /**
     * The {@link NavGraph} for dynamic features.
     */
    public static final class DynamicNavGraph extends NavGraph {

        private final NavigatorProvider mNavigatorProvider;
        @Nullable
        private String mModuleName;
        private int mProgressDestId;

        /**
         * Construct a new DynamicNavGraph. This DynamicNavGraph is not valid until you
         * {@link #addDestination(NavDestination) add a destination} and
         * {@link #setStartDestination(int) set the starting destination}.
         *
         * @param navGraphNavigator The {@link NavGraphNavigator} which this destination
         *                          will be associated with. Generally retrieved via a
         *                          {@link androidx.navigation.NavController}'s
         *                          {@link NavigatorProvider#getNavigator(Class)} method.
         * @param navigatorProvider The {@link NavigatorProvider} to be used here.
         */
        public DynamicNavGraph(
                @NonNull Navigator<? extends androidx.navigation.NavGraph> navGraphNavigator,
                @NonNull NavigatorProvider navigatorProvider) {
            super(navGraphNavigator);
            mNavigatorProvider = navigatorProvider;
        }

        /**
         * Get the {@link DynamicNavGraph} for a supplied {@link NavDestination} or throw an
         * exception if it's not a {@link DynamicNavGraph}.
         */
        @NonNull
        static DynamicNavGraph getOrThrow(@NonNull NavDestination destination) {
            if (destination.getParent() instanceof DynamicNavGraph) {
                return (DynamicNavGraph) destination.getParent();
            } else {
                throw new IllegalStateException(
                        "Dynamic destinations must be part of a DynamicNavGraph. \n"
                                + "You can use DynamicNavHostFragment, which will take care of "
                                + "setting up "
                                + "the NavController for Dynamic destinations.\n"
                                + "If you're not using Fragments, you must set up the "
                                + "NavigatorProvider "
                                + "manually.");
            }
        }

        @Override
        public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs) {
            super.onInflate(context, attrs);
            TypedArray a = context.getResources().obtainAttributes(attrs,
                    R.styleable.DynamicGraphNavigator);
            String moduleName = a.getString(R.styleable.DynamicGraphNavigator_moduleName);
            setModuleName(moduleName);
            mProgressDestId =
                    a.getResourceId(R.styleable.DynamicGraphNavigator_progressDestination, 0);
            a.recycle();
        }

        /**
         * @param moduleName The dynamic feature's module name.
         */
        public void setModuleName(@Nullable String moduleName) {
            mModuleName = moduleName;
        }

        /**
         * @return The dynamic feature's module name.
         */
        @Nullable
        public String getModuleName() {
            return mModuleName;
        }

        /**
         * @param progressDestId Resource id of the progress destination.
         * @see ProgressDestinationSupplier
         */
        public void setProgressDestination(int progressDestId) {
            mProgressDestId = progressDestId;
        }

        /**
         * @return Resource id of progress destination.
         * @see ProgressDestinationSupplier
         */
        public int getProgressDestination() {
            return mProgressDestId;
        }

        /**
         * @return The {@link NavigatorProvider} for this graph.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        @NonNull
        NavigatorProvider getNavigatorProvider() {
            return mNavigatorProvider;
        }
    }

    /**
     * Interface to supply {@link NavDestination}s to display installation progress if there is none
     * set through the {@link NavGraph} via <code>app:progressDestinationId</code>.
     */
    public interface ProgressDestinationSupplier {

        /**
         * @return The {@link NavDestination} that will be used for loading dynamic modules
         */
        @NonNull
        NavDestination getProgressDestination();
    }
}
