// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from SearchActivity.java.  DO NOT MODIFY. */

/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.leanback;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.SpeechRecognitionCallback;

public class SearchSupportActivity extends FragmentActivity
{
    private static final String TAG = "SearchSupportActivity";
    private static boolean DEBUG = true;

    /** If using internal speech recognizer, you must have RECORD_AUDIO permission */
    private static final boolean USE_INTERNAL_SPEECH_RECOGNIZER = true;
    private static final int REQUEST_SPEECH = 1;

    private SearchSupportFragment mFragment;
    private SpeechRecognitionCallback mSpeechRecognitionCallback;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_support);

        mFragment = (SearchSupportFragment) getSupportFragmentManager().findFragmentById(R.id.search_fragment);

        if (!USE_INTERNAL_SPEECH_RECOGNIZER) {
            mSpeechRecognitionCallback = new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    if (DEBUG) Log.v(TAG, "recognizeSpeech");
                    startActivityForResult(mFragment.getRecognizerIntent(), REQUEST_SPEECH);
                }
            };
            mFragment.setSpeechRecognitionCallback(mSpeechRecognitionCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.v(TAG, "onActivityResult requestCode=" + requestCode +
                " resultCode=" + resultCode +
                " data=" + data);
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            mFragment.setSearchQuery(data, true);
        }
    }

}
