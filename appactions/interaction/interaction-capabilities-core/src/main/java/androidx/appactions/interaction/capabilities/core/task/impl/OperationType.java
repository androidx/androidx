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

/**
 * Represents different operations possible in the Search/Update protocol.
 */
public enum OperationType {
    /** Supports adding to a field of the target object, for example adding to a list. */
    ADD("Add");

    private final String mOperationType;

    OperationType(String operationType) {
        this.mOperationType = operationType;
    }

    @Override
    public String toString() {
        return mOperationType;
    }
}
