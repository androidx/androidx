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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.os.Bundle;
import android.support.annotation.IdRes;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
@SmallTest
public class NavGraphNavigatorStateTest {
    @IdRes
    private static final int FIRST_DESTINATION_ID = 1;

    private NavGraphNavigator mNavGraphNavigator;
    private Navigator.OnNavigatorNavigatedListener mListener;

    @Before
    public void setup() {
        mNavGraphNavigator = new NavGraphNavigator(InstrumentationRegistry.getTargetContext());
        mListener = mock(Navigator.OnNavigatorNavigatedListener.class);
        mNavGraphNavigator.addOnNavigatorNavigatedListener(mListener);
    }

    @Test
    public void navigateSingleTopSaveState() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.setId(FIRST_DESTINATION_ID);
        final NavGraph graph = mNavGraphNavigator.createDestination();
        graph.addDestination(destination);
        graph.setStartDestination(FIRST_DESTINATION_ID);
        graph.navigate(null, null);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);

        // Save and restore the state, effectively resetting the NavGraphNavigator
        Bundle saveState = mNavGraphNavigator.onSaveState();
        mNavGraphNavigator.onRestoreState(saveState);

        graph.navigate(null, new NavOptions.Builder().setLaunchSingleTop(true).build());
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_UNCHANGED);
        verifyNoMoreInteractions(mListener);
    }
}
