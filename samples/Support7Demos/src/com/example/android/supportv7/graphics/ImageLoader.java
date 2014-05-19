/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.example.android.supportv7.graphics;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.graphics.BitmapCompat;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

/**
 * A very naive lazily implemented image loader. Do not use this in production code.
 */
class ImageLoader {

    /**
     * A LruCache used to store images which has a maximum size of 10% of the maximum heap size.
     */
    private static final BitmapCache CACHE = new BitmapCache(
            Math.round(Runtime.getRuntime().maxMemory() / 10));

    private ImageLoader() {
    }

    interface Listener {
        void onImageLoaded(Bitmap bitmap);
    }

    static void loadMediaStoreThumbnail(final ImageView imageView,
            final long id,
            final Listener listener) {

        final Bitmap cachedValue = CACHE.get(id);
        if (cachedValue != null) {
            // If the image is already in the cache, display the image,
            // call the listener now and return
            imageView.setImageBitmap(cachedValue);
            if (listener != null) {
                listener.onImageLoaded(cachedValue);
            }
            return;
        }

        AsyncTaskCompat.executeParallel(new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return MediaStore.Images.Thumbnails.getThumbnail(
                        imageView.getContext().getContentResolver(),
                        id,
                        MediaStore.Images.Thumbnails.MINI_KIND,
                        null);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                imageView.setImageBitmap(bitmap);

                if (bitmap != null) {
                    // Add the image to the memory cache first
                    CACHE.put(id, bitmap);

                    if (listener != null) {
                        listener.onImageLoaded(bitmap);
                    }
                }
            }
        });
    }

    /**
     * A simple cache implementation for {@link android.graphics.Bitmap} instances which uses
     * {@link android.support.v4.util.LruCache}.
     */
    private static class BitmapCache extends LruCache<Long, Bitmap> {
        BitmapCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(Long key, Bitmap value) {
            return BitmapCompat.getAllocationByteCount(value);
        }
    }

}
