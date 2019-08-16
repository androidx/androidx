/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.webkit.internal;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

/**
  * Handles opening resources and assets.
  * Forked from the chromuim project org.chromium.android_webview.AndroidProtocolHandler
  */
public class AssetHelper {
    private static final String TAG = "AssetHelper";

    /**
     * Default value to be used as MIME type if guessing MIME type failed.
     */
    public static final String DEFAULT_MIME_TYPE = "text/plain";

    @NonNull private Context mContext;

    public AssetHelper(@NonNull Context context) {
        this.mContext = context;
    }

    @Nullable
    private static InputStream handleSvgzStream(@NonNull String path,
            @Nullable InputStream stream) {
        if (stream != null && path.endsWith(".svgz")) {
            try {
                stream = new GZIPInputStream(stream);
            } catch (IOException e) {
                Log.e(TAG, "Error decompressing " + path + " - " + e.getMessage());
                return null;
            }
        }
        return stream;
    }

    @NonNull
    private static String removeLeadingSlash(@NonNull String path) {
        if (path.length() > 1 && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }

    private int getFieldId(@NonNull String assetType, @NonNull String assetName) {
        String packageName = mContext.getPackageName();
        int id = mContext.getResources().getIdentifier(assetName, assetType, packageName);
        return id;
    }

    private int getValueType(int fieldId) {
        TypedValue value = new TypedValue();
        mContext.getResources().getValue(fieldId, value, true);
        return value.type;
    }

    /**
     * Open an InputStream for an Android resource.
     *
     * @param path Path of the form "resource_type/resource_name.ext".
     * @return An InputStream to the Android resource.
     */
    @Nullable
    public InputStream openResource(@NonNull String path) {
        path = removeLeadingSlash(path);
        // The path must be of the form "asset_type/asset_name.ext".
        String[] pathSegments = path.split("/");
        if (pathSegments.length != 2) {
            Log.e(TAG, "Incorrect resource path: " + path);
            return null;
        }
        String assetType = pathSegments[0];
        String assetName = pathSegments[1];

        // Drop the file extension.
        assetName = assetName.split("\\.")[0];
        try {
            int fieldId = getFieldId(assetType, assetName);
            int valueType = getValueType(fieldId);
            if (valueType == TypedValue.TYPE_STRING) {
                return handleSvgzStream(path, mContext.getResources().openRawResource(fieldId));
            } else {
                Log.e(TAG, "Asset not of type string: " + path);
                return null;
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource not found from the path: " + path, e);
            return null;
        }
    }

    /**
     * Open an InputStream for an Android asset.
     *
     * @param path Path to the asset file to load.
     * @return An InputStream to the Android asset.
     */
    @Nullable
    public InputStream openAsset(@NonNull String path) {
        path = removeLeadingSlash(path);
        try {
            AssetManager assets = mContext.getAssets();
            return handleSvgzStream(path, assets.open(path, AssetManager.ACCESS_STREAMING));
        } catch (IOException e) {
            Log.e(TAG, "Unable to open asset path: " + path);
            return null;
        }
    }

    /**
     * Open an {@code InputStream} for a file in application data directories.
     *
     * @param file The the file to be opened.
     * @return An {@code InputStream} for the requested file or {@code null} if an error happens.
     */
    @Nullable
    public static InputStream openFile(@NonNull File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            return handleSvgzStream(file.getPath(), fis);
        } catch (IOException e) {
            Log.e(TAG, "Error opening the requested file " + file, e);
            return null;
        }
    }

    /**
     * Util method to test if the a given file is a child of the given parent directory.
     * It uses canonical paths to make sure to resolve any symlinks, {@code "../"}, {@code "./"}
     * ... etc in the given paths.
     *
     * @param parent the parent directory.
     * @param child the child file.
     * @return {@code true} if the canonical path of the given {@code child} starts with the
     *         canonical path of the given {@code parent}, {@code false} otherwise.
     */
    public static boolean isCanonicalChildOf(@NonNull File parent, @NonNull File child) {
        try {
            String parentCanonicalPath = parent.getCanonicalPath();
            String childCanonicalPath = child.getCanonicalPath();

            if (!parentCanonicalPath.endsWith("/")) parentCanonicalPath += "/";

            return childCanonicalPath.startsWith(parentCanonicalPath);
        } catch (IOException e) {
            Log.e(TAG, "Error getting the canonical path of file", e);
            return false;
        }
    }

    /**
     * Get the canonical path of the given directory with a slash {@code "/"} at the end.
     */
    @NonNull
    public static String getCanonicalPath(@NonNull File file) throws IOException {
        String path = file.getCanonicalPath();
        if (!path.endsWith("/")) path += "/";
        return path;
    }

    /**
     * Get the data dir for an application.
     *
     * @param context the {@link Context} used to get the data dir.
     * @return data dir {@link File} for that app.
     */
    @NonNull
    public static File getDataDir(@NonNull Context context) {
        // Context#getDataDir is only available in APIs >= 24.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getDataDir();
        } else {
            // For APIs < 24 cache dir is created under the data dir.
            return context.getCacheDir().getParentFile();
        }
    }

    /**
     * Use {@link URLConnection#guessContentTypeFromName} to guess MIME type or return the
     * {@link DEFAULT_MIME_TYPE} if it can't guess.
     *
     * @param filePath path of the file to guess its MIME type.
     * @return MIME type guessed from file extension or {@link DEFAULT_MIME_TYPE}.
     */
    @NonNull
    public static String guessMimeType(@NonNull String filePath) {
        String mimeType = URLConnection.guessContentTypeFromName(filePath);
        return mimeType == null ? DEFAULT_MIME_TYPE : mimeType;
    }
}
