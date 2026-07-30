/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigationevent.compose

import android.content.ContextWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.kruth.assertThat
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import androidx.navigationevent.testing.TestNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
internal class LocalNavigationEventDispatcherOwnerTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun localNavigationEventDispatcherOwner_whenNotProvided_fallsBackToContext() {
        val testOwner = TestNavigationEventDispatcherOwner()
        var wrappedContext: ContextWrapper?
        lateinit var resolvedOwner: NavigationEventDispatcherOwner

        rule.setContent {
            val baseContext = LocalContext.current
            // Simulates environments where the owner is implemented by the Activity context.
            wrappedContext =
                object : ContextWrapper(baseContext), NavigationEventDispatcherOwner {
                    override val navigationEventDispatcher = testOwner.navigationEventDispatcher
                }
            CompositionLocalProvider(LocalContext provides wrappedContext) {
                resolvedOwner = LocalNavigationEventDispatcherOwner.current!!
            }
        }

        assertThat(resolvedOwner.navigationEventDispatcher)
            .isEqualTo(testOwner.navigationEventDispatcher)
    }

    @Test
    fun localNavigationEventDispatcherOwner_whenNotProvided_fallsBackToViewTree() {
        val testOwner = TestNavigationEventDispatcherOwner()
        lateinit var resolvedOwner: NavigationEventDispatcherOwner

        rule.setContent {
            val view = LocalView.current
            // Simulates environments where the owner is set on the ViewTree.
            view.setViewTreeNavigationEventDispatcherOwner(testOwner)
            resolvedOwner = LocalNavigationEventDispatcherOwner.current!!
        }

        assertThat(resolvedOwner.navigationEventDispatcher)
            .isEqualTo(testOwner.navigationEventDispatcher)
    }
}
