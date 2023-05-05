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

package androidx.appactions.interaction.capabilities.core.impl.task.exceptions;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.InvalidRequestException;

/**
 * Represents an internal issue with the state sync between the SDK and Assistant. One example is
 * when the SDK places an argument in dismabig state, but then Assistant sends the same argument
 * data again without any grounding.
 */
public final class DisambigStateException extends InvalidRequestException {

    public DisambigStateException(@NonNull String message) {
        super(message);
    }
}
