/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.LocaleList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.os.LocaleListCompat;
import androidx.core.util.Preconditions;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Provides utils to convert between platform and support library objects.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
final class ConvertUtils {

    private ConvertUtils() {
    }

    @Nullable
    @RequiresApi(24)
    static LocaleList unwrapLocalListCompat(@Nullable LocaleListCompat localeListCompat) {
        if (localeListCompat == null) {
            return null;
        }
        return (LocaleList) localeListCompat.unwrap();
    }

    @Nullable
    @RequiresApi(24)
    static LocaleListCompat wrapLocalList(@Nullable LocaleList localeList) {
        if (localeList == null) {
            return null;
        }
        return LocaleListCompat.wrap(localeList);
    }

    @Nullable
    @RequiresApi(28)
    static android.view.textclassifier.TextClassifier.EntityConfig toPlatformEntityConfig(
            @Nullable TextClassifier.EntityConfig entityConfig) {
        if (entityConfig == null) {
            return null;
        }
        return entityConfig.toPlatform();
    }

    @Nullable
    @RequiresApi(26)
    static ZonedDateTime createZonedDateTimeFromUtc(Long timeInMs) {
        if (timeInMs == null) {
            return null;
        }
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeInMs), ZoneOffset.UTC);
    }

    @NonNull
    @RequiresApi(28)
    @SuppressLint("RestrictedApi")
    static Map<String, Float> createFloatMapFromTextLinks(
            @NonNull android.view.textclassifier.TextLinks.TextLink textLink) {
        Preconditions.checkNotNull(textLink);

        final int entityCount = textLink.getEntityCount();
        Map<String, Float> floatMap = new ArrayMap<>();
        for (int i = 0; i < entityCount; i++) {
            String entity = textLink.getEntity(i);
            floatMap.put(entity, textLink.getConfidenceScore(entity));
        }
        return floatMap;
    }

    @NonNull
    @RequiresApi(26)
    public static IconCompat createIconFromDrawable(@NonNull Drawable d) {
        if (d instanceof BitmapDrawable) {
            return IconCompat.createWithBitmap(((BitmapDrawable) d).getBitmap());
        }
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        return IconCompat.createWithBitmap(b);
    }

    @RequiresApi(28)
    @Nullable
    public static Long zonedDateTimeToUtcMs(@Nullable ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
