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
package android.support.v4.media;

/***
 * Defines the communication protocol for media browsers and media browser services.
 * @hide
 */
class MediaBrowserProtocol {

    /**
     * MediaBrowserCompat will check the version of the connected MediaBrowserServiceCompat,
     * and it will not send messages if they are introduced in the higher version of the
     * MediaBrowserServiceCompat.
     */
    public static final int SERVICE_VERSION_1 = 1;
    public static final int SERVICE_VERSION_CURRENT = SERVICE_VERSION_1;

    /*
     * Messages sent from the media browser service compat to the media browser compat.
     * (Compat implementation for IMediaBrowserServiceCallbacks)
     * DO NOT RENUMBER THESE!
     */

    /** (service v1)
     * Sent after {@link MediaBrowserCompat#connect()} when the request has successfully
     * completed.
     * - arg1 : The service version
     * - obj  : The root media item id
     * - data
     *     SERVICE_DATA_MEDIA_SESSION_TOKEN : Media session token
     *     SERVICE_DATA_EXTRAS : An extras bundle which contains EXTRA_SERVICE_VERSION
     */
    public static final int SERVICE_MSG_ON_CONNECT = 1;

    /** (service v1)
     * Sent after {@link MediaBrowserCompat#connect()} when the connection to the media browser
     * failed.
     * - arg1 : service version
     */
    public static final int SERVICE_MSG_ON_CONNECT_FAILED = 2;

    /** (service v1)
     * Sent when the list of children is loaded or updated.
     * - arg1 : The service version
     * - obj  : The parent media item id
     * - data
     *     SERVICE_DATA_MEDIA_ITEM_LIST : An array list for the media item children
     */
    public static final int SERVICE_MSG_ON_LOAD_CHILDREN = 3;

    public static final String SERVICE_DATA_MEDIA_SESSION_TOKEN = "data_media_session_token";
    public static final String SERVICE_DATA_EXTRAS = "data_extras";
    public static final String SERVICE_DATA_MEDIA_ITEM_LIST = "data_media_item_list";
    public static final String SERVICE_DATA_RESULT_RECEIVER = "data_result_receiver";

    public static final String EXTRA_SERVICE_VERSION = "extra_service_version";
    public static final String EXTRA_MESSENGER_BINDER = "extra_messenger";

    /**
     * MediaBrowserServiceCompat will check the version of the MediaBrowserCompat, and it will not
     * send messages if they are introduced in the higher version of the MediaBrowserCompat.
     */
    public static final int CLIENT_VERSION_1 = 1;
    public static final int CLIENT_VERSION_CURRENT = CLIENT_VERSION_1;

    /*
     * Messages sent from the media browser compat to the media browser service compat.
     * (Compat implementation for IMediaBrowserService)
     * DO NOT RENUMBER THESE!
     */

    /** (client v1)
     * Sent to connect to the media browse service compat.
     * - arg1 : The client version
     * - obj  : The package name
     * - data : An optional root hints bundle of service-specific arguments
     * - replayTo : Client messenger
     */
    public static final int CLIENT_MSG_CONNECT = 1;

    /** (client v1)
     * Sent to disconnect from the media browse service compat.
     * - arg1 : The client version
     * - replayTo : Client messenger
     */
    public static final int CLIENT_MSG_DISCONNECT = 2;

    /** (client v1)
     * Sent to subscribe for changes to the children of the specified media id.
     * - arg1 : The client version
     * - obj  : The media item id
     * - replayTo : Client messenger
     */
    public static final int CLIENT_MSG_ADD_SUBSCRIPTION = 3;

    /** (client v1)
     * Sent to unsubscribe for changes to the children of the specified media id.
     * - arg1 : The client version
     * - obj  : The media item id
     * - replayTo : Client messenger
     */
    public static final int CLIENT_MSG_REMOVE_SUBSCRIPTION = 4;

    /** (client v1)
     * Sent to retrieves a specific media item from the connected service.
     * - arg1 : The client version
     * - obj  : The media item id
     * - data
     *     SERVICE_DATA_RESULT_RECEIVER : Result receiver to get the result
     */
    public static final int CLIENT_MSG_GET_MEDIA_ITEM = 5;
}
