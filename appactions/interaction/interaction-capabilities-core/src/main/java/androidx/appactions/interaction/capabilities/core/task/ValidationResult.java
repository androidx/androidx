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

package androidx.appactions.interaction.capabilities.core.task;

import androidx.annotation.NonNull;

import com.google.auto.value.AutoOneOf;

/** Result from validating a single argument value. */
@AutoOneOf(ValidationResult.Kind.class)
public abstract class ValidationResult {
    /** Creates a new ACCEPTED ValidationResult. */
    @NonNull
    public static ValidationResult newAccepted() {
        return AutoOneOf_ValidationResult.accepted("accepted");
    }

    /** Creates a new REJECTED ValidationResult. */
    @NonNull
    public static ValidationResult newRejected() {
        return AutoOneOf_ValidationResult.rejected("rejected");
    }

    @NonNull
    public abstract Kind getKind();

    abstract String accepted();

    abstract String rejected();

    /** The state of the argument value after performing validation. */
    public enum Kind {
        ACCEPTED,
        REJECTED,
    }
}
