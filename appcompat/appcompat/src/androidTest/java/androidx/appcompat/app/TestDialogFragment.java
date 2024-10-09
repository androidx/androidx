/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.appcompat.app;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Test class extending DialogFragment used for testing of configuration changes like nightMode and
 * locales.
 */
public class TestDialogFragment extends DialogFragment {

    public TestDialogFragment() {
        // Public empty constructor used to handle lifecycle events.
    }

    public static TestDialogFragment newInstance() {
        return new TestDialogFragment();
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("TestDialogFragment");
        builder.setMessage("TestDialogFragment");
        return builder.create();
    }
}
