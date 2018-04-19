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
import android.support.annotation.NonNull;
import android.view.View;

/**
 * A host is a single context or container for navigation via a {@link NavController}.
 * <p>
 * Navigation hosts must:
 * <ul>
 * <li>Handle {@link NavController#saveState() saving} and
 * {@link NavController#restoreState(Bundle) restoring} their controller's state</li>
 * <li>Call {@link Navigation#setViewNavController(View, NavController)} on their root view</li>
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
