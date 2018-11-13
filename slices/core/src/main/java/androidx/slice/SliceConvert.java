/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.slice;


import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_LONG;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArraySet;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Set;

/**
 * Convert between {@link androidx.slice.Slice androidx.slice.Slice} and
 * {@link android.app.slice.Slice android.app.slice.Slice}
 */
@RequiresApi(28)
public class SliceConvert {

    private static final String TAG = "SliceConvert";

    /**
     * Convert {@link androidx.slice.Slice androidx.slice.Slice} to
     * {@link android.app.slice.Slice android.app.slice.Slice}
     */
    public static android.app.slice.Slice unwrap(androidx.slice.Slice slice) {
        if (slice == null || slice.getUri() == null) return null;
        android.app.slice.Slice.Builder builder = new android.app.slice.Slice.Builder(
                slice.getUri(), unwrap(slice.getSpec()));
        builder.addHints(slice.getHints());
        for (androidx.slice.SliceItem item : slice.getItemArray()) {
            switch (item.getFormat()) {
                case FORMAT_SLICE:
                    builder.addSubSlice(unwrap(item.getSlice()), item.getSubType());
                    break;
                case FORMAT_IMAGE:
                    builder.addIcon(item.getIcon().toIcon(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_REMOTE_INPUT:
                    builder.addRemoteInput(item.getRemoteInput(), item.getSubType(),
                            item.getHints());
                    break;
                case FORMAT_ACTION:
                    builder.addAction(item.getAction(), unwrap(item.getSlice()), item.getSubType());
                    break;
                case FORMAT_TEXT:
                    builder.addText(item.getText(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_INT:
                    builder.addInt(item.getInt(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_LONG:
                    builder.addLong(item.getLong(), item.getSubType(), item.getHints());
                    break;
            }
        }
        return builder.build();
    }

    private static android.app.slice.SliceSpec unwrap(androidx.slice.SliceSpec spec) {
        if (spec == null) return null;
        return new android.app.slice.SliceSpec(spec.getType(), spec.getRevision());
    }

    static Set<android.app.slice.SliceSpec> unwrap(
            Set<androidx.slice.SliceSpec> supportedSpecs) {
        Set<android.app.slice.SliceSpec> ret = new ArraySet<>();
        if (supportedSpecs != null) {
            for (androidx.slice.SliceSpec spec : supportedSpecs) {
                ret.add(unwrap(spec));
            }
        }
        return ret;
    }

    /**
     * Convert {@link android.app.slice.Slice android.app.slice.Slice} to
     * {@link androidx.slice.Slice androidx.slice.Slice}
     */
    public static androidx.slice.Slice wrap(android.app.slice.Slice slice, Context context) {
        if (slice == null || slice.getUri() == null) return null;
        androidx.slice.Slice.Builder builder = new androidx.slice.Slice.Builder(
                slice.getUri());
        builder.addHints(slice.getHints());
        builder.setSpec(wrap(slice.getSpec()));
        for (android.app.slice.SliceItem item : slice.getItems()) {
            switch (item.getFormat()) {
                case FORMAT_SLICE:
                    builder.addSubSlice(wrap(item.getSlice(), context), item.getSubType());
                    break;
                case FORMAT_IMAGE:
                    try {
                        builder.addIcon(IconCompat.createFromIcon(context, item.getIcon()),
                                item.getSubType(), item.getHints());
                    } catch (IllegalArgumentException e) {
                        Log.w(TAG, "The icon resource isn't available.", e);
                    } catch (Resources.NotFoundException e) {
                        Log.w(TAG, "The icon resource isn't available.", e);
                    }
                    break;
                case FORMAT_REMOTE_INPUT:
                    builder.addRemoteInput(item.getRemoteInput(), item.getSubType(),
                            item.getHints());
                    break;
                case FORMAT_ACTION:
                    builder.addAction(item.getAction(), wrap(item.getSlice(), context),
                            item.getSubType());
                    break;
                case FORMAT_TEXT:
                    builder.addText(item.getText(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_INT:
                    builder.addInt(item.getInt(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_LONG:
                    builder.addLong(item.getLong(), item.getSubType(), item.getHints());
                    break;
            }
        }
        return builder.build();
    }

    private static androidx.slice.SliceSpec wrap(android.app.slice.SliceSpec spec) {
        if (spec == null) return null;
        return new androidx.slice.SliceSpec(spec.getType(), spec.getRevision());
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Set<androidx.slice.SliceSpec> wrap(
            Set<android.app.slice.SliceSpec> supportedSpecs) {
        Set<androidx.slice.SliceSpec> ret = new ArraySet<>();
        if (supportedSpecs != null) {
            for (android.app.slice.SliceSpec spec : supportedSpecs) {
                ret.add(wrap(spec));
            }
        }
        return ret;
    }

    private SliceConvert() {
    }
}
