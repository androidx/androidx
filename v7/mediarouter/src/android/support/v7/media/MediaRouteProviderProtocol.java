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

package android.support.v7.media;

import android.content.Intent;
import android.os.Messenger;

/**
 * Defines the communication protocol for media route provider services.
 * @hide
 */
abstract class MediaRouteProviderProtocol {
    /**
     * The {@link Intent} that must be declared as handled by the service.
     * Put this in your manifest.
     */
    public static final String SERVICE_INTERFACE =
            "android.media.MediaRouteProviderService";

    /*
     * Messages sent from the client to the service.
     * DO NOT RENUMBER THESE!
     */

    /** (client v1)
     * Register client.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : client version
     */
    public static final int CLIENT_MSG_REGISTER = 1;

    /** (client v1)
     * Unregister client.
     * - replyTo : client messenger
     * - arg1    : request id
     */
    public static final int CLIENT_MSG_UNREGISTER = 2;

    /** (client v1)
     * Create route controller.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : route controller id
     * - CLIENT_DATA_ROUTE_ID : route id string
     */
    public static final int CLIENT_MSG_CREATE_ROUTE_CONTROLLER = 3;

    /** (client v1)
     * Release route controller.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : route controller id
     */
    public static final int CLIENT_MSG_RELEASE_ROUTE_CONTROLLER = 4;

    /** (client v1)
     * Select route.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : route controller id
     */
    public static final int CLIENT_MSG_SELECT_ROUTE = 5;

    /** (client v1)
     * Unselect route.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : route controller id
     */
    public static final int CLIENT_MSG_UNSELECT_ROUTE = 6;

    /** (client v1)
     * Set route volume.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : route controller id
     * - CLIENT_DATA_VOLUME : volume integer
     */
    public static final int CLIENT_MSG_SET_ROUTE_VOLUME = 7;

    /** (client v1)
     * Update route volume.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : route controller id
     * - CLIENT_DATA_VOLUME : volume delta integer
     */
    public static final int CLIENT_MSG_UPDATE_ROUTE_VOLUME = 8;

    /** (client v1)
     * Route control request.
     * - replyTo : client messenger
     * - arg1    : request id
     * - arg2    : route controller id
     * - obj     : media control intent
     */
    public static final int CLIENT_MSG_ROUTE_CONTROL_REQUEST = 9;

    /** (client v1)
     * Sets the discovery request.
     * - replyTo : client messenger
     * - arg1    : request id
     * - obj     : discovery request bundle, or null if none
     */
    public static final int CLIENT_MSG_SET_DISCOVERY_REQUEST = 10;

    public static final String CLIENT_DATA_ROUTE_ID = "routeId";
    public static final String CLIENT_DATA_VOLUME = "volume";

    /*
     * Messages sent from the service to the client.
     * DO NOT RENUMBER THESE!
     */

    /** (service v1)
     * Generic failure sent in response to any unrecognized or malformed request.
     * - arg1    : request id
     */
    public static final int SERVICE_MSG_GENERIC_FAILURE = 0;

    /** (service v1)
     * Generic failure sent in response to a successful message.
     * - arg1    : request id
     */
    public static final int SERVICE_MSG_GENERIC_SUCCESS = 1;

    /** (service v1)
     * Registration succeeded.
     * - arg1    : request id
     * - arg2    : server version
     * - obj     : route provider descriptor bundle, or null
     */
    public static final int SERVICE_MSG_REGISTERED = 2;

    /** (service v1)
     * Route control request success result.
     * - arg1    : request id
     * - obj     : result data bundle, or null
     */
    public static final int SERVICE_MSG_CONTROL_REQUEST_SUCCEEDED = 3;

    /** (service v1)
     * Route control request failure result.
     * - arg1    : request id
     * - obj     : result data bundle, or null
     * - SERVICE_DATA_ERROR: error message
     */
    public static final int SERVICE_MSG_CONTROL_REQUEST_FAILED = 4;

    /** (service v1)
     * Route provider descriptor changed.  (unsolicited event)
     * - arg1    : reserved (0)
     * - obj     : route provider descriptor bundle, or null
     */
    public static final int SERVICE_MSG_DESCRIPTOR_CHANGED = 5;

    public static final String SERVICE_DATA_ERROR = "error";

    /*
     * Recognized client version numbers.  (Reserved for future use.)
     * DO NOT RENUMBER THESE!
     */

    public static final int CLIENT_VERSION_1 = 1;
    public static final int CLIENT_VERSION_CURRENT = CLIENT_VERSION_1;

    /*
     * Recognized server version numbers.  (Reserved for future use.)
     * DO NOT RENUMBER THESE!
     */

    public static final int SERVICE_VERSION_1 = 1;
    public static final int SERVICE_VERSION_CURRENT = SERVICE_VERSION_1;

    /**
     * Returns true if the messenger object is valid.
     * <p>
     * The messenger constructor and unparceling code does not check whether the
     * provided IBinder is a valid IMessenger object.  As a result, it's possible
     * for a peer to send an invalid IBinder that will result in crashes downstream.
     * This method checks that the messenger is in a valid state.
     * </p>
     */
    public static boolean isValidRemoteMessenger(Messenger messenger) {
        try {
            return messenger != null && messenger.getBinder() != null;
        } catch (NullPointerException ex) {
            // If the messenger was constructed with a binder interface other than
            // IMessenger then the call to getBinder() will crash with an NPE.
            return false;
        }
    }
}
