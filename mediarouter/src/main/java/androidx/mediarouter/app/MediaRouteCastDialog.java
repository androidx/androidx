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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.app.AppCompatDialog;
import androidx.core.util.ObjectsCompat;
import androidx.mediarouter.R;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.mediarouter.media.MediaRouter;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
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

    private static final int ITEM_TYPE_GROUP_VOLUME = 1;
    private static final int ITEM_TYPE_SELECTED_ROUTE = 2;

    final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;
    final MediaRouter.RouteInfo mRoute;
    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;

    Context mContext;
    private boolean mCreated;
    private boolean mAttachedToWindow;
    private RecyclerAdapter mAdapter;
    private RecyclerView mRecyclerView;
    VolumeChangeListener mVolumeChangeListener;
    int mVolumeSliderColor;

    private ImageButton mCloseButton;
    private Button mStopCastingButton;

    private RelativeLayout mMetadataLayout;
    private ImageView mArtView;
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

    /**
     * Gets the media route selector for filtering the routes that the user can select.
     *
     * @return The selector, never null.
     */
    @NonNull
    public MediaRouteSelector getRouteSelector() {
        return mSelector;
    }

    /**
     * Sets the media route selector for filtering the routes that the user can select.
     *
     * @param selector The selector, must not be null.
     */
    public void setRouteSelector(@NonNull MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        if (!mSelector.equals(selector)) {
            mSelector = selector;

            if (mAttachedToWindow) {
                mRouter.removeCallback(mCallback);
                mRouter.addCallback(selector, mCallback,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }
        }
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

        mAdapter = new RecyclerAdapter();
        mRecyclerView = findViewById(R.id.mr_cast_list);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

        mMetadataLayout = findViewById(R.id.mr_cast_meta);
        mArtView = findViewById(R.id.mr_cast_meta_art);
        mTitleView = findViewById(R.id.mr_cast_meta_title);
        mSubtitleView = findViewById(R.id.mr_cast_meta_subtitle);

        mVolumeChangeListener = new VolumeChangeListener();
        mVolumeSliderColor = MediaRouterThemeHelper.getControllerColor(mContext, 0);

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

        mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
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

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int getDesiredArtHeight(int originalWidth, int originalHeight) {
        return mArtView.getHeight();
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

    // TODO(111421478): Implement actual VolumeChangeListener
    private class VolumeChangeListener implements SeekBar.OnSeekBarChangeListener {
        VolumeChangeListener() {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }
    }

    private final class RecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final String TAG = "RecyclerAdapter";
        ArrayList<Item> mItems;

        private final LayoutInflater mInflater;

        private final Drawable mDefaultIcon;
        private final Drawable mTvIcon;
        private final Drawable mSpeakerIcon;
        private final Drawable mSpeakerGroupIcon;

        RecyclerAdapter() {
            mInflater = LayoutInflater.from(mContext);
            mDefaultIcon = MediaRouterThemeHelper.getDefaultDrawableIcon(mContext);
            mTvIcon = MediaRouterThemeHelper.getTvDrawableIcon(mContext);
            mSpeakerIcon = MediaRouterThemeHelper.getSpeakerDrawableIcon(mContext);
            mSpeakerGroupIcon = MediaRouterThemeHelper.getSpeakerGropuIcon(mContext);
            setItems();
        }

        // Create a list of items with mRoutes and add them to mItems
        void setItems() {
            mItems = new ArrayList<>();
            // Add Group Volume item only when currently casting on a group
            if (mRoute instanceof MediaRouter.RouteGroup) {
                mItems.add(new Item(mRoute, ITEM_TYPE_GROUP_VOLUME));
                List<MediaRouter.RouteInfo> routes = ((MediaRouter.RouteGroup) mRoute).getRoutes();

                for (MediaRouter.RouteInfo route: routes) {
                    mItems.add(new Item(route, ITEM_TYPE_SELECTED_ROUTE));
                }
            } else {
                mItems.add(new Item(mRoute, ITEM_TYPE_SELECTED_ROUTE));
            }
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;

            switch (viewType) {
                case ITEM_TYPE_GROUP_VOLUME:
                    view = mInflater.inflate(R.layout.mr_cast_group_volume, parent, false);
                    return new GroupVolumeViewHolder(view);
                case ITEM_TYPE_SELECTED_ROUTE:
                    view = mInflater.inflate(R.layout.mr_cast_active_route, parent, false);
                    return new SelectedRouteViewHolder(view);
                default:
                    Log.w(TAG, "Cannot create ViewHolder because of wrong view type");
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            Item item = getItem(position);

            switch (viewType) {
                case ITEM_TYPE_GROUP_VOLUME:
                    ((GroupVolumeViewHolder) holder).bindGroupVolumeView(item);
                    break;
                case ITEM_TYPE_SELECTED_ROUTE:
                    ((SelectedRouteViewHolder) holder).bindSelectedRouteViewHolder(item);
                    break;
                default:
                    Log.w(TAG, "Cannot bind item to ViewHolder because of wrong view type");
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        Drawable getIconDrawable(MediaRouter.RouteInfo route) {
            Uri iconUri = route.getIconUri();
            if (iconUri != null) {
                try {
                    InputStream is = mContext.getContentResolver().openInputStream(iconUri);
                    Drawable drawable = Drawable.createFromStream(is, null);
                    if (drawable != null) {
                        return drawable;
                    }
                } catch (IOException e) {
                    Log.w(TAG, "Failed to load " + iconUri, e);
                    // Falls back.
                }
            }
            return getDefaultIconDrawable(route);
        }

        private Drawable getDefaultIconDrawable(MediaRouter.RouteInfo route) {
            // If the type of the receiver device is specified, use it.
            switch (route.getDeviceType()) {
                case MediaRouter.RouteInfo.DEVICE_TYPE_TV:
                    return mTvIcon;
                case MediaRouter.RouteInfo.DEVICE_TYPE_SPEAKER:
                    return mSpeakerIcon;
            }

            // Otherwise, make the best guess based on other route information.
            if (route instanceof MediaRouter.RouteGroup) {
                // Only speakers can be grouped for now.
                return mSpeakerGroupIcon;
            }
            return mDefaultIcon;
        }

        @Override
        public int getItemViewType(int position) {
            return mItems.get(position).getType();
        }

        public Item getItem(int position) {
            return mItems.get(position);
        }

        /**
         * Item class contains information of section header(text of section header) and
         * route(text of route name, icon of route type)
         */
        private class Item {
            private final Object mData;
            private final int mType;

            Item(Object data, int type) {
                mData = data;
                mType = type;
            }

            public Object getData() {
                return mData;
            }

            public int getType() {
                return mType;
            }
        }

        // ViewHolder for route list item
        private class GroupVolumeViewHolder extends RecyclerView.ViewHolder {
            TextView mTextView;
            MediaRouteVolumeSlider mGroupVolumeSlider;

            GroupVolumeViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.mr_group_volume_route_name);
                mGroupVolumeSlider = itemView.findViewById(R.id.mr_group_volume_slider);
            }

            public void bindGroupVolumeView(Item item) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();

                mTextView.setText(route.getName().toUpperCase());
                mGroupVolumeSlider.setColor(mVolumeSliderColor);
                mGroupVolumeSlider.setTag(route);
                mGroupVolumeSlider.setProgress(mRoute.getVolume());
                mGroupVolumeSlider.setOnSeekBarChangeListener(mVolumeChangeListener);
            }
        }

        private class SelectedRouteViewHolder extends RecyclerView.ViewHolder {
            ImageView mImageView;
            TextView mTextView;
            CheckBox mCheckBox;
            MediaRouteVolumeSlider mRouteVolumeSlider;

            SelectedRouteViewHolder(View itemView) {
                super(itemView);
                mImageView = itemView.findViewById(R.id.mr_cast_route_icon);
                mTextView = itemView.findViewById(R.id.mr_cast_route_name);
                mCheckBox = itemView.findViewById(R.id.mr_cast_checkbox);
                mRouteVolumeSlider = itemView.findViewById(R.id.mr_cast_volume_slider);
            }

            public void bindSelectedRouteViewHolder(Item item) {
                MediaRouter.RouteInfo route = (MediaRouter.RouteInfo) item.getData();

                mImageView.setImageDrawable(getIconDrawable(route));
                mTextView.setText(route.getName());
                mCheckBox.setChecked(true);
                mRouteVolumeSlider.setColor(mVolumeSliderColor);
                mRouteVolumeSlider.setProgress(route.getVolume());
                mRouteVolumeSlider.setOnSeekBarChangeListener(mVolumeChangeListener);
            }
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        MediaRouterCallback() {
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            update();
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
                    int reqHeight = getDesiredArtHeight(options.outWidth, options.outHeight);
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
