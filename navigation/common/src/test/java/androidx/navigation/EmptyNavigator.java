/*
 * Copyright (C) 2017 The Android Open Source Project
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

/**
 * An empty {@link Navigator} used to test {@link SimpleNavigatorProvider}.
 */
@Navigator.Name(EmptyNavigator.NAME)
class EmptyNavigator extends Navigator<NavDestination> {
    static final String NAME = "empty";

    @Override
    public NavDestination createDestination() {
        return new NavDestination(this);
    }

    @Override
    public void navigate(NavDestination destination, Bundle args, NavOptions navOptions) {
        throw new IllegalStateException("navigate is not supported");
    }

    @Override
    public boolean popBackStack() {
        throw new IllegalStateException("popBackStack is not supported");
    }
}
