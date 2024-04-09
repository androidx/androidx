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

@file:OptIn(ExperimentalSafeArgsApi::class)

package androidx.navigation

import androidx.annotation.IdRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.test.assertFailsWith
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavDestinationTest {
    private val provider = NavigatorProvider().apply {
        addNavigator(NavGraphNavigator(this))
        addNavigator(NoOpNavigator())
    }

    @Test
    fun navDestination() {
        val destination = provider.navDestination(DESTINATION_ID) { }
        assertWithMessage("NavDestination should have id set")
            .that(destination.id)
            .isEqualTo(DESTINATION_ID)
    }

    @Test
    fun navDestinationRoute() {
        val destination = provider.navDestination(DESTINATION_ROUTE) { }
        assertWithMessage("NavDestination should have route set")
            .that(destination.route)
            .isEqualTo(DESTINATION_ROUTE)
    }

    @Test
    fun navDestinationLabel() {
        val destination = provider.navDestination(DESTINATION_ID) {
            label = LABEL
        }
        assertWithMessage("NavDestination should have label set")
            .that(destination.label)
            .isEqualTo(LABEL)
    }

    @Test
    fun navDestinationKClass() {
        @Serializable
        class TestClass

        val destination = provider.navDestination(route = TestClass::class) { }
        assertWithMessage("NavDestination should have route set")
            .that(destination.route)
            .isEqualTo(
                "androidx.navigation.NavDestinationTest.navDestinationKClass.TestClass"
            )
        assertWithMessage("NavDestination should have id set")
            .that(destination.id)
            .isEqualTo(serializer<TestClass>().hashCode())
    }

    @Test
    fun navDestinationKClassArguments() {
        @Serializable
        @SerialName(DESTINATION_ROUTE)
        class TestClass(val arg: Int, val arg2: String = "123")

        val destination = provider.navDestination(route = TestClass::class) { }
        assertWithMessage("NavDestination should have route set")
            .that(destination.route)
            .isEqualTo(
                "$DESTINATION_ROUTE/{arg}?arg2={arg2}"
            )
        assertWithMessage("NavDestination should have id set")
            .that(destination.id)
            .isEqualTo(serializer<TestClass>().hashCode())
        assertWithMessage("NavDestination should have argument added")
            .that(destination.arguments["arg"])
            .isNotNull()
        assertWithMessage("NavArgument should have default value added")
            .that(destination.arguments["arg2"]?.isDefaultValuePresent)
            .isTrue()
    }

    @Test
    fun navDestinationDefaultArguments() {
        val destination = provider.navDestination(DESTINATION_ID) {
            argument("testArg") {
                defaultValue = "123"
                type = NavType.StringType
            }
            argument("testArg2") {
                type = NavType.StringType
            }
            argument("testArg3", NavArgument.Builder().setDefaultValue("123").build())
        }
        assertWithMessage("NavDestination should have default arguments set")
            .that(destination.arguments.get("testArg")?.defaultValue)
            .isEqualTo("123")
        assertWithMessage("NavArgument shouldn't have a default value")
            .that(destination.arguments.get("testArg2")?.isDefaultValuePresent)
            .isFalse()
        assertWithMessage("NavDestination should have implicit default arguments set")
            .that(destination.arguments.get("testArg3")?.defaultValue)
            .isEqualTo("123")
    }

    @Test
    fun navDestinationDefaultArgumentsInferred() {
        val destination = provider.navDestination(DESTINATION_ID) {
            argument("testArg") {
                defaultValue = 123
            }
        }
        assertWithMessage("NavDestination should have default arguments set")
            .that(destination.arguments.get("testArg")?.defaultValue)
            .isEqualTo(123)
    }

    @Suppress("DEPRECATION")
    @Test
    fun navDestinationAction() {
        val destination = provider.navDestination(DESTINATION_ID) {
            action(ACTION_ID) {
                destinationId = DESTINATION_ID
                navOptions {
                    popUpTo(DESTINATION_ID)
                }
                defaultArguments[ACTION_ARGUMENT_KEY] = ACTION_ARGUMENT_VALUE
            }
        }
        val action = destination.getAction(ACTION_ID)
        assertWithMessage("NavDestination should have action that was added")
            .that(action)
            .isNotNull()
        assertWithMessage("NavAction should have NavOptions set")
            .that(action?.navOptions?.popUpToId)
            .isEqualTo(DESTINATION_ID)
        assertWithMessage("NavAction should have its default argument set")
            .that(action?.defaultArguments?.getString(ACTION_ARGUMENT_KEY))
            .isEqualTo(ACTION_ARGUMENT_VALUE)
    }

    @Test
    fun navDestinationMissingRequiredArgument() {
        val expected = assertFailsWith<IllegalArgumentException> {
            provider.navDestination(DESTINATION_ROUTE) {
                argument("intArg") {
                    type = NavType.IntType
                    nullable = false
                }
            }
        }
        assertThat(expected.message).isEqualTo(
            "Deep link android-app://androidx.navigation/route can't be used to " +
                "open destination NavDestination(0xa2bd82dc).\n" +
                "Following required arguments are missing: [intArg]"
        )
    }

    @Test
    fun navDestinationRequiredArgument() {
        provider.navDestination("$DESTINATION_ROUTE/{intArg}") {
            argument("intArg") {
                type = NavType.IntType
                nullable = false
            }
        }
    }

    @Test
    fun navDestinationDefaultValuePresent() {
        val destination = provider.navDestination(DESTINATION_ID) {
            argument("arg1") {
                type = NavType.StringType
                unknownDefaultValuePresent = true
            }
            argument("arg2") {
                type = NavType.StringType
                unknownDefaultValuePresent = false
            }
        }
        val arg1 = destination.arguments["arg1"]
        assertThat(arg1?.isDefaultValuePresent).isTrue()

        val arg2 = destination.arguments["arg2"]
        assertThat(arg2?.isDefaultValuePresent).isFalse()
    }
}

private const val DESTINATION_ID = 1
private const val DESTINATION_ROUTE = "route"
private const val LABEL = "TEST"
private const val ACTION_ID = 1
private const val ACTION_ARGUMENT_KEY = "KEY"
private const val ACTION_ARGUMENT_VALUE = "VALUE"

/**
 * Instead of constructing a NavGraph from the NavigatorProvider, construct
 * a NavDestination directly to allow for testing NavDestinationBuilder in
 * isolation.
 */
@Suppress("DEPRECATION")
fun NavigatorProvider.navDestination(
    @IdRes id: Int,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
): NavDestination = NavDestinationBuilder(this[NoOpNavigator::class], id).apply(builder).build()

/**
 * Instead of constructing a NavGraph from the NavigatorProvider, construct
 * a NavDestination directly to allow for testing NavDestinationBuilder in
 * isolation.
 */
fun NavigatorProvider.navDestination(
    route: String,
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
): NavDestination =
    NavDestinationBuilder(this[NoOpNavigator::class], route = route).apply(builder).build()

/**
 * Instead of constructing a NavGraph from the NavigatorProvider, construct
 * a NavDestination directly to allow for testing NavDestinationBuilder in
 * isolation.
 */
fun NavigatorProvider.navDestination(
    route: KClass<*>,
    typeMap: Map<KType, NavType<*>> = emptyMap(),
    builder: NavDestinationBuilder<NavDestination>.() -> Unit
): NavDestination =
    NavDestinationBuilder(this[NoOpNavigator::class], route, typeMap).apply(builder).build()
