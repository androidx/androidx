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

import androidx.annotation.RestrictTo;

/**
 * Constants for the Shortcut Corpus.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class Constants {
    public static final String SHORTCUT_TYPE = "Shortcut";
    public static final String CAPABILITY_TYPE = "Capability";
    public static final String PARAMETER_TYPE = "Parameter";

    public static final String SHORTCUT_LABEL_KEY = "shortcutLabel";
    public static final String SHORTCUT_DESCRIPTION_KEY = "shortcutDescription";
    public static final String SHORTCUT_URL_KEY = "shortcutUrl";
    public static final String SHORTCUT_CAPABILITY_KEY = "capability";

    public static final String CAPABILITY_PARAMETER_KEY = "parameter";

    public static final String PARAMETER_VALUE_KEY = "value";

    private Constants() {}
}
