/*
 * Copyright (C) 2025 The Android Open Source Project
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
package androidx.compose.remote.player.view.action;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.RestrictTo;

import androidx.compose.remote.core.CoreDocument;
import androidx.compose.remote.player.view.state.StateUpdater;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** An implementation of {@link CoreDocument.ActionCallback} that uses a {@link StateUpdater}. */
@RestrictTo(LIBRARY_GROUP)
public abstract class StateUpdaterActionCallback implements CoreDocument.ActionCallback {

    private final @NonNull StateUpdater mStateUpdater;
    private final @NonNull NamedActionHandler mNamedActionHandler;

    public StateUpdaterActionCallback(
            @NonNull StateUpdater stateUpdater, @NonNull NamedActionHandler namedActionHandler) {
        this.mStateUpdater = stateUpdater;
        this.mNamedActionHandler = namedActionHandler;
    }

    @Override
    public void onAction(@NonNull String name, @Nullable Object value) {
        this.mNamedActionHandler.execute(name, value, this.mStateUpdater);
    }
}
