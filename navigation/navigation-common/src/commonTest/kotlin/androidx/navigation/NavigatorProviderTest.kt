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

package androidx.navigation

import androidx.kruth.assertThat
import androidx.kruth.assertWithMessage
import androidx.navigation.testing.TestNavigatorState
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers

@IgnoreAndroidHostTestTarget
class NavigatorProviderTest {

    @Test
    fun addWithExplicitNameGetWithExplicitName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator("name", navigator)

        assertThat(provider.getNavigator<EmptyNavigator>("name")).isEqualTo(navigator)
        try {
            provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME)
            fail("getNavigator(Class) with an invalid name should cause an IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test
    fun addWithAnnotationNameGetWithExplicitName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator(navigator)
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME)).isEqualTo(navigator)
    }

    @Test
    fun addExistingNavigatorDoesntReplace() {
        val navigatorState = TestNavigatorState(Dispatchers.Unconfined)
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()

        provider.addNavigator(navigator)
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME)).isEqualTo(navigator)

        navigator.onAttach(navigatorState)
        assertWithMessage("Navigator should be attached")
            .that(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME).isAttached)
            .isTrue()

        // addNavigator should throw when trying to replace an existing, attached navigator, but
        // we should have returned before that
        try {
            provider.addNavigator(navigator)
        } catch (navigatorAlreadyAttached: IllegalStateException) {
            fail(
                "addNavigator with an existing navigator should return early and not " +
                    "attempt to replace"
            )
        }
    }

    @Test
    fun addWithSameNameButUnequalNavigatorDoesReplace() {
        val provider = NavigatorProvider()
        val navigatorA = EmptyNavigator()
        val navigatorB = EmptyNavigator()

        assertThat(navigatorA).isNotEqualTo(navigatorB)

        provider.addNavigator(navigatorA)
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME)).isEqualTo(navigatorA)

        provider.addNavigator(navigatorB)
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME)).isEqualTo(navigatorB)
    }

    @Test
    fun replaceNavigatorOfCommonTypeWhenGetByType() {
        val provider = NavigatorProvider()
        val navigatorA = EmptyNavigator()

        val navigatorB = EmptyNavigator2()

        assertThat(navigatorA).isNotEqualTo(navigatorB)

        provider.addNavigator(navigatorA)
        assertThat(provider[EmptyNavigator::class]).isEqualTo(navigatorA)

        provider.addNavigator(navigatorB)
        assertThat(provider[EmptyNavigator::class]).isEqualTo(navigatorB)
    }

    private val provider = NavigatorProvider()

    @Test
    fun set() {
        val navigator = NoOpNavigator()
        provider[NAME] = navigator
        val foundNavigator: Navigator<NavDestination> = provider[NAME]
        assertWithMessage("Set destination should be retrieved with get")
            .that(foundNavigator)
            .isSameInstanceAs(navigator)
    }

    @Test
    fun plusAssign() {
        val navigator = NoOpNavigator()
        provider += navigator
        assertWithMessage("Set destination should be retrieved with get")
            .that(provider[NoOpNavigator::class])
            .isSameInstanceAs(navigator)
    }
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect class NoNameNavigator() : Navigator<NavDestination> {
    override fun createDestination(): NavDestination

    override fun popBackStack(): Boolean
}

/** An empty [Navigator] used to test [NavigatorProvider]. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect open class EmptyNavigator() : Navigator<NavDestination> {

    companion object {
        val NAME: String
    }

    override fun createDestination(): NavDestination

    override fun popBackStack(): Boolean
}

internal expect class EmptyNavigator2() : EmptyNavigator

private const val NAME = "TEST"
