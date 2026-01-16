/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.platform

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeXrOwnerLocalsKtTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun composeXrOwnerLocals_nonActivityContext_returnsNull() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalContext provides ApplicationProvider.getApplicationContext()
            ) {
                assertThat(LocalComposeXrOwners.current).isNull()
            }
        }
    }

    @Test
    fun composeXrOwnerLocals_activityContext_returnsNonNull() {
        composeTestRule.setContent { assertThat(LocalComposeXrOwners.current).isNotNull() }
    }

    @Test
    fun composeXrOwnerLocals_sessionCannotBeCreated_returnsNull() {
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory,
            { null },
        )

        composeTestRule.setContent { assertThat(LocalComposeXrOwners.current).isNull() }
    }

    @Test
    fun composeXrOwnerLocals_sessionThrowsException_returnsNull() {
        composeTestRule.activity.window.decorView.setTag(
            androidx.xr.compose.R.id.compose_xr_session_factory,
            { throw IllegalStateException() },
        )

        composeTestRule.setContent { assertThat(LocalComposeXrOwners.current).isNull() }
    }

    @Test
    fun getOrCreateXrOwnerLocals_isClearedAndRecreated_onActivityRecreation() {
        lateinit var decorView1: android.view.View
        lateinit var locals1: ComposeXrOwnerLocals
        lateinit var activity1: SubspaceTestingActivity

        ActivityScenario.launch(SubspaceTestingActivity::class.java).use { scenario ->
            // Phase 1: Create the initial instance in the first activity.
            scenario.onActivity { activity ->
                activity1 = activity
                decorView1 = activity.window.decorView
                locals1 = assertNotNull(activity.getOrCreateXrOwnerLocals())
                val locals2 = assertNotNull(activity.getOrCreateXrOwnerLocals())
                assertThat(locals1).isSameInstanceAs(locals2)
                assertThat(locals1.session).isSameInstanceAs(locals2.session)
            }

            // Phase 2: Recreate the activity. This destroys the old activity and its
            // lifecycle, which should trigger our observer to clear the cache.
            scenario.recreate()

            // Verify that the cache has been cleared.
            assertThat(decorView1.getTag(androidx.xr.compose.R.id.compose_xr_owner_locals)).isNull()

            // Phase 3: Verify that a new, distinct instance is created for the new activity.
            scenario.onActivity { activity2 ->
                // Check our understanding of the test infrastructure, that the activity is
                // recreated.
                assertThat(activity1.isDestroyed).isTrue()
                assertThat(activity2).isNotSameInstanceAs(activity1)

                val locals3 = assertNotNull(activity2.getOrCreateXrOwnerLocals())
                assertThat(locals3).isNotSameInstanceAs(locals1)
                assertThat(locals3.session).isNotSameInstanceAs(locals1.session)
            }
        }
    }
}
