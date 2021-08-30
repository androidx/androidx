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

package androidx.navigation.dynamicfeatures

import androidx.navigation.NavDestination
import androidx.navigation.NavigatorProvider
import androidx.navigation.dynamicfeatures.shared.TestDynamicInstallManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
public class DynamicGraphNavigatorTest {

    private val navigator =
        DynamicGraphNavigator(
            mock(NavigatorProvider::class.java),
            TestDynamicInstallManager()
        )

    @Test
    public fun testCreateDestination() {
        assertThat(navigator.createDestination()).isNotNull()
    }

    @Test
    public fun testInstallDefaultProgressDestination() {
        val navDestination = mock(NavDestination::class.java)
        navigator.installDefaultProgressDestination { navDestination }
        assertThat(navigator.defaultProgressDestinationSupplier!!.invoke())
            .isSameInstanceAs(navDestination)
    }
}
