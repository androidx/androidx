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

import android.app.slice.Slice;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringDef;
import androidx.autofill.inline.v1.InlineSuggestionUi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Defines the inline suggestion UI version constants.
 */
@RequiresApi(api = Build.VERSION_CODES.Q) //TODO(b/147116534): Update to R.
public final class UiVersions {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @StringDef({INLINE_UI_VERSION_1})
    @Retention(RetentionPolicy.SOURCE)
    public @interface InlineUiVersion {
    }

    /**
     * The ID for the version 1 implementation of the inline UI library.
     *
     * @see InlineSuggestionUi
     */
    public static final String INLINE_UI_VERSION_1 = "androidx.autofill.inline.ui.version:v1";

    /**
     * Versions supported by the current library.
     */
    private static final Set<String> UI_VERSIONS =
            new HashSet<String>(Arrays.asList(INLINE_UI_VERSION_1));

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    public static Set<String> getUiVersions() {
        return UI_VERSIONS;
    }

    /**
     * The {@code versionedBundle} is expected to be generated either by
     * {@link StylesBuilder#build()} or {@link Renderer#getSupportedInlineUiVersionsAsBundle()}.
     *
     * @param versionedBundle the bundle that encodes the versions information.
     * @return the list of versions that are both specified in the {@code versionedBundle} and
     * supported by the current library.
     */
    @NonNull
    public static List<String> getVersions(@NonNull Bundle versionedBundle) {
        return VersionUtils.getSupportedVersions(versionedBundle);
    }

    private UiVersions() {
    }

    /**
     * Represents the UI content that can be converted into a {@link Slice} to be read
     * to render a UI in a remote process.
     */
    public interface Content {

        /**
         * Returns the content represented as a {@link Slice} so it can be transported through IPC.
         */
        @NonNull
        Slice getSlice();
    }

    /**
     * Represents a UI style specification that contains the version information and can be
     * converted into a Bundle to be read to render a UI in a remote process.
     */
    public interface Style {
        /**
         * Returns the style represented as a {@link Bundle} so it can be transported through IPC.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        Bundle getBundle();

        /**
         * Returns the {@link InlineUiVersion} the style corresponds to.
         *
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        @InlineUiVersion
        String getVersion();
    }

    /**
     * Returns a new styles builder.
     */
    @NonNull
    public static StylesBuilder newStylesBuilder() {
        return new StylesBuilder();
    }

    /**
     * A builder responsible for providing a {@link Bundle} with encoded UI style
     * specifications for one or more versions of UI templates.
     */
    public static final class StylesBuilder {

        private final List<Style> mStyles;

        /**
         * Use {@link UiVersions#newStylesBuilder()} to instantiate this class.
         */
        StylesBuilder() {
            mStyles = new ArrayList<>();
        }

        /**
         * Adds a UI style.
         *
         * @param style the style being added
         * @throws IllegalArgumentException if the style version is not supported by the library
         */
        @NonNull
        public StylesBuilder addStyle(@NonNull Style style) {
            if (!VersionUtils.isVersionSupported(style.getVersion())) {
                throw new IllegalArgumentException(
                        "Unsupported style version: " + style.getVersion());
            }
            mStyles.add(style);
            return this;
        }

        /**
         * Returns the styles for one or more UI versions represented as a {@link Bundle} so it
         * can be transported through IPC.
         *
         * @throws IllegalStateException if no style has been put in the builder
         */
        @NonNull
        public Bundle build() {
            if (mStyles.isEmpty()) {
                throw new IllegalStateException("Please put at least one style in the builder");
            }
            Bundle bundle = new Bundle();
            VersionUtils.writeStylesToBundle(mStyles, bundle);
            return bundle;
        }
    }
}
