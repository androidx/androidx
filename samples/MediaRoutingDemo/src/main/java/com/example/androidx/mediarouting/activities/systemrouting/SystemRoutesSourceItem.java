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

package com.example.androidx.mediarouting.activities.systemrouting;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * A model that holds data about a system routes source.
 */
public final class SystemRoutesSourceItem implements SystemRoutesAdapterItem {

    @NonNull private final String mName;

    public SystemRoutesSourceItem(@NonNull String name) {
        mName = name;
    }

    @NonNull
    public String getSourceName() {
        return mName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemRoutesSourceItem that = (SystemRoutesSourceItem) o;
        return TextUtils.equals(mName, that.mName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName);
    }
}
