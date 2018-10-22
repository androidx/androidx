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

package androidx.lifecycle;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class SavedStateAccessorHolder implements SavedStateRegistry.SavedStateProvider<Bundle> {
    private static final String VALUES = "values";
    private static final String KEYS = "keys";

    private SavedStateAccessor mAccessor;

    SavedStateAccessorHolder(@Nullable Bundle savedState) {
        restoreState(savedState);
    }

    private void restoreState(@Nullable Bundle savedState) {
        if (mAccessor != null) {
            return;
        }
        if (savedState == null) {
            mAccessor = new SavedStateAccessor();
            return;
        }
        ArrayList keys = savedState.getParcelableArrayList(KEYS);
        ArrayList values = savedState.getParcelableArrayList(VALUES);
        if (keys == null || values == null || keys.size() != values.size()) {
            Log.e("SavedStateAccessor", "Invalid bundle passed to the restoration phase");
            mAccessor = new SavedStateAccessor();
            return;
        }
        Map<String, Object> initialState = new HashMap<>(keys.size());
        for (int i = 0; i < keys.size(); i++) {
            initialState.put((String) keys.get(i), values.get(i));
        }
        mAccessor = new SavedStateAccessor(initialState);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public Bundle saveState() {
        Set<String> keySet = mAccessor.keys();
        ArrayList keys = new ArrayList(keySet.size());
        ArrayList value = new ArrayList(keys.size());
        for (String key : keySet) {
            keys.add(key);
            value.add(mAccessor.get(key));
        }

        Bundle res = new Bundle();
        // "parcelable" arraylists - lol
        res.putParcelableArrayList("keys", keys);
        res.putParcelableArrayList("values", value);
        return res;
    }

    SavedStateAccessor savedStateAccessor() {
        if (mAccessor == null) {
            mAccessor = new SavedStateAccessor();
        }
        return mAccessor;
    }
}
