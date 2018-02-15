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

package android.arch.navigation.fragment;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.arch.navigation.NavController;
import android.arch.navigation.NavDeepLinkBuilder;
import android.arch.navigation.fragment.test.BaseNavigationActivity;
import android.arch.navigation.fragment.test.R;
import android.arch.navigation.testing.TestNavigator;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NavigationRes;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.TaskStackBuilder;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
public abstract class BaseNavControllerTest<A extends BaseNavigationActivity> {
    private static final String TEST_ARG = "test";
    private static final String TEST_ARG_VALUE = "value";
    private static final String TEST_DEEP_LINK_ACTION = "deep_link";

    /**
     * @return The concrete Activity class under test
     */
    protected abstract Class<A> getActivityClass();

    @Rule
    public ActivityTestRule<A> mActivityRule =
            new ActivityTestRule<>(getActivityClass(), false, false);

    private Instrumentation mInstrumentation;

    @Before
    public void getInstrumentation() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void testDeeplink() throws Throwable {
        BaseNavigationActivity activity = launchDeepLink(R.navigation.nav_deep_link,
                R.id.deep_link_test, null);
        NavController navController = activity.getNavController();

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = navController.getNavigatorProvider()
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
    public void testDeeplinkWithArgs() throws Throwable {
        Bundle args = new Bundle();
        args.putString(TEST_ARG, TEST_ARG_VALUE);
        BaseNavigationActivity activity = launchDeepLink(R.navigation.nav_deep_link,
                R.id.deep_link_test, args);
        NavController navController = activity.getNavController();

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = navController.getNavigatorProvider()
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
                        getActivityClass()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        BaseNavigationActivity activity = launchActivity(intent);
        NavController navController = activity.getNavController();
        navController.setGraph(R.navigation.nav_deep_link);

        assertThat(navController.getCurrentDestination().getId(), is(R.id.deep_link_test));
        TestNavigator navigator = navController.getNavigatorProvider()
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

    private BaseNavigationActivity launchActivity(Intent intent) throws Throwable {
        BaseNavigationActivity activity = mActivityRule.launchActivity(intent);
        mInstrumentation.waitForIdleSync();
        NavController navController = activity.getNavController();
        assertThat(navController, is(notNullValue(NavController.class)));
        TestNavigator navigator = new TestNavigator();
        navController.getNavigatorProvider().addNavigator(navigator);
        return activity;
    }

    private BaseNavigationActivity launchDeepLink(@NavigationRes int graphId, @IdRes int destId,
            Bundle args) throws Throwable {
        TaskStackBuilder intents = new NavDeepLinkBuilder(mInstrumentation.getTargetContext())
                .setGraph(graphId)
                .setDestination(destId)
                .setArguments(args)
                .createTaskStackBuilder();
        Intent intent = intents.editIntentAt(0);
        intent.setAction(TEST_DEEP_LINK_ACTION);

        // Now launch the deeplink Intent
        BaseNavigationActivity deeplinkActivity = launchActivity(intent);
        NavController navController = deeplinkActivity.getNavController();
        navController.setGraph(graphId);

        return deeplinkActivity;
    }
}
