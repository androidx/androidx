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

package androidx.compose.remote.core;

import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;

/**
 * Host-side support for editing document strings, installed on a {@link PaintContext} via {@link
 * PaintContext#setTextInputSupport(TextInputSupport)}.
 *
 * <p>A {@code TextInputLayout} asks the installed support to open a platform editor (IME)
 * over its bounds when it is clicked. The editor writes the user's edits back into the document
 * through {@link TextInputSession#setText(String)}; every binding on that string then updates
 * through the usual listener mechanism. When no support is installed the layout is inert and the
 * component simply renders its bound text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface TextInputSupport {

    /**
     * Starts editing. The host should present an editor matching {@link
     * TextInputSession#getBounds()}, configured from {@link TextInputSession#getLayout()} and
     * styled from {@link TextInputSession#getTextComponent()} when present, and call {@link
     * TextInputSession#end()} once editing is over.
     *
     * @param session the editing session
     */
    void startTextInput(@NonNull TextInputSession session);

    /**
     * Called when the bounds of the edited component changed (scroll, relayout) while the session
     * is active; the host should move its editor accordingly.
     *
     * @param session the editing session, whose {@link TextInputSession#getBounds()} are up to date
     */
    void updateTextInputBounds(@NonNull TextInputSession session);

    /**
     * Called when the document ends the session itself (for instance when the document is
     * unloaded). The host should dismiss its editor; {@link TextInputSession#end()} does not need
     * to be called.
     *
     * @param session the editing session
     */
    void stopTextInput(@NonNull TextInputSession session);
}
