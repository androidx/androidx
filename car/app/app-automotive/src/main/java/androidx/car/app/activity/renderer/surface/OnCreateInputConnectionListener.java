/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity.renderer.surface;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.view.inputmethod.EditorInfo;

import androidx.annotation.RestrictTo;
import androidx.car.app.activity.renderer.IProxyInputConnection;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Proxies the {@link android.view.View#onCreateInputConnection} method invocation events from
 * {@link TemplateSurfaceView} to the host renderer.
 *
 */
@RestrictTo(LIBRARY)
public interface OnCreateInputConnectionListener {
    /**
     * Creates a proxy to a remote {@link android.view.inputmethod.InputConnection}.
     *
     * @param editorInfo the {@link EditorInfo} for which the input connection should be created
     * @return an {@link IProxyInputConnection} through which communication to the
     * remote {@code InputConnection} should occur
     */
    @Nullable IProxyInputConnection onCreateInputConnection(@NonNull EditorInfo editorInfo);
}
