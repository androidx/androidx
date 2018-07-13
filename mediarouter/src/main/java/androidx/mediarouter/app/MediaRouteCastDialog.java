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

package androidx.mediarouter.app;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.util.ObjectsCompat;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.palette.graphics.Palette;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;

/**
 * This class implements the route cast dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to dynamically control or disconnect from the
 * currently selected route.
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class MediaRouteCastDialog extends AppCompatDialog {
    static final String TAG = "MediaRouteCastDialog";
    static final int CONNECTION_TIMEOUT_MILLIS = (int) TimeUnit.SECONDS.toMillis(30L);

    final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    final MediaRouter.RouteInfo mRoute;

    Context mContext;
    private boolean mCreated;
    private boolean mAttachedToWindow;

    private ImageButton mCloseButton;
    private Button mStopCastingButton;

    private RelativeLayout mMetadataLayout;
    ImageView mArtView;
    private TextView mTitleView;
    private TextView mSubtitleView;

    MediaControllerCompat mMediaController;
    MediaControllerCallback mControllerCallback;
    MediaDescriptionCompat mDescription;

    FetchArtTask mFetchArtTask;
    Bitmap mArtIconBitmap;
    Uri mArtIconUri;
    boolean mArtIconIsLoaded;
    Bitmap mArtIconLoadedBitmap;
    int mArtIconBackgroundColor;

    public MediaRouteCastDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteCastDialog(Context context, int theme) {
        super(context = MediaRouterThemeHelper.createThemedDialogContext(context, theme, false),
                MediaRouterThemeHelper.createThemedDialogStyle(context));
        mContext = getContext();

        mRouter = MediaRouter.getInstance(mContext);
        mCallback = new MediaRouterCallback();
        mRoute = mRouter.getSelectedRoute();

        mControllerCallback = new MediaControllerCallback();
        setMediaSession(mRouter.getMediaSessionToken());
    }

    /**
     * Set the session to use for metadata and transport controls. The dialog
     * will listen to changes on this session and update the UI automatically in
     * response to changes.
     *
     * @param sessionToken The token for the session to use.
     */
    private void setMediaSession(MediaSessionCompat.Token sessionToken) {
        if (mMediaController != null) {
            mMediaController.unregisterCallback(mControllerCallback);
            mMediaController = null;
        }
        if (sessionToken == null) {
            return;
        }
        if (!mAttachedToWindow) {
            return;
        }
        try {
            mMediaController = new MediaControllerCompat(mContext, sessionToken);
        } catch (RemoteException e) {
            Log.e(TAG, "Error creating media controller in setMediaSession.", e);
        }
        if (mMediaController != null) {
            mMediaController.registerCallback(mControllerCallback);
        }
        MediaMetadataCompat metadata = mMediaController == null ? null
                : mMediaController.getMetadata();
        mDescription = metadata == null ? null : metadata.getDescription();
        updateArtIconIfNeeded();
        update();
    }

    /**
     * Gets the session to use for metadata and transport controls.
     *
     * @return The token for the session to use or null if none.
     */
    public MediaSessionCompat.Token getMediaSession() {
        return mMediaController == null ? null : mMediaController.getSessionToken();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mr_cast_dialog);

        mCloseButton = findViewById(R.id.mr_cast_close_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mStopCastingButton = findViewById(R.id.mr_cast_stop_button);
        mStopCastingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRoute.isSelected()) {
                    mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED);
                }
                dismiss();
            }
        });
        mMetadataLayout = findViewById(R.id.mr_cast_meta);
        mArtView = findViewById(R.id.mr_cast_meta_art);
        mTitleView = findViewById(R.id.mr_cast_meta_title);
        mSubtitleView = findViewById(R.id.mr_cast_meta_subtitle);
        mCreated = true;
        updateLayout();
    }

    /**
     * Sets the width of the dialog. Also called when configuration changes.
     */
    // TODO: Support different size for tablets(use MediaRouteDialogHelper)
    void updateLayout() {
        // Set layout width and height to MATCH_PARENT to make full screen dialog
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        mArtIconBitmap = null;
        mArtIconUri = null;
        updateArtIconIfNeeded();
        update();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAttachedToWindow = true;

        mRouter.addCallback(MediaRouteSelector.EMPTY, mCallback,
                MediaRouter.CALLBACK_FLAG_UNFILTERED_EVENTS);
        setMediaSession(mRouter.getMediaSessionToken());
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;

        mRouter.removeCallback(mCallback);
        setMediaSession(null);
    }

    void update() {
        if (!mRoute.isSelected() || mRoute.isDefaultOrBluetooth()) {
            dismiss();
            return;
        }
        if (!mCreated) {
            return;
        }

        if (mArtIconIsLoaded) {
            if (isBitmapRecycled(mArtIconLoadedBitmap)) {
                Log.w(TAG, "Can't set artwork image with recycled bitmap: " + mArtIconLoadedBitmap);
            } else {
                mArtView.setImageBitmap(mArtIconLoadedBitmap);
                mArtView.setBackgroundColor(mArtIconBackgroundColor);
                mMetadataLayout.setBackgroundDrawable(
                        new BitmapDrawable(mArtIconLoadedBitmap));
            }
            clearLoadedBitmap();
        }
        updateMetadataLayout();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static boolean isBitmapRecycled(Bitmap bitmap) {
        return bitmap != null && bitmap.isRecycled();
    }

    void updateArtIconIfNeeded() {
        if (!isIconChanged()) {
            return;
        }
        if (mFetchArtTask != null) {
            mFetchArtTask.cancel(true);
        }
        mFetchArtTask = new FetchArtTask();
        mFetchArtTask.execute();
    }

    /**
     * Clear the bitmap loaded by FetchArtTask. Will be called after the loaded bitmaps are applied
     * to artwork, or no longer valid.
     */
    void clearLoadedBitmap() {
        mArtIconIsLoaded = false;
        mArtIconLoadedBitmap = null;
        mArtIconBackgroundColor = 0;
    }

    /**
     * Returns whether a new art image is different from an original art image. Compares
     * Bitmap objects first, and then compares URIs only if bitmap is unchanged with
     * a null value.
     */
    private boolean isIconChanged() {
        Bitmap newBitmap = mDescription == null ? null : mDescription.getIconBitmap();
        Uri newUri = mDescription == null ? null : mDescription.getIconUri();
        Bitmap oldBitmap = mFetchArtTask == null ? mArtIconBitmap : mFetchArtTask.getIconBitmap();
        Uri oldUri = mFetchArtTask == null ? mArtIconUri : mFetchArtTask.getIconUri();
        if (oldBitmap != newBitmap) {
            return true;
        } else if (oldBitmap == null && ObjectsCompat.equals(oldUri, newUri)) {
            return true;
        }
        return false;
    }

    private void updateMetadataLayout() {
        CharSequence title = mDescription == null ? null : mDescription.getTitle();
        boolean hasTitle = !TextUtils.isEmpty(title);

        CharSequence subtitle = mDescription == null ? null : mDescription.getSubtitle();
        boolean hasSubtitle = !TextUtils.isEmpty(title);

        if (hasTitle) {
            mTitleView.setText(title);
        }
        if (hasSubtitle) {
            mSubtitleView.setText(subtitle);
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            update();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            update();
        }
    }

    private final class MediaControllerCallback extends MediaControllerCompat.Callback {
        MediaControllerCallback() {
        }

        @Override
        public void onSessionDestroyed() {
            if (mMediaController != null) {
                mMediaController.unregisterCallback(mControllerCallback);
                mMediaController = null;
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mDescription = metadata == null ? null : metadata.getDescription();
            updateArtIconIfNeeded();
            update();
        }
    }

    private class FetchArtTask extends AsyncTask<Void, Void, Bitmap> {
        private final Bitmap mIconBitmap;
        private final Uri mIconUri;
        private int mBackgroundColor;
        private long mStartTimeMillis;

        FetchArtTask() {
            Bitmap bitmap = mDescription == null ? null : mDescription.getIconBitmap();
            if (isBitmapRecycled(bitmap)) {
                Log.w(TAG, "Can't fetch the given art bitmap because it's already recycled.");
                bitmap = null;
            }
            mIconBitmap = bitmap;
            mIconUri = mDescription == null ? null : mDescription.getIconUri();
        }

        public Bitmap getIconBitmap() {
            return mIconBitmap;
        }

        public Uri getIconUri() {
            return mIconUri;
        }

        @Override
        protected void onPreExecute() {
            mStartTimeMillis = SystemClock.uptimeMillis();
            clearLoadedBitmap();
        }

        @Override
        protected Bitmap doInBackground(Void... arg) {
            Bitmap art = null;
            if (mIconBitmap != null) {
                art = mIconBitmap;
            } else if (mIconUri != null) {
                InputStream stream = null;
                try {
                    if ((stream = openInputStreamByScheme(mIconUri)) == null) {
                        Log.w(TAG, "Unable to open: " + mIconUri);
                        return null;
                    }
                    // Query art size.
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(stream, null, options);
                    if (options.outWidth == 0 || options.outHeight == 0) {
                        return null;
                    }
                    // Rewind the stream in order to restart art decoding.
                    try {
                        stream.reset();
                    } catch (IOException e) {
                        // Failed to rewind the stream, try to reopen it.
                        stream.close();
                        if ((stream = openInputStreamByScheme(mIconUri)) == null) {
                            Log.w(TAG, "Unable to open: " + mIconUri);
                            return null;
                        }
                    }
                    // Calculate required size to decode the art and possibly resize it.
                    options.inJustDecodeBounds = false;
                    int reqHeight = mArtView.getHeight();
                    int ratio = options.outHeight / reqHeight;
                    options.inSampleSize = Math.max(1, Integer.highestOneBit(ratio));
                    if (isCancelled()) {
                        return null;
                    }
                    art = BitmapFactory.decodeStream(stream, null, options);
                } catch (IOException e) {
                    Log.w(TAG, "Unable to open: " + mIconUri, e);
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            if (isBitmapRecycled(art)) {
                Log.w(TAG, "Can't use recycled bitmap: " + art);
                return null;
            }
            if (art != null && art.getWidth() < art.getHeight()) {
                // Portrait art requires dominant color as background color.
                Palette palette = new Palette.Builder(art).maximumColorCount(1).generate();
                mBackgroundColor = palette.getSwatches().isEmpty()
                        ? 0 : palette.getSwatches().get(0).getRgb();
            }
            return art;
        }

        @Override
        protected void onPostExecute(Bitmap art) {
            mFetchArtTask = null;
            if (!ObjectsCompat.equals(mArtIconBitmap, mIconBitmap)
                    || !ObjectsCompat.equals(mArtIconUri, mIconUri)) {
                mArtIconBitmap = mIconBitmap;
                mArtIconLoadedBitmap = art;
                mArtIconUri = mIconUri;
                mArtIconBackgroundColor = mBackgroundColor;
                mArtIconIsLoaded = true;
                // Loaded bitmap will be applied on the next update
                update();
            }
        }

        private InputStream openInputStreamByScheme(Uri uri) throws IOException {
            String scheme = uri.getScheme().toLowerCase();
            InputStream stream = null;
            if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(scheme)
                    || ContentResolver.SCHEME_CONTENT.equals(scheme)
                    || ContentResolver.SCHEME_FILE.equals(scheme)) {
                stream = mContext.getContentResolver().openInputStream(uri);
            } else {
                URL url = new URL(uri.toString());
                URLConnection conn = url.openConnection();
                conn.setConnectTimeout(CONNECTION_TIMEOUT_MILLIS);
                conn.setReadTimeout(CONNECTION_TIMEOUT_MILLIS);
                stream = conn.getInputStream();
            }
            return (stream == null) ? null : new BufferedInputStream(stream);
        }
    }
}
