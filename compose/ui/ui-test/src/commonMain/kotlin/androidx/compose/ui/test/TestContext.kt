/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.test

import androidx.collection.mutableIntObjectMapOf
import androidx.compose.ui.node.RootForTest

/**
 * Provides storage of test related entities that must be accessible by anything other than
 * [ComposeUiTest] and friends, for example the [InputDispatcher] or the implementation of some
 * assertions and actions.
 */
class TestContext internal constructor(internal val testOwner: TestOwner) {

    /**
     * Stores the [InputDispatcherState] of each [RootForTest]. The state will be restored in an
     * [InputDispatcher] when it is created for an owner that has a state stored. To avoid leaking
     * the [RootForTest], the [identityHashCode] of the root is used as the key instead of the
     * actual object.
     */
    internal val states = mutableIntObjectMapOf<InputDispatcherState>()
}
