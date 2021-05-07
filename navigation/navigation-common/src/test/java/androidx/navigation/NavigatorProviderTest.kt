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

import android.os.Bundle
import androidx.navigation.testing.TestNavigatorState
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavigatorProviderTest {
    @Test
    fun addWithMissingAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = NoNameNavigator()
        try {
            provider.addNavigator(navigator)
            fail(
                "Adding a provider with no @Navigator.Name should cause an " +
                    "IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun addWithMissingAnnotationNameGetWithExplicitName() {
        val provider = NavigatorProvider()
        val navigator = NoNameNavigator()
        provider.addNavigator("name", navigator)
        assertThat(provider.getNavigator<NoNameNavigator>("name"))
            .isEqualTo(navigator)
    }

    @Test
    fun addWithExplicitNameGetWithExplicitName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator("name", navigator)

        assertThat(provider.getNavigator<EmptyNavigator>("name"))
            .isEqualTo(navigator)
        try {
            provider.getNavigator(EmptyNavigator::class.java)
            fail("getNavigator(Class) with an invalid name should cause an IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test
    fun addWithExplicitNameGetWithMissingAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = NoNameNavigator()
        provider.addNavigator("name", navigator)
        try {
            provider.getNavigator(NoNameNavigator::class.java)
            fail(
                "getNavigator(Class) with no @Navigator.Name should cause an " +
                    "IllegalArgumentException"
            )
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun addWithAnnotationNameGetWithAnnotationName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator(navigator)
        assertThat(provider.getNavigator(EmptyNavigator::class.java))
            .isEqualTo(navigator)
    }

    @Test
    fun addWithAnnotationNameGetWithExplicitName() {
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator(navigator)
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME))
            .isEqualTo(navigator)
    }

    @Test
    fun addExistingNavigatorDoesntReplace() {
        val navigatorState = TestNavigatorState()
        val provider = NavigatorProvider()
        val navigator = EmptyNavigator()

        provider.addNavigator(navigator)
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME))
            .isEqualTo(navigator)

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
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME))
            .isEqualTo(navigatorA)

        provider.addNavigator(navigatorB)
        assertThat(provider.getNavigator<EmptyNavigator>(EmptyNavigator.NAME))
            .isEqualTo(navigatorB)
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

class NoNameNavigator : Navigator<NavDestination>() {
    override fun createDestination(): NavDestination {
        throw IllegalStateException("createDestination is not supported")
    }

    override fun navigate(
        destination: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? {
        throw IllegalStateException("navigate is not supported")
    }

    override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}

/**
 * An empty [Navigator] used to test [NavigatorProvider].
 */
@Navigator.Name(EmptyNavigator.NAME)
internal open class EmptyNavigator : Navigator<NavDestination>() {

    companion object {
        const val NAME = "empty"
    }

    override fun createDestination(): NavDestination {
        throw IllegalStateException("createDestination is not supported")
    }

    override fun navigate(
        destination: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Extras?
    ): NavDestination? {
        throw IllegalStateException("navigate is not supported")
    }

    override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}

private const val NAME = "TEST"
