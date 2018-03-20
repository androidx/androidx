/*
* Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.supportv4.media;

import static com.example.android.supportv4.media.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.example.android.supportv4.media.utils.MediaIDHelper.MEDIA_ID_ROOT;
import static com.example.android.supportv4.media.utils.MediaIDHelper.createBrowseCategoryMediaID;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.android.supportv4.R;
import com.example.android.supportv4.media.model.MusicProvider;
import com.example.android.supportv4.media.utils.CarHelper;
import com.example.android.supportv4.media.utils.MediaIDHelper;
import com.example.android.supportv4.media.utils.QueueHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class provides a MediaBrowser through a service. It exposes the media library to a browsing
 * client, through the onGetRoot and onLoadChildren methods. It also creates a MediaSession and
 * exposes it through its MediaSession.Token, which allows the client to create a MediaController
 * that connects to and send control commands to the MediaSession remotely. This is useful for
 * user interfaces that need to interact with your media session, like Android Auto. You can
 * (should) also use the same service from your app's UI, which gives a seamless playback
 * experience to the user.
 * <p/>
 * To implement a MediaBrowserService, you need to:
 * <p/>
 * <ul>
 * <p/>
 * <li> Extend {@link android.service.media.MediaBrowserService}, implementing the media browsing
 * related methods {@link android.service.media.MediaBrowserService#onGetRoot} and
 * {@link android.service.media.MediaBrowserService#onLoadChildren};
 * <li> In onCreate, start a new {@link android.media.session.MediaSession} and notify its parent
 * with the session's token {@link android.service.media.MediaBrowserService#setSessionToken};
 * <p/>
 * <li> Set a callback on the
 * {@link android.media.session.MediaSession#setCallback(android.media.session.MediaSession.Callback)}.
 * The callback will receive all the user's actions, like play, pause, etc;
 * <p/>
 * <li> Handle all the actual music playing using any method your app prefers (for example,
 * {@link android.media.MediaPlayer})
 * <p/>
 * <li> Update playbackState, "now playing" metadata and queue, using MediaSession proper methods
 * {@link android.media.session.MediaSession#setPlaybackState(android.media.session.PlaybackState)}
 * {@link android.media.session.MediaSession#setMetadata(android.media.MediaMetadata)} and
 * {@link android.media.session.MediaSession#setQueue(java.util.List)})
 * <p/>
 * <li> Declare and export the service in AndroidManifest with an intent receiver for the action
 * android.media.browse.MediaBrowserService
 * <p/>
 * </ul>
 * <p/>
 * To make your app compatible with Android Auto, you also need to:
 * <p/>
 * <ul>
 * <p/>
 * <li> Declare a meta-data tag in AndroidManifest.xml linking to a xml resource
 * with a &lt;automotiveApp&gt; root element. For a media app, this must include
 * an &lt;uses name="media"/&gt; element as a child.
 * For example, in AndroidManifest.xml:
 * &lt;meta-data android:name="com.google.android.gms.car.application"
 * android:resource="@xml/automotive_app_desc"/&gt;
 * And in res/values/automotive_app_desc.xml:
 * &lt;automotiveApp&gt;
 * &lt;uses name="media"/&gt;
 * &lt;/automotiveApp&gt;
 * <p/>
 * </ul>
 *
 * @see <a href="README.md">README.md</a> for more details.
 */

public class MediaBrowserServiceSupport extends MediaBrowserServiceCompat
        implements Playback.Callback {

    // The action of the incoming Intent indicating that it contains a command
    // to be executed (see {@link #onStartCommand})
    public static final String ACTION_CMD = "com.example.android.supportv4.media.ACTION_CMD";
    // The key in the extras of the incoming Intent indicating the command that
    // should be executed (see {@link #onStartCommand})
    public static final String CMD_NAME = "CMD_NAME";
    // A value of a CMD_NAME key in the extras of the incoming Intent that
    // indicates that the music playback should be paused (see {@link #onStartCommand})
    public static final String CMD_PAUSE = "CMD_PAUSE";
    // Log tag must be <= 23 characters, so truncate class name.
    private static final String TAG = "MediaBrowserService";
    // Action to thumbs up a media item
    private static final String CUSTOM_ACTION_THUMBS_UP =
            "com.example.android.supportv4.media.THUMBS_UP";
    // Delay stopSelf by using a handler.
    private static final int STOP_DELAY = 30000;

    // Music catalog manager
    private MusicProvider mMusicProvider;
    private MediaSessionCompat mSession;
    // "Now playing" queue:
    private List<MediaSessionCompat.QueueItem> mPlayingQueue;
    private int mCurrentIndexOnQueue;
    private MediaNotificationManager mMediaNotificationManager;
    // Indicates whether the service was started.
    private boolean mServiceStarted;
    private DelayedStopHandler mDelayedStopHandler = new DelayedStopHandler(this);
    private Playback mPlayback;
    private PackageValidator mPackageValidator;

    /*
     * (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mPlayingQueue = new ArrayList<>();
        mMusicProvider = new MusicProvider();
        mPackageValidator = new PackageValidator(this);

        // Start a new MediaSession
        mSession = new MediaSessionCompat(this, "MusicService");
        setSessionToken(mSession.getSessionToken());
        mSession.setCallback(new MediaSessionCallback());
        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mPlayback = new Playback(this, mMusicProvider);
        mPlayback.setState(PlaybackStateCompat.STATE_NONE);
        mPlayback.setCallback(this);
        mPlayback.start();

        Context context = getApplicationContext();
        Intent intent = new Intent(context, MediaBrowserSupport.class);
        PendingIntent pi = PendingIntent.getActivity(context, 99 /*request code*/,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);

        Bundle extras = new Bundle();
        CarHelper.setSlotReservationFlags(extras, true, true, true);
        mSession.setExtras(extras);

        updatePlaybackState(null);

        mMediaNotificationManager = new MediaNotificationManager(this);
    }

    /**
     * (non-Javadoc)
     *
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent startIntent, int flags, int startId) {
        if (startIntent != null) {
            String action = startIntent.getAction();
            if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
                MediaButtonReceiver.handleIntent(mSession, startIntent);
            } else if (ACTION_CMD.equals(action)) {
                if (CMD_PAUSE.equals(startIntent.getStringExtra(CMD_NAME))) {
                    if (mPlayback != null && mPlayback.isPlaying()) {
                        handlePauseRequest();
                    }
                }
            }
        }
        return START_STICKY;
    }

    /**
     * (non-Javadoc)
     *
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        // Service is being killed, so make sure we release our resources
        handleStopRequest(null);

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        // Always release the MediaSession to clean up resources
        // and notify associated MediaController(s).
        mSession.release();
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        Log.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName + "; clientUid="
                + clientUid + " ; rootHints=" + rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!mPackageValidator.isCallerAllowed(this, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            Log.w(TAG, "OnGetRoot: IGNORING request from untrusted package " + clientPackageName);
            return null;
        }
        //noinspection StatementWithEmptyBody
        if (CarHelper.isValidCarPackage(clientPackageName)) {
            // Optional: if your app needs to adapt ads, music library or anything else that
            // needs to run differently when connected to the car, this is where you should handle
            // it.
        }
        return new BrowserRoot(MEDIA_ID_ROOT, null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result) {
        onLoadChildren(parentMediaId, result, null);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result,
            final Bundle options) {
        if (!mMusicProvider.isInitialized()) {
            // Use result.detach to allow calling result.sendResult from another thread:
            result.detach();

            mMusicProvider.retrieveMediaAsync(new MusicProvider.Callback() {
                @Override
                public void onMusicCatalogReady(boolean success) {
                    if (success) {
                        loadChildrenImpl(parentMediaId, result, options);
                    } else {
                        updatePlaybackState(getString(R.string.error_no_metadata));
                        result.sendResult(Collections.<MediaItem>emptyList());
                    }
                }
            });
        } else {
            // If our music catalog is already loaded/cached, load them into result immediately
            loadChildrenImpl(parentMediaId, result, options);
        }
    }

    /**
     * Actual implementation of onLoadChildren that assumes that MusicProvider is already
     * initialized.
     */
    private void loadChildrenImpl(final String parentMediaId,
            final Result<List<MediaItem>> result, final Bundle options) {
        Log.d(TAG, "OnLoadChildren: parentMediaId=" + parentMediaId + ", options=" + options);

        int page = -1;
        int pageSize = -1;

        if (options != null && (options.containsKey(MediaBrowserCompat.EXTRA_PAGE)
                || options.containsKey(MediaBrowserCompat.EXTRA_PAGE_SIZE))) {
            page = options.getInt(MediaBrowserCompat.EXTRA_PAGE, -1);
            pageSize = options.getInt(MediaBrowserCompat.EXTRA_PAGE_SIZE, -1);

            if (page < 0 || pageSize < 1) {
                result.sendResult(new ArrayList<MediaItem>());
                return;
            }
        }

        int fromIndex = page == -1 ? 0 : page * pageSize;
        int toIndex = 0;

        List<MediaItem> mediaItems = new ArrayList<>();

        if (MEDIA_ID_ROOT.equals(parentMediaId)) {
            Log.d(TAG, "OnLoadChildren.ROOT");
            if (page <= 0) {
                mediaItems.add(new MediaItem(
                        new MediaDescriptionCompat.Builder()
                                .setMediaId(MEDIA_ID_MUSICS_BY_GENRE)
                                .setTitle(getString(R.string.browse_genres))
                                .setIconUri(Uri.parse("android.resource://" +
                                        "com.example.android.supportv4.media/drawable/ic_by_genre"))
                                .setSubtitle(getString(R.string.browse_genre_subtitle))
                                .build(), MediaItem.FLAG_BROWSABLE));
            }

        } else if (MEDIA_ID_MUSICS_BY_GENRE.equals(parentMediaId)) {
            Log.d(TAG, "OnLoadChildren.GENRES");

            List<String> genres = mMusicProvider.getGenres();
            toIndex = page == -1 ? genres.size() : Math.min(fromIndex + pageSize, genres.size());

            for (int i = fromIndex; i < toIndex; i++) {
                String genre = genres.get(i);
                MediaItem item = new MediaItem(
                        new MediaDescriptionCompat.Builder()
                                .setMediaId(createBrowseCategoryMediaID(MEDIA_ID_MUSICS_BY_GENRE,
                                        genre))
                                .setTitle(genre)
                                .setSubtitle(
                                        getString(R.string.browse_musics_by_genre_subtitle, genre))
                                .build(), MediaItem.FLAG_BROWSABLE
                );
                mediaItems.add(item);
            }

        } else if (parentMediaId.startsWith(MEDIA_ID_MUSICS_BY_GENRE)) {
            String genre = MediaIDHelper.getHierarchy(parentMediaId)[1];
            Log.d(TAG, "OnLoadChildren.SONGS_BY_GENRE  genre=" + genre);

            List<MediaMetadataCompat> tracks = mMusicProvider.getMusicsByGenre(genre);
            toIndex = page == -1 ? tracks.size() : Math.min(fromIndex + pageSize, tracks.size());

            for (int i = fromIndex; i < toIndex; i++) {
                MediaMetadataCompat track = tracks.get(i);

                // Since mediaMetadata fields are immutable, we need to create a copy, so we
                // can set a hierarchy-aware mediaID. We will need to know the media hierarchy
                // when we get a onPlayFromMusicID call, so we can create the proper queue based
                // on where the music was selected from (by artist, by genre, random, etc)
                String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                        track.getDescription().getMediaId(), MEDIA_ID_MUSICS_BY_GENRE, genre);
                MediaMetadataCompat trackCopy = new MediaMetadataCompat.Builder(track)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                        .build();
                MediaItem bItem = new MediaItem(
                        trackCopy.getDescription(), MediaItem.FLAG_PLAYABLE);
                mediaItems.add(bItem);
            }
        } else {
            Log.w(TAG, "Skipping unmatched parentMediaId: " + parentMediaId);
        }
        Log.d(TAG, "OnLoadChildren sending " + mediaItems.size() + " results for "
                + parentMediaId);
        result.sendResult(mediaItems);
    }

    private final class MediaSessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            Log.d(TAG, "play");

            if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
                mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
                mSession.setQueue(mPlayingQueue);
                mSession.setQueueTitle(getString(R.string.random_queue_title));
                // start playing from the beginning of the queue
                mCurrentIndexOnQueue = 0;
            }

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                handlePlayRequest();
            }
        }

        @Override
        public void onSkipToQueueItem(long queueId) {
            Log.d(TAG, "OnSkipToQueueItem:" + queueId);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // set the current index on queue from the music Id:
                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, queueId);
                // play the music
                handlePlayRequest();
            }
        }

        @Override
        public void onSeekTo(long position) {
            Log.d(TAG, "onSeekTo:" + position);
            mPlayback.seekTo((int) position);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Log.d(TAG, "playFromMediaId mediaId:" + mediaId + "  extras=" + extras);

            // The mediaId used here is not the unique musicId. This one comes from the
            // MediaBrowser, and is actually a "hierarchy-aware mediaID": a concatenation of
            // the hierarchy in MediaBrowser and the actual unique musicID. This is necessary
            // so we can build the correct playing queue, based on where the track was
            // selected from.
            mPlayingQueue = QueueHelper.getPlayingQueue(mediaId, mMusicProvider);
            mSession.setQueue(mPlayingQueue);
            String queueTitle = getString(R.string.browse_musics_by_genre_subtitle,
                    MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaId));
            mSession.setQueueTitle(queueTitle);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // set the current index on queue from the media Id:
                mCurrentIndexOnQueue = QueueHelper.getMusicIndexOnQueue(mPlayingQueue, mediaId);

                if (mCurrentIndexOnQueue < 0) {
                    Log.e(TAG, "playFromMediaId: media ID " + mediaId
                            + " could not be found on queue. Ignoring.");
                } else {
                    // play the music
                    handlePlayRequest();
                }
            }
        }

        @Override
        public void onPause() {
            Log.d(TAG, "pause. current state=" + mPlayback.getState());
            handlePauseRequest();
        }

        @Override
        public void onStop() {
            Log.d(TAG, "stop. current state=" + mPlayback.getState());
            handleStopRequest(null);
        }

        @Override
        public void onSkipToNext() {
            Log.d(TAG, "skipToNext");
            mCurrentIndexOnQueue++;
            if (mPlayingQueue != null && mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                // This sample's behavior: skipping to next when in last song returns to the
                // first song.
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest();
            } else {
                Log.e(TAG, "skipToNext: cannot skip to next. next Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onSkipToPrevious() {
            Log.d(TAG, "skipToPrevious");
            mCurrentIndexOnQueue--;
            if (mPlayingQueue != null && mCurrentIndexOnQueue < 0) {
                // This sample's behavior: skipping to previous when in first song restarts the
                // first song.
                mCurrentIndexOnQueue = 0;
            }
            if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
                handlePlayRequest();
            } else {
                Log.e(TAG, "skipToPrevious: cannot skip to previous. previous Index=" +
                        mCurrentIndexOnQueue + " queue length=" +
                        (mPlayingQueue == null ? "null" : mPlayingQueue.size()));
                handleStopRequest("Cannot skip");
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            if (CUSTOM_ACTION_THUMBS_UP.equals(action)) {
                Log.i(TAG, "onCustomAction: favorite for current track");
                MediaMetadataCompat track = getCurrentPlayingMusic();
                if (track != null) {
                    String musicId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
                    mMusicProvider.setFavorite(musicId, !mMusicProvider.isFavorite(musicId));
                }
                // playback state needs to be updated because the "Favorite" icon on the
                // custom action will change to reflect the new favorite state.
                updatePlaybackState(null);
            } else {
                Log.e(TAG, "Unsupported action: " + action);
            }
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            Log.d(TAG, "playFromSearch  query=" + query);

            if (TextUtils.isEmpty(query)) {
                // A generic search like "Play music" sends an empty query
                // and it's expected that we start playing something. What will be played depends
                // on the app: favorite playlist, "I'm feeling lucky", most recent, etc.
                mPlayingQueue = QueueHelper.getRandomQueue(mMusicProvider);
            } else {
                mPlayingQueue = QueueHelper.getPlayingQueueFromSearch(query, mMusicProvider);
            }

            Log.d(TAG, "playFromSearch  playqueue.length=" + mPlayingQueue.size());
            mSession.setQueue(mPlayingQueue);

            if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
                // immediately start playing from the beginning of the search results
                mCurrentIndexOnQueue = 0;

                handlePlayRequest();
            } else {
                // if nothing was found, we need to warn the user and stop playing
                handleStopRequest(getString(R.string.no_search_results));
            }
        }
    }

    /**
     * Handle a request to play music
     */
    private void handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState=" + mPlayback.getState());

        mDelayedStopHandler.removeCallbacksAndMessages(null);
        if (!mServiceStarted) {
            Log.v(TAG, "Starting service");
            // The MusicService needs to keep running even after the calling MediaBrowser
            // is disconnected. Call startService(Intent) and then stopSelf(..) when we no longer
            // need to play media.
            startService(new Intent(getApplicationContext(), MediaBrowserServiceSupport.class));
            mServiceStarted = true;
        }

        if (!mSession.isActive()) {
            mSession.setActive(true);
        }

        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            updateMetadata();
            mPlayback.play(mPlayingQueue.get(mCurrentIndexOnQueue));
        }
    }

    /**
     * Handle a request to pause music
     */
    private void handlePauseRequest() {
        Log.d(TAG, "handlePauseRequest: mState=" + mPlayback.getState());
        mPlayback.pause();
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);
    }

    /**
     * Handle a request to stop music
     */
    private void handleStopRequest(String withError) {
        Log.d(TAG, "handleStopRequest: mState=" + mPlayback.getState() + " error=" + withError);
        mPlayback.stop(true);
        // reset the delayed stop handler.
        mDelayedStopHandler.removeCallbacksAndMessages(null);
        mDelayedStopHandler.sendEmptyMessageDelayed(0, STOP_DELAY);

        updatePlaybackState(withError);

        // service is no longer necessary. Will be started again if needed.
        stopSelf();
        mServiceStarted = false;
    }

    private void updateMetadata() {
        if (!QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            Log.e(TAG, "Can't retrieve current metadata.");
            updatePlaybackState(getResources().getString(R.string.error_no_metadata));
            return;
        }
        MediaSessionCompat.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
        String musicId = MediaIDHelper.extractMusicIDFromMediaID(
                queueItem.getDescription().getMediaId());
        MediaMetadataCompat track = mMusicProvider.getMusic(musicId);
        final String trackId = track.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
        if (!musicId.equals(trackId)) {
            IllegalStateException e = new IllegalStateException("track ID should match musicId.");
            Log.e(TAG, "track ID should match musicId. musicId=" + musicId + " trackId=" + trackId
                    + " mediaId from queueItem=" + queueItem.getDescription().getMediaId()
                    + " title from queueItem=" + queueItem.getDescription().getTitle()
                    + " mediaId from track=" + track.getDescription().getMediaId()
                    + " title from track=" + track.getDescription().getTitle()
                    + " source.hashcode from track=" + track.getString(
                            MusicProvider.CUSTOM_METADATA_TRACK_SOURCE).hashCode(), e);
            throw e;
        }
        Log.d(TAG, "Updating metadata for MusicID= " + musicId);
        mSession.setMetadata(track);

        // Set the proper album artwork on the media session, so it can be shown in the
        // locked screen and in other places.
        if (track.getDescription().getIconBitmap() == null &&
                track.getDescription().getIconUri() != null) {
            String albumUri = track.getDescription().getIconUri().toString();
            AlbumArtCache.getInstance().fetch(albumUri, new AlbumArtCache.FetchListener() {
                @Override
                public void onFetched(String artUrl, Bitmap bitmap, Bitmap icon) {
                    MediaSessionCompat.QueueItem queueItem = mPlayingQueue.get(mCurrentIndexOnQueue);
                    MediaMetadataCompat track = mMusicProvider.getMusic(trackId);
                    track = new MediaMetadataCompat.Builder(track)
                            // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used,
                            // for example, on the lockscreen background when the media session is
                            // active.
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, bitmap)
                            // set small version of the album art in the DISPLAY_ICON. This is used
                            // on the MediaDescription and thus it should be small to be serialized
                            // if necessary.
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
                            .build();

                    mMusicProvider.updateMusic(trackId, track);

                    // If we are still playing the same music
                    String currentPlayingId = MediaIDHelper.extractMusicIDFromMediaID(
                            queueItem.getDescription().getMediaId());
                    if (trackId.equals(currentPlayingId)) {
                        mSession.setMetadata(track);
                    }
                }
            });
        }
    }

    /**
     * Update the current media player state, optionally showing an error message.
     *
     * @param error if not null, error message to present to the user.
     */
    private void updatePlaybackState(String error) {
        Log.d(TAG, "updatePlaybackState, playback state=" + mPlayback.getState());
        long position = PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN;
        if (mPlayback != null && mPlayback.isConnected()) {
            position = mPlayback.getCurrentStreamPosition();
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions());

        setCustomAction(stateBuilder);
        int state = mPlayback.getState();

        // If there is an error message, send it to the playback state:
        if (error != null) {
            // Error states are really only supposed to be used for errors that cause playback to
            // stop unexpectedly and persist until the user takes action to fix it.
            stateBuilder.setErrorMessage(error);
            state = PlaybackStateCompat.STATE_ERROR;
        }
        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime());

        // Set the activeQueueItemId if the current index is valid.
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            stateBuilder.setActiveQueueItemId(item.getQueueId());
        }

        mSession.setPlaybackState(stateBuilder.build());

        if (state == PlaybackStateCompat.STATE_PLAYING
                || state == PlaybackStateCompat.STATE_PAUSED) {
            mMediaNotificationManager.startNotification();
        }
    }

    private void setCustomAction(PlaybackStateCompat.Builder stateBuilder) {
        MediaMetadataCompat currentMusic = getCurrentPlayingMusic();
        if (currentMusic != null) {
            // Set appropriate "Favorite" icon on Custom action:
            String musicId = currentMusic.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
            int favoriteIcon = R.drawable.ic_star_off;
            if (mMusicProvider.isFavorite(musicId)) {
                favoriteIcon = R.drawable.ic_star_on;
            }
            Log.d(TAG, "updatePlaybackState, setting Favorite custom action of music "
                    + musicId + " current favorite=" + mMusicProvider.isFavorite(musicId));
            stateBuilder.addCustomAction(CUSTOM_ACTION_THUMBS_UP, getString(R.string.favorite),
                    favoriteIcon);
        }
    }

    private long getAvailableActions() {
        long actions = PlaybackStateCompat.ACTION_PLAY
                | PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                | PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH;
        if (mPlayingQueue == null || mPlayingQueue.isEmpty()) {
            return actions;
        }
        if (mPlayback.isPlaying()) {
            actions |= PlaybackStateCompat.ACTION_PAUSE;
        }
        if (mCurrentIndexOnQueue > 0) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS;
        }
        if (mCurrentIndexOnQueue < mPlayingQueue.size() - 1) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT;
        }
        return actions;
    }

    private MediaMetadataCompat getCurrentPlayingMusic() {
        if (QueueHelper.isIndexPlayable(mCurrentIndexOnQueue, mPlayingQueue)) {
            MediaSessionCompat.QueueItem item = mPlayingQueue.get(mCurrentIndexOnQueue);
            if (item != null) {
                Log.d(TAG, "getCurrentPlayingMusic for musicId="
                        + item.getDescription().getMediaId());
                return mMusicProvider.getMusic(
                        MediaIDHelper
                                .extractMusicIDFromMediaID(item.getDescription().getMediaId()));
            }
        }
        return null;
    }

    /**
     * Implementation of the Playback.Callback interface
     */
    @Override
    public void onCompletion() {
        // The media player finished playing the current song, so we go ahead
        // and start the next.
        if (mPlayingQueue != null && !mPlayingQueue.isEmpty()) {
            // In this sample, we restart the playing queue when it gets to the end:
            mCurrentIndexOnQueue++;
            if (mCurrentIndexOnQueue >= mPlayingQueue.size()) {
                mCurrentIndexOnQueue = 0;
            }
            handlePlayRequest();
        } else {
            // If there is nothing to play, we stop and release the resources:
            handleStopRequest(null);
        }
    }

    @Override
    public void onPlaybackStatusChanged(int state) {
        updatePlaybackState(null);
    }

    @Override
    public void onError(String error) {
        updatePlaybackState(error);
    }

    /**
     * A simple handler that stops the service if playback is not active (playing)
     */
    private static class DelayedStopHandler extends Handler {
        private final WeakReference<MediaBrowserServiceSupport> mWeakReference;

        private DelayedStopHandler(MediaBrowserServiceSupport service) {
            mWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            MediaBrowserServiceSupport service = mWeakReference.get();
            if (service != null && service.mPlayback != null) {
                if (service.mPlayback.isPlaying()) {
                    Log.d(TAG, "Ignoring delayed stop since the media player is in use.");
                    return;
                }
                Log.d(TAG, "Stopping service with delay handler.");
                service.stopSelf();
                service.mServiceStarted = false;
            }
        }
    }
}
