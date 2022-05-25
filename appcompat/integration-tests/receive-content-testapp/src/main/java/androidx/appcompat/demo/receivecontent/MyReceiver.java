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
import android.content.ClipDescription;
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample {@link OnReceiveContentListener} implementation that accepts all URIs, and delegates
 * handling for all other content to the platform.
 */
final class MyReceiver implements OnReceiveContentListener {
    public static final String[] SUPPORTED_MIME_TYPES = new String[]{"image/*"};

    final AttachmentsRepo mAttachmentsRepo;
    final AttachmentsRecyclerViewAdapter mAttachmentsRecyclerViewAdapter;

    MyReceiver(@NonNull AttachmentsRepo attachmentsRepo,
            @NonNull AttachmentsRecyclerViewAdapter attachmentsRecyclerViewAdapter) {
        mAttachmentsRepo = attachmentsRepo;
        mAttachmentsRecyclerViewAdapter = attachmentsRecyclerViewAdapter;
    }

    @Nullable
    @Override
    public ContentInfoCompat onReceiveContent(@NonNull View view,
            @NonNull ContentInfoCompat payload) {
        // Split the incoming content into two groups: content URIs and everything else.
        // This way we can implement custom handling for URIs and delegate the rest.
        Pair<ContentInfoCompat, ContentInfoCompat> split = payload.partition(
                item -> item.getUri() != null);
        ContentInfoCompat uriContent = split.first;
        ContentInfoCompat remaining = split.second;
        if (uriContent != null) {
            receive(view.getContext(), uriContent);
        }
        // Return anything that we didn't handle ourselves. This preserves the default platform
        // behavior for text and anything else for which we are not implementing custom handling.
        return remaining;
    }

    /**
     * Handles incoming content URIs. If the content is an image, stores it as an attachment in the
     * app's private storage. If the content is any other type, simply shows a toast with the type
     * of the content and its size in bytes.
     *
     * <p><strong>Important:</strong> It is significant that we pass along the {@code payload}
     * object to the worker thread that will process the content, because URI permissions are tied
     * to the payload object's lifecycle. If that object is not passed along, it could be garbage
     * collected and permissions would be revoked prematurely (before we have a chance to process
     * the content).
     */
    private void receive(@NonNull Context context, @NonNull ContentInfoCompat payload) {
        Context applicationContext = context.getApplicationContext();
        ContentResolver contentResolver = applicationContext.getContentResolver();
        ListenableFuture<List<Uri>> addAttachmentsFuture = MyExecutors.bg().submit(() -> {
            List<Uri> uris = collectUris(payload.getClip());
            List<Uri> localUris = new ArrayList<>(uris.size());
            for (Uri uri : uris) {
                String mimeType = contentResolver.getType(uri);
                Log.i(Logcat.TAG, "Processing URI: " + uri + " (type: " + mimeType + ")");
                if (ClipDescription.compareMimeTypes(mimeType, "image/*")) {
                    // Read the image at the given URI and write it to private storage.
                    localUris.add(mAttachmentsRepo.write(uri));
                } else {
                    showMessage(applicationContext, uri, mimeType);
                }
            }
            return localUris;
        });
        Futures.addCallback(addAttachmentsFuture, new FutureCallback<List<Uri>>() {
            @Override
            public void onSuccess(List<Uri> localUris) {
                // Show the image in the UI by passing the URI pointing to the locally stored copy
                // to the recycler view adapter.
                mAttachmentsRecyclerViewAdapter.addAttachments(localUris);
                mAttachmentsRecyclerViewAdapter.notifyDataSetChanged();
                Log.i(Logcat.TAG, "Processed content: " + payload);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(Logcat.TAG, "Error processing content: " + payload, t);
            }
        }, MyExecutors.main());
    }

    /**
     * Reads the size of the given content URI and shows a toast with the type of the content and
     * its size in bytes.
     */
    private void showMessage(@NonNull Context applicationContext,
            @NonNull Uri uri, @NonNull String mimeType) {
        MyExecutors.bg().execute(() -> {
            ContentResolver contentResolver = applicationContext.getContentResolver();
            long lengthBytes;
            try {
                AssetFileDescriptor fd = contentResolver.openAssetFileDescriptor(uri, "r");
                lengthBytes = fd.getLength();
            } catch (FileNotFoundException e) {
                Log.e(Logcat.TAG, "Error opening content URI: " + uri, e);
                return;
            }
            String msg = "Content of type " + mimeType + " (" + lengthBytes + " bytes): " + uri;
            Log.i(Logcat.TAG, msg);
            MyExecutors.main().execute(() -> {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show();
            });
        });
    }

    private static List<Uri> collectUris(ClipData clip) {
        List<Uri> uris = new ArrayList<>(clip.getItemCount());
        for (int i = 0; i < clip.getItemCount(); i++) {
            Uri uri = clip.getItemAt(i).getUri();
            if (uri != null) {
                uris.add(uri);
            }
        }
        return uris;
    }
}
