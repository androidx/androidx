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

package androidx.xr.arcore.testing

import androidx.xr.arcore.testing.internal.FakeRuntimeConversationState
import androidx.xr.runtime.PreviewSpatialApi

/** A test utility for manipulating Conversation Scene Signals in the mocked AR environment. */
@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP)
@PreviewSpatialApi
public class ConversationSceneSignalTester
internal constructor(
    private val arCoreTestRule: ArCoreTestRule,
    private val fakeConversationSceneSignal: FakeRuntimeConversationState,
) {

    /** The simulated confidence level of the conversation detection. Ranges from 0.0 to 1.0. */
    public var confidence: Float
        get() = fakeConversationSceneSignal.confidence
        set(value) {
            fakeConversationSceneSignal.confidence = value
        }

    /**
     * The simulated conversation type integer (e.g., 0 for NOT_DETECTED, 1 for VIRTUAL, 2 for
     * IN_PERSON).
     */
    public var type: Int
        get() = fakeConversationSceneSignal.type
        set(value) {
            fakeConversationSceneSignal.type = value
        }
}
