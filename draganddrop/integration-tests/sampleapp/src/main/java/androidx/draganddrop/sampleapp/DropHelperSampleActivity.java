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

package androidx.draganddrop.sampleapp;

import android.content.ClipData;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentListener;
import androidx.draganddrop.DropHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Sample activity for {@link DropHelper}/ */
public class DropHelperSampleActivity extends AppCompatActivity {
    private static final String TAG = "DropHelperSampleActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drop_helper_sample_activity);
    }

    @Override
    protected void onStart() {
        super.onStart();

        OnReceiveContentListener toastingOnReceiveContentListener = (view, payload) -> {
            Toast.makeText(DropHelperSampleActivity.this.getApplicationContext(),
                    "Drop", Toast.LENGTH_SHORT).show();
            return payload;
        };

        OnReceiveContentListener imageDisplayingOnReceiveContentListener = (view, payload) -> {
            Pair<ContentInfoCompat, ContentInfoCompat> split =
                    payload.partition(item -> item.getUri() != null);
            ContentInfoCompat uriContent = split.first;
            ContentInfoCompat textContent = split.second;
            if (uriContent != null && uriContent.getClip() != null) {
                ClipData clipData = uriContent.getClip();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    try {
                        Uri localImageUri = storeImage(clipData.getItemAt(i).getUri());
                        showImage(localImageUri);
                    } catch (IOException e) {
                        Log.w(TAG, "Error resolving and storing dropped data", e);
                    }
                }
            }
            return textContent;
        };

        DropHelper.configureView(this,
                findViewById(R.id.drop_target),
                new String[]{"image/*"},
                imageDisplayingOnReceiveContentListener);

        DropHelper.configureView(this,
                findViewById(R.id.drop_target_2),
                new String[]{"text/*", "image/*"},
                new DropHelper.Options.Builder()
                        .setHighlightColor(Color.RED)
                        .build(),
                toastingOnReceiveContentListener);

        DropHelper.configureView(this,
                findViewById(R.id.outer_drop_target),
                new String[]{"image/*", "text/*"},
                new DropHelper.Options.Builder()
                        .addInnerEditTexts(findViewById(R.id.inner_edit_text))
                        .build(),
                toastingOnReceiveContentListener);
    }

    /**
     * Stores the image found at the provided Uri and returns a Uri to our local copy.
     *
     * <p>Apps *SHOULD NOT* use the dropped URI(s) directly (there may be, e.g. an expiration for
     * the URI). You should read and store the content in your app.
     */
    private Uri storeImage(Uri uri) throws IOException {
        InputStream input = getContentResolver().openTypedAssetFileDescriptor(
                uri, "image/*", /* opts= */ null).createInputStream();
        File file = File.createTempFile("droppedimage", /* suffix= */ null);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        byte[] buffer = new byte[1024];
        try {
            int len = input.read(buffer);
            while (len != -1) {
                fileOutputStream.write(buffer, 0, len);
                len = input.read(buffer);
            }
        } finally {
            fileOutputStream.close();
        }
        return Uri.fromFile(file);
    }

    private void showImage(Uri imageUri) {
        ImageView view = new ImageView(this);
        view.setAdjustViewBounds(true);
        view.setMaxHeight(100);
        view.setImageURI(imageUri);
        ((ViewGroup) findViewById(R.id.dropped_data_display)).addView(view);
    }
}
