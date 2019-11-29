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

package androidx.ui.test

import androidx.ui.core.SemanticsTreeProvider
import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.engine.geometry.Rect
import androidx.ui.test.android.AndroidSemanticsTreeInteraction

/**
 * Provides abstraction for writing queries, actions and asserts over Compose semantics tree using
 * extension functions. This class is expected to have Android and host side specific
 * implementations.
 */
// TODO(jellefresen): Convert to interface, no need for an abstract class
internal abstract class SemanticsTreeInteraction {

    internal abstract fun findAllMatching(): List<SemanticsNodeInteraction>

    internal abstract fun findOne(): SemanticsNodeInteraction

    internal abstract fun performAction(action: (SemanticsTreeProvider) -> Unit)

    internal abstract fun sendInput(action: (InputDispatcher) -> Unit)

    internal abstract fun contains(semanticsConfiguration: SemanticsConfiguration): Boolean

    internal abstract fun isInScreenBounds(rectangle: Rect): Boolean

    internal abstract fun waitForIdleCompose(): Boolean
}

internal var semanticsTreeInteractionFactory: (
    selector: SemanticsConfiguration.() -> Boolean
) -> SemanticsTreeInteraction = {
        selector ->
    AndroidSemanticsTreeInteraction(throwOnRecomposeTimeout, selector)
}

/**
 * This allow internal tests to enable a mode in which [RecomposeTimeOutException] is thrown
 * in case we exceed timeout when waiting for recomposition. The reason why this is not turned
 * on by default is that if developers have animations or any other actions that run infinitely
 * we would always throw exception. The assumption here is that instead of breaking them we
 * just wait for longer timeouts as the tests might still have a value.
 */
// TODO(pavlis): Turn this on by default for all our Compose tests
internal var throwOnRecomposeTimeout = false
