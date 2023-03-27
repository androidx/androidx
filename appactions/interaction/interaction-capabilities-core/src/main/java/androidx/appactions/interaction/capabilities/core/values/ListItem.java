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

package androidx.appactions.interaction.capabilities.core.values;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;

import com.google.auto.value.AutoValue;

/** Represents a single item in an item list. */
@AutoValue
public abstract class ListItem extends Thing {

    /** Creates a ListItem given its name. */
    @NonNull
    public static ListItem create(@NonNull String id, @NonNull String name) {
        return newBuilder().setId(id).setName(name).build();
    }

    /** Create a new ListItem.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_ListItem.Builder();
    }

    /** Builder class for building item lists with items. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<ListItem> {
    }
}
