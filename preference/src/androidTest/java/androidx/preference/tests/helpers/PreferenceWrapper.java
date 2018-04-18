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

package androidx.preference.tests.helpers;

import android.content.Context;

import androidx.preference.Preference;

import java.util.Set;

/**
 * Wrapper to allow to easily call protected methods.
 */
public final class PreferenceWrapper extends Preference {

    Object mDefaultValue;

    public PreferenceWrapper(Context context) {
        super(context);
    }

    public Object getDefaultValue() {
        return mDefaultValue;
    }

    public boolean putString(String value) {
        return persistString(value);
    }

    public String getString(String defaultValue) {
        return getPersistedString(defaultValue);
    }

    public boolean putStringSet(Set<String> values) {
        return persistStringSet(values);
    }

    public Set<String> getStringSet(Set<String> defaultValues) {
        return getPersistedStringSet(defaultValues);
    }

    public boolean putInt(int value) {
        return persistInt(value);
    }

    public int getInt(int defaultValue) {
        return getPersistedInt(defaultValue);
    }

    public boolean putLong(long value) {
        return persistLong(value);
    }

    public long getLong(long defaultValue) {
        return getPersistedLong(defaultValue);
    }

    public boolean putFloat(float value) {
        return persistFloat(value);
    }

    public float getFloat(float defaultValue) {
        return getPersistedFloat(defaultValue);
    }

    public boolean putBoolean(boolean value) {
        return persistBoolean(value);
    }

    public boolean getBoolean(boolean defaultValue) {
        return getPersistedBoolean(defaultValue);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        this.mDefaultValue = defaultValue;
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }
}
