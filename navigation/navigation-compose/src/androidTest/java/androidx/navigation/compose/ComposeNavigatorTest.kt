/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.navigation.compose

import androidx.navigation.navOptions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ComposeNavigatorTest {

    @Test
    fun testNavigateConfigChangeThenPop() {
        val navigator = ComposeNavigator()
        val destination = navigator.createDestination()
        destination.id = FIRST_DESTINATION_ID

        assertThat(navigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        destination.id = SECOND_DESTINATION_ID
        assertThat(navigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        val savedState = navigator.onSaveState()!!
        val restoredNavigator = ComposeNavigator()

        restoredNavigator.onRestoreState(savedState)

        assertWithMessage("ComposeNavigator should return true when popping the second destination")
            .that(navigator.popBackStack())
            .isTrue()
    }

    @Test
    fun testNavigateWithPopUpToThenPop() {
        val navigator = ComposeNavigator()
        val destination = navigator.createDestination()
        destination.id = FIRST_DESTINATION_ID

        assertThat(navigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        destination.id = SECOND_DESTINATION_ID
        assertThat(navigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        assertWithMessage("ComposeNavigator should return true when popping the third destination")
            .that(navigator.popBackStack())
            .isTrue()
        destination.id = THIRD_DESTINATION_ID
        assertThat(
            navigator.navigate(
                destination, null,
                navOptions { popUpTo(FIRST_DESTINATION_ID) { inclusive = false } }, null
            )
        ).isEqualTo(destination)

        assertWithMessage("ComposeNavigator should return true when popping the third destination")
            .that(navigator.popBackStack())
            .isTrue()
    }

    @Test
    fun testSingleTopInitial() {
        val navigator = ComposeNavigator()
        val destination = navigator.createDestination()
        destination.id = FIRST_DESTINATION_ID

        navigator.navigate(destination, null, null, null)

        assertThat(
            navigator.navigate(
                destination, null,
                navOptions { launchSingleTop = true }, null
            )
        ).isNull()
    }

    @Test
    fun testSingleTop() {
        val navigator = ComposeNavigator()
        val destination = navigator.createDestination()
        destination.id = FIRST_DESTINATION_ID

        assertThat(navigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        destination.id = SECOND_DESTINATION_ID
        assertThat(navigator.navigate(destination, null, null, null))
            .isEqualTo(destination)

        assertThat(
            navigator.navigate(
                destination, null,
                navOptions { launchSingleTop = true }, null
            )
        ).isNull()

        destination.id = FIRST_DESTINATION_ID
        assertThat(
            navigator.navigate(
                destination, null,
                navOptions { launchSingleTop = true }, null
            )
        ).isEqualTo(destination)

        assertWithMessage("ComposeNavigator should return true when popping the first destination")
            .that(navigator.popBackStack())
            .isTrue()
    }
}

private const val FIRST_DESTINATION_ID = 1
private const val SECOND_DESTINATION_ID = 2
private const val THIRD_DESTINATION_ID = 3
