/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.truth

import androidx.navigation.NavController
import androidx.navigation.plusAssign
import androidx.navigation.truth.test.R
import androidx.navigation.truth.NavControllerSubject.Companion.assertThat
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import androidx.testutils.TestNavigator
import androidx.testutils.assertThrows
import org.junit.Before
import org.junit.Test

@SmallTest
class NavControllerSubjectTest {
    private lateinit var navController: NavController

    @UiThreadTest
    @Before
    fun setUp() {
        navController = NavController(ApplicationProvider.getApplicationContext()).apply {
            navigatorProvider += TestNavigator()
            setGraph(R.navigation.test_graph)
        }
    }

    @Test
    fun testIsCurrentDestination() {
        assertThat(navController).isCurrentDestination(R.id.start_test)
    }

    @Test
    fun testIsCurrentDestinationFailure() {
        with(
            assertThrows {
                assertThat(navController).isCurrentDestination(R.id.second_test)
            }
        ) {
            factValue("expected id")
                .isEqualTo("0x${R.id.second_test.toString(16)}")
            factValue("but was")
                .isEqualTo("0x${navController.currentDestination?.id?.toString(16)}")
            factValue("current destination is")
                .isEqualTo(navController.currentDestination.toString())
        }
    }

    @Test
    fun testIsGraph() {
        assertThat(navController).isGraph(R.id.test_graph)
    }

    @Test
    fun testIsGraphFailure() {
        with(
            assertThrows {
                assertThat(navController).isGraph(R.id.second_test_graph)
            }
        ) {
            factValue("expected id")
                .isEqualTo("0x${R.id.second_test_graph.toString(16)}")
            factValue("but was")
                .isEqualTo("0x${navController.graph.id.toString(16)}")
            factValue("current graph is")
                .isEqualTo(navController.graph.toString())
        }
    }
}
