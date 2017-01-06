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
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;

import com.android.sample.moviebrowser.model.MovieDataFullModel;
import com.android.support.lifecycle.LifecycleRegistry;
import com.android.support.lifecycle.LifecycleRegistryProvider;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

/**
 * Edit details fragment.
 */
public class EditDetailsFragment extends DialogFragment implements LifecycleRegistryProvider {
    public static final String IMDB_ID = "edit.imdbId";
    public static final String KEY_RUNTIME = "edit.runtime";
    public static final String KEY_RATED = "edit.rated";

    private LifecycleRegistry mLifecycleRegistry = new LifecycleRegistry(this);

    public EditDetailsFragment() {
    }

    @Override
    public LifecycleRegistry getLifecycle() {
        return mLifecycleRegistry;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String imdbId = getArguments().getString(IMDB_ID);

        // Configure the dialog to pass the data back when "OK" button is clicked
        AlertDialog.Builder editBuilder = new AlertDialog.Builder(getContext())
                .setTitle("Edit details")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent edited = new Intent();
                        edited.putExtra(KEY_RUNTIME, getCurrentRuntime());
                        edited.putExtra(KEY_RATED, getCurrentRated());
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_OK, edited);                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getTargetFragment().onActivityResult(getTargetRequestCode(),
                                Activity.RESULT_CANCELED, null);
                    }
                });

        // Inflate the main editor area and set it as custom view on the dialog
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final ViewGroup editor = (ViewGroup) inflater.inflate(R.layout.edit_details, null, false);
        editBuilder.setView(editor);
        final AlertDialog result = editBuilder.create();

        // Get our view model instance and register ourselves to observe change to the
        // movie data. When a change is reported, update all UI elements based on the new
        // data.
        final MovieDataFullModel movieDataFullModel = ViewModelStore.get(this,
                imdbId + ".full", MovieDataFullModel.class);
        movieDataFullModel.getMovieData().observe(this, new Observer<MovieDataFull>() {
            @Override
            public void onChanged(@Nullable MovieDataFull movieDataFull) {
                if (movieDataFull != null) {
                    android.util.Log.e("MovieBrowser", "Got data for editing from model");
                    getDialog().setTitle(movieDataFull.Title);
                    showEditableFields(editor, movieDataFull.Runtime, movieDataFull.Rated);
                }
            }
        });

        // Get the data from our view model
        final MovieDataFull fullData = movieDataFullModel.getMovieData().getValue();
        if (fullData == null) {
            // Ask the model to load the data for this movie. When the data is loaded, we will be
            // notified since this fragment registered itself as an observer on the matching
            // live data object.
            movieDataFullModel.setImdbId(getContext(), imdbId);
        } else {
            android.util.Log.e("MovieBrowser", "Got data for editing from saved state");
            result.setTitle(fullData.Title);
            showEditableFields(editor, fullData.Runtime, fullData.Rated);
        }

        return result;
    }

    private void showEditableFields(ViewGroup editor, String runtime, String rated) {
        ((EditText) editor.findViewById(R.id.runtime)).setText(runtime);
        ((EditText) editor.findViewById(R.id.rated)).setText(rated);
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
