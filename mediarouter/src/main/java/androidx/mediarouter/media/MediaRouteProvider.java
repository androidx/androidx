/*
 * Copyright (C) 2013 The Android Open Source Project
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

package androidx.mediarouter.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;

/**
 * Media route providers are used to publish additional media routes for
 * use within an application.  Media route providers may also be declared
 * as a service to publish additional media routes to all applications
 * in the system.
 * <p>
 * The purpose of a media route provider is to discover media routes that satisfy
 * the criteria specified by the current {@link MediaRouteDiscoveryRequest} and publish a
 * {@link MediaRouteProviderDescriptor} with information about each route by calling
 * {@link #setDescriptor} to notify the currently registered {@link Callback}.
 * </p><p>
 * The provider should watch for changes to the discovery request by implementing
 * {@link #onDiscoveryRequestChanged} and updating the set of routes that it is
 * attempting to discover.  It should also handle route control requests such
 * as volume changes or {@link MediaControlIntent media control intents}
 * by implementing {@link #onCreateRouteController} to return a {@link RouteController}
 * for a particular route.
 * </p><p>
 * A media route provider may be used privately within the scope of a single
 * application process by calling {@link MediaRouter#addProvider MediaRouter.addProvider}
 * to add it to the local {@link MediaRouter}.  A media route provider may also be made
 * available globally to all applications by registering a {@link MediaRouteProviderService}
 * in the provider's manifest.  When the media route provider is registered
 * as a service, all applications that use the media router API will be able to
 * discover and used the provider's routes without having to install anything else.
 * </p><p>
 * This object must only be accessed on the main thread.
 * </p>
 */
public abstract class MediaRouteProvider {
    static final int MSG_DELIVER_DESCRIPTOR_CHANGED = 1;
    static final int MSG_DELIVER_DISCOVERY_REQUEST_CHANGED = 2;

    private final Context mContext;
    private final ProviderMetadata mMetadata;
    private final ProviderHandler mHandler = new ProviderHandler();

    private Callback mCallback;

    private MediaRouteDiscoveryRequest mDiscoveryRequest;
    private boolean mPendingDiscoveryRequestChange;

    private MediaRouteProviderDescriptor mDescriptor;
    private boolean mPendingDescriptorChange;

    /**
     * Creates a media route provider.
     *
     * @param context The context.
     */
    public MediaRouteProvider(@NonNull Context context) {
        this(context, null);
    }

    MediaRouteProvider(Context context, ProviderMetadata metadata) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        mContext = context;
        if (metadata == null) {
            mMetadata = new ProviderMetadata(new ComponentName(context, getClass()));
        } else {
            mMetadata = metadata;
        }
    }

    /**
     * Gets the context of the media route provider.
     */
    public final Context getContext() {
        return mContext;
    }

    /**
     * Gets the provider's handler which is associated with the main thread.
     */
    public final Handler getHandler() {
        return mHandler;
    }

    /**
     * Gets some metadata about the provider's implementation.
     */
    public final ProviderMetadata getMetadata() {
        return mMetadata;
    }

    /**
     * Sets a callback to invoke when the provider's descriptor changes.
     *
     * @param callback The callback to use, or null if none.
     */
    public final void setCallback(@Nullable Callback callback) {
        MediaRouter.checkCallingThread();
        mCallback = callback;
    }

    /**
     * Gets the current discovery request which informs the provider about the
     * kinds of routes to discover and whether to perform active scanning.
     *
     * @return The current discovery request, or null if no discovery is needed at this time.
     *
     * @see #onDiscoveryRequestChanged
     */
    @Nullable
    public final MediaRouteDiscoveryRequest getDiscoveryRequest() {
        return mDiscoveryRequest;
    }

    /**
     * Sets a discovery request to inform the provider about the kinds of
     * routes that its clients would like to discover and whether to perform active scanning.
     *
     * @param request The discovery request, or null if no discovery is needed at this time.
     *
     * @see #onDiscoveryRequestChanged
     */
    public final void setDiscoveryRequest(MediaRouteDiscoveryRequest request) {
        MediaRouter.checkCallingThread();

        if (ObjectsCompat.equals(mDiscoveryRequest, request)) {
            return;
        }

        mDiscoveryRequest = request;
        if (!mPendingDiscoveryRequestChange) {
            mPendingDiscoveryRequestChange = true;
            mHandler.sendEmptyMessage(MSG_DELIVER_DISCOVERY_REQUEST_CHANGED);
        }
    }

    void deliverDiscoveryRequestChanged() {
        mPendingDiscoveryRequestChange = false;
        onDiscoveryRequestChanged(mDiscoveryRequest);
    }

    /**
     * Called by the media router when the {@link MediaRouteDiscoveryRequest discovery request}
     * has changed.
     * <p>
     * Whenever an applications calls {@link MediaRouter#addCallback} to register
     * a callback, it also provides a selector to specify the kinds of routes that
     * it is interested in.  The media router combines all of these selectors together
     * to generate a {@link MediaRouteDiscoveryRequest} and notifies each provider when a change
     * occurs by calling {@link #setDiscoveryRequest} which posts a message to invoke
     * this method asynchronously.
     * </p><p>
     * The provider should examine the {@link MediaControlIntent media control categories}
     * in the discovery request's {@link MediaRouteSelector selector} to determine what
     * kinds of routes it should try to discover and whether it should perform active
     * or passive scans.  In many cases, the provider may be able to save power by
     * determining that the selector does not contain any categories that it supports
     * and it can therefore avoid performing any scans at all.
     * </p>
     *
     * @param request The new discovery request, or null if no discovery is needed at this time.
     *
     * @see MediaRouter#addCallback
     */
    public void onDiscoveryRequestChanged(@Nullable MediaRouteDiscoveryRequest request) {
    }

    /**
     * Gets the provider's descriptor.
     * <p>
     * The descriptor describes the state of the media route provider and
     * the routes that it publishes.  Watch for changes to the descriptor
     * by registering a {@link Callback callback} with {@link #setCallback}.
     * </p>
     *
     * @return The media route provider descriptor, or null if none.
     *
     * @see Callback#onDescriptorChanged
     */
    @Nullable
    public final MediaRouteProviderDescriptor getDescriptor() {
        return mDescriptor;
    }

    /**
     * Sets the provider's descriptor.
     * <p>
     * The provider must call this method to notify the currently registered
     * {@link Callback callback} about the change to the provider's descriptor.
     * </p>
     *
     * @param descriptor The updated route provider descriptor, or null if none.
     *
     * @see Callback#onDescriptorChanged
     */
    public final void setDescriptor(@Nullable MediaRouteProviderDescriptor descriptor) {
        MediaRouter.checkCallingThread();

        if (mDescriptor != descriptor) {
            mDescriptor = descriptor;
            if (!mPendingDescriptorChange) {
                mPendingDescriptorChange = true;
                mHandler.sendEmptyMessage(MSG_DELIVER_DESCRIPTOR_CHANGED);
            }
        }
    }

    void deliverDescriptorChanged() {
        mPendingDescriptorChange = false;

        if (mCallback != null) {
            mCallback.onDescriptorChanged(this, mDescriptor);
        }
    }

    /**
     * Called by the media router to obtain a route controller for a particular route.
     * <p>
     * The media router will invoke the {@link RouteController#onRelease} method of the route
     * controller when it is no longer needed to allow it to free its resources.
     * </p>
     *
     * @param routeId The unique id of the route.
     * @return The route controller.  Returns null if there is no such route or if the route
     * cannot be controlled using the route controller interface.
     */
    @Nullable
    public RouteController onCreateRouteController(@NonNull String routeId) {
        if (routeId == null) {
            throw new IllegalArgumentException("routeId cannot be null");
        }
        return null;
    }

    /**
     * Called by the media router to obtain a route controller for a particular route which is a
     * member of {@link MediaRouter.RouteGroup}.
     * <p>
     * The media router will invoke the {@link RouteController#onRelease} method of the route
     * controller when it is no longer needed to allow it to free its resources.
     * </p>
     *
     * @param routeId The unique id of the member route.
     * @param routeGroupId The unique id of the route group.
     * @return The route controller.  Returns null if there is no such route or if the route
     * cannot be controlled using the route controller interface.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Nullable
    public RouteController onCreateRouteController(@NonNull String routeId,
            @NonNull String routeGroupId) {
        if (routeId == null) {
            throw new IllegalArgumentException("routeId cannot be null");
        }
        if (routeGroupId == null) {
            throw new IllegalArgumentException("routeGroupId cannot be null");
        }
        return onCreateRouteController(routeId);
    }

    /**
     * Describes properties of the route provider's implementation.
     * <p>
     * This object is immutable once created.
     * </p>
     */
    public static final class ProviderMetadata {
        private final ComponentName mComponentName;

        ProviderMetadata(ComponentName componentName) {
            if (componentName == null) {
                throw new IllegalArgumentException("componentName must not be null");
            }
            mComponentName = componentName;
        }

        /**
         * Gets the provider's package name.
         */
        public String getPackageName() {
            return mComponentName.getPackageName();
        }

        /**
         * Gets the provider's component name.
         */
        public ComponentName getComponentName() {
            return mComponentName;
        }

        @Override
        public String toString() {
            return "ProviderMetadata{ componentName="
                    + mComponentName.flattenToShortString() + " }";
        }
    }

    /**
     * Provides control over a particular route.
     * <p>
     * The media router obtains a route controller for a route whenever it needs
     * to control a route.  When a route is selected, the media router invokes
     * the {@link #onSelect} method of its route controller.  While selected,
     * the media router may call other methods of the route controller to
     * request that it perform certain actions to the route.  When a route is
     * unselected, the media router invokes the {@link #onUnselect} method of its
     * route controller.  When the media route no longer needs the route controller
     * it will invoke the {@link #onRelease} method to allow the route controller
     * to free its resources.
     * </p><p>
     * There may be multiple route controllers simultaneously active for the
     * same route.  Each route controller will be released separately.
     * </p><p>
     * All operations on the route controller are asynchronous and
     * results are communicated via callbacks.
     * </p>
     */
    public static abstract class RouteController {
        /**
         * Releases the route controller, allowing it to free its resources.
         */
        public void onRelease() {
        }

        /**
         * Selects the route.
         */
        public void onSelect() {
        }

        /**
         * Unselects the route.
         */
        public void onUnselect() {
        }

        /**
         * Unselects the route and provides a reason. The default implementation
         * calls {@link #onUnselect()}.
         * <p>
         * The reason provided will be one of the following:
         * <ul>
         * <li>{@link MediaRouter#UNSELECT_REASON_UNKNOWN}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_DISCONNECTED}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_STOPPED}</li>
         * <li>{@link MediaRouter#UNSELECT_REASON_ROUTE_CHANGED}</li>
         * </ul>
         *
         * @param reason The reason for unselecting the route.
         */
        public void onUnselect(int reason) {
            onUnselect();
        }

        /**
         * Requests to set the volume of the route.
         *
         * @param volume The new volume value between 0 and {@link MediaRouteDescriptor#getVolumeMax}.
         */
        public void onSetVolume(int volume) {
        }

        /**
         * Requests an incremental volume update for the route.
         *
         * @param delta The delta to add to the current volume.
         */
        public void onUpdateVolume(int delta) {
        }

        /**
         * Performs a {@link MediaControlIntent media control} request
         * asynchronously on behalf of the route.
         *
         * @param intent A {@link MediaControlIntent media control intent}.
         * @param callback A {@link ControlRequestCallback} to invoke with the result
         * of the request, or null if no result is required.
         * @return True if the controller intends to handle the request and will
         * invoke the callback when finished.  False if the controller will not
         * handle the request and will not invoke the callback.
         *
         * @see MediaControlIntent
         */
        public boolean onControlRequest(Intent intent, @Nullable ControlRequestCallback callback) {
            return false;
        }
    }

    /**
     * Callback which is invoked when route information becomes available or changes.
     */
    public static abstract class Callback {
        /**
         * Called when information about a route provider and its routes changes.
         *
         * @param provider The media route provider that changed, never null.
         * @param descriptor The new media route provider descriptor, or null if none.
         */
        public void onDescriptorChanged(@NonNull MediaRouteProvider provider,
                @Nullable MediaRouteProviderDescriptor descriptor) {
        }
    }

    private final class ProviderHandler extends Handler {
        ProviderHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DELIVER_DESCRIPTOR_CHANGED:
                    deliverDescriptorChanged();
                    break;
                case MSG_DELIVER_DISCOVERY_REQUEST_CHANGED:
                    deliverDiscoveryRequestChanged();
                    break;
            }
        }
    }
}
