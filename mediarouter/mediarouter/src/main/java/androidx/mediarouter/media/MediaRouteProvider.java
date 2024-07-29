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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.GuardedBy;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.mediarouter.media.MediaRouter.ControlRequestCallback;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;

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
 * A media route provider can support
 * {@link MediaRouteProviderDescriptor#supportsDynamicGroupRoute dynamic group} that
 * allows the user to add or remove a route from the selected route dynamically.
 * To control dynamic group, {@link DynamicGroupRouteController} returned by
 * {@link #onCreateDynamicGroupRouteController} is used instead of {@link RouteController}.
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
    @NonNull
    public final Context getContext() {
        return mContext;
    }

    /**
     * Gets the provider's handler which is associated with the main thread.
     */
    @NonNull
    public final Handler getHandler() {
        return mHandler;
    }

    /**
     * Gets some metadata about the provider's implementation.
     */
    @NonNull
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
    public final void setDiscoveryRequest(@Nullable MediaRouteDiscoveryRequest request) {
        MediaRouter.checkCallingThread();

        if (ObjectsCompat.equals(mDiscoveryRequest, request)) {
            return;
        }

        setDiscoveryRequestInternal(request);
    }

    final void setDiscoveryRequestInternal(@Nullable MediaRouteDiscoveryRequest request) {
        mDiscoveryRequest = request;
        if (!mPendingDiscoveryRequestChange) {
            mPendingDiscoveryRequestChange = true;
            mHandler.sendEmptyMessage(MSG_DELIVER_DISCOVERY_REQUEST_CHANGED);
        }
    }

    /* package */ final void deliverDiscoveryRequestChanged() {
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

    /* package */ final void deliverDescriptorChanged() {
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
     * member of a route group.
     * <p>
     * The media router will invoke the {@link RouteController#onRelease} method of the route
     * controller when it is no longer needed to allow it to free its resources.
     * </p>
     *
     * @param routeId The unique id of the member route.
     * @param routeGroupId The unique id of the route group.
     * @return The route controller.  Returns null if there is no such route or if the route
     * cannot be controlled using the route controller interface.
     */
    @RestrictTo(LIBRARY)
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
     * Creates a {@link DynamicGroupRouteController}.
     *
     * <p>It will be called from an app or {@link MediaRouter} when a single route or a single
     * static group is selected.
     *
     * @param initialMemberRouteId initially selected route's id.
     * @param controlHints the hints passed by the client application for creating the route
     *     controller, or {@code null} if the client has not provided control hints. The
     *     controlHints may be provided by {@link
     *     android.media.MediaRouter2.OnGetControllerHintsListener}.
     * @return {@link DynamicGroupRouteController}. Returns null if there is no such route or if the
     *     route cannot be controlled using the {@link DynamicGroupRouteController} interface.
     */
    @Nullable
    public DynamicGroupRouteController onCreateDynamicGroupRouteController(
            @NonNull String initialMemberRouteId, @Nullable Bundle controlHints) {
        return onCreateDynamicGroupRouteController(initialMemberRouteId);
    }

    /**
     * Creates a {@link DynamicGroupRouteController}.
     *
     * <p>It is equivalent to {@link #onCreateDynamicGroupRouteController(String, Bundle)}, except
     * it doesn't take {@code controlHints}.
     *
     * <p>This method is only called when the subclass doesn't implement {@link
     * #onCreateDynamicGroupRouteController(String, Bundle)}.
     *
     * @param initialMemberRouteId initially selected route's id.
     * @return {@link DynamicGroupRouteController}. Returns null if there is no such route or if the
     *     route cannot be controlled using the {@link DynamicGroupRouteController} interface.
     */
    @Nullable
    public DynamicGroupRouteController onCreateDynamicGroupRouteController(
            @NonNull String initialMemberRouteId) {
        if (initialMemberRouteId == null) {
            throw new IllegalArgumentException("initialMemberRouteId cannot be null.");
        }
        return null;
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
        @NonNull
        public String getPackageName() {
            return mComponentName.getPackageName();
        }

        /**
         * Gets the provider's component name.
         */
        @NonNull
        public ComponentName getComponentName() {
            return mComponentName;
        }

        @NonNull
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
     * unselected, the media router invokes the {@link #onUnselect(int)} method of its
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
         *
         * @deprecated Use {@link #onUnselect(int)} instead.
         */
        @Deprecated
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
        public void onUnselect(@MediaRouter.UnselectReason int reason) {
            onUnselect();
        }

        /**
         * Requests to set the volume of the route.
         *
         * @param volume The new volume value between 0 and
         * {@link MediaRouteDescriptor#getVolumeMax}.
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
        public boolean onControlRequest(@NonNull Intent intent,
                @Nullable ControlRequestCallback callback) {
            return false;
        }
    }

    /**
     * Provides control over a dynamic group route.
     * A dynamic group route is a group of routes such that a route can be added or removed
     * from the group by the user dynamically.
     */
    public abstract static class DynamicGroupRouteController extends RouteController {
        private final Object mLock = new Object();

        @GuardedBy("mLock")
        Executor mExecutor;
        @GuardedBy("mLock")
        OnDynamicRoutesChangedListener mListener;
        @GuardedBy("mLock")
        MediaRouteDescriptor mPendingGroupRoute;
        @GuardedBy("mLock")
        Collection<DynamicRouteDescriptor> mPendingRoutes;

        /**
         * Gets the title of the groupable routes section which will be shown to the user.
         * It is provided by {@link MediaRouteProvider}.
         * e.g. "Add a device."
         */
        @Nullable
        public String getGroupableSelectionTitle() {
            return null;
        }

        /**
         * Gets the title of the transferable routes section which will be shown to the user.
         * It is provided by {@link MediaRouteProvider}.
         * {@link MediaRouteProvider}.
         * e.g. "Play on group."
         */
        @Nullable
        public String getTransferableSectionTitle() {
            return null;
        }

        /**
         * Called when a user selects a new set of routes s/he wants the session to be played.
         */
        public abstract void onUpdateMemberRoutes(@Nullable List<String> routeIds);

        /**
         * Called when a user adds a route into the casting session.
         */
        public abstract void onAddMemberRoute(@NonNull String routeId);

        /**
         * Called when a user removes a route from casting session.
         */
        public abstract void onRemoveMemberRoute(@NonNull String routeId);

        /**
         * Called by {@link MediaRouter} to set the listener.
         */
        void setOnDynamicRoutesChangedListener(
                @NonNull Executor executor,
                @NonNull OnDynamicRoutesChangedListener listener) {
            synchronized (mLock) {
                if (executor == null) {
                    throw new NullPointerException("Executor shouldn't be null");
                }
                if (listener == null) {
                    throw new NullPointerException("Listener shouldn't be null");
                }
                mExecutor = executor;
                mListener = listener;

                if (mPendingRoutes != null && !mPendingRoutes.isEmpty()) {
                    MediaRouteDescriptor groupRoute = mPendingGroupRoute;
                    Collection<DynamicRouteDescriptor> routes = mPendingRoutes;
                    mPendingGroupRoute = null;
                    mPendingRoutes = null;
                    mExecutor.execute(
                            () ->
                                    listener.onRoutesChanged(
                                            DynamicGroupRouteController.this, groupRoute, routes));
                }
            }
        }

        /**
         * Sets the dynamic route descriptors for the dynamic group.
         * <p>
         * The dynamic group controller should call this method to notify the current
         * dynamic group state.
         * </p>
         * @param routes The dynamic route descriptors for published routes.
         *               At least a selected or selecting route must be included.
         * @deprecated Use {@link #notifyDynamicRoutesChanged(MediaRouteDescriptor, Collection)}
         * instead to notify information of the group.
         */
        @Deprecated
        public final void notifyDynamicRoutesChanged(
                @NonNull final Collection<DynamicRouteDescriptor> routes) {
            if (routes == null) {
                throw new NullPointerException("routes must not be null");
            }
            synchronized (mLock) {
                if (mExecutor != null) {
                    final OnDynamicRoutesChangedListener listener = mListener;
                    mExecutor.execute(
                            () ->
                                    listener.onRoutesChanged(
                                            DynamicGroupRouteController.this,
                                            /* groupRoute= */ null,
                                            routes));
                } else {
                    mPendingRoutes = new ArrayList<>(routes);
                }
            }
        }

        /**
         * Sets the group route descriptor and the dynamic route descriptors for the dynamic group.
         * <p>
         * The dynamic group controller should call this method to notify the current
         * dynamic group state.
         * </p>
         * @param groupRoute The media route descriptor describing the dynamic group.
         *                   The {@link MediaRouter#getSelectedRoute() selected route} of the
         *                   media router will contain this information.
         *                   If it is {@link MediaRouteDescriptor#isEnabled() disabled},
         *                   the media router will unselect the dynamic group and release
         *                   the route controller.
         * @param dynamicRoutes The dynamic route descriptors for published routes.
         *                      At least a selected or selecting route should be included.
         */
        public final void notifyDynamicRoutesChanged(
                @NonNull MediaRouteDescriptor groupRoute,
                @NonNull Collection<DynamicRouteDescriptor> dynamicRoutes) {
            if (groupRoute == null) {
                throw new NullPointerException("groupRoute must not be null");
            }
            if (dynamicRoutes == null) {
                throw new NullPointerException("dynamicRoutes must not be null");
            }
            synchronized (mLock) {
                if (mExecutor != null) {
                    final OnDynamicRoutesChangedListener listener = mListener;
                    mExecutor.execute(
                            () ->
                                    listener.onRoutesChanged(
                                            DynamicGroupRouteController.this,
                                            groupRoute,
                                            dynamicRoutes));
                } else {
                    mPendingGroupRoute = groupRoute;
                    mPendingRoutes = new ArrayList<>(dynamicRoutes);
                }
            }
        }

        /**
         * Used to notify media router each route's property changes regarding this
         * {@link DynamicGroupRouteController} instance.
         * <p> Here are some examples when this notification is called :
         * <ul>
         *     <li> a route is newly turned on and it can be grouped with this dynamic group route.
         *     </li>
         *     <li> a route is selecting as a member of this dynamic group route.</li>
         *     <li> a route is selected as a member of this dynamic group route.</li>
         *     <li> a route is unselecting.</li>
         *     <li> a route is unselected.</li>
         *     <li> a route is turned off.</li>
         * </ul>
         * </p>
         */
        interface OnDynamicRoutesChangedListener {
            /**
             * The provider should call this method when routes' properties change (for example,
             * when a route becomes ungroupable).
             *
             * @param controller the {@link DynamicGroupRouteController} which keeps this listener.
             * @param groupRoute the route descriptor about the dynamic group. May be null if the
             *     provider notified the update via {@link #notifyDynamicRoutesChanged(Collection)}.
             * @param routes the collection of routes contains selected routes. (can be unselectable
             *     or not) and unselected routes (can be groupable or transferable or not).
             */
            void onRoutesChanged(
                    @NonNull DynamicGroupRouteController controller,
                    @Nullable MediaRouteDescriptor groupRoute,
                    @NonNull Collection<DynamicRouteDescriptor> routes);
        }

        /**
         * Contains a route, its selection state and its capabilities.
         */
        public static final class DynamicRouteDescriptor {
            static final String KEY_MEDIA_ROUTE_DESCRIPTOR = "mrDescriptor";
            static final String KEY_SELECTION_STATE = "selectionState";
            static final String KEY_IS_UNSELECTABLE = "isUnselectable";
            static final String KEY_IS_GROUPABLE = "isGroupable";
            static final String KEY_IS_TRANSFERABLE = "isTransferable";

            /**
             */
            @RestrictTo(LIBRARY)
            @IntDef({
                    UNSELECTING,
                    UNSELECTED,
                    SELECTING,
                    SELECTED
            })
            @Retention(RetentionPolicy.SOURCE)
            public @interface SelectionState {}
            /**
             * After a user unselects a route, it might take some time for a provider to complete
             * the operation. This state is used in this between time. MediaRouter can either
             * block the UI or show the route as unchecked.
             */
            public static final int UNSELECTING = 0;

            /**
             * The route is unselected.
             * <p>
             * Unselect operation is done by the route provider.
             * </p>
             */
            public static final int UNSELECTED = 1;

            /**
             * After a user selects a route, it might take some time for a provider to complete
             * the operation. This state is used in this between time. MediaRouter can either
             * block the UI or show the route as checked.
             */
            public static final int SELECTING = 2;

            /**
             * The route is selected.
             * <p>
             * Select operation is done by the route provider.
             * </p>
             */
            public static final int SELECTED = 3;

            //TODO: mMediaRouteDescriptor could have an old info. We should provide a way to
            // update it or use only the route ID.
            final MediaRouteDescriptor mMediaRouteDescriptor;
            @SelectionState
            final int mSelectionState;
            final boolean mIsUnselectable;
            final boolean mIsGroupable;
            final boolean mIsTransferable;
            Bundle mBundle;

            DynamicRouteDescriptor(
                    MediaRouteDescriptor mediaRouteDescriptor, @SelectionState int selectionState,
                    boolean isUnselectable, boolean isGroupable, boolean isTransferable) {
                mMediaRouteDescriptor = mediaRouteDescriptor;
                mSelectionState = selectionState;
                mIsUnselectable = isUnselectable;
                mIsGroupable = isGroupable;
                mIsTransferable = isTransferable;
            }

            /**
             * Gets this route's {@link MediaRouteDescriptor}. i.e. which route this info is for.
             */
            @NonNull
            public MediaRouteDescriptor getRouteDescriptor() {
                return mMediaRouteDescriptor;
            }

            /**
             * Gets the selection state.
             */
            public @SelectionState int getSelectionState() {
                return mSelectionState;
            }

            /**
             * Returns true if the route can be unselected.
             * <p>
             * For example, a static group has an old build which doesn't support dynamic group.
             * All its members can't be removed.
             * </p>
             * <p>
             * Only applicable to selected/selecting routes.
             * </p>
             */
            public boolean isUnselectable() {
                return mIsUnselectable;
            }

            /**
             * Returns true if the route can be grouped into the dynamic group route.
             * <p>
             * Only applicable to unselected/unselecting routes.
             * Note that this is NOT mutually exclusive with {@link #isTransferable()}.
             * </p>
             */
            public boolean isGroupable() {
                return mIsGroupable;
            }

            /**
             * Returns true if the current dynamic group route can be transferred to this route.
             * <p>
             * Only applicable to unselected/unselecting routes.
             * Note that this is NOT mutually exclusive with {@link #isGroupable()}.
             * </p>
             */
            public boolean isTransferable() {
                return mIsTransferable;
            }

            Bundle toBundle() {
                if (mBundle == null) {
                    mBundle = new Bundle();
                    mBundle.putBundle(KEY_MEDIA_ROUTE_DESCRIPTOR, mMediaRouteDescriptor.asBundle());
                    mBundle.putInt(KEY_SELECTION_STATE, mSelectionState);
                    mBundle.putBoolean(KEY_IS_UNSELECTABLE, mIsUnselectable);
                    mBundle.putBoolean(KEY_IS_GROUPABLE, mIsGroupable);
                    mBundle.putBoolean(KEY_IS_TRANSFERABLE, mIsTransferable);
                }
                return mBundle;
            }

            static DynamicRouteDescriptor fromBundle(Bundle bundle) {
                if (bundle == null) {
                    return null;
                }
                MediaRouteDescriptor descriptor = MediaRouteDescriptor.fromBundle(
                        bundle.getBundle(KEY_MEDIA_ROUTE_DESCRIPTOR));
                int selectionState = bundle.getInt(KEY_SELECTION_STATE, UNSELECTED);
                boolean isUnselectable = bundle.getBoolean(KEY_IS_UNSELECTABLE, false);
                boolean isGroupable = bundle.getBoolean(KEY_IS_GROUPABLE, false);
                boolean isTransferable = bundle.getBoolean(KEY_IS_TRANSFERABLE, false);
                return new DynamicRouteDescriptor(descriptor, selectionState, isUnselectable,
                        isGroupable, isTransferable);
            }

            /**
             * Builder for {@link DynamicRouteDescriptor}
             */
            public static final class  Builder {
                private final MediaRouteDescriptor mRouteDescriptor;
                private @SelectionState int mSelectionState = UNSELECTED;
                private boolean mIsUnselectable = false;
                private boolean mIsGroupable = false;
                private boolean mIsTransferable = false;

                /**
                 * A constructor with {@link MediaRouteDescriptor}.
                 */
                public Builder(@NonNull MediaRouteDescriptor descriptor) {
                    if (descriptor == null) {
                        throw new NullPointerException("descriptor must not be null");
                    }
                    mRouteDescriptor = descriptor;
                }

                /**
                 * Copies the properties from the given {@link DynamicRouteDescriptor}
                 */
                public Builder(@NonNull DynamicRouteDescriptor dynamicRouteDescriptor) {
                    if (dynamicRouteDescriptor == null) {
                        throw new NullPointerException("dynamicRouteDescriptor must not be null");
                    }
                    mRouteDescriptor = dynamicRouteDescriptor.getRouteDescriptor();
                    mSelectionState = dynamicRouteDescriptor.getSelectionState();
                    mIsUnselectable = dynamicRouteDescriptor.isUnselectable();
                    mIsGroupable = dynamicRouteDescriptor.isGroupable();
                    mIsTransferable = dynamicRouteDescriptor.isTransferable();
                }

                /**
                 * Sets the selection state of this route within the associated dynamic group route.
                 */
                @NonNull
                public Builder setSelectionState(@SelectionState int state) {
                    mSelectionState = state;
                    return this;
                }

                /**
                 * Sets if this route can be unselected.
                 */
                @NonNull
                public Builder setIsUnselectable(boolean value) {
                    mIsUnselectable = value;
                    return this;
                }

                /**
                 * Sets if this route can be a selected as a member of the associated dynamic
                 * group route.
                 */
                @NonNull
                public Builder setIsGroupable(boolean value) {
                    mIsGroupable = value;
                    return this;
                }

                /**
                 * Sets if the associated dynamic group route can be transferred to this route.
                 */
                @NonNull
                public Builder setIsTransferable(boolean value) {
                    mIsTransferable = value;
                    return this;
                }

                /**
                 * Builds the {@link DynamicRouteDescriptor}.
                 */
                @NonNull
                public DynamicRouteDescriptor build() {
                    return new DynamicRouteDescriptor(
                            mRouteDescriptor, mSelectionState, mIsUnselectable, mIsGroupable,
                            mIsTransferable);
                }
            }
        }
    }

    /**
     * Callback which is invoked when route information becomes available or changes.
     */
    public static abstract class Callback {
        /**
         * Called when information about a route provider and its routes change.
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
