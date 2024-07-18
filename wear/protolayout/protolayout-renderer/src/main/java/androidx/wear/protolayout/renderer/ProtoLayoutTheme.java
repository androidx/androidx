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

package androidx.wear.protolayout.renderer;

import android.content.res.Resources.Theme;
import android.graphics.Typeface;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;

/** Theme customization for ProtoLayout texts, which includes Font types and variants. */
@RestrictTo(Scope.LIBRARY)
public interface ProtoLayoutTheme {

    /** Holder for different weights of the same font variant. */
    interface FontSet {

        @NonNull
        Typeface getNormalFont();

        @NonNull
        Typeface getMediumFont();

        @NonNull
        Typeface getBoldFont();
    }

    /**
     * Gets the FontSet for a given font variant.
     *
     * @param fontVariant the numeric value of the proto enum {@link
     *     androidx.wear.protolayout.proto.LayoutElementProto.FontVariant}.
     */
    @NonNull
    FontSet getFontSet(int fontVariant);

    /** Gets an Android Theme object styled with TextAppearance attributes. */
    @NonNull
    Theme getTheme();

    /**
     * Gets an Attribute resource Id for a fallback TextAppearance. The resource with this id should
     * be present in the Android Theme returned by {@link ProtoLayoutTheme#getTheme()}.
     */
    @AttrRes
    int getFallbackTextAppearanceResId();
}
