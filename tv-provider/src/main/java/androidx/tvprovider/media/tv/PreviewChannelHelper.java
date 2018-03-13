/*
 * Copyright 2018 The Android Open Source Project
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

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * From a user's perspective, the TV home screen has two types of channels: the single Live
 * Channels row versus the App preview Channels. This class is concerned with App Channels; or more
 * precisely: <i>your</i> app's preview Channels. In API 26+, all TV apps are allowed to create
 * multiple channels and publish those Channels to the home screen.
 * <p>
 * This class provides convenience methods to help you publish, update and delete channels; add,
 * update or remove programs in a channel. You do not need to know anything about Content
 * Providers, Content Resolvers, Cursors or such to publish your channels. This class abstracts
 * away all database interactions for you.
 * <p>
 * To make it easy for you to distinguish classes that help you build App Channels, the support
 * library uses the prefix Preview- to denote the classes that pertain to app Channels. Hence,
 * the classes {@link PreviewChannel} and {@link PreviewProgram} help your app add channels to the
 * TV home page.
 *
 * All calls to methods in the class should be made on worker threads.
 */

@TargetApi(26)
@WorkerThread
public class PreviewChannelHelper {

    private static final String TAG = "PreviewChannelHelper";
    private static final int DEFAULT_URL_CONNNECTION_TIMEOUT_MILLIS =
            (int) (3 * DateUtils.SECOND_IN_MILLIS);
    private static final int DEFAULT_READ_TIMEOUT_MILLIS = (int) (10 * DateUtils.SECOND_IN_MILLIS);
    private static final int INVALID_CONTENT_ID = -1;
    private final int mUrlConnectionTimeoutMillis;
    private final int mUrlReadTimeoutMillis;
    private final Context mContext;

    public PreviewChannelHelper(Context context) {
        this(context, DEFAULT_URL_CONNNECTION_TIMEOUT_MILLIS, DEFAULT_READ_TIMEOUT_MILLIS);
    }

    /**
     * @param urlConnectionTimeoutMillis see {@link URLConnection#setConnectTimeout(int)}
     * @param urlReadTimeoutMillis       see {@link URLConnection#setReadTimeout(int)}
     */
    public PreviewChannelHelper(Context context, int urlConnectionTimeoutMillis,
            int urlReadTimeoutMillis) {
        mContext = context;
        mUrlConnectionTimeoutMillis = urlConnectionTimeoutMillis;
        mUrlReadTimeoutMillis = urlReadTimeoutMillis;
    }

    /**
     * Publishing a channel to the TV home screen is a two step process: first, you add the
     * channel to the TV content provider; second, you make the channel browsable (i.e. visible).
     * {@link #publishChannel(PreviewChannel) This method} adds the channel to the
     * TV content provider for you and returns a channelId. Next you must use the channelId
     * to make the channel browsable.
     * </br>
     * There are two ways you can make a channel browsable:
     * </br>
     * a) For your first channel, simply ask the system to make the channel browsable:
     * TvContractCompat.requestChannelBrowsable(context,channelId)
     * </br>
     * b) For any additional channel beyond the first channel, you must get permission
     * from the user. So if this channel is not your first channel, you must request user
     * permission through the following intent. So take the channelId returned by
     * {@link #publishChannel(PreviewChannel) this method} and do the following
     * inside an Activity or Fragment:
     * </br>
     * <pre>
     * intent = new Intent(TvContractCompat.ACTION_REQUEST_CHANNEL_BROWSABLE);
     * intent.putExtra(TvContractCompat.EXTRA_CHANNEL_ID, channelId);
     * startActivityForResult(intent, REQUEST_CHANNEL_BROWSABLE);
     * </pre>
     *
     * <p>
     * Creating a PreviewChannel, you may pass to the builder a
     * {@link PreviewChannel.Builder#setLogo(Uri) url as your logo}. In such case,
     * {@link #updatePreviewChannel(long, PreviewChannel)} will load the logo over the network. To
     * use your own networking code, override {@link #downloadBitmap(Uri)}.
     *
     * @return channelId or -1 if insertion fails. This is the id the system assigns to your
     * published channel. You can use it later to get a reference to this published PreviewChannel.
     */
    public long publishChannel(@NonNull PreviewChannel channel) throws IOException {
        try {
            Uri channelUri = mContext.getContentResolver().insert(
                    TvContractCompat.Channels.CONTENT_URI,
                    channel.toContentValues());
            if (null == channelUri || channelUri.equals(Uri.EMPTY)) {
                throw new NullPointerException("Channel insertion failed");
            }
            long channelId = ContentUris.parseId(channelUri);
            boolean logoAdded = addChannelLogo(channelId, channel);
            // Rollback channel insertion if logo could not be added.
            if (!logoAdded) {
                deletePreviewChannel(channelId);
                throw new IOException("Failed to add logo, so channel (ID="
                        + channelId + ") was not created");
            }
            return channelId;
        } catch (SecurityException e) {
            Log.e(TAG, "Your app's ability to insert data into the TvProvider"
                    + " may have been revoked.", e);
        }
        return INVALID_CONTENT_ID;
    }

    /**
     * This is a convenience method that simply publishes your first channel for you. After calling
     * {@link #publishChannel(PreviewChannel)} to add the channel to the TvProvider, it
     * calls {@link TvContractCompat#requestChannelBrowsable(Context, long)} to make the channel
     * visible.
     * <p>
     * Only use this method to publish your first channel as you do not need user permission to
     * make your first channel browsable (i.e. visible on home screen). For additional channels,
     * see the documentations for {@link #publishChannel(PreviewChannel)}.
     *
     * <p>
     * Creating a PreviewChannel, you may pass to the builder a
     * {@link PreviewChannel.Builder#setLogo(Uri) url as your logo}. In such case,
     * {@link #updatePreviewChannel(long, PreviewChannel)} will load the logo over the network. To
     * use your own networking code, override {@link #downloadBitmap(Uri)}.
     *
     * @return channelId: This is the id the system assigns to your published channel. You can
     * use it later to get a reference to this published PreviewChannel.
     */
    public long publishDefaultChannel(@NonNull PreviewChannel channel)
            throws IOException {
        long channelId = publishChannel(channel);
        TvContractCompat.requestChannelBrowsable(mContext, channelId);
        return channelId;
    }

    /**
     * The TvProvider does not allow select queries. Hence, unless you are querying for a
     * {@link #getPreviewChannel(long) single PreviewChannel by id}, you must get all of
     * your channels at once and then use the returned list as necessary.
     */
    public List<PreviewChannel> getAllChannels() {
        Cursor cursor = mContext.getContentResolver()
                .query(
                        TvContractCompat.Channels.CONTENT_URI,
                        PreviewChannel.Columns.PROJECTION,
                        null,
                        null,
                        null);

        List<PreviewChannel> channels = new ArrayList<>();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                channels.add(PreviewChannel.fromCursor(cursor));
            } while (cursor.moveToNext());
        }
        return channels;
    }

    /**
     * Retrieves a single preview channel from the TvProvider. When you publish a preview channel,
     * the TvProvider assigns an ID to it. That's the channelId to use here.
     *
     * @param channelId ID of preview channel in TvProvider
     * @return PreviewChannel or null if not found
     */
    public PreviewChannel getPreviewChannel(long channelId) {
        PreviewChannel channel = null;
        Uri channelUri = TvContractCompat.buildChannelUri(channelId);
        Cursor cursor = mContext.getContentResolver()
                .query(channelUri, PreviewChannel.Columns.PROJECTION, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            channel = PreviewChannel.fromCursor(cursor);
        }
        return channel;
    }

    /**
     * To update a preview channel, you need to use the {@link PreviewChannel.Builder} to set the
     * attributes you wish to change. Then simply pass in the built channel and the channelId of the
     * preview channel. (The channelId is the ID you received when you originally
     * {@link #publishChannel(PreviewChannel) published} the preview channel.)
     * <p>
     * Creating a PreviewChannel, you may pass to the builder a
     * {@link PreviewChannel.Builder#setLogo(Uri) url as your logo}. In such case,
     * {@link #updatePreviewChannel(long, PreviewChannel)} will load the logo over the network. To
     * use your own networking code, override {@link #downloadBitmap(Uri)}.
     */
    public void updatePreviewChannel(long channelId,
            @NonNull PreviewChannel update) throws IOException {
        // To avoid possibly expensive no-op updates, first check that the current content that's
        // in the database is different from the new content to be added.
        PreviewChannel curr = getPreviewChannel(channelId);
        if (curr != null && curr.hasAnyUpdatedValues(update)) {
            updatePreviewChannelInternal(channelId, update);
        }
        if (update.isLogoChanged()) {
            boolean logoAdded = addChannelLogo(channelId, update);
            if (!logoAdded) {
                throw new IOException("Fail to update channel (ID=" + channelId + ") logo.");
            }
        }
    }

    /**
     * Inner methods that does the actual work of updating a Preview Channel. The method is
     * extracted to make {@link #updatePreviewChannel(long, PreviewChannel)} testable.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    protected void updatePreviewChannelInternal(long channelId, @NonNull PreviewChannel upgrade) {
        mContext.getContentResolver().update(
                TvContractCompat.buildChannelUri(channelId),
                upgrade.toContentValues(),
                null,
                null);
    }

    /**
     * Internally, a logo is added to a channel after the channel has been added to the TvProvider.
     * This private method is called by one of the publish methods, to add a logo to the TvProvider
     * and associate the logo to the given channel identified by channelId. Because each channel
     * must have a logo, a NullPointerException is thrown if the channel being published has no
     * associated logo to publish with it.
     */
    private boolean addChannelLogo(long channelId, @NonNull PreviewChannel channel) {
        boolean result = false;
        if (!channel.isLogoChanged()) {
            return result;
        }
        Bitmap logo = channel.getLogo(mContext);
        if (logo == null) {
            logo = getLogoFromUri(channel.getLogoUri());
        }
        Uri logoUri = TvContractCompat.buildChannelLogoUri(channelId);
        try (OutputStream outputStream = mContext.getContentResolver().openOutputStream(
                logoUri)) {
            result = logo.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
        } catch (SQLiteException | IOException | NullPointerException e) {
            Log.i(TAG, "Failed to add logo to the published channel (ID= " + channelId + ")", e);
        }
        return result;
    }

    /**
     * Handles the case where the Bitmap must be fetched from a known uri. First the
     * method checks if the Uri is local. If not, the method makes a connection to fetch the Bitmap
     * data from its remote location. To use your own networking implementation, simply override
     * {@link #downloadBitmap(Uri)}
     */
    private Bitmap getLogoFromUri(@NonNull Uri logoUri) {
        String scheme = logoUri.normalizeScheme().getScheme();
        InputStream inputStream = null;
        Bitmap logoImage = null;

        try {
            if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)
                    || ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                // for local resource
                inputStream = mContext.getContentResolver().openInputStream(logoUri);
                logoImage = BitmapFactory.decodeStream(inputStream);
            } else {
                logoImage = downloadBitmap(logoUri);
            }

        } catch (IOException e) {
            Log.e(TAG, "Failed to get logo from the URI: " + logoUri, e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }
        return logoImage;
    }

    /**
     * Downloads a Bitmap from a remote server. It is declared protected to allow you
     * to override it to use your own networking implementation if you so wish.
     */
    protected Bitmap downloadBitmap(@NonNull Uri logoUri) throws IOException {
        URLConnection urlConnection = null;
        InputStream inputStream = null;
        Bitmap logoImage = null;
        try {
            // for remote resource
            urlConnection = new URL(logoUri.toString()).openConnection();
            urlConnection.setConnectTimeout(mUrlConnectionTimeoutMillis);
            urlConnection.setReadTimeout(mUrlReadTimeoutMillis);
            inputStream = urlConnection.getInputStream();
            logoImage = BitmapFactory.decodeStream(inputStream);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection) urlConnection).disconnect();
            }
        }
        return logoImage;
    }

    /**
     * Removes a preview channel from the system's content provider (aka TvProvider).
     */
    public void deletePreviewChannel(long channelId) {
        mContext.getContentResolver().delete(
                TvContractCompat.buildChannelUri(channelId),
                null,
                null);
    }

    /**
     * Adds programs to a preview channel.
     */
    public long publishPreviewProgram(@NonNull PreviewProgram program) {
        try {
            Uri programUri = mContext.getContentResolver().insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    program.toContentValues());
            long programId = ContentUris.parseId(programUri);
            return programId;
        } catch (SecurityException e) {
            Log.e(TAG, "Your app's ability to insert data into the TvProvider"
                    + " may have been revoked.", e);
        }
        return INVALID_CONTENT_ID;
    }

    /**
     * Retrieves a single preview program from the system content provider (aka TvProvider).
     */
    public PreviewProgram getPreviewProgram(long programId) {
        PreviewProgram program = null;
        Uri programUri = TvContractCompat.buildPreviewProgramUri(programId);
        Cursor cursor = mContext.getContentResolver().query(programUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            program = PreviewProgram.fromCursor(cursor);
        }
        return program;
    }

    /**
     * Updates programs in a preview channel.
     */
    public void updatePreviewProgram(long programId, @NonNull PreviewProgram update) {
        // To avoid possibly expensive no-op updates, first check that the current content that's
        // in the database is different from the new content to be added.
        PreviewProgram curr = getPreviewProgram(programId);
        if (curr != null && curr.hasAnyUpdatedValues(update)) {
            updatePreviewProgramInternal(programId, update);
        }
    }

    /**
     * Inner methods that does the actual work of updating a Preview Program. The method is
     * extracted to make {@link #updatePreviewProgram(long, PreviewProgram)} testable.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    void updatePreviewProgramInternal(long programId, @NonNull PreviewProgram upgrade) {
        mContext.getContentResolver().update(
                TvContractCompat.buildPreviewProgramUri(programId),
                upgrade.toContentValues(), null, null);
    }

    /**
     * Removes programs from a preview channel.
     */
    public void deletePreviewProgram(long programId) {
        mContext.getContentResolver().delete(
                TvContractCompat.buildPreviewProgramUri(programId), null, null);
    }

    /**
     * Adds a program to the Watch Next channel
     */
    public long publishWatchNextProgram(@NonNull WatchNextProgram program) {
        try {
            Uri programUri = mContext.getContentResolver().insert(
                    TvContractCompat.WatchNextPrograms.CONTENT_URI, program.toContentValues());
            return ContentUris.parseId(programUri);
        } catch (SecurityException e) {
            Log.e(TAG, "Your app's ability to insert data into the TvProvider"
                    + " may have been revoked.", e);
        }
        return INVALID_CONTENT_ID;
    }

    /**
     * Retrieves a single WatchNext program from the system content provider (aka TvProvider).
     */
    public WatchNextProgram getWatchNextProgram(long programId) {
        WatchNextProgram program = null;
        Uri programUri = TvContractCompat.buildWatchNextProgramUri(programId);
        Cursor cursor = mContext.getContentResolver().query(programUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            program = WatchNextProgram.fromCursor(cursor);
        }
        return program;
    }

    /**
     * Updates a WatchNext program.
     */
    public void updateWatchNextProgram(@NonNull WatchNextProgram upgrade, long programId) {
        // To avoid possibly expensive no-op updates, first check that the current content that's in
        // the database is different from the new content to be added.
        WatchNextProgram curr = getWatchNextProgram(programId);
        if (curr != null && curr.hasAnyUpdatedValues(upgrade)) {
            updateWatchNextProgram(programId, upgrade);
        }
    }

    /**
     * Inner methods that does the actual work of updating a Watch Next Program. The method is
     * extracted to make {@link #updateWatchNextProgram(WatchNextProgram, long)} testable.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    void updateWatchNextProgram(long programId, @NonNull WatchNextProgram upgrade) {
        mContext.getContentResolver().update(
                TvContractCompat.buildWatchNextProgramUri(programId),
                upgrade.toContentValues(), null, null);
    }
}
