package androidx.wear.protolayout.renderer;

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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * View provider for a View ExtensionLayoutElement. This should check that the given renderer
 * extension ID matches the expected renderer extension ID, then return a View based on the given
 * payload. The returned View will be measured using the width/height from the {@link
 * androidx.wear.protolayout.LayoutElementBuilders.ExtensionLayoutElement} message.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface ProtoLayoutExtensionViewProvider {
    /**
     * Return an Android View from the given renderer extension. In case of an error, this method
     * should return null, and not throw any exceptions.
     *
     * <p>Note: The renderer extension must not set the default tag of the returned View object.
     */
    @Nullable
    View provideView(@NonNull byte[] extensionPayload, @NonNull String vendorId);
}
