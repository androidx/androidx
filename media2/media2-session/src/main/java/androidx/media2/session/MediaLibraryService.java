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

package androidx.media2.session;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.media2.session.LibraryResult.RESULT_ERROR_NOT_SUPPORTED;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.media.MediaSessionManager.RemoteUserInfo;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.LibraryResult.ResultCode;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.concurrent.Executor;

/**
 * Base class for media library services, which is the service containing
 * {@link MediaLibrarySession}.
 * <p>
 * Media library services enable applications to browse media content provided by an application
 * and ask the application to start playing it. They may also be used to control content that
 * is already playing by way of a {@link MediaSession}.
 * <p>
 * When extending this class, also add the following to your {@code AndroidManifest.xml}.
 * <pre>
 * &lt;service android:name="component_name_of_your_implementation" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="androidx.media2.session.MediaLibraryService" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/service&gt;</pre>
 * <p>
 * You may also declare <pre>android.media.browse.MediaBrowserService</pre> for compatibility with
 * {@link android.support.v4.media.MediaBrowserCompat}. This service can handle it automatically.
 *
 * @see MediaSessionService
 */
public abstract class MediaLibraryService extends MediaSessionService {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE = "androidx.media2.session.MediaLibraryService";

    /**
     * Session for the {@link MediaLibraryService}. Build this object with
     * {@link Builder} and return in {@link MediaSessionService#onGetSession(ControllerInfo)}.
     *
     * <h3 id="BackwardCompatibility">Backward compatibility with legacy media browser APIs</h3>
     * Media library session supports connection from both {@link MediaBrowser} and
     * {@link android.support.v4.media.MediaBrowserCompat}, but {@link ControllerInfo} may not be
     * precise. Here are current limitations with details.
     *
     * <table>
     * <tr><th>SDK version</th>
     *     <th>{@link ControllerInfo#getPackageName()}<br>for legacy browser</th>
     *     <th>{@link ControllerInfo#getUid()}<br>for legacy browser</th></tr>
     * <tr><td>{@code SDK_VERSION} &lt; 21</td>
     *     <td>Actual package name via {@link Context#getPackageName()}</td>
     *     <td>Actual UID</td></tr>
     * <tr><td>21 &ge; {@code SDK_VERSION} &lt; 28,<br>
     *         {@code MediaLibrarySessionCallback#onConnect} and<br>
     *         {@code MediaLibrarySessionCallback#onGetLibraryRoot}</td>
     *     <td>Actual package name via {@link Context#getPackageName()}</td>
     *     <td>Actual UID</td></tr>
     * <tr><td>21 &ge; {@code SDK_VERSION} &lt; 28, for other callbacks</td>
     *     <td>{@link RemoteUserInfo#LEGACY_CONTROLLER}</td>
     *     <td>Negative value</td></tr>
     * <tr><td>28 &ge; {@code SDK_VERSION}</td>
     *     <td>Actual package name via {@link Context#getPackageName()}</td>
     *     <td>Actual UID</td></tr>
     * </table>
     **/
    public static final class MediaLibrarySession extends MediaSession {
        private final boolean mThrowsWhenInvalidReturn;

        /**
         * Callback for the {@link MediaLibrarySession}.
         * <p>
         * When you return {@link LibraryResult} with media items,
         * items must have valid {@link MediaMetadata#METADATA_KEY_MEDIA_ID} and
         * specify {@link MediaMetadata#METADATA_KEY_BROWSABLE} and
         * {@link MediaMetadata#METADATA_KEY_PLAYABLE}.
         */
        public static class MediaLibrarySessionCallback extends MediaSession.SessionCallback {
            /**
             * Called to get the root information for browsing by a {@link MediaBrowser}.
             * <p>
             * To allow browsing media information, return the {@link LibraryResult} with the
             * {@link LibraryResult#RESULT_SUCCESS} and the root media item with the valid
             * {@link MediaMetadata#METADATA_KEY_MEDIA_ID media id}. The media id must be included
             * for the browser to get the children under it.
             * <p>
             * Interoperability: this callback may be called on the main thread, regardless of the
             * callback executor.
             *
             * @param session the session for this event
             * @param controller information of the controller requesting access to browse media.
             * @param params An optional library params of service-specific arguments to send
             *               to the media library service when connecting and retrieving the
             *               root id for browsing, or {@code null} if none.
             * @return a library result with the root media item with the id. A runtime exception
             *         will be thrown if an invalid result is returned.
             * @see SessionCommand#COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT
             * @see MediaMetadata#METADATA_KEY_MEDIA_ID
             * @see LibraryParams
             */
            @NonNull
            public LibraryResult onGetLibraryRoot(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @Nullable LibraryParams params) {
                return new LibraryResult(RESULT_ERROR_NOT_SUPPORTED);
            }

            /**
             * Called to get an item.
             * <p>
             * To allow getting the item, return the {@link LibraryResult} with the
             * {@link LibraryResult#RESULT_SUCCESS} and the media item.
             *
             * @param session the session for this event
             * @param controller controller
             * @param mediaId non-empty media id of the requested item
             * @return a library result with a media item with the id. A runtime exception
             *         will be thrown if an invalid result is returned.
             * @see SessionCommand#COMMAND_CODE_LIBRARY_GET_ITEM
             */
            @NonNull
            public LibraryResult onGetItem(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String mediaId) {
                return new LibraryResult(RESULT_ERROR_NOT_SUPPORTED);
            }

            /**
             * Called to get children of given parent id. Return the children here for the browser.
             * <p>
             * To allow getting the children, return the {@link LibraryResult} with the
             * {@link LibraryResult#RESULT_SUCCESS} and the list of media item. Return an empty
             * list for no children rather than using result code for error.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId non-empty parent id to get children
             * @param page page number. Starts from {@code 0}.
             * @param pageSize page size. Should be greater or equal to {@code 1}.
             * @param params library params
             * @return a library result with a list of media item with the id. A runtime exception
             *         will be thrown if an invalid result is returned.
             * @see SessionCommand#COMMAND_CODE_LIBRARY_GET_CHILDREN
             * @see LibraryParams
             */
            @NonNull
            public LibraryResult onGetChildren(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @IntRange(from = 0) int page, @IntRange(from = 1) int pageSize,
                    @Nullable LibraryParams params) {
                return new LibraryResult(RESULT_ERROR_NOT_SUPPORTED);
            }

            /**
             * Called when a controller subscribes to the parent.
             * <p>
             * It's your responsibility to keep subscriptions by your own and call
             * {@link MediaLibrarySession#notifyChildrenChanged(
             * ControllerInfo, String, int, LibraryParams)} when the parent is changed until it's
             * unsubscribed.
             * <p>
             * Interoperability: This will be called when
             * {@link android.support.v4.media.MediaBrowserCompat#subscribe} is called.
             * However, this won't be called when {@link MediaBrowser#subscribe} is called.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId non-empty parent id
             * @param params library params
             * @return result code
             * @see SessionCommand#COMMAND_CODE_LIBRARY_SUBSCRIBE
             * @see LibraryParams
             */
            @ResultCode
            public int onSubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @Nullable LibraryParams params) {
                return RESULT_ERROR_NOT_SUPPORTED;
            }

            /**
             * Called when a controller unsubscribes to the parent.
             * <p>
             * Interoperability: This wouldn't be called if {@link MediaBrowser#unsubscribe} is
             * called while works well with
             * {@link android.support.v4.media.MediaBrowserCompat#unsubscribe}.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId non-empty parent id
             * @return result code
             * @see SessionCommand#COMMAND_CODE_LIBRARY_UNSUBSCRIBE
             */
            @ResultCode
            public int onUnsubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId) {
                return RESULT_ERROR_NOT_SUPPORTED;
            }

            /**
             * Called when a controller requests search.
             * <p>
             * Return immediately with the result of the attempt to search with the query. Notify
             * the number of search result through
             * {@link #notifySearchResultChanged(ControllerInfo, String, int, LibraryParams)}.
             * {@link MediaBrowser} will ask the search result with the pagination later.
             *
             * @param session the session for this event
             * @param controller controller
             * @param query The non-empty search query sent from the media browser.
             *              It contains keywords separated by space.
             * @param params library params
             * @return result code
             * @see SessionCommand#COMMAND_CODE_LIBRARY_SEARCH
             * @see #notifySearchResultChanged(ControllerInfo, String, int, LibraryParams)
             * @see LibraryParams
             */
            @ResultCode
            public int onSearch(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String query,
                    @Nullable LibraryParams params) {
                return RESULT_ERROR_NOT_SUPPORTED;
            }

            /**
             * Called to get the search result.
             * <p>
             * To allow getting the search result, return the {@link LibraryResult} with the
             * {@link LibraryResult#RESULT_SUCCESS} and the list of media item. Return an empty
             * list for no search result rather than using result code for error.
             * <p>
             * This may be called with a query that hasn't called with {@link #onSearch}, especially
             * when {@link android.support.v4.media.MediaBrowserCompat#search} is used.
             *
             * @param session the session for this event
             * @param controller controller
             * @param query The non-empty search query which was previously sent through
             *              {@link #onSearch}.
             * @param page page number. Starts from {@code 0}.
             * @param pageSize page size. Should be greater or equal to {@code 1}.
             * @param params library params
             * @return a library result with a list of media item with the id. A runtime exception
             *         will be thrown if an invalid result is returned.
             * @see SessionCommand#COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT
             * @see LibraryParams
             */
            @NonNull
            public LibraryResult onGetSearchResult(
                    @NonNull MediaLibrarySession session, @NonNull ControllerInfo controller,
                    @NonNull String query, @IntRange(from = 0) int page,
                    @IntRange(from = 1) int pageSize, @Nullable LibraryParams params) {
                return new LibraryResult(RESULT_ERROR_NOT_SUPPORTED);
            }
        }

        /**
         * Builder for {@link MediaLibrarySession}.
         * <p>
         * Any incoming event from the {@link MediaController} will be handled on the callback
         * executor. If it's not set, {@link ContextCompat#getMainExecutor(Context)} will be used by
         * default.
         */
        // Override all methods just to show them with the type instead of generics in Javadoc.
        // This workarounds javadoc issue described in the MediaSession.BuilderBase.
        // Note: Don't override #setSessionCallback() because the callback can be set by the
        // constructor.
        public static final class Builder extends MediaSession.BuilderBase<MediaLibrarySession,
                Builder, MediaLibrarySessionCallback> {
            private boolean mThrowsWhenInvalidReturn = true;

            // Builder requires MediaLibraryService instead of Context just to ensure that the
            // builder can be only instantiated within the MediaLibraryService.
            // Ideally it's better to make it inner class of service to enforce, but it violates API
            // guideline that Builders should be the inner class of the building target.
            public Builder(@NonNull MediaLibraryService service,
                    @NonNull SessionPlayer player,
                    @NonNull Executor callbackExecutor,
                    @NonNull MediaLibrarySessionCallback callback) {
                super(service, player);
                setSessionCallback(callbackExecutor, callback);
            }

            @Override
            @NonNull
            public Builder setSessionActivity(@Nullable PendingIntent pi) {
                return super.setSessionActivity(pi);
            }

            @Override
            @NonNull
            public Builder setId(@NonNull String id) {
                return super.setId(id);
            }

            @Override
            @NonNull
            public Builder setExtras(@NonNull Bundle extras) {
                return super.setExtras(extras);
            }

            /**
             * Prevents session to be crashed when it returns any invalid return.
             *
             * @hide
             **/
            @RestrictTo(LIBRARY)
            @NonNull
            @VisibleForTesting
            public Builder setThrowsWhenInvalidReturn(boolean throwsWhenInvalidReturn) {
                mThrowsWhenInvalidReturn = throwsWhenInvalidReturn;
                return this;
            }

            @Override
            @NonNull
            public MediaLibrarySession build() {
                if (mCallbackExecutor == null) {
                    mCallbackExecutor = ContextCompat.getMainExecutor(mContext);
                }
                if (mCallback == null) {
                    mCallback = new MediaLibrarySession.MediaLibrarySessionCallback() {};
                }
                return new MediaLibrarySession(mContext, mId, mPlayer, mSessionActivity,
                        mCallbackExecutor, mCallback, mExtras, mThrowsWhenInvalidReturn);
            }
        }

        MediaLibrarySession(Context context, String id, SessionPlayer player,
                PendingIntent sessionActivity, Executor callbackExecutor,
                MediaSession.SessionCallback callback, Bundle tokenExtras,
                boolean throwsWhenInvalidReturn) {
            super(context, id, player, sessionActivity, callbackExecutor, callback, tokenExtras);
            mThrowsWhenInvalidReturn = throwsWhenInvalidReturn;
        }

        @Override
        MediaLibrarySessionImpl createImpl(Context context, String id, SessionPlayer player,
                PendingIntent sessionActivity, Executor callbackExecutor,
                MediaSession.SessionCallback callback, Bundle tokenExtras) {
            return new MediaLibrarySessionImplBase(this, context, id, player, sessionActivity,
                    callbackExecutor, callback, tokenExtras, mThrowsWhenInvalidReturn);
        }

        @Override
        MediaLibrarySessionImpl getImpl() {
            return (MediaLibrarySessionImpl) super.getImpl();
        }

        /**
         * Notifies the controller of the change in a parent's children.
         * <p>
         * If the controller hasn't subscribed to the parent, the API will do nothing.
         * <p>
         * Controllers will use {@link MediaBrowser#getChildren(String, int, int, LibraryParams)}
         * to get the list of children.
         *
         * @param controller controller to notify
         * @param parentId non-empty parent id with changes in its children
         * @param itemCount number of children.
         * @param params library params
         */
        public void notifyChildrenChanged(@NonNull ControllerInfo controller,
                @NonNull String parentId, @IntRange(from = 0) int itemCount,
                @Nullable LibraryParams params) {
            if (controller == null) {
                throw new NullPointerException("controller shouldn't be null");
            }
            if (parentId == null) {
                throw new NullPointerException("parentId shouldn't be null");
            } else if (TextUtils.isEmpty(parentId)) {
                throw new IllegalArgumentException("parentId shouldn't be empty");
            }
            if (itemCount < 0) {
                throw new IllegalArgumentException("itemCount shouldn't be negative");
            }
            getImpl().notifyChildrenChanged(controller, parentId, itemCount, params);
        }

        /**
         * Notifies all controllers that subscribed to the parent about change in the parent's
         * children, regardless of the library params supplied by
         * {@link MediaBrowser#subscribe(String, LibraryParams)}.
         *  @param parentId non-empty parent id
         * @param itemCount number of children
         * @param params library params
         */
        // This is for the backward compatibility.
        public void notifyChildrenChanged(@NonNull String parentId, int itemCount,
                @Nullable LibraryParams params) {
            if (TextUtils.isEmpty(parentId)) {
                throw new IllegalArgumentException("parentId shouldn't be empty");
            }
            if (itemCount < 0) {
                throw new IllegalArgumentException("itemCount shouldn't be negative");
            }
            getImpl().notifyChildrenChanged(parentId, itemCount, params);
        }

        /**
         * Notifies controller about change in the search result.
         *
         * @param controller controller to notify
         * @param query previously sent non-empty search query from the controller.
         * @param itemCount the number of items that have been found in the search.
         * @param params library params
         */
        public void notifySearchResultChanged(@NonNull ControllerInfo controller,
                @NonNull String query, @IntRange(from = 0) int itemCount,
                @Nullable LibraryParams params) {
            if (controller == null) {
                throw new NullPointerException("controller shouldn't be null");
            }
            if (query == null) {
                throw new NullPointerException("query shouldn't be null");
            } else if (TextUtils.isEmpty(query)) {
                throw new IllegalArgumentException("query shouldn't be empty");
            }
            if (itemCount < 0) {
                throw new IllegalArgumentException("itemCount shouldn't be negative");
            }
            getImpl().notifySearchResultChanged(controller, query, itemCount, params);
        }

        @Override
        @NonNull
        MediaLibrarySessionCallback getCallback() {
            return (MediaLibrarySessionCallback) super.getCallback();
        }

        interface MediaLibrarySessionImpl extends MediaSessionImpl {
            // LibrarySession methods
            void notifyChildrenChanged(
                    @NonNull String parentId, int itemCount, @Nullable LibraryParams params);
            void notifyChildrenChanged(@NonNull ControllerInfo controller,
                    @NonNull String parentId, int itemCount, @Nullable LibraryParams params);
            void notifySearchResultChanged(@NonNull ControllerInfo controller,
                    @NonNull String query, int itemCount, @Nullable LibraryParams params);

            // LibrarySession callback implementations called on the executors
            LibraryResult onGetLibraryRootOnExecutor(@NonNull ControllerInfo controller,
                    @Nullable LibraryParams params);
            LibraryResult onGetItemOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String mediaId);
            LibraryResult onGetChildrenOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String parentId, int page, int pageSize,
                    @Nullable LibraryParams params);
            int onSubscribeOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String parentId, @Nullable LibraryParams params);
            int onUnsubscribeOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String parentId);
            int onSearchOnExecutor(@NonNull ControllerInfo controller, @NonNull String query,
                    @Nullable LibraryParams params);
            LibraryResult onGetSearchResultOnExecutor(@NonNull ControllerInfo controller,
                    @NonNull String query, int page, int pageSize, @Nullable LibraryParams params);

            // Internally used methods - only changing return type
            @Override
            MediaLibrarySession getInstance();

            @Override
            MediaLibrarySessionCallback getCallback();
        }
    }

    @Override
    MediaSessionServiceImpl createImpl() {
        return new MediaLibraryServiceImplBase();
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return super.onBind(intent);
    }

    @Override
    @Nullable
    public abstract MediaLibrarySession onGetSession(@NonNull ControllerInfo controllerInfo);

    /**
     * Contains information that the library service needs to send to the client.
     * <p>
     * When the browser supplies {@link LibraryParams}, it's optional field when getting the media
     * item(s). The library session is recommended to do the best effort to provide such result.
     * It's not an error even when the library session didn't return such items.
     * <p>
     * The library params returned in the library session callback must include the information
     * about the returned media item(s).
     */
    @VersionedParcelize
    public static final class LibraryParams implements VersionedParcelable {
        @ParcelField(1)
        Bundle mBundle;

        // Types are intentionally Integer for future extension of the value with less effort.
        @ParcelField(2)
        int mRecent;
        @ParcelField(3)
        int mOffline;
        @ParcelField(4)
        int mSuggested;

        // WARNING: Adding a new ParcelField may break old library users (b/152830728)

        // For versioned parcelable.
        LibraryParams() {
            // no-op
        }

        @SuppressWarnings("WeakerAccess") /* synthetic access */
        LibraryParams(Bundle bundle, boolean recent, boolean offline, boolean suggested) {
            // Keeps the booleans in Integer type.
            // Types are intentionally Integer for future extension of the value with less effort.
            this(bundle,
                    convertToInteger(recent),
                    convertToInteger(offline),
                    convertToInteger(suggested));
        }

        private LibraryParams(Bundle bundle, int recent, int offline, int suggested) {
            mBundle = bundle;
            mRecent = recent;
            mOffline = offline;
            mSuggested = suggested;
        }

        private static int convertToInteger(boolean a) {
            return a ? 1 : 0;
        }

        private static boolean convertToBoolean(int a) {
            return a == 0 ? false : true;
        }

        /**
         * Returns {@code true} for recent media items.
         * <p>
         * When the browser supplies {@link LibraryParams} with the {@code true}, library
         * session is recommended to provide such media items. If so, the library session
         * implementation must return the params with the {@code true} as well. The list of
         * media items is considered ordered by relevance, first being the top suggestion.
         *
         * @return {@code true} for recent items. {@code false} otherwise.
         */
        public boolean isRecent() {
            return convertToBoolean(mRecent);
        }

        /**
         * Returns {@code true} for offline media items, which can be played without an internet
         * connection.
         * <p>
         * When the browser supplies {@link LibraryParams} with the {@code true}, library
         * session is recommended to provide such media items. If so, the library session
         * implementation must return the params with the {@code true} as well.
         *
         * @return {@code true} for offline items. {@code false} otherwise.
         **/
        public boolean isOffline() {
            return convertToBoolean(mOffline);
        }

        /**
         * Returns {@code true} for suggested media items.
         * <p>
         * When the browser supplies {@link LibraryParams} with the {@code true}, library
         * session is recommended to provide such media items. If so, the library session
         * implementation must return the params with the {@code true} as well. The list of
         * media items is considered ordered by relevance, first being the top suggestion.
         *
         * @return {@code true} for suggested items. {@code false} otherwise
         **/
        public boolean isSuggested() {
            return convertToBoolean(mSuggested);
        }

        /**
         * Gets the extras.
         * <p>
         * Extras are the private contract between browser and library session.
         */
        @Nullable
        public Bundle getExtras() {
            return mBundle;
        }

        /**
         * Builds a {@link LibraryParams}.
         */
        public static final class Builder {
            private boolean mRecent;
            private boolean mOffline;
            private boolean mSuggested;

            private Bundle mBundle;

            /**
             * Sets whether recently played media item.
             * <p>
             * When the browser supplies the {@link LibraryParams} with the {@code true}, library
             * session is recommended to provide such media items. If so, the library session
             * implementation must return the params with the {@code true} as well.
             *
             * @param recent {@code true} for recent items. {@code false} otherwise.
             * @return this builder
             */
            @NonNull
            public Builder setRecent(boolean recent) {
                mRecent = recent;
                return this;
            }

            /**
             * Sets whether offline media items, which can be played without an internet connection.
             * <p>
             * When the browser supplies {@link LibraryParams} with the {@code true}, library
             * session is recommended to provide such media items. If so, the library session
             * implementation must return the params with the {@code true} as well.
             *
             * @param offline {@code true} for offline items. {@code false} otherwise.
             * @return this builder
             */
            @NonNull
            public Builder setOffline(boolean offline) {
                mOffline = offline;
                return this;
            }

            /**
             * Sets whether suggested media items.
             * <p>
             * When the browser supplies {@link LibraryParams} with the {@code true}, library
             * session is recommended to provide such media items. If so, the library session
             * implementation must return the params with the {@code true} as well. The list of
             * media items is considered ordered by relevance, first being the top suggestion.
             *
             * @param suggested {@code true} for suggested items. {@code false} otherwise
             * @return this builder
             */
            @NonNull
            public Builder setSuggested(boolean suggested) {
                mSuggested = suggested;
                return this;
            }

            /**
             * Set a bundle of extras, that browser and library session can understand each other.
             *
             * @param extras The extras or null.
             * @return this builder
             */
            @NonNull
            public Builder setExtras(@Nullable Bundle extras) {
                mBundle = extras;
                return this;
            }

            /**
             * Builds a {@link LibraryParams}.
             *
             * @return new LibraryParams
             */
            @NonNull
            public LibraryParams build() {
                return new LibraryParams(mBundle, mRecent, mOffline, mSuggested);
            }
        }
    }
}
