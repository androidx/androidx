/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a type of form widget.
 * Ids must be kept in sync with the definitions in third_party/pdfium/public/cpp/fpdf_formfill.h.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public enum WidgetType {
    NONE(-1), UNKNOWN(0), PUSHBUTTON(1), CHECKBOX(2), RADIOBUTTON(3), COMBOBOX(4), LISTBOX(
            5), TEXTFIELD(6), SIGNATURE(7);

    /**
     * Map is preferred to SparseArray because Android classes cannot be static in Robolectric
     * tests.
     */
    @SuppressLint("UseSparseArrays")
    private static final Map<Integer, WidgetType> LOOKUP_MAP = new HashMap<>();

    static {
        for (WidgetType widgetType : WidgetType.values()) {
            LOOKUP_MAP.put(widgetType.mId, widgetType);
        }
    }

    private final int mId;

    WidgetType(int id) {
        this.mId = id;
    }

    /** Returns the WidgetType corresponding to the id. */
    @NonNull
    public static WidgetType of(int id) {
        return LOOKUP_MAP.get(id);
    }

    public int getId() {
        return mId;
    }
}
