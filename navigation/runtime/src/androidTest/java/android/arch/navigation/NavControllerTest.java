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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.XmlRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;

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
        navController.addNavigator(navigator);
        navController.setGraph(R.xml.nav_start_destination);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
    }

    @Test
    public void testNestedStartDestination() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavController navController = new NavController(context);
        TestNavigator navigator = new TestNavigator();
        navController.addNavigator(navigator);
        navController.setGraph(R.xml.nav_nested_start_destination);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.nested_test));
    }

    @Test
    public void testSetGraph() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        assertThat(navController.getGraph(), is(nullValue(NavGraph.class)));

        navController.setGraph(R.xml.nav_start_destination);
        assertThat(navController.getGraph(), is(notNullValue(NavGraph.class)));
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
    }

    @Test
    public void testNavigate() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.xml.nav_simple);
        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second_test);
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
        navController.setGraph(R.xml.nav_arguments);

        navController.navigate(R.id.second_test, args);

        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
        args = navigator.mBackStack.peekLast().second;
        assertThat(args, is(notNullValue(Bundle.class)));

        return args;
    }

    @Test
    public void testNavigateThenPop() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.xml.nav_simple);
        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
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
        navController.setGraph(R.xml.nav_simple);
        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
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
        navController.setGraph(R.xml.nav_simple);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(1));

        navController.navigate(R.id.second);
        assertThat(navController.getCurrentDestination().getId(), is(R.id.second_test));
        assertThat(navigator.mBackStack.size(), is(2));
    }

    @Test
    public void testNavigateViaActionWithArgs() throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(R.xml.nav_arguments);

        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        args.putString(TEST_OVERRIDDEN_VALUE_ARG, TEST_OVERRIDDEN_VALUE_ARG_VALUE);
        navController.navigate(R.id.second, args);

        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
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
        NavigationActivity activity = launchDeepLink(R.xml.nav_deep_link, R.id.deep_link_test,
                null);
        NavController navController = activity.getNavController();

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(1));

        // Test that the deep link Intent was passed through even though we don't pass in any args
        Intent deepLinkIntent = navigator.mBackStack.peekLast().second
                .getParcelable(NavController.KEY_DEEP_LINK_INTENT);
        assertThat(deepLinkIntent, is(notNullValue(Intent.class)));
        //noinspection ConstantConditions
        assertThat(deepLinkIntent.getAction(), is(TEST_DEEP_LINK_ACTION));
    }

    @Test
    public void testDeeplinkWithArgs() throws Throwable {
        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        NavigationActivity activity = launchDeepLink(R.xml.nav_deep_link, R.id.deep_link_test,
                args);
        NavController navController = activity.getNavController();

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(1));
        assertThat(navigator.mBackStack.peekLast().second.getString(TEST_ARG), is(TEST_ARG_VALUE));

        // Test that the deep link Intent was passed in alongside our args
        Intent deepLinkIntent = navigator.mBackStack.peekLast().second
                .getParcelable(NavController.KEY_DEEP_LINK_INTENT);
        assertThat(deepLinkIntent, is(notNullValue(Intent.class)));
        //noinspection ConstantConditions
        assertThat(deepLinkIntent.getAction(), is(TEST_DEEP_LINK_ACTION));
    }

    @Test
    public void testDeeplinkNavigateUp() throws Throwable {
        NavigationActivity activity = launchDeepLink(R.xml.nav_deep_link, R.id.deep_link_test,
                null);
        NavController navController = activity.getNavController();
        navController.navigateUp();

        assertThat(navController.getCurrentDestination().getId(), is(R.id.start_test));
        TestNavigator navigator = (TestNavigator) navController.getNavigator(TestNavigator.class);
        assertThat(navigator.mBackStack.size(), is(1));
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
        navController.addNavigator(navigator);
        return activity;
    }

    private NavigationActivity launchDeepLink(@XmlRes int graphId, @IdRes int destId, Bundle args)
            throws Throwable {
        NavigationActivity activity = launchActivity();
        NavController navController = activity.getNavController();
        navController.setGraph(graphId);

        Intent intent = navController.createDeepLinkIntent(destId, args);
        assertThat(intent, is(notNullValue(Intent.class)));
        intent.setAction(TEST_DEEP_LINK_ACTION);

        // Now launch the deeplink Intent
        NavigationActivity deeplinkActivity = launchActivity(intent);
        navController = deeplinkActivity.getNavController();
        navController.setGraph(graphId);

        return deeplinkActivity;
    }
}
