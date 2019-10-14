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

package androidx.navigation;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelStore;

/**
 * A host is a single context or container for navigation via a {@link NavController}.
 * <p>
 * It is strongly recommended to construct the nav controller by instantiating a
 * {@link NavHostController}, which offers additional APIs specifically for a NavHost.
 * The NavHostController should still only be externally accessible as a {@link NavController},
 * rather than directly exposing it as a {@link NavHostController}.
 * <p>
 * Navigation hosts must:
 * <ul>
 * <li>Handle {@link NavController#saveState() saving} and
 * {@link NavController#restoreState(Bundle) restoring} their controller's state</li>
 * <li>Call {@link Navigation#setViewNavController(View, NavController)} on their root view</li>
 * <li>Route system Back button events to the NavController either by manually calling
 * {@link NavController#popBackStack()} or by calling
 * {@link NavHostController#setOnBackPressedDispatcher(androidx.activity.OnBackPressedDispatcher)}
 * when constructing the NavController.</li>
 * </ul>
 * Optionally, a navigation host should consider calling:
 * <ul>
 * <li>Call {@link NavHostController#setLifecycleOwner(LifecycleOwner)} to associate the
 * NavController with a specific Lifecycle.</li>
 * <li>Call {@link NavHostController#setViewModelStore(ViewModelStore)} to enable usage of
 * {@link NavController#getViewModelStoreOwner(int)} and navigation graph scoped ViewModels.</li>
 * </ul>
 */
public interface NavHost {

    /**
     * Returns the {@link NavController navigation controller} for this navigation host.
     *
     * @return this host's navigation controller
     */
    @NonNull
    NavController getNavController();
}
