/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.integration.core;

import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.camera.core.VideoCapture;
import androidx.camera.core.VideoCapture.OnVideoSavedListener;
import androidx.camera.core.VideoCapture.UseCaseError;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Basic functionality required for interfacing the {@link VideoCapture}.
 */
public class VideoFileSaver implements OnVideoSavedListener {
    private static final String TAG = "VideoFileSaver";
    private final Format mFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH);
    private final Object mLock = new Object();
    private File mRootDirectory;
    @GuardedBy("mLock")
    private boolean mIsSaving = false;

    @Override
    public void onVideoSaved(File file) {

        Log.d(TAG, "Saved file: " + file.getPath());
        synchronized (mLock) {
            mIsSaving = false;
        }
    }

    @Override
    public void onError(UseCaseError useCaseError, String message, @Nullable Throwable cause) {

        Log.e(TAG, "Error: " + useCaseError + ", " + message);
        if (cause != null) {
            Log.e(TAG, "Error cause: " + cause.getCause());
        }

        synchronized (mLock) {
            mIsSaving = false;
        }
    }

    /** Returns a new {@link File} where to save a video. */
    public File getNewVideoFile() {
        Date date = Calendar.getInstance().getTime();
        File file = new File(mRootDirectory + "/" + mFormatter.format(date) + ".mp4");
        return file;
    }

    /** Sets the directory for saving files. */
    public void setRootDirectory(File rootDirectory) {

        mRootDirectory = rootDirectory;
    }

    boolean isSaving() {
        synchronized (mLock) {
            return mIsSaving;
        }
    }

    /** Sets saving state after video startRecording */
    void setSaving() {
        synchronized (mLock) {
            mIsSaving = true;
        }
    }
}
