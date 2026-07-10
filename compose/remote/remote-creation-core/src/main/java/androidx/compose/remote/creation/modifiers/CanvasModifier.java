/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.modifiers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Canvas modifier, allows to inject canvas operations */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CanvasModifier implements RecordingModifier.Element {
    private final CanvasCallback mCallback;

    /**
     * Callback for the canvas ops
     */
    public interface CanvasCallback {
        /**
         * callback to draw operations
         * @param writer
         */
        void onDraw(@NonNull RemoteComposeWriter writer);
    }

    public CanvasModifier(@NonNull CanvasCallback callback) {
        mCallback = callback;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        writer.startCanvasOperations();
        mCallback.onDraw(writer);
        writer.endCanvasOperations();
    }
}
