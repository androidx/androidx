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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.createFakeSession
import androidx.xr.compose.testing.session
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeXrOwnerLocalsKtTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

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
        composeTestRule.session = createFakeSession(composeTestRule.activity)

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
    @Ignore("b/454042420 This test is failing randomly")
    fun getOrCreateXrOwnerLocals_isClearedAndRecreated_onActivityRecreation() {
        // Phase 1: Create the initial instance in the first activity.
        val activity1 = composeTestRule.activity
        val decorView1 = activity1.window.decorView
        val locals1 = assertNotNull(decorView1.getOrCreateXrOwnerLocals(activity1))
        val locals2 = assertNotNull(decorView1.getOrCreateXrOwnerLocals(activity1))
        assertThat(locals1).isSameInstanceAs(locals2)
        assertThat(locals1.session).isSameInstanceAs(locals2.session)

        // Phase 2: Recreate the activity. This destroys the old activity and its
        // lifecycle, which should trigger our observer to clear the cache.
        // Recreating the activity in this way does not work with setContent and compose.
        composeTestRule.activityRule.scenario.recreate()

        // Verify that the cache has been cleared.
        assertThat(decorView1.getTag(androidx.xr.compose.R.id.compose_xr_owner_locals)).isNull()

        // Phase 3: Verify that a new, distinct instance is created for the new activity.
        val activity2 = composeTestRule.activity

        // Check our understanding of the test infrastructure, that the activity is recreated.
        assertThat(activity1.isDestroyed).isTrue()
        assertThat(activity2).isNotSameInstanceAs(activity1)

        val decorView2 = activity2.window.decorView
        val locals3 = assertNotNull(decorView2.getOrCreateXrOwnerLocals(activity2))
        assertThat(locals3).isNotSameInstanceAs(locals1)
        assertThat(locals3.session).isNotSameInstanceAs(locals1.session)
    }
}
