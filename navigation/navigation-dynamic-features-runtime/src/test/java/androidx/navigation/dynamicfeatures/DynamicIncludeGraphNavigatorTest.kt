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
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@RunWith(JUnit4::class)
@SmallTest
class DynamicIncludeGraphNavigatorTest {

    private lateinit var provider: NavigatorProvider
    private lateinit var navigator: DynamicIncludeGraphNavigator
    private lateinit var dynamicNavGraph: DynamicIncludeGraphNavigator.DynamicIncludeNavGraph
    private lateinit var noOpNavigator: NoOpNavigator
    private lateinit var installManager: TestDynamicInstallManager
    private lateinit var inflater: NavInflater
    private lateinit var context: Context

    @Before
    fun setup() {
        context = mock(Context::class.java)
        `when`(context.packageName).thenReturn(PACKAGE_NAME)
        provider = NavigatorProvider()
        noOpNavigator = NoOpNavigator()
        installManager = TestDynamicInstallManager()
        inflater = NavInflater(context, provider)

        navigator = DynamicIncludeGraphNavigator(
            context,
            provider,
            inflater,
            installManager
        )
        provider.addNavigator(noOpNavigator)
        dynamicNavGraph = navigator.createDestination()
        dynamicNavGraph.moduleName = FEATURE_NAME
    }

    @Test
    fun testCreateDestination() {
        assertNotNull(navigator.createDestination())
    }

    @Test
    fun testReplacePackagePlaceholder() {
        assertThat(
            dynamicNavGraph.getPackageOrDefault(context, "\${applicationId}.something" +
                    ".$FEATURE_NAME")
        ).isEqualTo("$PACKAGE_NAME.something.$FEATURE_NAME")

        assertThat(
            dynamicNavGraph.getPackageOrDefault(context, "something.\${applicationId}" +
                    ".$FEATURE_NAME")
        ).isEqualTo("something.$PACKAGE_NAME.$FEATURE_NAME")

        assertThat(
            dynamicNavGraph.getPackageOrDefault(context, null)
        ).isEqualTo("$PACKAGE_NAME.$FEATURE_NAME")
    }
}

private const val PACKAGE_NAME = "com.test.app"
private const val FEATURE_NAME = "myfeature"
