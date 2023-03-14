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

package androidx.appactions.interaction.capabilities.core.task.impl;

import com.google.auto.value.AutoOneOf;

/** Contains either an AssistantUpdateRequest or a TouchEventUpdateRequest */
@AutoOneOf(UpdateRequest.Kind.class)
abstract class UpdateRequest {
    static UpdateRequest of(AssistantUpdateRequest request) {
        return AutoOneOf_UpdateRequest.assistant(request);
    }

    static UpdateRequest of(TouchEventUpdateRequest request) {
        return AutoOneOf_UpdateRequest.touchEvent(request);
    }

    abstract Kind getKind();

    abstract AssistantUpdateRequest assistant();

    abstract TouchEventUpdateRequest touchEvent();

    enum Kind {
        ASSISTANT,
        TOUCH_EVENT
    }
}
