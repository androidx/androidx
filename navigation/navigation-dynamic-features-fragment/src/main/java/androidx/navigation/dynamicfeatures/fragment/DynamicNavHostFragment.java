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

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavigatorProvider;
import androidx.navigation.dynamicfeatures.DynamicGraphNavigator;
import androidx.navigation.dynamicfeatures.DynamicIncludeGraphNavigator;
import androidx.navigation.dynamicfeatures.DynamicInstallManager;
import androidx.navigation.dynamicfeatures.activity.DynamicActivityNavigator;
import androidx.navigation.dynamicfeatures.fragment.ui.DefaultProgressFragment;
import androidx.navigation.fragment.FragmentNavigator;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;

/**
 * The {@link NavHostFragment} for dynamic features.
 */
public class DynamicNavHostFragment extends NavHostFragment {

    @Override
    protected void onCreateNavController(@NonNull NavController navController) {
        super.onCreateNavController(navController);

        DynamicInstallManager installManager = new DynamicInstallManager(
                requireContext(),
                createSplitInstallManager()
        );

        NavigatorProvider navigatorProvider = navController.getNavigatorProvider();

        navigatorProvider.addNavigator(
                new DynamicActivityNavigator(
                        requireActivity(),
                        installManager
                )
        );

        DynamicFragmentNavigator fragmentNavigator = new DynamicFragmentNavigator(
                requireContext(),
                getChildFragmentManager(),
                getId(),
                installManager
        );
        navigatorProvider.addNavigator(fragmentNavigator);


        DynamicGraphNavigator graphNavigator = new DynamicGraphNavigator(
                navigatorProvider,
                installManager
        );

        graphNavigator.installDefaultProgressDestination(
                new DynamicGraphNavigator.ProgressDestinationSupplier() {
                    @NonNull
                    @Override
                    public NavDestination getProgressDestination() {
                        FragmentNavigator.Destination defaultProgress =
                                fragmentNavigator.createDestination();
                        defaultProgress.setClassName(DefaultProgressFragment.class.getName());
                        defaultProgress.setId(R.id.dfn_progress_fragment);
                        return defaultProgress;
                    }
                });

        navigatorProvider.addNavigator(graphNavigator);

        navigatorProvider.addNavigator(
                new DynamicIncludeGraphNavigator(
                        requireContext(),
                        navigatorProvider,
                        navController.getNavInflater(),
                        installManager)
        );
    }

    /**
     * Create a new {@link SplitInstallManager}.
     */
    @NonNull
    protected SplitInstallManager createSplitInstallManager() {
        return SplitInstallManagerFactory.create(requireContext());
    }
}
