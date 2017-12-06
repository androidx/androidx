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

package android.arch.navigation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.arch.navigation.activity.NavigationActivity;
import android.arch.navigation.test.R;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NavigationRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.TaskStackBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public class NavControllerTest {
    private static final String TEST_ARG = "test";
    private static final String TEST_ARG_VALUE = "value";
    private static final String TEST_OVERRIDDEN_VALUE_ARG = "test_overriden_value";
    private static final String TEST_OVERRIDDEN_VALUE_ARG_VALUE = "override";
    private static final String TEST_DEEP_LINK_ACTION = "deep_link";

    @Rule
    public ActivityTestRule<NavigationActivity> mActivityRule =
            new ActivityTestRule<>(NavigationActivity.class, false, false);

    private Instrumentation mInstrumentation;

    @Before
    public void getInstrumentation() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void testStartDestination() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavController navController = new NavController(context);
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        navController.setGraph(R.navigation.nav_start_destination);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
    }

    @Test(expected = IllegalStateException.class)
    public void testMissingStartDestination() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavController navController = new NavController(context);
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        navController.setGraph(R.navigation.nav_missing_start_destination);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidStartDestination() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavController navController = new NavController(context);
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        navController.setGraph(R.navigation.nav_invalid_start_destination);
    }

    @Test
    public void testNestedStartDestination() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavController navController = new NavController(context);
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        navController.setGraph(R.navigation.nav_nested_start_destination);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.nested_test));
    }

    @Test
    public void testSetGraph() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        assertThat(navController.getGraph(), is(nullValue(NavGraph.class)));

        navController.setGraph(R.navigation.nav_start_destination);
        assertThat(navController.getGraph(), is(notNullValue(NavGraph.class)));
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
    }

    @Test
    public void testNavigate() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
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
    public void testNavigateWithNoDefaultValue() throws Throwable {
        Bundle returnedArgs = navigateWithArgs(null);

        // Test that arguments without a default value aren't passed through at all
        assertThat(returnedArgs.containsKey("test_no_default_value"), is(false));
    }

    @Test
    public void testNavigateWithDefaultArgs() throws Throwable {
        Bundle returnedArgs = navigateWithArgs(null);

        // Test that default values are passed through
        assertThat(returnedArgs.getString("test_default_value"), is("default"));
    }

    @Test
    public void testNavigateWithArgs() throws Throwable {
        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        Bundle returnedArgs = navigateWithArgs(args);

        // Test that programmatically constructed arguments are passed through
        assertThat(returnedArgs.getString(TEST_ARG), is(TEST_ARG_VALUE));
    }

    @Test
    public void testNavigateWithOverriddenDefaultArgs() throws Throwable {
        Bundle args = new Bundle();
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE);
        Bundle returnedArgs = navigateWithArgs(args);

        // Test that default values can be overridden by programmatic values
        assertThat(returnedArgs.getString(TEST_OVERRIDDEN_VALUE_ARG),
                is(TEST_OVERRIDDEN_VALUE_ARG_VALUE));
    }

    private Bundle navigateWithArgs(Bundle args) throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_arguments);

        navController.navigate(R.id.second_test, args);

        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        args = navigator.mBackStack.peekLast().second;
        assertThat(args, is(notNullValue(Bundle.class)));

        return args;
    }

    @Test
    public void testNavigateThenPop() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
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
    public void testNavigateThenNavigateUp() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
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
    public void testNavigateViaAction() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testNavigateOptionSingleTop() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        navController.navigate(R.id.self);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testNavigateOptionPopUpToInAction() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        navController.navigate(R.id.finish);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNavigateWithPopUpOptionsOnly() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        NavOptions navOptions = new NavOptions.Builder().setPopUpTo(R.id.start_test, false).build();
        // the same as to call .navigate(R.id.finish)
        navController.navigate(0, null, navOptions);

        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNoDestinationNoPopUpTo() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
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
    public void testNavigateOptionPopSelf() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_simple);
        navController.navigate(R.id.second_test);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        navController.navigate(R.id.finish_self);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));
    }

    @Test
    public void testNavigateViaActionWithArgs() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_arguments);

        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE);
        navController.navigate(R.id.second, args);

        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
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
    public void testDeeplink() throws Throwable {
        NavigationActivity activity = launchDeepLink(R.navigation.nav_deep_link,
                R.id.deep_link_test, null);
        NavController navController = activity.getNavController();

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));

        // Test that the deep link Intent was passed through even though we don't pass in any args
        Intent deepLinkIntent = navigator.mBackStack.peekLast().second
                .getParcelable(NavController.KEY_DEEP_LINK_INTENT);
        assertThat(deepLinkIntent, is(notNullValue(Intent.class)));
        //noinspection ConstantConditions
        assertThat(deepLinkIntent.getAction(), is(TEST_DEEP_LINK_ACTION));
    }

    @Test
    public void testDeepLinkFromNavGraph() throws Throwable {
        NavController navController = new NavController(mInstrumentation.getTargetContext());
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        navController.setGraph(R.navigation.nav_deep_link);

        TaskStackBuilder taskStackBuilder = navController.createDeepLink()
                .setDestination(R.id.deep_link_test)
                .createTaskStackBuilder();
        assertThat(taskStackBuilder, is(notNullValue(TaskStackBuilder.class)));
        assertThat(taskStackBuilder.getIntentCount(), is(1));
    }

    @Test
    public void testDeeplinkWithArgs() throws Throwable {
        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        NavigationActivity activity = launchDeepLink(R.navigation.nav_deep_link,
                R.id.deep_link_test, args);
        NavController navController = activity.getNavController();

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));
        assertThat(navigator.mBackStack.peekLast().second.getString(TEST_ARG), is(TEST_ARG_VALUE));

        // Test that the deep link Intent was passed in alongside our args
        Intent deepLinkIntent = navigator.mBackStack.peekLast().second
                .getParcelable(NavController.KEY_DEEP_LINK_INTENT);
        assertThat(deepLinkIntent, is(notNullValue(Intent.class)));
        //noinspection ConstantConditions
        assertThat(deepLinkIntent.getAction(), is(TEST_DEEP_LINK_ACTION));
    }

    @Test
    public void testUriDeepLink() throws Throwable {
        Uri deepLinkUri = Uri.parse("http://www.example.com/" + TEST_ARG_VALUE);
        Intent intent = new Intent(Intent.ACTION_VIEW, deepLinkUri)
                .setComponent(new ComponentName(mInstrumentation.getContext(),
                        NavigationActivity.class))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        NavigationActivity activity = launchActivity(intent);
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_deep_link);

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigatorProvider()
                .getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(2));
        assertThat(navigator.mBackStack.peekLast().second.getString(TEST_ARG), is(TEST_ARG_VALUE));

        // Test that the deep link Intent was passed in alongside our args
        Intent deepLinkIntent = navigator.mBackStack.peekLast().second
                .getParcelable(NavController.KEY_DEEP_LINK_INTENT);
        assertThat(deepLinkIntent, is(notNullValue(Intent.class)));
        //noinspection ConstantConditions
        assertThat(deepLinkIntent.getData(), is(deepLinkUri));
    }

    private NavigationActivity launchActivity() throws Throwable {
        return launchActivity(new Intent(mInstrumentation.getTargetContext(),
                NavigationActivity.class));
    }

    private NavigationActivity launchActivity(Intent intent) throws Throwable {
        NavigationActivity activity = mActivityRule.launchActivity(intent);
        mInstrumentation.waitForIdleSync();
        NavController navController = activity.getNavController();
        assertThat(navController, is(notNullValue(NavController.class)));
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        return activity;
    }

    private NavigationActivity launchDeepLink(@NavigationRes int graphId, @IdRes int destId,
            Bundle args) throws Throwable {
        TaskStackBuilder intents = new NavDeepLinkBuilder(mInstrumentation.getTargetContext())
                .setGraph(graphId)
                .setDestination(destId)
                .setArguments(args)
                .createTaskStackBuilder();
        Intent intent = intents.editIntentAt(0);
        intent.setAction(TEST_DEEP_LINK_ACTION);

        // Now launch the deeplink Intent
        NavigationActivity deeplinkActivity = launchActivity(intent);
        NavController navController = deeplinkActivity.getNavController();
        navController.setGraph(graphId);

        return deeplinkActivity;
    }
}
