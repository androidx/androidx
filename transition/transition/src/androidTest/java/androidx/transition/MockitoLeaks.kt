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

package androidx.transition

import shark.AndroidReferenceMatchers

object MockitoLeaks {
    // Mockito store stubs in a ThreadLocal and doesn't have tooling to clear this state.
    // Many of transition's tests use mocking, which can trigger leak detection in later tests.
    // https://github.com/mockito/mockito/issues/177
    val OngoingStubbing =
        AndroidReferenceMatchers.instanceFieldLeak(
            className = "org.mockito.internal.progress.MockingProgressImpl",
            fieldName = "ongoingStubbing",
            description = "Mockito leaks to a ThreadLocal",
        )
}
