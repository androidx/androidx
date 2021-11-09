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

package androidx.core.google.shortcuts.builders;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.core.google.shortcuts.builders.Constants.SHORTCUT_CAPABILITY_KEY;
import static androidx.core.google.shortcuts.builders.Constants.SHORTCUT_DESCRIPTION_KEY;
import static androidx.core.google.shortcuts.builders.Constants.SHORTCUT_LABEL_KEY;
import static androidx.core.google.shortcuts.builders.Constants.SHORTCUT_TYPE;
import static androidx.core.google.shortcuts.builders.Constants.SHORTCUT_URL_KEY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.google.firebase.appindexing.builders.IndexableBuilder;

/**
 * Builder for the Shortcut Corpus.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class ShortcutBuilder extends IndexableBuilder<ShortcutBuilder> {
    public ShortcutBuilder() {
        super(SHORTCUT_TYPE);
    }

    /** Sets the label for the shortcut. */
    @NonNull
    public ShortcutBuilder setShortcutLabel(@NonNull String shortcutLabel) {
        setName(shortcutLabel);
        return put(SHORTCUT_LABEL_KEY, shortcutLabel);
    }

    /** Sets the description for the shortcut. */
    @NonNull
    public ShortcutBuilder setShortcutDescription(@NonNull String shortcutDescription) {
        setDescription(shortcutDescription);
        return put(SHORTCUT_DESCRIPTION_KEY, shortcutDescription);
    }

    /** Sets the {@link android.content.Intent} url for the shortcut. */
    @NonNull
    public ShortcutBuilder setShortcutUrl(@NonNull String shortcutUrl) {
        return put(SHORTCUT_URL_KEY, shortcutUrl);
    }

    /** Sets one or more capabilities for the shortcut. */
    @NonNull
    public ShortcutBuilder setCapability(@NonNull CapabilityBuilder... capability) {
        return put(SHORTCUT_CAPABILITY_KEY, capability);
    }
}
