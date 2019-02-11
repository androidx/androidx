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

package androidx.camera.testapp.hellocamerax;

import android.support.annotation.GuardedBy;
import android.support.annotation.Nullable;
import android.util.Log;

import androidx.camera.core.VideoCaptureUseCase.OnVideoSavedListener;
import androidx.camera.core.VideoCaptureUseCase.UseCaseError;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Basic functionality required for interfacing the {@link
 * androidx.camera.core.VideoCaptureUseCase}.
 */
public class VideoFileSaver implements OnVideoSavedListener {
    private static final String TAG = "VideoFileSaver";
    private final Format formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.ENGLISH);
    private final Object lock = new Object();
    private File rootDirectory;
    @GuardedBy("lock")
    private boolean isSaving = false;

    @Override
    public void onVideoSaved(File file) {

        Log.d(TAG, "Saved file: " + file.getPath());
        synchronized (lock) {
            isSaving = false;
        }
    }

    @Override
    public void onError(UseCaseError useCaseError, String message, @Nullable Throwable cause) {

        Log.e(TAG, "Error: " + useCaseError + ", " + message);
        if (cause != null) {
            Log.e(TAG, "Error cause: " + cause.getCause());
        }

        synchronized (lock) {
            isSaving = false;
        }
    }

    /** Returns a new {@link File} where to save a video. */
    public File getNewVideoFile() {
        Date date = Calendar.getInstance().getTime();
        File file = new File(rootDirectory + "/" + formatter.format(date) + ".mp4");
        return file;
    }

    /** Sets the directory for saving files. */
    public void setRootDirectory(File rootDirectory) {
        this.rootDirectory = rootDirectory;
    }

    boolean isSaving() {
        synchronized (lock) {
            return isSaving;
        }
    }

    /** Sets saving state after video startRecording */
    void setSaving() {
        synchronized (lock) {
            isSaving = true;
        }
    }
}
