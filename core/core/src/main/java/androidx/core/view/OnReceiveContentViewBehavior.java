/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.core.view;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Interface for widgets to implement default behavior for receiving content. Content may be both
 * text and non-text (plain/styled text, HTML, images, videos, audio files, etc).
 *
 * <p>Widgets should implement this interface to define the default behavior for receiving content
 * when the SDK is <= 30. When doing so, widgets should also override
 * {@link android.view.View#onReceiveContent} for SDK > 30.
 *
 * <p>Apps wishing to provide custom behavior for receiving content should not implement this
 * interface but rather set a listener via {@link ViewCompat#setOnReceiveContentListener}. See
 * {@link ViewCompat#performReceiveContent} for more info.
 */
public interface OnReceiveContentViewBehavior {
    /**
     * Implements a view's default behavior for receiving content.
     *
     * @param payload The content to insert and related metadata.
     *
     * @return The portion of the passed-in content that was not handled (may be all, some, or none
     * of the passed-in content).
     */
    @Nullable
    ContentInfoCompat onReceiveContent(@NonNull ContentInfoCompat payload);
}
