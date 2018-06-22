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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
@SmallTest
public class NavGraphNavigatorTest {
    @IdRes
    private static final int FIRST_DESTINATION_ID = 1;
    @IdRes
    private static final int SECOND_DESTINATION_ID = 2;

    private NavGraphNavigator mNavGraphNavigator;
    private Navigator.OnNavigatorNavigatedListener mListener;

    @Before
    public void setup() {
        mNavGraphNavigator = new NavGraphNavigator(mock(Context.class));
        mListener = mock(Navigator.OnNavigatorNavigatedListener.class);
        mNavGraphNavigator.addOnNavigatorNavigatedListener(mListener);
    }

    private NavDestination createFirstDestination() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.setId(FIRST_DESTINATION_ID);
        return destination;
    }

    private NavDestination createSecondDestination() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.setId(SECOND_DESTINATION_ID);
        return destination;
    }

    private NavGraph createGraphWithDestination(NavDestination destination) {
        NavGraph graph = mNavGraphNavigator.createDestination();
        graph.addDestination(destination);
        return graph;
    }

    @Test(expected = IllegalStateException.class)
    public void navigateWithoutStartDestination() {
        NavDestination destination = createFirstDestination();
        final NavGraph graph = createGraphWithDestination(destination);
        graph.navigate(null, null);
    }

    @Test
    public void navigate() {
        NavDestination destination = createFirstDestination();
        final NavGraph graph = createGraphWithDestination(destination);
        graph.setStartDestination(FIRST_DESTINATION_ID);
        graph.navigate(null, null);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void popWithEmptyStack() {
        boolean success = mNavGraphNavigator.popBackStack();
        assertFalse("popBackStack should return false on an empty stack", success);
    }

    @Test
    public void navigateThenPop() {
        NavDestination destination = createFirstDestination();
        final NavGraph graph = createGraphWithDestination(destination);
        graph.setStartDestination(FIRST_DESTINATION_ID);
        graph.navigate(null, null);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        boolean success = mNavGraphNavigator.popBackStack();
        assertTrue("popBackStack should return true", success);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_POPPED);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void navigateSingleTopOnEmptyStack() {
        NavDestination destination = createFirstDestination();
        final NavGraph graph = createGraphWithDestination(destination);
        graph.setStartDestination(FIRST_DESTINATION_ID);
        // singleTop should still show as added on an empty stack
        graph.navigate(null, new NavOptions.Builder().setLaunchSingleTop(true).build());
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void navigateSingleTop() {
        NavDestination destination = createFirstDestination();
        final NavGraph graph = createGraphWithDestination(destination);
        graph.setStartDestination(FIRST_DESTINATION_ID);
        graph.navigate(null, null);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        graph.navigate(null, new NavOptions.Builder().setLaunchSingleTop(true).build());
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_UNCHANGED);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void navigateSingleTopNotTop() {
        NavDestination destination = createFirstDestination();
        final NavGraph graph = createGraphWithDestination(destination);
        graph.setStartDestination(FIRST_DESTINATION_ID);
        NavDestination secondDestination = createSecondDestination();
        final NavGraph secondGraph = createGraphWithDestination(secondDestination);
        secondGraph.setId(SECOND_DESTINATION_ID);
        secondGraph.setStartDestination(SECOND_DESTINATION_ID);
        graph.navigate(null, null);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        secondGraph.navigate(null, new NavOptions.Builder().setLaunchSingleTop(true).build());
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                secondGraph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        verifyNoMoreInteractions(mListener);
    }

    @Test
    public void navigateSingleTopNested() {
        NavDestination destination = createFirstDestination();
        final NavGraph nestedGraph = createGraphWithDestination(destination);
        nestedGraph.setId(FIRST_DESTINATION_ID);
        nestedGraph.setStartDestination(FIRST_DESTINATION_ID);
        final NavGraph graph = createGraphWithDestination(nestedGraph);
        graph.setStartDestination(FIRST_DESTINATION_ID);
        graph.navigate(null, null);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                nestedGraph.getId(),
                Navigator.BACK_STACK_DESTINATION_ADDED);
        graph.navigate(null, new NavOptions.Builder().setLaunchSingleTop(true).build());
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                graph.getId(),
                Navigator.BACK_STACK_UNCHANGED);
        verify(mListener).onNavigatorNavigated(mNavGraphNavigator,
                nestedGraph.getId(),
                Navigator.BACK_STACK_UNCHANGED);
        verifyNoMoreInteractions(mListener);
    }
}
