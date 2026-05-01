/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.testing

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that allows you to set a Remote Compose content without the necessity to provide a
 * host for the content. The host, such as an Activity, will be created by the test rule.
 *
 * The [CoreDocument] player implementation should be provided, giving the flexibility for
 * developers to choose their own implementation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBaseDocContentTestRule : TestRule {
    /** [ComposeContentTestRule] used by this [TestRule]. */
    public val composeTestRule: ComposeContentTestRule = createComposeRule(StandardTestDispatcher())

    override fun apply(base: Statement, description: Description): Statement =
        composeTestRule.apply(base, description)

    /**
     * Sets the given [CoreDocument] as a content of the current screen.
     *
     * @param coreDocument The [CoreDocument] to play.
     */
    public fun setContent(coreDocument: CoreDocument, player: Player, size: Size) {
        composeTestRule.setContent { player.Play(coreDocument = coreDocument, size = size) }
    }

    public interface Player {
        @Composable public fun Play(coreDocument: CoreDocument, size: Size)
    }
}
