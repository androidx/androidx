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

import android.content.Context;
import android.view.View;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Common interface for component-specific delegates in AndroidCustomSupport.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AndroidComponentSupport {
    /**
     * Creates the concrete View instance.
     */
    @NonNull View createView(@NonNull Context context);

    /**
     * Configures a String property on the target View.
     */
    void configure(@NonNull View view, int type, @NonNull String value);

    /**
     * Configures an integer property on the target View.
     */
    void configure(@NonNull View view, int type, int value);

    /**
     * Configures a float property on the target View.
     */
    void configure(@NonNull View view, int type, float value);
}
