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

package androidx.appcompat.resources;

import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Unified ApiXXImpls for appcompat-resources.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class Compatibility {
    private Compatibility() {
        // This class is not instantiable.
    }

    @RequiresApi(21)
    public static class Api21Impl {
        private Api21Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        public static void inflate(@NonNull Drawable drawable, @NonNull Resources r,
                @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
                @Nullable Resources.Theme theme) throws IOException, XmlPullParserException {
            drawable.inflate(r, parser, attrs, theme);
        }

        @DoNotInline
        public static int getChangingConfigurations(@NonNull TypedArray typedArray) {
            return typedArray.getChangingConfigurations();
        }

        @NonNull
        @DoNotInline
        public static Drawable createFromXmlInner(@NonNull Resources r,
                @NonNull XmlPullParser parser, @NonNull AttributeSet attrs,
                @Nullable Resources.Theme theme) throws IOException, XmlPullParserException {
            return Drawable.createFromXmlInner(r, parser, attrs, theme);
        }
    }

    @RequiresApi(18)
    public static class Api18Impl {
        private Api18Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        public static void setAutoCancel(@NonNull ObjectAnimator objectAnimator, boolean cancel) {
            objectAnimator.setAutoCancel(cancel);
        }
    }

    @RequiresApi(15)
    public static class Api15Impl {
        private Api15Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        public static void getValueForDensity(@NonNull Resources resources, int id, int density,
                @NonNull TypedValue outValue, boolean resolveRefs) {
            resources.getValueForDensity(id, density, outValue, resolveRefs);
        }
    }
}
