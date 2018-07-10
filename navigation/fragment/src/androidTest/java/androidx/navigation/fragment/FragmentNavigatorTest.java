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

package androidx.navigation.fragment;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.arch.lifecycle.Lifecycle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import androidx.navigation.NavOptions;
import androidx.navigation.Navigator;
import androidx.navigation.fragment.test.EmptyActivity;
import androidx.navigation.fragment.test.EmptyFragment;
import androidx.navigation.fragment.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class FragmentNavigatorTest {

    private static final int INITIAL_FRAGMENT = 1;

    @Rule
    public ActivityTestRule<EmptyActivity> mActivityRule =
            new ActivityTestRule<>(EmptyActivity.class);

    private EmptyActivity mEmptyActivity;
    private FragmentManager mFragmentManager;

    @Before
    public void setup() {
        mEmptyActivity = mActivityRule.getActivity();
        mFragmentManager = mEmptyActivity.getSupportFragmentManager();
    }

    @UiThreadTest
    @Test
    public void testNavigate() {
        FragmentNavigator fragmentNavigator = new FragmentNavigator(mEmptyActivity,
                mFragmentManager, R.id.container);
        FragmentNavigator.Destination destination = fragmentNavigator.createDestination();
        destination.setFragmentClass(EmptyFragment.class);

        fragmentNavigator.navigate(destination, null, null);
        mFragmentManager.executePendingTransactions();
        Fragment fragment = mFragmentManager.findFragmentById(R.id.container);
        assertThat("Fragment should be added", fragment, is(notNullValue()));
        assertThat("Fragment should be the correct type", fragment,
                is(CoreMatchers.<Fragment>instanceOf(EmptyFragment.class)));
    }

    @UiThreadTest
    @Test
    public void testSingleTopInitial() {
        FragmentNavigator fragmentNavigator = new FragmentNavigator(mEmptyActivity,
                mFragmentManager, R.id.container);
        FragmentNavigator.Destination destination = fragmentNavigator.createDestination();
        destination.setFragmentClass(EmptyFragment.class);

        fragmentNavigator.navigate(destination, null, null);
        mFragmentManager.executePendingTransactions();
        Fragment fragment = mFragmentManager.findFragmentById(R.id.container);
        assertThat("Fragment should be added", fragment, is(notNullValue()));

        fragmentNavigator.navigate(destination, null,
                new NavOptions.Builder().setLaunchSingleTop(true).build());
        mFragmentManager.executePendingTransactions();
        Fragment replacementFragment = mFragmentManager.findFragmentById(R.id.container);
        assertThat("Replacement Fragment should be added", replacementFragment,
                is(notNullValue()));
        assertThat("Replacement Fragment should be the correct type", replacementFragment,
                is(CoreMatchers.<Fragment>instanceOf(EmptyFragment.class)));
        assertThat("Replacement should be a new instance", replacementFragment,
                is(not(equalTo(fragment))));
        assertThat("Old instance should be destroyed", fragment.getLifecycle().getCurrentState(),
                is(equalTo(Lifecycle.State.DESTROYED)));
    }

    @UiThreadTest
    @Test
    public void testSingleTop() {
        FragmentNavigator fragmentNavigator = new FragmentNavigator(mEmptyActivity,
                mFragmentManager, R.id.container);
        FragmentNavigator.Destination destination = fragmentNavigator.createDestination();
        destination.setFragmentClass(EmptyFragment.class);

        // First push an initial Fragment
        fragmentNavigator.navigate(destination, null, null);

        // Now push the Fragment that we want to replace with a singleTop operation
        fragmentNavigator.navigate(destination, null, null);
        mFragmentManager.executePendingTransactions();
        Fragment fragment = mFragmentManager.findFragmentById(R.id.container);
        assertThat("Fragment should be added", fragment, is(notNullValue()));

        fragmentNavigator.navigate(destination, null,
                new NavOptions.Builder().setLaunchSingleTop(true).build());
        mFragmentManager.executePendingTransactions();
        Fragment replacementFragment = mFragmentManager.findFragmentById(R.id.container);
        assertThat("Replacement Fragment should be added", replacementFragment,
                is(notNullValue()));
        assertThat("Replacement Fragment should be the correct type", replacementFragment,
                is(CoreMatchers.<Fragment>instanceOf(EmptyFragment.class)));
        assertThat("Replacement should be a new instance", replacementFragment,
                is(not(equalTo(fragment))));
        assertThat("Old instance should be destroyed", fragment.getLifecycle().getCurrentState(),
                is(equalTo(Lifecycle.State.DESTROYED)));
    }

    @UiThreadTest
    @Test
    public void testPopInitial() {
        FragmentNavigator fragmentNavigator = new FragmentNavigator(mEmptyActivity,
                mFragmentManager, R.id.container);
        Navigator.OnNavigatorNavigatedListener listener =
                mock(Navigator.OnNavigatorNavigatedListener.class);
        fragmentNavigator.addOnNavigatorNavigatedListener(listener);
        FragmentNavigator.Destination destination = fragmentNavigator.createDestination();
        destination.setId(INITIAL_FRAGMENT);
        destination.setFragmentClass(EmptyFragment.class);

        // First push an initial Fragment
        fragmentNavigator.navigate(destination, null, null);
        verify(listener).onNavigatorNavigated(
                fragmentNavigator,
                INITIAL_FRAGMENT,
                Navigator.BACK_STACK_DESTINATION_ADDED);

        // Now pop the initial Fragment
        boolean popped = fragmentNavigator.popBackStack();
        assertThat("FragmentNavigator should return false when popping the initial Fragment",
                popped, is(false));
        verify(listener).onNavigatorNavigated(
                fragmentNavigator,
                0,
                Navigator.BACK_STACK_DESTINATION_POPPED);
        verifyNoMoreInteractions(listener);
    }
}
