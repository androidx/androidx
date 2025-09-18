/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core.operations.layout;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.core.RemoteContext;
import androidx.compose.remote.core.operations.utilities.StringSerializer;
import androidx.compose.remote.core.serialize.Serializable;

import org.jspecify.annotations.NonNull;

/** Operations representing actions on the document */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ActionOperation extends Serializable {
    /**
     * Serialize the string
     *
     * @param indent padding to display
     * @param serializer append the string
     */
    void serializeToString(int indent, @NonNull StringSerializer serializer);

    /**
     * Run the action
     *
     * @param context remote context
     * @param document document
     * @param component component
     * @param x the x location of the action
     * @param y the y location of the action
     */
    void runAction(
            @NonNull RemoteContext context,
            @NonNull CoreDocument document,
            @NonNull Component component,
            float x,
            float y);
}
