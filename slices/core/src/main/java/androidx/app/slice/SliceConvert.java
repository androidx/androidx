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
package androidx.app.slice;


import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_COLOR;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;

/**
 * Convert between {@link androidx.app.slice.Slice} and {@link android.app.slice.Slice}
 */
@RequiresApi(28)
public class SliceConvert {

    /**
     * Convert {@link androidx.app.slice.Slice} to {@link android.app.slice.Slice}
     */
    public static android.app.slice.Slice unwrap(androidx.app.slice.Slice slice) {
        android.app.slice.Slice.Builder builder = new android.app.slice.Slice.Builder(
                slice.getUri());
        builder.addHints(slice.getHints());
        builder.setSpec(unwrap(slice.getSpec()));
        for (androidx.app.slice.SliceItem item : slice.getItems()) {
            switch (item.getFormat()) {
                case FORMAT_SLICE:
                    builder.addSubSlice(unwrap(item.getSlice()), item.getSubType());
                    break;
                case FORMAT_IMAGE:
                    builder.addIcon(item.getIcon(), item.getSubType(), item.getHints());
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
                case FORMAT_COLOR:
                    builder.addColor(item.getColor(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_TIMESTAMP:
                    builder.addTimestamp(item.getTimestamp(), item.getSubType(), item.getHints());
                    break;
            }
        }
        return builder.build();
    }

    private static android.app.slice.SliceSpec unwrap(androidx.app.slice.SliceSpec spec) {
        if (spec == null) return null;
        return new android.app.slice.SliceSpec(spec.getType(), spec.getRevision());
    }

    static List<android.app.slice.SliceSpec> unwrap(
            List<androidx.app.slice.SliceSpec> supportedSpecs) {
        List<android.app.slice.SliceSpec> ret = new ArrayList<>();
        for (androidx.app.slice.SliceSpec spec : supportedSpecs) {
            ret.add(unwrap(spec));
        }
        return ret;
    }

    /**
     * Convert {@link android.app.slice.Slice} to {@link androidx.app.slice.Slice}
     */
    public static androidx.app.slice.Slice wrap(android.app.slice.Slice slice) {
        androidx.app.slice.Slice.Builder builder = new androidx.app.slice.Slice.Builder(
                slice.getUri());
        builder.addHints(slice.getHints());
        builder.setSpec(wrap(slice.getSpec()));
        for (android.app.slice.SliceItem item : slice.getItems()) {
            switch (item.getFormat()) {
                case FORMAT_SLICE:
                    builder.addSubSlice(wrap(item.getSlice()), item.getSubType());
                    break;
                case FORMAT_IMAGE:
                    builder.addIcon(item.getIcon(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_REMOTE_INPUT:
                    builder.addRemoteInput(item.getRemoteInput(), item.getSubType(),
                            item.getHints());
                    break;
                case FORMAT_ACTION:
                    builder.addAction(item.getAction(), wrap(item.getSlice()), item.getSubType());
                    break;
                case FORMAT_TEXT:
                    builder.addText(item.getText(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_COLOR:
                    builder.addColor(item.getColor(), item.getSubType(), item.getHints());
                    break;
                case FORMAT_TIMESTAMP:
                    builder.addTimestamp(item.getTimestamp(), item.getSubType(), item.getHints());
                    break;
            }
        }
        return builder.build();
    }

    private static androidx.app.slice.SliceSpec wrap(android.app.slice.SliceSpec spec) {
        if (spec == null) return null;
        return new androidx.app.slice.SliceSpec(spec.getType(), spec.getRevision());
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static List<androidx.app.slice.SliceSpec> wrap(
            List<android.app.slice.SliceSpec> supportedSpecs) {
        List<androidx.app.slice.SliceSpec> ret = new ArrayList<>();
        for (android.app.slice.SliceSpec spec : supportedSpecs) {
            ret.add(wrap(spec));
        }
        return ret;
    }
}
