/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.core.platform;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.graphics.Canvas;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.CustomContext;
import androidx.compose.remote.core.RemoteContext;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Platform-specific support manager delegating Native Android View instantiations,
 */
@RestrictTo(LIBRARY_GROUP)
public interface AndroidCustomContext extends CustomContext {

    /**
     * Sets the active Android Context for view instantiation.
     * @param context the context to use
     */
    void setContext(@NonNull Context context);

    /**
     * Sets the active Android Canvas for custom view drawing.
     * @param canvas the canvas to use
     */
    void setCanvas(@Nullable Canvas canvas);

    /**
     * Sets the active RemoteContext.
     */
    void setRemoteContext(@Nullable RemoteContext remoteContext);
}
