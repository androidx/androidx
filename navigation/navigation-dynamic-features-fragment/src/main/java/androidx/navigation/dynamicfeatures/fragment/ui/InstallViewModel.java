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

package androidx.navigation.dynamicfeatures.fragment.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.navigation.dynamicfeatures.DynamicInstallMonitor;

/**
 * View model for installation of dynamic feature modules.
 */
final class InstallViewModel extends ViewModel {

    private static final ViewModelProvider.Factory FACTORY = new ViewModelProvider.Factory() {
        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            InstallViewModel viewModel = new InstallViewModel();
            return (T) viewModel;
        }
    };

    @NonNull
    static InstallViewModel getInstance(ViewModelStore viewModelStore) {
        ViewModelProvider viewModelProvider = new ViewModelProvider(viewModelStore,
                FACTORY);
        return viewModelProvider.get(InstallViewModel.class);
    }

    private DynamicInstallMonitor mInstallMonitor;

    @Nullable
    DynamicInstallMonitor getInstallMonitor() {
        return mInstallMonitor;
    }

    void setInstallMonitor(@Nullable DynamicInstallMonitor installMonitor) {
        mInstallMonitor = installMonitor;
    }
}
