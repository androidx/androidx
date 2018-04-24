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

package androidx.media;

import static android.support.v4.media.MediaBrowserCompat.EXTRA_PAGE;
import static android.support.v4.media.MediaBrowserCompat.EXTRA_PAGE_SIZE;

import static androidx.media.MediaConstants2.ARGUMENT_EXTRAS;
import static androidx.media.MediaConstants2.ARGUMENT_PAGE;
import static androidx.media.MediaConstants2.ARGUMENT_PAGE_SIZE;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.media.MediaBrowserCompat.MediaItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media.MediaLibraryService2.MediaLibrarySession.Builder;
import androidx.media.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionCallback;
import androidx.media.MediaSession2.ControllerInfo;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Base class for media library services.
 * <p>
 * Media library services enable applications to browse media content provided by an application
 * and ask the application to start playing it. They may also be used to control content that
 * is already playing by way of a {@link MediaSession2}.
 * <p>
 * When extending this class, also add the following to your {@code AndroidManifest.xml}.
 * <pre>
 * &lt;service android:name="component_name_of_your_implementation" &gt;
 *   &lt;intent-filter&gt;
 *     &lt;action android:name="android.media.MediaLibraryService2" /&gt;
 *   &lt;/intent-filter&gt;
 * &lt;/service&gt;</pre>
 * <p>
 * The {@link MediaLibraryService2} class derives from {@link MediaSessionService2}. IDs shouldn't
 * be shared between the {@link MediaSessionService2} and {@link MediaSession2}. By
 * default, an empty string will be used for ID of the service. If you want to specify an ID,
 * declare metadata in the manifest as follows.
 *
 * @see MediaSessionService2
 */
public abstract class MediaLibraryService2 extends MediaSessionService2 {
    /**
     * This is the interface name that a service implementing a session service should say that it
     * support -- that is, this is the action it uses for its intent filter.
     */
    public static final String SERVICE_INTERFACE = "android.media.MediaLibraryService2";

    /**
     * Session for the {@link MediaLibraryService2}. Build this object with
     * {@link Builder} and return in {@link #onCreateSession(String)}.
     */
    public static final class MediaLibrarySession extends MediaSession2 {
        /**
         * Callback for the {@link MediaLibrarySession}.
         */
        public static class MediaLibrarySessionCallback extends MediaSession2.SessionCallback {
            /**
             * Called to get the root information for browsing by a particular client.
             * <p>
             * The implementation should verify that the client package has permission
             * to access browse media information before returning the root id; it
             * should return null if the client is not allowed to access this
             * information.
             * <p>
             * Note: this callback may be called on the main thread, regardless of the callback
             * executor.
             *
             * @param session the session for this event
             * @param controllerInfo information of the controller requesting access to browse
             *                       media.
             * @param extras An optional bundle of service-specific arguments to send
             * to the media library service when connecting and retrieving the
             * root id for browsing, or null if none. The contents of this
             * bundle may affect the information returned when browsing.
             * @return The {@link LibraryRoot} for accessing this app's content or null.
             * @see LibraryRoot#EXTRA_RECENT
             * @see LibraryRoot#EXTRA_OFFLINE
             * @see LibraryRoot#EXTRA_SUGGESTED
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT
             */
            public @Nullable LibraryRoot onGetLibraryRoot(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controllerInfo, @Nullable Bundle extras) {
                return null;
            }

            /**
             * Called to get an item. Return result here for the browser.
             * <p>
             * Return {@code null} for no result or error.
             *
             * @param session the session for this event
             * @param mediaId item id to get media item.
             * @return a media item. {@code null} for no result or error.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_ITEM
             */
            public @Nullable MediaItem2 onGetItem(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controllerInfo, @NonNull String mediaId) {
                return null;
            }

            /**
             * Called to get children of given parent id. Return the children here for the browser.
             * <p>
             * Return an empty list for no children, and return {@code null} for the error.
             *
             * @param session the session for this event
             * @param parentId parent id to get children
             * @param page number of page
             * @param pageSize size of the page
             * @param extras extra bundle
             * @return list of children. Can be {@code null}.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_CHILDREN
             */
            public @Nullable List<MediaItem2> onGetChildren(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId, int page,
                    int pageSize, @Nullable Bundle extras) {
                return null;
            }

            /**
             * Called when a controller subscribes to the parent.
             * <p>
             * It's your responsibility to keep subscriptions by your own and call
             * {@link MediaLibrarySession#notifyChildrenChanged(ControllerInfo, String, int, Bundle)}
             * when the parent is changed.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId parent id
             * @param extras extra bundle
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_SUBSCRIBE
             */
            public void onSubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId,
                    @Nullable Bundle extras) {
            }

            /**
             * Called when a controller unsubscribes to the parent.
             *
             * @param session the session for this event
             * @param controller controller
             * @param parentId parent id
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_UNSUBSCRIBE
             */
            public void onUnsubscribe(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controller, @NonNull String parentId) {
            }

            /**
             * Called when a controller requests search.
             *
             * @param session the session for this event
             * @param query The search query sent from the media browser. It contains keywords
             *              separated by space.
             * @param extras The bundle of service-specific arguments sent from the media browser.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_SEARCH
             */
            public void onSearch(@NonNull MediaLibrarySession session,
                    @NonNull ControllerInfo controllerInfo, @NonNull String query,
                    @Nullable Bundle extras) {
            }

            /**
             * Called to get the search result. Return search result here for the browser which has
             * requested search previously.
             * <p>
             * Return an empty list for no search result, and return {@code null} for the error.
             *
             * @param session the session for this event
             * @param controllerInfo Information of the controller requesting the search result.
             * @param query The search query which was previously sent through
             *              {@link #onSearch(MediaLibrarySession, ControllerInfo, String, Bundle)}.
             * @param page page number. Starts from {@code 1}.
             * @param pageSize page size. Should be greater or equal to {@code 1}.
             * @param extras The bundle of service-specific arguments sent from the media browser.
             * @return search result. {@code null} for error.
             * @see SessionCommand2#COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT
             */
            public @Nullable List<MediaItem2> onGetSearchResult(
                    @NonNull MediaLibrarySession session, @NonNull ControllerInfo controllerInfo,
                    @NonNull String query, int page, int pageSize, @Nullable Bundle extras) {
                return null;
            }
        }

        /**
         * Builder for {@link MediaLibrarySession}.
         */
        // Override all methods just to show them with the type instead of generics in Javadoc.
        // This workarounds javadoc issue described in the MediaSession2.BuilderBase.
        // Note: Don't override #setSessionCallback() because the callback can be set by the
        // constructor.
        public static final class Builder extends MediaSession2.BuilderBase<MediaLibrarySession,
                Builder, MediaLibrarySessionCallback> {
            private MediaLibrarySessionImplBase.Builder mImpl;

            // Builder requires MediaLibraryService2 instead of Context just to ensure that the
            // builder can be only instantiated within the MediaLibraryService2.
            // Ideally it's better to make it inner class of service to enforce, it violates API
            // guideline that Builders should be the inner class of the building target.
            public Builder(@NonNull MediaLibraryService2 service,
                    @NonNull Executor callbackExecutor,
                    @NonNull MediaLibrarySessionCallback callback) {
                super(service);
                mImpl = new MediaLibrarySessionImplBase.Builder(service);
                setImpl(mImpl);
                setSessionCallback(callbackExecutor, callback);
            }

            @Override
            public @NonNull Builder setPlayer(@NonNull MediaPlayerInterface player) {
                return super.setPlayer(player);
            }

            @Override
            public @NonNull Builder setPlaylistAgent(@NonNull MediaPlaylistAgent playlistAgent) {
                return super.setPlaylistAgent(playlistAgent);
            }

            @Override
            public @NonNull Builder setVolumeProvider(
                    @Nullable VolumeProviderCompat volumeProvider) {
                return super.setVolumeProvider(volumeProvider);
            }

            @Override
            public @NonNull Builder setSessionActivity(@Nullable PendingIntent pi) {
                return super.setSessionActivity(pi);
            }

            @Override
            public @NonNull Builder setId(@NonNull String id) {
                return super.setId(id);
            }

            @Override
            public @NonNull MediaLibrarySession build() {
                return super.build();
            }
        }

        MediaLibrarySession(SupportLibraryImpl impl) {
            super(impl);
        }

        /**
         * Notify the controller of the change in a parent's children.
         * <p>
         * If the controller hasn't subscribed to the parent, the API will do nothing.
         * <p>
         * Controllers will use {@link MediaBrowser2#getChildren(String, int, int, Bundle)} to get
         * the list of children.
         *
         * @param controller controller to notify
         * @param parentId parent id with changes in its children
         * @param itemCount number of children.
         * @param extras extra information from session to controller
         */
        public void notifyChildrenChanged(@NonNull ControllerInfo controller,
                @NonNull String parentId, int itemCount, @Nullable Bundle extras) {
            List<MediaSessionManager.RemoteUserInfo> subscribingBrowsers =
                    getServiceCompat().getSubscribingBrowsers(parentId);
            getImpl().notifyChildrenChanged(controller, parentId, itemCount, extras,
                    subscribingBrowsers);
        }

        /**
         * Notify all controllers that subscribed to the parent about change in the parent's
         * children, regardless of the extra bundle supplied by
         * {@link MediaBrowser2#subscribe(String, Bundle)}.
         *
         * @param parentId parent id
         * @param itemCount number of children
         * @param extras extra information from session to controller
         */
        // This is for the backward compatibility.
        public void notifyChildrenChanged(@NonNull String parentId, int itemCount,
                @Nullable Bundle extras) {
            if (extras == null) {
                getServiceCompat().notifyChildrenChanged(parentId);
            } else {
                getServiceCompat().notifyChildrenChanged(parentId, extras);
            }
        }

        /**
         * Notify controller about change in the search result.
         *
         * @param controller controller to notify
         * @param query previously sent search query from the controller.
         * @param itemCount the number of items that have been found in the search.
         * @param extras extra bundle
         */
        public void notifySearchResultChanged(@NonNull ControllerInfo controller,
                @NonNull String query, int itemCount, @Nullable Bundle extras) {
            getImpl().notifySearchResultChanged(controller, query, itemCount, extras);
        }

        private MediaLibraryService2 getService() {
            return (MediaLibraryService2) getContext();
        }

        private MediaBrowserServiceCompat getServiceCompat() {
            return getService().getServiceCompat();
        }

        @Override
        MediaLibrarySessionCallback getCallback() {
            return (MediaLibrarySessionCallback) super.getCallback();
        }
    }

    @Override
    MediaBrowserServiceCompat createBrowserServiceCompat() {
        return new MyBrowserService();
    }

    @Override
    int getSessionType() {
        return SessionToken2.TYPE_LIBRARY_SERVICE;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        MediaSession2 session = getSession();
        if (!(session instanceof MediaLibrarySession)) {
            throw new RuntimeException("Expected MediaLibrarySession, but returned MediaSession2");
        }
    }

    private MediaLibrarySession getLibrarySession() {
        return (MediaLibrarySession) getSession();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    /**
     * Called when another app requested to start this service.
     * <p>
     * Library service will accept or reject the connection with the
     * {@link MediaLibrarySessionCallback} in the created session.
     * <p>
     * Service wouldn't run if {@code null} is returned or session's ID doesn't match with the
     * expected ID that you've specified through the AndroidManifest.xml.
     * <p>
     * This method will be called on the main thread.
     *
     * @param sessionId session id written in the AndroidManifest.xml.
     * @return a new library session
     * @see Builder
     * @see #getSession()
     * @throws RuntimeException if returned session is invalid
     */
    @Override
    public @NonNull abstract MediaLibrarySession onCreateSession(String sessionId);

    /**
     * Contains information that the library service needs to send to the client when
     * {@link MediaBrowser2#getLibraryRoot(Bundle)} is called.
     */
    public static final class LibraryRoot {
        /**
         * The lookup key for a boolean that indicates whether the library service should return a
         * librar root for recently played media items.
         *
         * <p>When creating a media browser for a given media library service, this key can be
         * supplied as a root hint for retrieving media items that are recently played.
         * If the media library service can provide such media items, the implementation must return
         * the key in the root hint when
         * {@link MediaLibrarySessionCallback#onGetLibraryRoot}
         * is called back.
         *
         * <p>The root hint may contain multiple keys.
         *
         * @see #EXTRA_OFFLINE
         * @see #EXTRA_SUGGESTED
         */
        public static final String EXTRA_RECENT = "android.media.extra.RECENT";

        /**
         * The lookup key for a boolean that indicates whether the library service should return a
         * library root for offline media items.
         *
         * <p>When creating a media browser for a given media library service, this key can be
         * supplied as a root hint for retrieving media items that are can be played without an
         * internet connection.
         * If the media library service can provide such media items, the implementation must return
         * the key in the root hint when
         * {@link MediaLibrarySessionCallback#onGetLibraryRoot}
         * is called back.
         *
         * <p>The root hint may contain multiple keys.
         *
         * @see #EXTRA_RECENT
         * @see #EXTRA_SUGGESTED
         */
        public static final String EXTRA_OFFLINE = "android.media.extra.OFFLINE";

        /**
         * The lookup key for a boolean that indicates whether the library service should return a
         * library root for suggested media items.
         *
         * <p>When creating a media browser for a given media library service, this key can be
         * supplied as a root hint for retrieving the media items suggested by the media library
         * service. The list of media items is considered ordered by relevance, first being the top
         * suggestion.
         * If the media library service can provide such media items, the implementation must return
         * the key in the root hint when
         * {@link MediaLibrarySessionCallback#onGetLibraryRoot}
         * is called back.
         *
         * <p>The root hint may contain multiple keys.
         *
         * @see #EXTRA_RECENT
         * @see #EXTRA_OFFLINE
         */
        public static final String EXTRA_SUGGESTED = "android.media.extra.SUGGESTED";

        private final String mRootId;
        private final Bundle mExtras;

        //private final LibraryRootProvider mProvider;

        /**
         * Constructs a library root.
         * @param rootId The root id for browsing.
         * @param extras Any extras about the library service.
         */
        public LibraryRoot(@NonNull String rootId, @Nullable Bundle extras) {
            if (rootId == null) {
                throw new IllegalArgumentException("rootId shouldn't be null");
            }
            mRootId = rootId;
            mExtras = extras;
        }

        /**
         * Gets the root id for browsing.
         */
        public String getRootId() {
            return mRootId;
        }

        /**
         * Gets any extras about the library service.
         */
        public Bundle getExtras() {
            return mExtras;
        }
    }

    private class MyBrowserService extends MediaBrowserServiceCompat {
        @Override
        public BrowserRoot onGetRoot(String clientPackageName, int clientUid,
                final Bundle extras) {
            if (MediaUtils2.isDefaultLibraryRootHint(extras)) {
                // For connection request from the MediaController2. accept the connection from
                // here, and let MediaLibrarySession decide whether to accept or reject the
                // controller.
                return sDefaultBrowserRoot;
            }
            final ControllerInfo controller = getController();
            MediaLibrarySession session = getLibrarySession();
            // Call onGetLibraryRoot() directly instead of execute on the executor. Here's the
            // reason.
            // We need to return browser root here. So if we run the callback on the executor, we
            // should wait for the completion.
            // However, we cannot wait if the callback executor is the main executor, which posts
            // the runnable to the main thread's. In that case, since this onGetRoot() always runs
            // on the main thread, the posted runnable for calling onGetLibraryRoot() wouldn't run
            // in here. Even worse, we cannot know whether it would be run on the main thread or
            // not.
            // Because of the reason, just call onGetLibraryRoot directly here. onGetLibraryRoot()
            // has documentation that it may be called on the main thread.
            LibraryRoot libraryRoot = session.getCallback().onGetLibraryRoot(
                    session, controller, extras);
            if (libraryRoot == null) {
                return null;
            }
            return new BrowserRoot(libraryRoot.getRootId(), libraryRoot.getExtras());
        }

        @Override
        public void onLoadChildren(String parentId, Result<List<MediaItem>> result) {
            onLoadChildren(parentId, result, null);
        }

        @Override
        public void onLoadChildren(final String parentId, final Result<List<MediaItem>> result,
                final Bundle options) {
            result.detach();
            final ControllerInfo controller = getController();
            getLibrarySession().getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    if (options != null) {
                        options.setClassLoader(MediaLibraryService2.this.getClassLoader());
                        try {
                            int page = options.getInt(EXTRA_PAGE);
                            int pageSize = options.getInt(EXTRA_PAGE_SIZE);
                            if (page > 0 && pageSize > 0) {
                                // Requesting the list of children through pagination.
                                List<MediaItem2> children = getLibrarySession().getCallback()
                                        .onGetChildren(getLibrarySession(), controller, parentId,
                                                page, pageSize, options);
                                result.sendResult(MediaUtils2.fromMediaItem2List(children));
                                return;
                            } else if (options.containsKey(
                                    MediaBrowser2.MEDIA_BROWSER2_SUBSCRIBE)) {
                                // This onLoadChildren() was triggered by MediaBrowser2.subscribe().
                                options.remove(MediaBrowser2.MEDIA_BROWSER2_SUBSCRIBE);
                                getLibrarySession().getCallback().onSubscribe(getLibrarySession(),
                                        controller, parentId, options.getBundle(ARGUMENT_EXTRAS));
                                return;
                            }
                        } catch (BadParcelableException e) {
                            // pass-through.
                        }
                    }
                    List<MediaItem2> children = getLibrarySession().getCallback()
                            .onGetChildren(getLibrarySession(), controller, parentId,
                                    1 /* page */, Integer.MAX_VALUE /* pageSize*/,
                                    null /* extras */);
                    result.sendResult(MediaUtils2.fromMediaItem2List(children));
                }
            });
        }

        @Override
        public void onLoadItem(final String itemId, final Result<MediaItem> result) {
            result.detach();
            final ControllerInfo controller = getController();
            getLibrarySession().getCallbackExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    MediaItem2 item = getLibrarySession().getCallback().onGetItem(
                            getLibrarySession(), controller, itemId);
                    if (item == null) {
                        result.sendResult(null);
                    } else {
                        result.sendResult(MediaUtils2.createMediaItem(item));
                    }
                }
            });
        }

        @Override
        public void onSearch(final String query, final Bundle extras,
                final Result<List<MediaItem>> result) {
            result.detach();
            final ControllerInfo controller = getController();
            extras.setClassLoader(MediaLibraryService2.this.getClassLoader());
            try {
                final int page = extras.getInt(ARGUMENT_PAGE);
                final int pageSize = extras.getInt(ARGUMENT_PAGE_SIZE);
                if (!(page > 0 && pageSize > 0)) {
                    getLibrarySession().getCallbackExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            getLibrarySession().getCallback().onSearch(
                                    getLibrarySession(), controller, query, extras);
                        }
                    });
                } else {
                    getLibrarySession().getCallbackExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            List<MediaItem2> searchResult = getLibrarySession().getCallback()
                                    .onGetSearchResult(getLibrarySession(), controller, query,
                                            page, pageSize, extras);
                            if (searchResult == null) {
                                result.sendResult(null);
                                return;
                            }
                            result.sendResult(MediaUtils2.fromMediaItem2List(searchResult));
                        }
                    });
                }
            } catch (BadParcelableException e) {
                // Do nothing.
            }
        }

        @Override
        public void onCustomAction(String action, Bundle extras, Result<Bundle> result) {
            // No-op. Library session will handle the custom action.
        }

        private ControllerInfo getController() {
            MediaLibrarySession session = getLibrarySession();
            List<ControllerInfo> controllers = session.getConnectedControllers();

            MediaSessionManager.RemoteUserInfo info = getCurrentBrowserInfo();
            if (info == null) {
                return null;
            }

            for (int i = 0; i < controllers.size(); i++) {
                // Note: This cannot pick the right controller between two controllers in same
                // process.
                ControllerInfo controller = controllers.get(i);
                if (controller.getPackageName().equals(info.getPackageName())
                        && controller.getUid() == info.getUid()) {
                    return controller;
                }
            }
            return null;
        }
    }
}
