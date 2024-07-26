/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.appwidget.testing.unit

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.testing.test.R
import androidx.glance.layout.Column
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
/** Holds tests that use Robolectric for providing application resources and context. */
class GlanceAppWidgetUnitTestEnvironmentRobolectricTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Ignore // b/355680002
    @Test
    fun runTest_localContextRead() = runGlanceAppWidgetUnitTest {
        setContext(context)

        provideComposable { ComposableReadingLocalContext() }

        onNode(hasTestTag("test-tag")).assert(hasText("Test string: MyTest"))
    }

    @Composable
    fun ComposableReadingLocalContext() {
        val context = LocalContext.current

        Column {
            Text(
                text = "Test string: ${context.getString(R.string.glance_test_string)}",
                modifier = GlanceModifier.semantics { testTag = "test-tag" }
            )
        }
    }
}
