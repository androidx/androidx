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

package androidx.appactions.interaction.capabilities.core;

import androidx.annotation.NonNull;

import java.util.Optional;

/**
 * <b>Do not implement this interface yourself.</b>
 *
 * <p>An ActionCapability represents some supported App Action that can be given to App Control.
 *
 * <p>Use helper classes provided by the capability library to get instances of this interface.
 */
public interface ActionCapability {

    /** Returns the unique Id of this capability declaration. */
    @NonNull
    Optional<String> getId();
}
