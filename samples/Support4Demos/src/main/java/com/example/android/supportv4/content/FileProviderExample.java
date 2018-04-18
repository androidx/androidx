/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.example.android.supportv4.content;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.FileProvider;

import com.example.android.supportv4.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Sample that shows how private files can be easily shared.
 */
public class FileProviderExample extends Activity {
    private static final String AUTHORITY = "com.example.android.supportv4.my_files";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_provider_example);
    }

    public void onShareFileClick(View view) {
        // Save a thumbnail to file
        final File thumbsDir = new File(getFilesDir(), "thumbs");
        thumbsDir.mkdirs();
        final File file = new File(thumbsDir, "private.png");
        saveThumbnail(view, file);

        // Now share that private file using FileProvider
        final Uri uri = FileProvider.getUriForFile(this, AUTHORITY, file);
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/png");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(intent);
    }

    /**
     * Save thumbnail of given {@link View} to {@link File}.
     */
    private void saveThumbnail(View view, File file) {
        final Bitmap bitmap = Bitmap.createBitmap(
                view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        try {
            final OutputStream os = new FileOutputStream(file);
            try {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
            } finally {
                os.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
