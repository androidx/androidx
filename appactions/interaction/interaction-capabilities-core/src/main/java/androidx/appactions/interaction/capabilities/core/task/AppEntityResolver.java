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
import androidx.appactions.interaction.capabilities.core.values.SearchAction;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Similar to ValueListener, but also need to handle grounding of ungrounded values.
 *
 * @param <T>
 */
public interface AppEntityResolver<T> extends ValueListener<T> {
    /**
     * Given a search criteria, looks up the inventory during runtime, renders the search result
     * within the app's own UI and then returns it to the Assistant so that the task can be kept in
     * sync with the app UI.
     */
    @NonNull
    ListenableFuture<EntitySearchResult<T>> lookupAndRender(@NonNull SearchAction<T> searchAction);
}
