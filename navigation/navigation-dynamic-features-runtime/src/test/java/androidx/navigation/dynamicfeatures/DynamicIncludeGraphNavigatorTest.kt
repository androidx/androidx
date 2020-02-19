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

import android.content.Context
import androidx.navigation.NavInflater
import androidx.navigation.NavigatorProvider
import androidx.navigation.NoOpNavigator
import androidx.navigation.dynamicfeatures.shared.TestDynamicInstallManager
import androidx.test.filters.SmallTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.spy

@RunWith(JUnit4::class)
@SmallTest
class DynamicIncludeGraphNavigatorTest {

    private lateinit var provider: NavigatorProvider
    private lateinit var navigator: DynamicIncludeGraphNavigator
    private lateinit var dynamicNavGraph: DynamicIncludeGraphNavigator.DynamicIncludeNavGraph
    private lateinit var noOpNavigator: NoOpNavigator
    private lateinit var installManager: TestDynamicInstallManager
    private lateinit var inflater: NavInflater

    @Before
    fun setup() {
        val contextSpy = spy(Context::class.java)
        provider = NavigatorProvider()
        noOpNavigator = NoOpNavigator()
        installManager = TestDynamicInstallManager()
        inflater = NavInflater(contextSpy, provider)

        navigator = DynamicIncludeGraphNavigator(
            contextSpy,
            provider,
            inflater,
            installManager
        )
        provider.addNavigator(noOpNavigator)
        dynamicNavGraph = navigator.createDestination()
    }

    @Test
    fun testCreateDestination() {
        assertNotNull(navigator.createDestination())
    }
}
