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

package androidx.navigation

import androidx.navigation.serialization.expectedSafeArgsId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavOptionsTest {

    @Serializable class TestClass

    private val TEST_CLASS_ROUTE = "androidx.navigation.NavControllerRouteTest.TestClass"

    @Test
    fun launchSingleTop() {
        val navOptions = navOptions { launchSingleTop = true }
        assertWithMessage("NavOptions should have launchSingleTop set")
            .that(navOptions.shouldLaunchSingleTop())
            .isTrue()
    }

    @Test
    fun restoreState() {
        val navOptions = navOptions { restoreState = true }
        assertWithMessage("NavOptions should have restoreState set")
            .that(navOptions.shouldRestoreState())
            .isTrue()
    }

    @Test
    fun popUpTo() {
        val navOptions = navOptions { popUpTo(DESTINATION_ID) }
        assertWithMessage("NavOptions should have popUpTo destination id set")
            .that(navOptions.popUpToId)
            .isEqualTo(DESTINATION_ID)
        assertWithMessage("NavOptions should have isPopUpToInclusive false by default")
            .that(navOptions.isPopUpToInclusive())
            .isFalse()
        assertWithMessage("NavOptions should have shouldPopUpToSaveState false by default")
            .that(navOptions.shouldPopUpToSaveState())
            .isFalse()
    }

    @Test
    fun popUpToRoute() {
        val navOptions = navOptions { popUpTo(DESTINATION_ROUTE) }
        assertWithMessage("NavOptions should have popUpTo destination route set")
            .that(navOptions.popUpToRoute)
            .isEqualTo(DESTINATION_ROUTE)
        assertWithMessage("NavOptions should have isPopUpToInclusive false by default")
            .that(navOptions.isPopUpToInclusive())
            .isFalse()
    }

    @Test
    fun popUpToKClass() {
        val navOptions = navOptions { popUpTo<TestClass>() }
        assertWithMessage("NavOptions should have popUpTo destination route set")
            .that(navOptions.popUpToRouteClass)
            .isEqualTo(TestClass::class)
        assertWithMessage("NavOptions should have popUpTo destination id set")
            .that(navOptions.popUpToId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
        assertWithMessage("NavOptions should have not have popUpTo destination route set")
            .that(navOptions.popUpToRoute)
            .isEqualTo(null)
        assertWithMessage("NavOptions should have isPopUpToInclusive false by default")
            .that(navOptions.isPopUpToInclusive())
            .isFalse()
    }

    @Test
    fun popUpToObject() {
        val popObj = TestClass()
        val navOptions = navOptions { popUpTo(popObj) }
        assertWithMessage("NavOptions should have popUpTo destination id set")
            .that(navOptions.popUpToId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
        assertWithMessage("NavOptions should have popUpTo destination route set")
            .that(navOptions.popUpToRouteObject)
            .isEqualTo(popObj)
        assertWithMessage("NavOptions should have isPopUpToInclusive false by default")
            .that(navOptions.isPopUpToInclusive())
            .isFalse()
    }

    @Test
    fun popUpToBuilder() {
        val navOptions = navOptions {
            popUpTo(DESTINATION_ID) {
                inclusive = true
                saveState = true
            }
        }
        assertWithMessage("NavOptions should have popUpTo destination id set")
            .that(navOptions.popUpToId)
            .isEqualTo(DESTINATION_ID)
        assertWithMessage("NavOptions should have isPopUpToInclusive set")
            .that(navOptions.isPopUpToInclusive())
            .isTrue()
        assertWithMessage("NavOptions should have shouldPopUpToSaveState set")
            .that(navOptions.shouldPopUpToSaveState())
            .isTrue()
    }

    @Test
    fun popUpToKClassBuilder() {
        val navOptions = navOptions {
            popUpTo<TestClass> {
                inclusive = true
                saveState = true
            }
        }
        assertWithMessage("NavOptions should have popUpTo destination class set")
            .that(navOptions.popUpToRouteClass)
            .isEqualTo(TestClass::class)
        assertWithMessage("NavOptions should have popUpTo destination id set")
            .that(navOptions.popUpToId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
        assertWithMessage("NavOptions should have not have popUpTo destination route set")
            .that(navOptions.popUpToRoute)
            .isEqualTo(null)
        assertWithMessage("NavOptions should have isPopUpToInclusive set")
            .that(navOptions.isPopUpToInclusive())
            .isTrue()
        assertWithMessage("NavOptions should have shouldPopUpToSaveState set")
            .that(navOptions.shouldPopUpToSaveState())
            .isTrue()
    }

    @Test
    fun popUpToObjectBuilder() {
        val popObject = TestClass()
        val navOptions = navOptions {
            popUpTo(popObject) {
                inclusive = true
                saveState = true
            }
        }
        assertWithMessage("NavOptions should have popUpTo destination id set")
            .that(navOptions.popUpToId)
            .isEqualTo(serializer<TestClass>().expectedSafeArgsId())
        assertWithMessage("NavOptions should have popUpTo destination class set")
            .that(navOptions.popUpToRouteObject)
            .isEqualTo(popObject)
        assertWithMessage("NavOptions should have isPopUpToInclusive set")
            .that(navOptions.isPopUpToInclusive())
            .isTrue()
        assertWithMessage("NavOptions should have shouldPopUpToSaveState set")
            .that(navOptions.shouldPopUpToSaveState())
            .isTrue()
    }

    @Test
    fun popUpToRouteInclusive() {
        val navOptions = navOptions { popUpTo(DESTINATION_ROUTE) { inclusive = true } }
        assertWithMessage("NavOptions should have popUpTo destination id set")
            .that(navOptions.popUpToRoute)
            .isEqualTo(DESTINATION_ROUTE)
        assertWithMessage("NavOptions should have isPopUpToInclusive set")
            .that(navOptions.isPopUpToInclusive())
            .isTrue()
    }

    @Test
    fun anim() {
        val navOptions = navOptions {
            anim {
                enter = ENTER_ANIM_ID
                exit = EXIT_ANIM_ID
                popEnter = POP_ENTER_ANIM_ID
                popExit = POP_EXIT_ANIM_ID
            }
        }
        assertWithMessage("NavOptions should have enter animation set")
            .that(navOptions.enterAnim)
            .isEqualTo(ENTER_ANIM_ID)
        assertWithMessage("NavOptions should have exit animation set")
            .that(navOptions.exitAnim)
            .isEqualTo(EXIT_ANIM_ID)
        assertWithMessage("NavOptions should have pop enter animation set")
            .that(navOptions.popEnterAnim)
            .isEqualTo(POP_ENTER_ANIM_ID)
        assertWithMessage("NavOptions should have pop exit animation set")
            .that(navOptions.popExitAnim)
            .isEqualTo(POP_EXIT_ANIM_ID)
    }
}

private const val DESTINATION_ID = 1
private const val DESTINATION_ROUTE = "destination_route"

private const val ENTER_ANIM_ID = 10
private const val EXIT_ANIM_ID = 11
private const val POP_ENTER_ANIM_ID = 12
private const val POP_EXIT_ANIM_ID = 13
