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

package android.support.v17.leanback.supportleanbackshowcase;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    /**
     * Will read the content from a given {@link InputStream} and return it as a {@link String}.
     *
     * @param inputStream The {@link InputStream} which should be read.
     * @return Returns <code>null</code> if the the {@link InputStream} could not be read. Else
     * returns the content of the {@link InputStream} as {@link String}.
     */
    public static String inputStreamToString(InputStream inputStream) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes, 0, bytes.length);
            String json = new String(bytes);
            return json;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * The method uses {@link Picasso} to fetch an image from a given url, resize it (if required)
     * and display it inside an {@link ImageView}.
     *
     * @param context Context which is used to create a {@link Picasso} instance.
     * @param uri The {@link URI} to fetch the image from.
     * @param target The {@link ImageView} which shall display the image.
     * @param resizeWidthInDp The target width of the image. Pass <code>-1</code> if you don't want
     * to resize the image.
     * @param resizeHeightInDp The target height of the image. Pass <code>-1</code> if you don't
     * want to resize the image.
     * @param centerCrop Centers and scales an image to fit the requested bounds.
     * @param errorDrawable A drawable which will be shown in case the image could not be fetched
     * from the server.
     * @see {@link Picasso#with(Context)}
     * @see {@link RequestCreator#resize(int, int)}
     * @see {@link RequestCreator#centerCrop()}
     * @see {@link RequestCreator#error(Drawable)}
     */
    public static void loadImageFromUri(Context context, URI uri, ImageView target,
                                        int resizeWidthInDp, int resizeHeightInDp,
                                        boolean centerCrop, Drawable errorDrawable) {
        if (uri == null) return;
        RequestCreator builder = Picasso.with(context).load(uri.toString());
        if (resizeHeightInDp != -1 && resizeWidthInDp != -1)
            builder.resize(Utils.convertDpToPixel(context, resizeWidthInDp),
                           Utils.convertDpToPixel(context, resizeHeightInDp));
        if (centerCrop) builder.centerCrop();
        builder.error(errorDrawable).into(target);
    }

    public static Uri getResourceUri(Context context, int resID) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                                 context.getResources().getResourcePackageName(resID) + '/' +
                                 context.getResources().getResourceTypeName(resID) + '/' +
                                 context.getResources().getResourceEntryName(resID));
    }
}
