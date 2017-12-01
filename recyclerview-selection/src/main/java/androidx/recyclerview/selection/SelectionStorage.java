/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.recyclerview.selection;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;
import static android.support.v4.util.Preconditions.checkArgument;

import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Set;

/**
 * Helper class binding SelectionHelper and Activity lifecycle events facilitating
 * persistence of selection across activity lifecycle events.
 *
 * <p>Usage:<br><pre>
 void onCreate() {
    mLifecycleHelper = new SelectionStorage<>(SelectionStorage.TYPE_STRING, mSelectionHelper);
    if (savedInstanceState != null) {
        mSelectionStorage.onRestoreInstanceState(savedInstanceState);
    }
 }
 protected void onSaveInstanceState(Bundle outState) {
     super.onSaveInstanceState(outState);
     mSelectionStorage.onSaveInstanceState(outState);
 }
 </pre>
 * @param <K> Selection key type. Usually String or Long.
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public final class SelectionStorage<K> {

    @VisibleForTesting
    static final String EXTRA_SAVED_SELECTION_TYPE = "androidx.recyclerview.selection.type";

    @VisibleForTesting
    static final String EXTRA_SAVED_SELECTION_ENTRIES = "androidx.recyclerview.selection.entries";

    public static final int TYPE_STRING = 0;
    public static final int TYPE_LONG = 1;
    @IntDef({
            TYPE_STRING,
            TYPE_LONG
    })
    @Retention(RetentionPolicy.SOURCE)
    @interface KeyType {}

    private final @KeyType int mKeyType;
    private final SelectionHelper<K> mHelper;

    /**
     * Creates a new lifecycle helper. {@code keyType}.
     *
     * @param keyType
     * @param helper
     */
    public SelectionStorage(@KeyType int keyType, SelectionHelper<K> helper) {
        checkArgument(
                keyType == TYPE_STRING || keyType == TYPE_LONG,
                "Only String and Integer presistence are supported by default.");
        checkArgument(helper != null);

        mKeyType = keyType;
        mHelper = helper;
    }

    /**
     * Preserves selection, if any.
     *
     * @param state
     */
    @SuppressWarnings("unchecked")
    public void onSaveInstanceState(Bundle state) {
        MutableSelection<K> sel = new MutableSelection<>();
        mHelper.copySelection(sel);

        state.putInt(EXTRA_SAVED_SELECTION_TYPE, mKeyType);
        switch (mKeyType) {
            case TYPE_STRING:
                writeStringSelection(state, ((Selection<String>) sel).mSelection);
                break;
            case TYPE_LONG:
                writeLongSelection(state, ((Selection<Long>) sel).mSelection);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported key type: " + mKeyType);
        }
    }

    /**
     * Restores selection from previously saved state.
     *
     * @param state
     */
    public void onRestoreInstanceState(@Nullable Bundle state) {
        if (state == null) {
            return;
        }

        int keyType = state.getInt(EXTRA_SAVED_SELECTION_TYPE, -1);
        switch(keyType) {
            case TYPE_STRING:
                Selection<String> stringSel = readStringSelection(state);
                if (stringSel != null && !stringSel.isEmpty()) {
                    mHelper.restoreSelection(stringSel);
                }
                break;
            case TYPE_LONG:
                Selection<Long> longSel = readLongSelection(state);
                if (longSel != null && !longSel.isEmpty()) {
                    mHelper.restoreSelection(longSel);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported selection key type.");
        }
    }

    private @Nullable Selection<String> readStringSelection(Bundle state) {
        @Nullable ArrayList<String> stored =
                state.getStringArrayList(EXTRA_SAVED_SELECTION_ENTRIES);
        if (stored == null) {
            return null;
        }

        Selection<String> selection = new Selection<>();
        selection.mSelection.addAll(stored);
        return selection;
    }

    private @Nullable Selection<Long> readLongSelection(Bundle state) {
        @Nullable long[] stored = state.getLongArray(EXTRA_SAVED_SELECTION_ENTRIES);
        if (stored == null) {
            return null;
        }

        Selection<Long> selection = new Selection<>();
        for (long key : stored) {
            selection.mSelection.add(key);
        }
        return selection;
    }

    private void writeStringSelection(Bundle state, Set<String> selected) {
        ArrayList<String> value = new ArrayList<>(selected.size());
        value.addAll(selected);
        state.putStringArrayList(EXTRA_SAVED_SELECTION_ENTRIES, value);
    }

    private void writeLongSelection(Bundle state, Set<Long> selected) {
        long[] value = new long[selected.size()];
        int i = 0;
        for (Long key : selected) {
            value[i++] = key;
        }
        state.putLongArray(EXTRA_SAVED_SELECTION_ENTRIES, value);
    }
}
