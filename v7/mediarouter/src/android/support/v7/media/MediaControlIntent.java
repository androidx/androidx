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
 * Constants for media control intents.
 * <p>
 * This class declares a set of standard media control intent categories and actions that
 * applications can use to identify the capabilities of media routes and control them.
 * </p>
 *
 * <h3>Media control intent categories</h3>
 * <p>
 * Media control intent categories specify means by which applications can
 * send media to the destination of a media route.  Categories are sometimes referred
 * to as describing "types" or "kinds" of routes.
 * </p><p>
 * For example, if a route supports the {@link #CATEGORY_REMOTE_PLAYBACK remote playback category},
 * then an application can ask it to play media remotely by sending a {@link #ACTION_PLAY play}
 * intent with the Uri of the media content to play.  Such a route may then be referred to as
 * a "remote playback route" because it supports remote playback requests.  It is common
 * for a route to support multiple categories of requests at the same time, such as
 * live audio and live video.
 * </p><p>
 * The following standard route categories are defined.
 * </p><ul>
 * <li>{@link #CATEGORY_LIVE_AUDIO Live audio}: The route supports streaming live audio
 * from the device to the destination.  Live audio routes include local speakers
 * and Bluetooth headsets.
 * <li>{@link #CATEGORY_LIVE_VIDEO Live video}: The route supports streaming live video
 * from the device to the destination.  Live video routes include local displays
 * and wireless displays that support mirroring and
 * {@link android.app.Presentation presentations}.  Live video routes typically also
 * support live audio capabilities.
 * <li>{@link #CATEGORY_REMOTE_PLAYBACK Remote playback}: The route supports sending
 * remote playback requests for media content to the destination.  The content to be
 * played is identified by a Uri and mime-type.
 * </ul><p>
 * Media route providers may define custom media control intent categories of their own in
 * addition to the standard ones.  Custom categories can be used to provide a variety
 * of features to applications that recognize and know how to use them.  For example,
 * a media route provider might define a custom category to indicate that its routes
 * support a special device-specific control interface in addition to other
 * standard features.
 * </p><p>
 * Applications can determine which categories a route supports by using the
 * {@link MediaRouter.RouteInfo#supportsControlCategory MediaRouter.RouteInfo.supportsControlCategory}
 * or {@link MediaRouter.RouteInfo#getControlFilters MediaRouter.RouteInfo.getControlFilters}
 * methods.  Applications can also specify the types of routes that they want to use by
 * creating {@link MediaRouteSelector media route selectors} that contain the desired
 * categories and are used to filter routes in several parts of the media router API.
 * </p>
 *
 * <h3>Media control intent actions</h3>
 * <p>
 * Media control intent actions specify particular functions that applications
 * can ask the destination of a media route to perform.  Media route control requests
 * take the form of intents in a similar manner to other intents used to start activities
 * or send broadcasts.  The difference is that media control intents are directed to
 * routes rather than activity or broadcast receiver components.
 * </p><p>
 * Each media route control intent specifies an action, a category and some number of parameters.
 * Applications send media control requests to routes using the
 * {@link MediaRouter.RouteInfo#sendControlRequest MediaRouter.RouteInfo.sendControlRequest}
 * method and receive results via a callback.
 * </p><p>
 * All media control intent actions are associated with the media control intent categories
 * that support them.  Thus only remote playback routes may perform remote playback actions.
 * The documentation of each action specifies the category to which the action belongs,
 * the parameters it requires, and the results it returns.
 * </p>
 *
 * <h3>Live audio and live video routes</h3>
 * <p>
 * {@link #CATEGORY_LIVE_AUDIO Live audio} and {@link #CATEGORY_LIVE_VIDEO live video}
 * routes present media using standard system interfaces such as audio streams,
 * {@link android.app.Presentation presentations} or display mirroring.  These routes are
 * the easiest to use because applications simply render content locally on the device
 * and the system streams it to the route destination automatically.
 * </p><p>
 * In most cases, applications can stream content to live audio and live video routes in
 * the same way they would play the content locally without any modification.  However,
 * applications may also be able to take advantage of more sophisticated features such
 * as second-screen presentation APIs that are particular to these routes.
 * </p>
 *
 * <h3>Remote playback routes</h3>
 * <p>
 * Remote playback routes present media remotely by playing content from a Uri.
 * These routes destinations take responsibility for fetching and rendering content
 * on their own.  Applications do not render the content themselves; instead, applications
 * send control requests to initiate playback, pause, resume, or manipulate queues of
 * media items and receive status updates when the state of each item changes.
 * This allows applications to queue several items to play one after another and
 * provide feedback to the user as playback progresses.
 * </p>
 *
 * <h4>Actions</h4>
 * <p>
 * The following actions are defined:
 * </p><ul>
 * <li>{@link #ACTION_PLAY Play}: Starts playing or enqueues content specified by a given Uri
 * and returns a new media item id to describe the request.  Implicitly creates a new
 * queue of media items if none was specified.
 * <li>{@link #ACTION_CANCEL Cancel}: Cancels playback of a media item and removes it
 * from the queue of items to be played.
 * <li>{@link #ACTION_SEEK Seek}: Sets the content playback position of a media item.
 * <li>{@link #ACTION_GET_STATUS Get status}: Gets the status of a media item including
 * the item's current playback position and progress.
 * <li>{@link #ACTION_PAUSE_QUEUE Pause queue}: Pauses a queue of media items.
 * <li>{@link #ACTION_RESUME_QUEUE Resume queue}: Resumes a queue of media items.
 * <li>{@link #ACTION_CLEAR_QUEUE Clear queue}: Cancels and removes all items from a
 * media queue.
 * </ul>
 *
 * <h4>Media items</h4>
 * <p>
 * Each successful {@link #ACTION_PLAY play action} returns a unique media item id that
 * an application can use to monitor and control playback.  The media item id may be passed
 * to other actions such as {@link #ACTION_CANCEL cancel}, {@link #ACTION_SEEK seek}
 * or {@link #ACTION_GET_STATUS get status}.  It will also appear as a parameter in
 * status update broadcasts to identify the associated playback request.
 * </p>
 * 
 * <h4>Queues</h4>
 * <p>
 * Each successful {@link #ACTION_PLAY play action} has the effect of adding a new media
 * item to a queue of media items to be played.  Queues are created implicitly as part
 * of issuing playback requests and are identified by unique queue ids.
 * </p><p>
 * There is at most one valid queue in existence at any given time for a given route.
 * If an application sends a request that has the effect of creating a new queue then
 * the previously valid queue is cleared and all of its items are canceled before the
 * new queue is created.  In this way, one application can determine when another
 * application has taken control of a route because its own items will all be canceled
 * as soon as the other application begins playing something else.
 * </p><p>
 * Queues are intended to hold a small number of items to help media routes optimize
 * the playback experience.  As each item in the queue completes playback, the next item
 * in the queue should begin playing immediately without delay.
 * </p><p>
 * It is usually sufficient for an application to enqueue no more than a few items at a time
 * to ensure continuous playback.  Typically the application will start by enqueuing two
 * media items at once: one item to play now and one item to play next.  When the first
 * item finishes, the second item will begin playing immediately.  The application will
 * receive one status update broadcast indicating that the first item finished playing
 * and another status update broadcast indicating that the second item has started playing.
 * Upon receipt of such broadcasts, the application may choose to enqueue another media
 * item to play once the second one finishes.
 * </p><p>
 * Media route providers are required to support queues of at least 3 items.
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
     * independently by the destination.  The application will receive status updates
     * as the content is played.
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
     * <h3>Queuing</h3>
     * <p>
     * This request has the effect of implicitly creating a media queue whenever the
     * application does not specify the {@link #EXTRA_QUEUE_ID} parameter.  Because there
     * can only be one valid queue at a time, creating a new queue has the side-effect
     * of invalidating any existing queues and canceling all of their items before
     * enqueuing the new playback request media item onto the newly created queue.
     * </p><p>
     * If the application specifies an invalid queue id, then the request has no effect
     * and an error is returned.  The application may then ask that a new queue be
     * created (and the current one invalidated) by issuing a new playback request without
     * a queue id parameter.  However, it should only do this at the user's request
     * (say, by the user explicitly clicking a play button) since another application may
     * be trying to take control of the route.
     * </p><p>
     * For more information on queuing, please refer to the class documentation.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(optional)</i>: specifies the queue id of the queue
     * to which the new playback request should be appended.  If omitted, a new queue
     * is created.
     * <li>{@link #EXTRA_ITEM_CONTENT_POSITION} <i>(optional)</i>: specifies the initial
     * content playback position as a long integer number of milliseconds from
     * the beginning of the content.
     * <li>{@link #EXTRA_ITEM_METADATA} <i>(optional)</i>: specifies metadata associated
     * with the content such as the title of a song.
     * <li>{@link #EXTRA_ITEM_HTTP_HEADERS} <i>(optional)</i>: specifies HTTP headers to
     * supply to the server when fetching the content.
     * <li>{@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER} <i>(optional)</i>: specifies a
     * {@link PendingIntent} for a broadcast receiver that will receive status updates
     * about the media item.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the queue id of the queue
     * to which the new media item was appended.  This will be a new queue in
     * the case where no queue id was supplied as a parameter.
     * <li>{@link #EXTRA_ITEM_ID} <i>(required)</i>: specifies an opaque string identifier
     * to use to refer to the media item in subsequent requests such as {@link #ACTION_CANCEL}.
     * <li>{@link #EXTRA_ITEM_STATUS} <i>(required)</i>: specifies the initial status of
     * the item that has been enqueued.
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
     * if (route.supportsControlRequest(intent)) {
     *     MediaRouter.ControlRequestCallback callback = new MediaRouter.ControlRequestCallback() {
     *         public void onResult(Bundle data) {
     *             // The request succeeded.
     *             // Playback may be controlled using the returned queue and item id.
     *             String queueId = data.getString(MediaControlIntent.EXTRA_QUEUE_ID);
     *             String itemId = data.getString(MediaControlIntent.EXTRA_ITEM_ID);
     *             MediaItemStatus status = MediaItemStatus.fromBundle(data.getBundle(
     *                     MediaControlIntent.EXTRA_ITEM_STATUS));
     *             // ...
     *         }
     *
     *         public void onError(String message, Bundle data) {
     *             // An error occurred!
     *         }
     *     };
     *     route.sendControlRequest(intent, callback);
     * }</pre>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_CANCEL
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     */
    public static final String ACTION_PLAY = "android.media.intent.action.PLAY";

    /**
     * Media control action: Cancel media item playback.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to cancel playback of the
     * specified media item and remove it from the queue.
     * </p><p>
     * This action has no effect if the media item's status is
     * {@link MediaItemStatus#PLAYBACK_STATE_CANCELED} or
     * {@link MediaItemStatus#PLAYBACK_STATE_ERROR}.
     * Otherwise the media item's status is set to
     * {@link MediaItemStatus#PLAYBACK_STATE_CANCELED}, playback of this media item is
     * stopped if it had been playing and the item is removed from the queue (skipped).
     * </p><p>
     * A status update is sent to the status update receiver indicating the new status
     * of the item.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the queue id of the queue
     * to which the media item belongs.
     * <li>{@link #EXTRA_ITEM_ID} (required): specifies the media item id of media item
     * to cancel.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS} <i>(required)</i>: specifies the new status of the item.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_CANCEL = "android.media.intent.action.CANCEL";

    /**
     * Media control action: Seek media item to a new playback position.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to modify the current playback
     * position of the specified media item.
     * </p><p>
     * This action only affects the playback position of the media item; not its playback state.
     * If the item is paused, then seeking sets the position but the item remains paused.
     * Likewise if the item is playing, then seeking will cause playback to jump to the
     * new position and continue playing from that point.  If the item has not yet started
     * playing, then the new playback position is be remembered and used as the item's
     * initial content position when playback eventually begins.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the queue id of the queue
     * to which the media item belongs.
     * <li>{@link #EXTRA_ITEM_ID} <i>(required)</i>: specifies the media item id of
     * the media item to seek.
     * <li>{@link #EXTRA_ITEM_CONTENT_POSITION} <i>(required)</i>: specifies the new
     * content position for playback as a long integer number of milliseconds from
     * the beginning of the content.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS} <i>(required)</i>: specifies the new status of the item.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_SEEK = "android.media.intent.action.SEEK";

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
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the queue id of the queue
     * to which the media item belongs.
     * <li>{@link #EXTRA_ITEM_ID} <i>(required)</i>: specifies the media item id of
     * the media item to query.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_ITEM_STATUS} <i>(required)</i>: specifies the current status of the item.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #EXTRA_ITEM_STATUS_UPDATE_RECEIVER
     */
    public static final String ACTION_GET_STATUS = "android.media.intent.action.GET_STATUS";

    /**
     * Media control action: Pause media queue playback.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes playback on the specified media queue to be paused.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the queue id of the queue
     * to be paused.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li><i>None</i>
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_RESUME_QUEUE
     */
    public static final String ACTION_PAUSE_QUEUE = "android.media.intent.action.PAUSE_QUEUE";

    /**
     * Media control action: Resume media queue playback (unpause).
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes playback on the specified media queue to be resumed.
     * Reverses the effects of {@link #ACTION_PAUSE_QUEUE}.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the queue id of the queue
     * to be resumed.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li><i>None</i>
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_PAUSE_QUEUE
     */
    public static final String ACTION_RESUME_QUEUE = "android.media.intent.action.RESUME_QUEUE";

    /**
     * Media control action: Clear media queue.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes all media items in the specified media queue to be canceled
     * and removed.  The queue is left in an empty state.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the queue id of the queue
     * to be cleared.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li><i>None</i>
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_CLEAR_QUEUE = "android.media.intent.action.CLEAR_QUEUE";

    /**
     * Bundle extra: Media queue id.
     * <p>
     * An opaque unique identifier returned as a result from {@link #ACTION_PLAY} that
     * represents the queue of media items to which an item was appended.  Subsequent
     * playback requests may specify the same queue id to enqueue addition items onto
     * the same queue.
     * </p><p>
     * Used with various actions to specify the id of the media queue to be controlled.
     * </p><p>
     * Included in broadcast intents sent to
     * {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER status update receivers} to identify
     * the queue to which the item in question belongs.
     * </p><p>
     * The value is a unique string value generated by the media route provider
     * to represent one particular media queue.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_CANCEL
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     * @see #ACTION_PAUSE_QUEUE
     * @see #ACTION_RESUME_QUEUE
     * @see #ACTION_CLEAR_QUEUE
     */
    public static final String EXTRA_QUEUE_ID =
            "android.media.intent.extra.QUEUE_ID";

    /**
     * Bundle extra: Media item id.
     * <p>
     * An opaque unique identifier returned as a result from {@link #ACTION_PLAY} that
     * represents the media item that was created by the playback request.
     * </p><p>
     * Used with various actions to specify the id of the media item to be controlled.
     * </p><p>
     * Included in broadcast intents sent to
     * {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER status update receivers} to identify
     * the item in question.
     * </p><p>
     * The value is a unique string value generated by the media route provider
     * to represent one particular media item.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_CANCEL
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_ITEM_ID =
            "android.media.intent.extra.ITEM_ID";

    /**
     * Bundle extra: Media item status.
     * <p>
     * Returned as a result from media item actions such as {@link #ACTION_PLAY},
     * {@link #ACTION_SEEK}, {@link #ACTION_CANCEL} and {@link #ACTION_GET_STATUS}
     * to describe the status of the relevant media item.
     * </p><p>
     * Included in broadcast intents sent to
     * {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER status update receivers} to provide
     * updated status information.
     * </p><p>
     * The value is a {@link android.os.Bundle} of data that can be converted into
     * a {@link MediaItemStatus} object using
     * {@link MediaItemStatus#fromBundle MediaItemStatus.fromBundle}.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_CANCEL
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_ITEM_STATUS =
            "android.media.intent.extra.ITEM_STATUS";

    /**
     * Long extra: Media item content position.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify the starting playback position.
     * </p><p>
     * Used with {@link #ACTION_SEEK} to set a new playback position.
     * </p><p>
     * The value is a long integer number of milliseconds from the beginning of the content.
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
     * The value is a {@link android.os.Bundle} of metadata key-value pairs as defined
     * in {@link MediaItemMetadata}.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_ITEM_METADATA =
            "android.media.intent.extra.ITEM_METADATA";

    /**
     * Bundle extra: HTTP headers.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify HTTP headers to be included when
     * fetching to the content indicated by the media item's data Uri.
     * </p><p>
     * This extra may be used to provide authentication tokens and other
     * parameters to the server separately from the media item's data Uri.
     * </p><p>
     * The value is a {@link android.os.Bundle} of string based key-value pairs
     * that describe the HTTP headers.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_ITEM_HTTP_HEADERS =
            "android.media.intent.extra.HTTP_HEADERS";

    /**
     * Bundle extra: Media item status update receiver.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify a {@link PendingIntent} for a
     * broadcast receiver that will receive status updates about a particular
     * media item.
     * </p><p>
     * Whenever the status of the media item changes, the media route provider will
     * send a broadcast to the pending intent with extras that identify the queue
     * to which the item belongs, the item itself and the item's updated status.
     * </p><p>
     * The same pending intent and broadcast receiver may be shared by any number of
     * media items since the broadcast intent includes the media queue id and media item id.
     * </p><p>
     * The value is a {@link PendingIntent}.
     * </p>
     *
     * <h3>Broadcast extras</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_ID} <i>(required)</i>: specifies the media queue id of the
     * queue to which the item in question belongs.
     * <li>{@link #EXTRA_ITEM_ID} <i>(required)</i>: specifies the media item id of the
     * media item in question.
     * <li>{@link #EXTRA_ITEM_STATUS} <i>(required)</i>: specifies the status of the
     * item as a bundle that can be decoded into a {@link MediaItemStatus} object.
     * </ul>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_ITEM_STATUS_UPDATE_RECEIVER =
            "android.media.intent.extra.ITEM_STATUS_UPDATE_RECEIVER";

    private MediaControlIntent() {
    }
}
