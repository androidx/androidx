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

package androidx.appactions.interaction.capabilities.core.entity

import androidx.appactions.interaction.capabilities.core.values.Thing

/**
 * EntityProvider could provide candidates for assistant's search actions.
 *
 * <p>Use abstract classes within the library to create instances of the {@link EntityProvider}.
 */
abstract class EntityProvider<T : Thing> {
    /**
     * Unique identifier for this EntityFilter. Must match the shortcuts.xml declaration, which allows
     * different filters to be assigned to types on a per-BII basis.
     */
    abstract fun getId(): String

    /**
     * Executes the entity lookup.
     *
     * @param request The request includes e.g. entity, search metadata, etc.
     */
    abstract fun lookup(request: EntityLookupRequest<T>): EntityLookupResponse<T>
}