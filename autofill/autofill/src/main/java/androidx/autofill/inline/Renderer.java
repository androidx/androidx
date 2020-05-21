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

package androidx.autofill.inline;

import android.app.PendingIntent;
import android.app.slice.Slice;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.autofill.inline.common.SlicedContent;
import androidx.autofill.inline.v1.InlineSuggestionUi;

/**
 * Renderer class responsible for rendering the inline suggestion UI.
 */
@RequiresApi(api = Build.VERSION_CODES.Q) //TODO(b/147116534): Update to R.
public final class Renderer {

    private static final String TAG = "Renderer";

    /**
     * Returns all the supported versions wrapped in a {@link Bundle}.
     */
    @NonNull
    public static Bundle getSupportedInlineUiVersionsAsBundle() {
        Bundle bundle = new Bundle();
        VersionUtils.writeSupportedVersions(bundle);
        return bundle;
    }

    /**
     * @param context the context used to render the view
     * @param content represents the UI content
     * @param styles  contains a mapping from UI version to corresponding UI style specification
     * @return a view rendered based on the provided UI content and the style with corresponding
     * version or {@code null} when the UI version indicated by the slice is either unsupported
     * by the Renderer, or not provided in the {@code styles}.
     */
    @Nullable
    public static View render(@NonNull Context context, @NonNull Slice content,
            @NonNull Bundle styles) {
        String contentVersion = SlicedContent.getVersion(content);
        if (!VersionUtils.isVersionSupported(contentVersion)) {
            Log.w(TAG, "Content version unsupported.");
            return null;
        }
        Bundle styleForSliceVersion = VersionUtils.readStyleByVersion(styles, contentVersion);
        if (styleForSliceVersion == null) {
            Log.w(TAG, "Cannot find a style with the same version as the slice.");
            return null;
        }
        switch (contentVersion) {
            case UiVersions.INLINE_UI_VERSION_1:
                InlineSuggestionUi.Style style = InlineSuggestionUi.fromBundle(
                        styleForSliceVersion);
                InlineSuggestionUi.Content contentV1 = InlineSuggestionUi.fromSlice(content);
                if (style == null || content == null) {
                    return null;
                }
                return InlineSuggestionUi.render(context, contentV1, style);
        }
        Log.w(TAG, "Renderer does not support the style/content version: " + contentVersion);
        return null;
    }

    /**
     * Returns a {@link PendingIntent} that will be launched on long clicking the UI
     * to show attribution information via a {@link android.app.Dialog}.
     *
     * <p>The attribution UI indicates to the user the source of the UI content.
     *
     * @param content the UI content which contains a {@link PendingIntent} representing the
     *                attribution information
     */
    @Nullable
    public static PendingIntent getAttributionIntent(@NonNull Slice content) {
        String contentVersion = SlicedContent.getVersion(content);
        if (!VersionUtils.isVersionSupported(contentVersion)) {
            Log.w(TAG, "Content version unsupported.");
            return null;
        }
        switch (contentVersion) {
            case UiVersions.INLINE_UI_VERSION_1:
                InlineSuggestionUi.Content contentV1 = InlineSuggestionUi.fromSlice(content);
                if (contentV1 == null) {
                    return null;
                }
                return InlineSuggestionUi.getAttributionIntent(contentV1);
        }
        Log.w(TAG, "Renderer does not support the content version: " + contentVersion);
        return null;
    }

    private Renderer() {
    }
}
