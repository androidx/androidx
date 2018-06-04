/*
 * Copyright 2017 The Android Open Source Project
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.v4.app.TaskStackBuilder;

import androidx.navigation.test.R;
import androidx.navigation.testing.TestNavigator;

import org.junit.Assert;
import org.junit.Test;

@SmallTest
public class NavControllerTest {
    private static final String TEST_ARG = "test";
    private static final String TEST_ARG_VALUE = "value";
    private static final String TEST_OVERRIDDEN_VALUE_ARG = "test_overriden_value";
    private static final String TEST_OVERRIDDEN_VALUE_ARG_VALUE = "override";

    @Test
    public void testStartDestination() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_start_destination);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingStartDestination() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_missing_start_destination);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidStartDestination() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_invalid_start_destination);
    }

    @Test
    public void testNestedStartDestination() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_nested_start_destination);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.nested_test));
    }

    @Test
    public void testSetGraph() {
        NavController navController = createNavController();
        assertThat(navController.getGraph(), is(nullValue(NavGraph.class)));

        navController.setGraph(R.navigation.nav_start_destination);
        assertThat(navController.getGraph(), is(notNullValue(NavGraph.class)));
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
    }

    @Test
    public void testNavigate() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testSaveRestoreStateXml() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavController navController = new NavController(context);
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);

        Bundle savedState = navController.saveState();
        navController = new NavController(context);
        navController.getNavigatorProvider().addNavigator(navigator);

        // Restore state should automatically re-inflate the graph
        // Since the graph has a set id
        navController.restoreState(savedState);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testSaveRestoreStateProgrammatic() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavController navController = new NavController(context);
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        NavGraph graph = new NavInflater(context, navController.getNavigatorProvider())
                .inflate(R.navigation.nav_simple);
        navController.setGraph(graph);
        navController.navigate(R.id.second_test);

        Bundle savedState = navController.saveState();
        navController = new NavController(context);
        navController.getNavigatorProvider().addNavigator(navigator);

        // Restore state doesn't recreate any graph
        navController.restoreState(savedState);
        assertThat(navController.getGraph(), is(nullValue(NavGraph.class)));

        // Explicitly setting a graph then restores the state
        navController.setGraph(graph);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testNavigateWithNoDefaultValue() {
        Bundle returnedArgs = navigateWithArgs(null);

        // Test that arguments without a default value aren't passed through at all
        assertThat(returnedArgs.containsKey("test_no_default_value"), is(false));
    }

    @Test
    public void testNavigateWithDefaultArgs() {
        Bundle returnedArgs = navigateWithArgs(null);

        // Test that default values are passed through
        assertThat(returnedArgs.getString("test_default_value"), is("default"));
    }

    @Test
    public void testNavigateWithArgs() {
        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        Bundle returnedArgs = navigateWithArgs(args);

        // Test that programmatically constructed arguments are passed through
        assertThat(returnedArgs.getString(TEST_ARG), is(TEST_ARG_VALUE));
    }

    @Test
    public void testNavigateWithOverriddenDefaultArgs() {
        Bundle args = new Bundle();
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE);
        Bundle returnedArgs = navigateWithArgs(args);

        // Test that default values can be overridden by programmatic values
        assertThat(returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG),
                is(TEST_OVERRIDDEN_VALUE_ARG_VALUE));
    }

    private Bundle navigateWithArgs(Bundle args) {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_arguments);

        navController.navigate(R.id.second_test, args);

        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        args = navigator.mBackStack.peekLast().second;
        assertThat(args, is(notNullValue(Bundle.class)));

        return args;
    }

    @Test
    public void testNavigateThenPop() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));

        navController.popBackStack();
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNavigateFromNestedThenNavigatorInstigatedPop() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_nested_start_destination);
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.nested_test));
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));

        // A Navigator can pop a destination off its own back stack
        // then inform the NavController via dispatchOnNavigatorNavigated
        navigator.mBackStack.removeLast();
        NavDestination newDestination = navigator.mBackStack.peekLast().first;
        assertThat(newDestination, is(notNullValue()));
        navigator.dispatchOnNavigatorNavigated(newDestination.getId(),
                Navigator.BACK_STACK_DESTINATION_POPPED);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.nested_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNavigateThenNavigateUp() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));

        // This should function identically to popBackStack()
        navController.navigateUp();
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNavigateViaAction() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testNavigateOptionSingleTop() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        navController.navigate(R.id.self);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testNavigateOptionPopUpToInAction() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        navController.navigate(R.id.finish);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNavigateWithPopUpOptionsOnly() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.start_test, false).build();
        // the same as to call .navigate(R.id.finish)
        navController.navigate(0, null, navOptions);

        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNoDestinationNoPopUpTo() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        NavOptions options = new NavOptions.Builder().build();
        try {
            navController.navigate(0, null, options);
            Assert.fail("navController.navigate must throw");
        } catch (IllegalArgumentException e) {
            // expected exception
        }
    }

    @Test
    public void testNavigateOptionPopSelf() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        navController.navigate(R.id.finish_self);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNavigateViaActionWithArgs() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_arguments);

        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE);
        navController.navigate(R.id.second, args);

        TestNavigator navigator = navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        Bundle returnedArgs = navigator.mBackStack.peekLast().second;
        assertThat(returnedArgs, is(notNullValue(Bundle.class)));

        // Test that arguments without a default value aren't passed through at all
        assertThat(returnedArgs.containsKey("test_no_default_value"), is(false));
        // Test that default values are passed through
        assertThat(returnedArgs.getString("test_default_value"), is("default"));
        // Test that programmatically constructed arguments are passed through
        assertThat(returnedArgs.getString(TEST_ARG), is(TEST_ARG_VALUE));
        // Test that default values can be overridden by programmatic values
        assertThat(returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG),
                is(TEST_OVERRIDDEN_VALUE_ARG_VALUE));
    }

    @Test
    public void testDeepLinkFromNavGraph() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);

        TaskStackBuilder taskStackBuilder = navController.createDeepLink()
                .setDestination(R.id.second_test)
                .createTaskStackBuilder();
        assertThat(taskStackBuilder, is(notNullValue(TaskStackBuilder.class)));
        assertThat(taskStackBuilder.getIntentCount(), is(1));
    }

    @Test
    public void testDeepLinkIntent() {
        NavController navController = createNavController();
        navController.setGraph(R.navigation.nav_simple);

        Bundle args = new Bundle();
        args.putString("test", "test");
        TaskStackBuilder taskStackBuilder = navController.createDeepLink()
                .setDestination(R.id.second_test)
                .setArguments(args)
                .createTaskStackBuilder();

        Intent intent = taskStackBuilder.editIntentAt(0);
        assertThat(intent, is(notNullValue()));
        navController.onHandleDeepLink(intent);

        // The original Intent should be untouched and safely writable to a Parcel
        Parcel p = Parcel.obtain();
        intent.writeToParcel(p, 0);
    }

    private NavController createNavController() {
        NavController navController = new NavController(InstrumentationRegistry.getTargetContext());
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        return navController;
    }
}
