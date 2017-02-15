package com.android.flatfoot.apireviewdemo.exercise;

import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.flatfoot.apireviewdemo.R;
import com.android.support.lifecycle.LifecycleActivity;

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
