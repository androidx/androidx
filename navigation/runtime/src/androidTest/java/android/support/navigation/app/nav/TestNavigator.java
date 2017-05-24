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

package android.support.navigation.app.nav;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.ArrayDeque;

/**
 * A simple Navigator that doesn't actually navigate anywhere, but does dispatch correctly
 */
@Navigator.Name("test")
public class TestNavigator extends Navigator<TestNavigator.Destination> {

    private final ArrayDeque<Destination> mBackStack = new ArrayDeque<>();

    @Override
    public Destination createDestination() {
        return new Destination(this);
    }

    @Override
    public void navigate(Destination destination, Bundle args,
            NavOptions navOptions) {
        mBackStack.add(destination);
        dispatchOnNavigatorNavigated(destination.getId(), BACK_STACK_DESTINATION_ADDED);
    }

    @Override
    public boolean popBackStack() {
        mBackStack.pop();
        dispatchOnNavigatorNavigated(mBackStack.isEmpty() ? 0 : mBackStack.peekLast().getId(),
                BACK_STACK_DESTINATION_POPPED);
        return true;
    }

    static class Destination extends NavDestination {
        /**
         * NavDestinations should be created via {@link Navigator#createDestination}.
         */
        Destination(@NonNull Navigator<? extends NavDestination> navigator) {
            super(navigator);
        }
    }
}
