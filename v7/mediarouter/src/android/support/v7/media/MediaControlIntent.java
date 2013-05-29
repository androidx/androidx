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

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;

/**
 * Constants for identifying media route capabilities and controlling media routes
 * by sending an {@link Intent}.
 * <p>
 * The basic capabilities of a media route may be determined by looking at the
 * media control intent categories and actions supported by the route.
 * </p><ul>
 * <li>A media control intent category specifies the type of the route
 * and the manner in which applications send media to its destination.
 * <li>A media control intent action specifies a command to be delivered to
 * the media route's destination to control media playback.  Media control
 * actions may only apply to routes that support certain media control categories.
 * </ul>
 *
 * <h3>Route Categories</h3>
 * <p>
 * Routes are classified by the categories of actions that they support.  The following
 * standard categories are defined.
 * </p><ul>
 * <li>{@link #CATEGORY_LIVE_AUDIO Live audio}: The route supports streaming live audio
 * from the device to the destination.  Live audio routes include local speakers
 * and Bluetooth headsets.
 * <li>{@link #CATEGORY_LIVE_VIDEO Live video}: The route supports streaming live video
 * from the device to the destination.  Live video routes include local displays
 * and wireless displays that support mirroring and
 * {@link android.app.Presentation presentations}.
 * <li>{@link #CATEGORY_REMOTE_PLAYBACK Remote playback}: The route supports sending
 * remote playback requests for media content to the destination.  The content to be
 * played is identified by a Uri and mime-type.
 * </ul>
 *
 * <h3>Remote Playback</h3>
 * <p>
 * Media control intents are frequently used to start remote playback of media
 * on a destination using remote playback actions from the
 * {@link #CATEGORY_REMOTE_PLAYBACK remote playback category}.
 * </p><p>
 * The {@link #ACTION_PLAY} action enqueues the Uri of content to be played and obtains
 * a media item id that can be used to control playback.
 * </p>
 *
 * <h4>Media Items</h4>
 * <p>
 * A media item id is an opaque token that represents the playback request.
 * The application must supply the media item id when sending control requests to
 * {@link #ACTION_PAUSE pause}, {@link #ACTION_RESUME resume}, {@link #ACTION_SEEK seek},
 * {@link #ACTION_GET_STATUS get status}, or perform other actions to affect playback.
 * </p><p>
 * Each remote playback action is bound to a specific media item.  If a
 * media item has finished, been canceled or encountered an error, then most
 * actions other than status requests will fail.  In particular, actions such as
 * {@link #ACTION_PAUSE} always control playback of a specific media item rather
 * than acting globally upon whatever happens to be playing at the moment.
 * </p>
 *
 * <h4>Queue Behavior</h4>
 * <p>
 * To provide a seamless media experience, the application can enqueue a limited number
 * of items to play in succession.  The destination can take advantage of its
 * queue to optimize continuous playback, starting the next media item automatically
 * as soon as the previous one finishes.
 * </p><p>
 * By default, the {@link #ACTION_PLAY play action} causes the destination to stop
 * whatever is currently playing, clear the queue of pending items, then begin playing
 * the newly requested content.  By supplying a
 * {@link #EXTRA_ITEM_QUEUE_BEHAVIOR queue behavior} parameter as part of the playback
 * request, the application can specify whether the media item should
 * {@link #ITEM_QUEUE_BEHAVIOR_PLAY_NOW play now},
 * {@link #ITEM_QUEUE_BEHAVIOR_PLAY_NOW play next},
 * or {@link #ITEM_QUEUE_BEHAVIOR_PLAY_NOW play later},
 * </p><p>
 * Typically the application will start by queuing two media items at once: one item to
 * play now and one item to play next.  When the first item finishes, the next item
 * will begin playing immediately.  The application can then enqueue a new media item to
 * play next (without interrupting current playback), and so on.
 * </p><p>
 * The application can also enqueue additional items to play later although queuing
 * one item to play now and one item to play next is usually sufficient to ensure
 * continuous playback.
 * </p>
 */
public final class MediaControlIntent {
    /**
     * Media control category: Live audio.
     * <p>
     * A route that supports live audio routing will allow the media audio stream
     * to be sent to supported destinations.  This can include internal speakers or
     * audio jacks on the device itself, A2DP devices, and more.
     * </p><p>
     * When a live audio route is selected, audio routing is transparent to the application.
     * All audio played on the media stream will be routed to the selected destination.
     * </p>
     */
    public static final String CATEGORY_LIVE_AUDIO = "android.media.intent.category.LIVE_AUDIO";

    /**
     * Media control category: Live video.
     * <p>
     * A route that supports live video routing will allow a mirrored version
     * of the device's primary display or a customized
     * {@link android.app.Presentation Presentation} to be sent to supported
     * destinations.
     * </p><p>
     * When a live video route is selected, audio and video routing is transparent
     * to the application.  By default, audio and video is routed to the selected
     * destination.  For certain live video routes, the application may also use a
     * {@link android.app.Presentation Presentation} to replace the mirrored view
     * on the external display with different content.
     * </p>
     *
     * @see MediaRouter.RouteInfo#getPresentationDisplay()
     * @see android.app.Presentation
     */
    public static final String CATEGORY_LIVE_VIDEO = "android.media.intent.category.LIVE_VIDEO";

    /**
     * Media control category: Remote playback.
     * <p>
     * A route that supports remote playback routing will allow an application to send
     * requests to play content remotely to supported destinations.
     * </p><p>
     * Remote playback routes destinations operate independently of the local device.
     * When a remote playback route is selected, the application can control the content
     * playing on the destination by sending media control actions to the route.
     * The application may also receive status updates from the route regarding
     * remote playback.
     * </p>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     */
    public static final String CATEGORY_REMOTE_PLAYBACK =
            "android.media.intent.category.REMOTE_PLAYBACK";

    /**
     * Media control action: Play media item.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to start playing content with
     * the {@link Uri} specified in the {@link Intent}'s {@link Intent#getData() data uri}.
     * The action returns a media item id which can be used to control playback
     * using other remote playback actions.
     * </p><p>
     * Once initiated, playback of the specified content will be queued and managed
     * independently by the destination.  The application will receive status
     * and progress updates as the content is played.
     * </p><p>
     * If the data uri specifies an HTTP or HTTPS scheme, then the destination is
     * responsible for following HTTP redirects to a reasonable depth of at least 3
     * levels as might typically be handled by a web browser.  If an HTTP error
     * occurs, then the destination should send a {@link MediaItemStatus status update}
     * back to the client indicating the {@link MediaItemStatus#PLAYBACK_STATE_ERROR error}
     * {@link MediaItemStatus#getPlaybackState() playback state}
     * and include the {@link MediaItemStatus#getHttpStatusCode() HTTP status code}.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_QUEUE_BEHAVIOR}: specifies when the content should be played.
     * <li>{@link #EXTRA_ITEM_CONTENT_POSITION}: specifies the initial content playback position.
     * <li>{@link #EXTRA_ITEM_METADATA}: specifies metadata associated with the
     * content such as the title of a song.
     * <li>{@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER}: specifies a {@link PendingIntent}
     * for a broadcast receiver that will receive status updates about the media item.
     * <li>{@link #EXTRA_ITEM_HTTP_HEADERS}: specifies HTTP headers to supply to the
     * server when fetching the content.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_ID}: specifies an opaque string identifier to use to refer
     * to the media item in subsequent requests such as {@link #ACTION_PAUSE}.
     * <li>{@link #EXTRA_ITEM_STATUS}: specifies the initial status of the item
     * that has been enqueued.
     * </ul>
     *
     * <h3>Status updates</h3>
     * <p>
     * If the client supplies a {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER status update receiver}
     * then the media route provider is responsible for sending status updates to the receiver
     * when significant media item state changes occur such as when playback starts or
     * stops.  The receiver will not be invoked for content playback position changes.
     * The application may retrieve the current playback position when necessary
     * using the {@link #ACTION_GET_STATUS} request.
     * </p><p>
     * Refer to {@link MediaItemStatus} for details.
     * </p>
     *
     * <h3>Example</h3>
     * <pre>
     * MediaRouter mediaRouter = MediaRouter.getInstance(context);
     * MediaRouter.RouteInfo route = mediaRouter.getSelectedRoute();
     * Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
     * intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
     * intent.setDataAndType("http://example.com/videos/movie.mp4", "video/mp4");
     * intent.putExtra(MediaControlIntent.EXTRA_ITEM_QUEUE_BEHAVIOR,
     *         MediaControlIntent.ITEM_QUEUE_BEHAVIOR_PLAY_NEXT);
     * if (route.supportsControlRequest(intent)) {
     *     MediaRouter.ControlRequestCallback callback = new MediaRouter.ControlRequestCallback() {
     *         public void onResult(int result, Bundle data) {
     *             if (result == REQUEST_SUCCEEDED) {
     *                 // The request succeeded.
     *                 // Playback may be controlled using the returned item id...
     *                 String id = data.getStringExtra(MediaControlIntent.EXTRA_ITEM_ID);
     *             }
     *         }
     *     };
     *     route.sendControlRequest(intent, callback);
     * }</pre>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_SEEK
     * @see #ACTION_STOP
     * @see #ACTION_PAUSE
     * @see #ACTION_RESUME
     * @see #ACTION_GET_STATUS
     */
    public static final String ACTION_PLAY = "android.media.intent.action.PLAY";

    /**
     * Media control action: Seek media item to a new playback position.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to modify the current playback
     * position of the specified media item.
     * </p><p>
     * This action should generally not affect the current playback state of the media item.
     * If the item is paused, then seeking should set the position but leave
     * the item paused.  Likewise if the item is playing, then seeking should
     * continue playing from the new position.  If the item has not yet started
     * playing, then the new playback position should be remembered and used as the
     * initial position for the item.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_ID}: specifies the media item id of the playback to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * <li>{@link #EXTRA_ITEM_CONTENT_POSITION}: specifies the new position of the content.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS}: specifies the status of the stream.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_SEEK = "android.media.intent.action.SEEK";

    /**
     * Media control action: Stop media item playback.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to stop playback of the
     * specified media item.
     * </p><p>
     * If the media item has not started playing yet, then the media item should
     * be stopped and removed from the queue.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_ID}: specifies the media item id of the playback to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS}: specifies the status of the stream.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_STOP = "android.media.intent.action.STOP";


    /**
     * Media control action: Pause media item playback.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to pause playback of the
     * specified media item.
     * </p><p>
     * If the media item has not started playing yet, then the request to pause should
     * be remembered such that the item will initially start in a paused state.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_ID}: specifies the media item id of the playback to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS}: specifies the status of the stream.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_RESUME
     */
    public static final String ACTION_PAUSE = "android.media.intent.action.PAUSE";

    /**
     * Media control action: Resume media item playback (unpause).
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to resume playback of the
     * specified media item.  Reverses the effects of {@link #ACTION_PAUSE}.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_ID}: specifies the media item id of the playback to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS}: specifies the status of the stream.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_PAUSE
     */
    public static final String ACTION_RESUME = "android.media.intent.action.RESUME";

    /**
     * Media control action: Get media item playback status and progress information.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action asks a remote playback route to provide updated playback status and progress
     * information about the specified media item.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_ID}: specifies the media item id of the playback to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS}: specifies the status of the stream.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #EXTRA_ITEM_STATUS_UPDATE_RECEIVER
     */
    public static final String ACTION_GET_STATUS = "android.media.intent.action.GET_STATUS";

    /**
     * Integer extra: Media item queue behavior.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify when the requested  should be
     * played.  The default is to play the content immediately.
     * </p><p>
     * The value must be one of {@link #ITEM_QUEUE_BEHAVIOR_PLAY_NOW},
     * {@link #ITEM_QUEUE_BEHAVIOR_PLAY_NEXT}, or {@link #ITEM_QUEUE_BEHAVIOR_PLAY_LATER}.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_ITEM_QUEUE_BEHAVIOR =
            "android.media.intent.extra.QUEUE_BEHAVIOR";

    /**
     * Value for {@link #EXTRA_ITEM_QUEUE_BEHAVIOR}: Play now.
     * <p>
     * This is the default queue behavior.
     * </p><p>
     * Requests that the new content be played immediately, canceling the currently playing
     * media item and all subsequent items in the queue.  When this control request returns,
     * the queue will contain exactly one item consisting of the newly requested content.
     * </p>
     *
     * @see #EXTRA_ITEM_QUEUE_BEHAVIOR
     */
    public static final int ITEM_QUEUE_BEHAVIOR_PLAY_NOW = 0;

    /**
     * Value for {@link #EXTRA_ITEM_QUEUE_BEHAVIOR}: Play next.
     * <p>
     * Requests that the new content be enqueued to play next after the currently playing
     * media item, canceling all subsequent items in the queue.  When this control request
     * returns, the queue will contain either one or two items consisting of the currently
     * playing content, if any, followed by the newly requested content.
     * </p>
     *
     * @see #EXTRA_ITEM_QUEUE_BEHAVIOR
     */
    public static final int ITEM_QUEUE_BEHAVIOR_PLAY_NEXT = 1;

    /**
     * Value for {@link #EXTRA_ITEM_QUEUE_BEHAVIOR}: Play later.
     * <p>
     * Requests that the new content be enqueued to play later after all other media items
     * currently in the queue.  When this control request returns, the queue will contain at
     * least one item consisting of the currently playing content and all previously
     * enqueued items followed by the newly requested content.
     * </p>
     *
     * @see #EXTRA_ITEM_QUEUE_BEHAVIOR
     */
    public static final int ITEM_QUEUE_BEHAVIOR_PLAY_LATER = 2;

    /**
     * Double extra: Media item content position.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify the starting playback position.
     * </p><p>
     * Used with {@link #ACTION_SEEK} to set a new playback position.
     * </p><p>
     * The value is a double-precision floating point number of seconds
     * from the beginning of the content.
     * <p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_SEEK
     */
    public static final String EXTRA_ITEM_CONTENT_POSITION =
            "android.media.intent.extra.ITEM_POSITION";

    /**
     * Bundle extra: Media item metadata.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify metadata associated with the content
     * of a media item.
     * </p><p>
     * The value is a {@link android.os.Bundle} of metadata keys and values as defined
     * in {@link MediaItemMetadata}.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_ITEM_METADATA =
            "android.media.intent.extra.ITEM_METADATA";

    /**
     * Bundle extra: Media item id.
     * <p>
     * Returned as a result from {@link #ACTION_PLAY} to provide an opaque unique id
     * for the requested media item which may then be used to issue subsequent
     * requests to control the content.
     * </p><p>
     * Used with various actions to specify the id of the media item to be controlled.
     * </p><p>
     * The value is a unique string value generated by the media route provider
     * to represent one particular media item.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_STOP
     * @see #ACTION_SEEK
     * @see #ACTION_PAUSE
     * @see #ACTION_RESUME
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_ITEM_ID =
            "android.media.intent.extra.ITEM_ID";

    /**
     * Bundle extra: Media item status.
     * <p>
     * Returned as a result from {@link #ACTION_GET_STATUS} and in broadcasts
     * sent to a {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER status update receiver}
     * to describe the status of the media item.
     * </p><p>
     * The value is a {@link android.os.Bundle} of status keys and values as defined
     * in {@link MediaItemStatus}.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_ITEM_STATUS =
            "android.media.intent.extra.ITEM_STATUS";

    /**
     * Bundle extra: Media item status update receiver.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify a {@link PendingIntent} for a
     * broadcast receiver that will receive status updates about a media item.
     * </p><p>
     * Whenever the status of the media item changes, the media route provider will
     * send a broadcast to the pending intent with extras that describe
     * the status of the media item.
     * </p><p>
     * The value is a {@link PendingIntent}.
     * </p>
     *
     * <h3>Broadcast extras</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_ID}: specifies the media item id of the playback to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * <li>{@link #EXTRA_ITEM_STATUS}: specifies the status of the stream.
     * </ul>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_ITEM_STATUS_UPDATE_RECEIVER =
            "android.media.intent.extra.ITEM_STATUS_UPDATE_RECEIVER";

    /**
     * Bundle extra: HTTP headers.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify HTTP headers to be included when
     * fetching to the content indicated by the media item's data Uri.
     * </p><p>
     * This extra may be used to provide authentication tokens and other
     * parameters to the server separately from the media item's data Uri.
     * </p><p>
     * The value is a {@link android.os.Bundle} of string based key value pairs
     * that describe the HTTP headers.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_ITEM_HTTP_HEADERS =
            "android.media.intent.extra.HTTP_HEADERS";

    private MediaControlIntent() {
    }
}
