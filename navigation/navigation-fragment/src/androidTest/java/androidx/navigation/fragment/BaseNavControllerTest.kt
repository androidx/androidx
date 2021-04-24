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

package androidx.navigation.fragment

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.annotation.IdRes
import androidx.annotation.NavigationRes
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkBuilder
import androidx.navigation.Navigation
import androidx.navigation.fragment.test.R
import androidx.navigation.get
import androidx.navigation.plusAssign
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.TestNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SmallTest
abstract class BaseNavControllerTest<A : BaseNavigationActivity>(
    private val activityClass: Class<A>
) {

    companion object {
        private const val TEST_ARG = "test"
        private const val TEST_ARG_VALUE = "value"
        private const val TEST_DEEP_LINK_ACTION = "deep_link"
    }

    @Suppress("DEPRECATION")
    @get:Rule
    var activityRule = androidx.test.rule.ActivityTestRule(
        activityClass,
        false,
        false
    )

    private lateinit var instrumentation: Instrumentation

    @Before
    fun getInstrumentation() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
    }

    @Test
    fun testStartDestinationDeeplink() {
        assertDeeplink(R.id.start_test, 1)
    }

    @Test
    fun testDeeplink() {
        assertDeeplink(R.id.deep_link_test, 2)
    }

    @Test
    fun testNestedStartDestinationDeeplink() {
        assertDeeplink(R.id.nested_start_test, 2)
    }

    @Test
    fun testNestedDeeplink() {
        assertDeeplink(R.id.nested_deep_link_test, 3)
    }

    @Test
    fun testDoubleNestedStartDestinationDeeplink() {
        assertDeeplink(R.id.double_nested_start_test, 2)
    }

    @Test
    fun testDoubleNestedDeeplink() {
        assertDeeplink(R.id.double_nested_deep_link_test, 3)
    }

    private fun assertDeeplink(@IdRes destId: Int, expectedStackSize: Int) {
        val activity = launchDeepLink(
            R.navigation.nav_deep_link,
            destId, null
        )
        val navController = activity.navController

        assertEquals(destId, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertEquals(expectedStackSize, navigator.backStack.size)

        // Test that the deep link Intent was passed through even though we don't pass in any args

        val deepLinkIntent = navigator.current.arguments?.getParcelable<Intent>(
            NavController.KEY_DEEP_LINK_INTENT
        )
        assertNotNull(deepLinkIntent)
        assertEquals(TEST_DEEP_LINK_ACTION, deepLinkIntent?.action)
    }

    @Test
    fun testStartDestinationDeeplinkWithArgs() {
        assertDeepLinkWithArgs(R.id.start_test, 1)
    }

    @Test
    fun testDeeplinkWithArgs() {
        assertDeepLinkWithArgs(R.id.deep_link_test, 2)
    }

    @Test
    fun testNestedStartDestinationDeeplinkWithArgs() {
        assertDeepLinkWithArgs(R.id.nested_start_test, 2)
    }

    @Test
    fun testNestedDeeplinkWithArgs() {
        assertDeepLinkWithArgs(R.id.nested_deep_link_test, 3)
    }

    @Test
    fun testDoubleNestedStartDestinationDeeplinkWithArgs() {
        assertDeepLinkWithArgs(R.id.double_nested_start_test, 2)
    }

    @Test
    fun testDoubleNestedDeeplinkWithArgs() {
        assertDeepLinkWithArgs(R.id.double_nested_deep_link_test, 3)
    }

    private fun assertDeepLinkWithArgs(@IdRes destId: Int, expectedStackSize: Int) {
        val args = Bundle().apply {
            putString(TEST_ARG, TEST_ARG_VALUE)
        }
        val activity = launchDeepLink(
            R.navigation.nav_deep_link,
            destId, args
        )
        val navController = activity.navController

        assertEquals(destId, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertEquals(expectedStackSize, navigator.backStack.size)

        assertEquals(TEST_ARG_VALUE, navigator.current.arguments?.getString(TEST_ARG))

        // Test that the deep link Intent was passed in alongside our args
        val deepLinkIntent = navigator.current.arguments?.getParcelable<Intent>(
            NavController.KEY_DEEP_LINK_INTENT
        )
        assertNotNull(deepLinkIntent)
        assertEquals(TEST_DEEP_LINK_ACTION, deepLinkIntent?.action)
    }

    @Test
    fun testStartDestinationUriDeepLink() {
        assertUriDeepLink("start", R.id.start_test, 1)
    }

    @Test
    fun testUriDeepLink() {
        assertUriDeepLink("deep_link", R.id.deep_link_test, 2)
    }

    @Test
    fun testNestedStartDestinationUriDeepLink() {
        assertUriDeepLink("nested_start", R.id.nested_start_test, 2)
    }

    @Test
    fun testNestedUriDeepLink() {
        assertUriDeepLink("nested_deep_link", R.id.nested_deep_link_test, 3)
    }

    @Test
    fun testNestedUriDeepLinkWithSlash() {
        assertUriDeepLink(
            "nested_deep_link/$TEST_ARG_VALUE/", TEST_ARG_VALUE,
            R.id.nested_deep_link_test, 3
        )
    }

    @Test
    fun testDoubleNestedStartDestinationUriDeepLink() {
        assertUriDeepLink("double_nested_start", R.id.double_nested_start_test, 2)
    }

    @Test
    fun testDoubleNestedUriDeepLink() {
        assertUriDeepLink("double_nested_deep_link", R.id.double_nested_deep_link_test, 3)
    }

    /**
     * Test a deep link path with the default [TEST_ARG_VALUE] suffix
     */
    private fun assertUriDeepLink(path: String, @IdRes destId: Int, expectedStackSize: Int) {
        assertUriDeepLink("$path/$TEST_ARG_VALUE", TEST_ARG_VALUE, destId, expectedStackSize)
    }

    private fun assertUriDeepLink(
        fullPath: String,
        expectedValue: String,
        @IdRes destId: Int,
        expectedStackSize: Int
    ) {
        val deepLinkUri = Uri.parse("http://www.example.com/$fullPath")
        val intent = Intent(Intent.ACTION_VIEW, deepLinkUri)
            .setComponent(
                ComponentName(
                    instrumentation.context,
                    activityClass
                )
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val activity = launchActivity(intent)
        val navController = activity.navController
        activityRule.runOnUiThread {
            navController.setGraph(R.navigation.nav_deep_link)
        }

        assertEquals(destId, navController.currentDestination?.id ?: 0)
        val navigator = navController.navigatorProvider[TestNavigator::class]
        assertEquals(expectedStackSize, navigator.backStack.size)
        assertEquals(expectedValue, navigator.current.arguments?.getString(TEST_ARG))

        // Test that the deep link Intent was passed in alongside our args
        val deepLinkIntent = navigator.current.arguments?.getParcelable<Intent>(
            NavController.KEY_DEEP_LINK_INTENT
        )
        assertNotNull(deepLinkIntent)
        assertEquals(deepLinkUri, deepLinkIntent?.data)
    }

    private fun launchActivity(intent: Intent): BaseNavigationActivity {
        val activity = activityRule.launchActivity(intent)
        instrumentation.waitForIdleSync()
        val navController = activity.navController
        assertNotNull(navController)
        navController.navigatorProvider += TestNavigator()
        return activity
    }

    private fun launchDeepLink(
        @NavigationRes graphId: Int,
        @IdRes destId: Int,
        args: Bundle?
    ): BaseNavigationActivity {
        val intents = NavDeepLinkBuilder(instrumentation.targetContext)
            .setGraph(graphId)
            .setDestination(destId)
            .setArguments(args)
            .createTaskStackBuilder()
        val intent = intents.editIntentAt(0)!!
        intent.action = TEST_DEEP_LINK_ACTION

        // Now launch the deeplink Intent
        val deeplinkActivity = launchActivity(intent)
        val navController = deeplinkActivity.navController
        activityRule.runOnUiThread {
            navController.setGraph(graphId)
        }

        return deeplinkActivity
    }
}

/**
 * Base Navigation Activity.
 *
 * You must call [NavController.setGraph]
 * to set the appropriate graph for your test.
 */
abstract class BaseNavigationActivity : FragmentActivity() {

    val navController: NavController
        get() = Navigation.findNavController(this, R.id.nav_host)
}
