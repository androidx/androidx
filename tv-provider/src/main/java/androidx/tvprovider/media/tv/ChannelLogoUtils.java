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

package androidx.tvprovider.media.tv;

import android.content.ContentResolver;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

/** A utility class for conveniently storing and loading channel logos. */
@WorkerThread
public class ChannelLogoUtils {
    private static final String TAG = "ChannelLogoUtils";

    private static final int CONNECTION_TIMEOUT_MS_FOR_URLCONNECTION = 3000;  // 3 sec
    private static final int READ_TIMEOUT_MS_FOR_URLCONNECTION = 10000;  // 10 sec

    /**
     * Stores channel logo in the system content provider from the given URI. The method will try
     * to fetch the image file and decode it into {@link Bitmap}. Once the image is successfully
     * fetched, it will be stored in the system content provider and associated with the given
     * channel ID.
     *
     * <p>The URI provided to this method can be a URL referring to a image file residing in some
     * remote site/server, or a URI in one of the following formats:
     *
     *    <ul>
     *        <li>content ({@link android.content.ContentResolver#SCHEME_CONTENT})</li>
     *        <li>android.resource ({@link android.content.ContentResolver
     *                                     #SCHEME_ANDROID_RESOURCE})</li>
     *        <li>file ({@link android.content.ContentResolver#SCHEME_FILE})</li>
     *    </ul>
     *
     * <p>This method should be run in a worker thread since it may require network connection,
     * which will raise an exception if it's running in the main thread.
     *
     * @param context the context used to access the system content provider
     * @param channelId the ID of the target channel with which the fetched logo should be
     *                  associated
     * @param logoUri the {@link Uri} of the logo file to be fetched and stored in the system
     *                provider
     *
     * @return {@code true} if successfully fetched the image file referred by the give logo URI
     *         and stored it in the system content provider, or {@code false} if failed.
     *
     * @see #loadChannelLogo(Context, long)
     */
    public static boolean storeChannelLogo(@NonNull Context context, long channelId,
            @NonNull Uri logoUri) {
        String scheme = logoUri.normalizeScheme().getScheme();
        URLConnection urlConnection = null;
        InputStream inputStream = null;
        Bitmap fetchedLogo = null;
        try {
            if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)
                    || ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                // A local resource
                inputStream = context.getContentResolver().openInputStream(logoUri);
            } else {
                // A remote resource, should be an valid URL.
                urlConnection = getUrlConnection(logoUri.toString());
                inputStream = urlConnection.getInputStream();
            }
            fetchedLogo = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.i(TAG, "Failed to get logo from the URI: " + logoUri + "\n", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Do nothing.
                }
            }
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConnection).disconnect();
            }
        }
        return fetchedLogo != null && storeChannelLogo(context, channelId, fetchedLogo);
    }

    /**
     * Stores the given channel logo {@link Bitmap} in the system content provider and associate
     * it with the given channel ID.
     *
     * @param context the context used to access the system content provider
     * @param channelId the ID of the target channel with which the given logo should be associated
     * @param logo the logo image to be stored
     *
     * @return {@code true} if successfully stored the logo in the system content provider,
     *         otherwise {@code false}.
     *
     * @see #loadChannelLogo(Context, long)
     */
    public static boolean storeChannelLogo(@NonNull Context context, long channelId,
            @NonNull Bitmap logo) {
        boolean result = false;
        Uri localUri = TvContract.buildChannelLogoUri(channelId);
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(localUri)) {
            result = logo.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
        } catch (SQLiteException | IOException e) {
            Log.i(TAG, "Failed to store the logo to the system content provider.\n", e);
        }
        return result;
    }

    /**
     * A convenient helper method to get the channel logo associated to the given channel ID from
     * the system content provider.
     *
     * @param context the context used to access the system content provider
     * @param channelId the ID of the channel whose logo is supposed to be loaded
     *
     * @return the requested channel logo in {@link Bitmap}, or {@code null} if not available.
     *
     * @see #storeChannelLogo(Context, long, Uri)
     * @see #storeChannelLogo(Context, long, Bitmap)
     */
    public static Bitmap loadChannelLogo(@NonNull Context context, long channelId) {
        Bitmap channelLogo = null;
        try {
            channelLogo = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(
                    TvContract.buildChannelLogoUri(channelId)));
        } catch (FileNotFoundException e) {
            // Channel logo is not found in the content provider.
            Log.i(TAG, "Channel logo for channel (ID:" + channelId + ") not found.", e);
        }
        return channelLogo;
    }

    private static URLConnection getUrlConnection(String uriString) throws IOException {
        URLConnection urlConnection = new URL(uriString).openConnection();
        urlConnection.setConnectTimeout(CONNECTION_TIMEOUT_MS_FOR_URLCONNECTION);
        urlConnection.setReadTimeout(READ_TIMEOUT_MS_FOR_URLCONNECTION);
        return urlConnection;
    }

    /** @deprecated This type should not be instantiated as it contains only static methods. */
    @Deprecated
    @SuppressWarnings("PrivateConstructorForUtilityClass")
    public ChannelLogoUtils() {
    }
}
