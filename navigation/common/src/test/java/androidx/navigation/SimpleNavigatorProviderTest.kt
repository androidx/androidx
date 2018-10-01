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
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class SimpleNavigatorProviderTest {
    @Test
    fun addWithMissingAnnotationName() {
        val provider = SimpleNavigatorProvider()
        val navigator = NoNameNavigator()
        try {
            provider.addNavigator(navigator)
            fail("Adding a provider with no @Navigator.Name should cause an " +
                    "IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun addWithMissingAnnotationNameGetWithExplicitName() {
        val provider = SimpleNavigatorProvider()
        val navigator = NoNameNavigator()
        provider.addNavigator("name", navigator)
        assertEquals(navigator, provider.getNavigator<NavDestination, NoNameNavigator>("name"))
    }

    @Test
    fun addWithExplicitNameGetWithExplicitName() {
        val provider = SimpleNavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator("name", navigator)
        assertEquals(navigator, provider.getNavigator<NavDestination, EmptyNavigator>("name"))
        try {
            provider.getNavigator(EmptyNavigator::class.java)
            fail("getNavigator(Class) with an invalid name should cause an IllegalStateException")
        } catch (e: IllegalStateException) {
            // Expected
        }
    }

    @Test
    fun addWithExplicitNameGetWithMissingAnnotationName() {
        val provider = SimpleNavigatorProvider()
        val navigator = NoNameNavigator()
        provider.addNavigator("name", navigator)
        try {
            provider.getNavigator(NoNameNavigator::class.java)
            fail("getNavigator(Class) with no @Navigator.Name should cause an " +
                    "IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun addWithAnnotationNameGetWithAnnotationName() {
        val provider = SimpleNavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator(navigator)
        assertEquals(navigator, provider.getNavigator(EmptyNavigator::class.java))
    }

    @Test
    fun addWithAnnotationNameGetWithExplicitName() {
        val provider = SimpleNavigatorProvider()
        val navigator = EmptyNavigator()
        provider.addNavigator(navigator)
        assertEquals(navigator,
                provider.getNavigator<NavDestination, EmptyNavigator>(EmptyNavigator.NAME))
    }
}

class NoNameNavigator : Navigator<NavDestination>() {
    override fun createDestination(): NavDestination {
        return NavDestination(this)
    }

    override fun navigate(
        destination: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        throw IllegalStateException("navigate is not supported")
    }

    override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}

/**
 * An empty [Navigator] used to test [SimpleNavigatorProvider].
 */
@Navigator.Name(EmptyNavigator.NAME)
internal open class EmptyNavigator : Navigator<NavDestination>() {

    companion object {
        const val NAME = "empty"
    }

    override fun createDestination(): NavDestination {
        return NavDestination(this)
    }

    override fun navigate(
        destination: NavDestination,
        args: Bundle?,
        navOptions: NavOptions?,
        navigatorExtras: Navigator.Extras?
    ) {
        throw IllegalStateException("navigate is not supported")
    }

    override fun popBackStack(): Boolean {
        throw IllegalStateException("popBackStack is not supported")
    }
}
