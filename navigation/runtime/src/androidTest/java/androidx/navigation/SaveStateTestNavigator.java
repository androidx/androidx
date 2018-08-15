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

package androidx.navigation;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.testing.TestNavigator;

/**
 * {@code TestNavigator} that helps with testing saving and restoring state.
 */
@Navigator.Name("test")
public class SaveStateTestNavigator extends TestNavigator {

    private static final String STATE_SAVED_COUNT = "saved_count";

    public int mSaveStateCount = 0;

    @Nullable
    @Override
    public Bundle onSaveState() {
        mSaveStateCount += 1;
        Bundle state = new Bundle();
        state.putInt(STATE_SAVED_COUNT, mSaveStateCount);
        return state;
    }

    @Override
    public void onRestoreState(@NonNull Bundle savedState) {
        mSaveStateCount = savedState.getInt(STATE_SAVED_COUNT);
    }

}
