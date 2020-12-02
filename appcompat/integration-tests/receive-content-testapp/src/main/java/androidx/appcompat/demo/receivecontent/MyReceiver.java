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

package androidx.appcompat.demo.receivecontent;

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.OnReceiveContentListener;

import java.io.FileNotFoundException;

/**
 * Sample {@link OnReceiveContentListener} implementation that accepts all URIs, and delegates
 * handling for all other content to the platform.
 */
public class MyReceiver implements OnReceiveContentListener {
    public static final String[] SUPPORTED_MIME_TYPES = new String[]{"image/*"};

    @Nullable
    @Override
    public ContentInfoCompat onReceiveContent(@NonNull View view,
            @NonNull ContentInfoCompat contentInfo) {
        Pair<ContentInfoCompat, ContentInfoCompat> split = contentInfo.partition(
                item -> item.getUri() != null);
        ContentInfoCompat uriContent = split.first;
        ContentInfoCompat remaining = split.second;
        if (uriContent != null) {
            ClipData clip = uriContent.getClip();
            for (int i = 0; i < clip.getItemCount(); i++) {
                receive(view, clip.getItemAt(i).getUri());
            }
        }
        return remaining;
    }

    private static void receive(@NonNull View view, @NonNull Uri contentUri) {
        final Context applicationContext = view.getContext().getApplicationContext();
        MyExecutors.bg().execute(() -> {
            ContentResolver contentResolver = applicationContext.getContentResolver();
            String mimeType = contentResolver.getType(contentUri);
            long lengthBytes;
            try {
                AssetFileDescriptor fd = contentResolver.openAssetFileDescriptor(contentUri, "r");
                lengthBytes = fd.getLength();
            } catch (FileNotFoundException e) {
                Log.e(Logcat.TAG, "Error opening content URI: " + contentUri, e);
                return;
            }
            String msg = "Received " + mimeType + " (" + lengthBytes + " bytes): " + contentUri;
            Log.i(Logcat.TAG, msg);
            MyExecutors.main().post(() -> {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show();
            });
        });
    }
}
