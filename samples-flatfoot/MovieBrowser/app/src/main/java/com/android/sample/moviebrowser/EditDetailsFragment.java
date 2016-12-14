/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.sample.moviebrowser;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * Edit details fragment.
 */
public class EditDetailsFragment extends DialogFragment {
    public static final String FULL = "edit.FULL";
    public static final String KEY_RUNTIME = "edit.runtime";
    public static final String KEY_RATED = "edit.rated";

    public EditDetailsFragment() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final MovieDataFull initialData = getArguments().getParcelable(FULL);
        String initialRuntime = (savedInstanceState != null)
                ? savedInstanceState.getString(KEY_RUNTIME)
                : initialData.Runtime;
        String initialRated = (savedInstanceState != null)
                ? savedInstanceState.getString(KEY_RATED)
                : initialData.Rated;

        AlertDialog.Builder editBuilder = new AlertDialog.Builder(getContext())
                .setTitle(initialData.Title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent edited = new Intent();
                        edited.putExtra(KEY_RUNTIME, getCurrentRuntime());
                        edited.putExtra(KEY_RATED, getCurrentRated());
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_OK, edited);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_CANCELED, null);
                    }
                });

        LayoutInflater inflater = LayoutInflater.from(getContext());
        ViewGroup editor = (ViewGroup) inflater.inflate(R.layout.edit_details, null, false);
        ((EditText) editor.findViewById(R.id.runtime)).setText(initialRuntime);
        ((EditText) editor.findViewById(R.id.rated)).setText(initialRated);
        editBuilder.setView(editor);

        return editBuilder.create();
    }

    private String getCurrentRuntime() {
        EditText runtime = (EditText) getDialog().findViewById(R.id.runtime);
        return runtime.getText().toString();
    }

    private String getCurrentRated() {
        EditText rated = (EditText) getDialog().findViewById(R.id.rated);
        return rated.getText().toString();
    }
}
