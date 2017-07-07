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

package com.android.flatfoot.apireviewdemo.exercise;

import android.arch.lifecycle.LifecycleActivity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.flatfoot.apireviewdemo.R;

public class NoteActivity extends LifecycleActivity {

    private TextView mNoteLabelView;
    private TextView mNoteBodyView;
    private ProgressBar mProgressBar;

    private void showNote(Note note) {
        mNoteLabelView.setText(note.getLabel());
        mNoteBodyView.setText(note.getBody());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_activity_layout);
        mNoteLabelView = (TextView) findViewById(R.id.note_label);
        mNoteBodyView = (TextView) findViewById(R.id.note_body);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        //TODO complete the exercise into steps:
        // 1. create a ViewModel and load a Note via NoteWebService
        // 2. persist a loaded note to NoteDatabase.
        showNote(new Note(0, "a", "b"));
    }
}
