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

package androidx.appactions.interaction.capabilities.core.impl;

import androidx.annotation.RestrictTo;

/** A class to define exceptions that are reported from dialog capability API. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum ErrorStatusInternal {
    CANCELLED(0),
    TIMEOUT(1),
    INVALID_REQUEST(2),
    UNCHANGED_DISAMBIG_STATE(3),
    INVALID_RESOLVER(4),
    STRUCT_CONVERSION_FAILURE(5),
    // TODO(b/276354491): remove SYNC / CONFIRM / TOUCH_EVENT failure codes
    SYNC_REQUEST_FAILURE(6),
    CONFIRMATION_REQUEST_FAILURE(7),
    TOUCH_EVENT_REQUEST_FAILURE(8),
    EXTERNAL_EXCEPTION(9),
    SESSION_ALREADY_DESTROYED(10);

    private final int mCode;

    ErrorStatusInternal(int code) {
        this.mCode = code;
    }

    public int getCode() {
        return mCode;
    }
}
