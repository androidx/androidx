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

package androidx.recyclerview.selection;

import static androidx.core.util.Preconditions.checkArgument;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;

/**
 * Strategy for storing keys in saved state. Extend this class when using custom
 * key types that aren't supported by default. Prefer use of builtin storage strategies:
 * {@link #createStringStorage()}, {@link #createLongStorage()},
 * {@link #createParcelableStorage(Class)}.
 *
 * <p>
 * See
 * {@link androidx.recyclerview.selection.SelectionTracker.Builder SelectionTracker.Builder}
 * for more detailed advice on which key type to use for your selection keys.
 *
 * @param <K> Selection key type. Built in support is provided for String, Long, and Parcelable
 *           types. Use the respective factory method to create a StorageStrategy instance
 *           appropriate to the desired type.
 *           {@link #createStringStorage()},
 *           {@link #createParcelableStorage(Class)},
 *           {@link #createLongStorage()}
 */
public abstract class StorageStrategy<K> {

    @VisibleForTesting
    static final String SELECTION_ENTRIES = "androidx.recyclerview.selection.entries";

    @VisibleForTesting
    static final String SELECTION_KEY_TYPE = "androidx.recyclerview.selection.type";

    private final Class<K> mType;

    /**
     * Creates a new instance.
     *
     * @param type the key type class that is being used.
     */
    public StorageStrategy(@NonNull Class<K> type) {
        checkArgument(type != null);
        mType = type;
    }

    /**
     * Create a {@link Selection} from supplied {@link Bundle}.
     *
     * @param state Bundle instance that may contain parceled Selection instance.
     * @return
     */
    public abstract @Nullable Selection<K> asSelection(@NonNull Bundle state);

    /**
     * Creates a {@link Bundle} from supplied {@link Selection}.
     *
     * @param selection The selection to asBundle.
     * @return
     */
    public abstract @NonNull Bundle asBundle(@NonNull Selection<K> selection);

    String getKeyTypeName() {
        return mType.getCanonicalName();
    }

    /**
     * @return StorageStrategy suitable for use with {@link Parcelable} keys
     * (like {@link android.net.Uri}).
     */
    public static <K extends Parcelable> StorageStrategy<K> createParcelableStorage(Class<K> type) {
        return new ParcelableStorageStrategy<>(type);
    }

    /**
     * @return StorageStrategy suitable for use with {@link String} keys.
     */
    public static StorageStrategy<String> createStringStorage() {
        return new StringStorageStrategy();
    }

    /**
     * @return StorageStrategy suitable for use with {@link Long} keys.
     */
    public static StorageStrategy<Long> createLongStorage() {
        return new LongStorageStrategy();
    }

    private static class StringStorageStrategy extends StorageStrategy<String> {

        StringStorageStrategy() {
            super(String.class);
        }

        @Override
        public @Nullable Selection<String> asSelection(@NonNull Bundle state) {

            String keyType = state.getString(SELECTION_KEY_TYPE, null);
            if (keyType == null || !keyType.equals(getKeyTypeName())) {
                return null;
            }

            @Nullable ArrayList<String> stored = state.getStringArrayList(SELECTION_ENTRIES);
            if (stored == null) {
                return null;
            }

            Selection<String> selection = new Selection<>();
            selection.mSelection.addAll(stored);
            return selection;
        }

        @Override
        public @NonNull Bundle asBundle(@NonNull Selection<String> selection) {

            Bundle bundle = new Bundle();

            bundle.putString(SELECTION_KEY_TYPE, getKeyTypeName());

            ArrayList<String> value = new ArrayList<>(selection.size());
            value.addAll(selection.mSelection);
            bundle.putStringArrayList(SELECTION_ENTRIES, value);

            return bundle;
        }
    }

    private static class LongStorageStrategy extends StorageStrategy<Long> {

        LongStorageStrategy() {
            super(Long.class);
        }

        @Override
        public @Nullable Selection<Long> asSelection(@NonNull Bundle state) {
            String keyType = state.getString(SELECTION_KEY_TYPE, null);
            if (keyType == null || !keyType.equals(getKeyTypeName())) {
                return null;
            }

            @Nullable long[] stored = state.getLongArray(SELECTION_ENTRIES);
            if (stored == null) {
                return null;
            }

            Selection<Long> selection = new Selection<>();
            for (long key : stored) {
                selection.mSelection.add(key);
            }
            return selection;
        }

        @Override
        public @NonNull Bundle asBundle(@NonNull Selection<Long> selection) {

            Bundle bundle = new Bundle();
            bundle.putString(SELECTION_KEY_TYPE, getKeyTypeName());

            long[] value = new long[selection.size()];
            int i = 0;
            for (Long key : selection) {
                value[i++] = key;
            }
            bundle.putLongArray(SELECTION_ENTRIES, value);

            return bundle;
        }
    }

    private static class ParcelableStorageStrategy<K extends Parcelable>
            extends StorageStrategy<K> {

        ParcelableStorageStrategy(Class<K> type) {
            super(type);
            checkArgument(Parcelable.class.isAssignableFrom(type));
        }

        @Override
        public @Nullable Selection<K> asSelection(@NonNull Bundle state) {

            String keyType = state.getString(SELECTION_KEY_TYPE, null);
            if (keyType == null || !keyType.equals(getKeyTypeName())) {
                return null;
            }

            @Nullable ArrayList<K> stored = state.getParcelableArrayList(SELECTION_ENTRIES);
            if (stored == null) {
                return null;
            }

            Selection<K> selection = new Selection<>();
            selection.mSelection.addAll(stored);
            return selection;
        }

        @Override
        public @NonNull Bundle asBundle(@NonNull Selection<K> selection) {

            Bundle bundle = new Bundle();
            bundle.putString(SELECTION_KEY_TYPE, getKeyTypeName());

            ArrayList<K> value = new ArrayList<>(selection.size());
            value.addAll(selection.mSelection);
            bundle.putParcelableArrayList(SELECTION_ENTRIES, value);

            return bundle;
        }
    }
}
