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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Represents an ItemList object. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class ItemList extends Thing {

    /** Create a new ItemList.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_ItemList.Builder();
    }

    /** Returns the optional list items in this ItemList. */
    @NonNull
    public abstract List<ListItem> getListItems();

    /** Builder class for building item lists with items. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder>
            implements BuilderOf<ItemList> {

        private final List<ListItem> mListItemsToBuild = new ArrayList<>();

        /** Add one or more ListItem to the ItemList to be built. */
        @NonNull
        public final Builder addListItem(@NonNull ListItem... listItems) {
            Collections.addAll(mListItemsToBuild, listItems);
            return this;
        }

        /** Add a list of ListItem to the ItemList to be built. */
        @NonNull
        public final Builder addAllListItems(@NonNull List<ListItem> listItems) {
            return addListItem(listItems.toArray(ListItem[]::new));
        }

        abstract Builder setListItems(List<ListItem> listItems);

        /** Builds and returns the ItemList. */
        @Override
        @NonNull
        public final ItemList build() {
            setListItems(Collections.unmodifiableList(mListItemsToBuild));
            return autoBuild();
        }

        @NonNull
        abstract ItemList autoBuild();
    }
}
