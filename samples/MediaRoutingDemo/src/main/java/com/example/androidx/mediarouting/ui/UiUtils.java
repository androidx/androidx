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
package com.example.androidx.mediarouting.ui;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

/** Contains utility methods related to UI management. */
public final class UiUtils {

    /**
     * Populates the given {@link Spinner} using an {@link Enum} and its possible values.
     *
     * @param context The context in which the spinner is to be inflated.
     * @param spinner The {@link Spinner} to populate.
     * @param anEnum The initially selected value.
     * @param selectionConsumer A consumer to invoke when an element is selected.
     */
    public static void setUpEnumBasedSpinner(
            @NonNull Context context,
            @NonNull Spinner spinner,
            @NonNull Enum<?> anEnum,
            @NonNull Consumer<Enum<?>> selectionConsumer) {
        Enum<?>[] enumValues = anEnum.getDeclaringClass().getEnumConstants();
        ArrayAdapter<Enum<?>> adapter =
                new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, enumValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(anEnum.ordinal());

        spinner.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> adapterView, View view, int i, long l) {
                        selectionConsumer.accept(anEnum.getDeclaringClass().getEnumConstants()[i]);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {}
                });
    }

    private UiUtils() {
        // Prevent instantiation.
    }
}
