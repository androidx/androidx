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
 * then an application can ask it to play media remotely by sending a
 * {@link #ACTION_PLAY play} or {@link #ACTION_ENQUEUE enqueue} intent with the Uri of the
 * media content to play.  Such a route may then be referred to as
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
 * Each media route control intent specifies an action, a category and some number of parameters
 * that are supplied as extras.  Applications send media control requests to routes using the
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
 * {@link #CATEGORY_REMOTE_PLAYBACK Remote playback} routes present media remotely
 * by playing content from a Uri.
 * These routes destinations take responsibility for fetching and rendering content
 * on their own.  Applications do not render the content themselves; instead, applications
 * send control requests to initiate play, pause, resume, or stop media items and receive
 * status updates as they change state.
 * </p>
 *
 * <h4>Sessions</h4>
 * <p>
 * Each remote media playback action is conducted within the scope of a session.
 * Sessions are used to prevent applications from accidentally interfering with one
 * another because at most one session can be valid at a time.
 * </p><p>
 * A session can be created using the {@link #ACTION_START_SESSION start session action}
 * and terminated using the {@link #ACTION_END_SESSION end session action} when the
 * route provides explicit session management features.
 * </p><p>
 * Explicit session management was added in a later revision of the protocol so not
 * all routes support it.  If the route does not support explicit session management
 * then implicit session management may still be used.  Implicit session management
 * relies on the use of the {@link #ACTION_PLAY play} and {@link #ACTION_ENQUEUE enqueue}
 * actions which have the side-effect of creating a new session if none is provided
 * as argument.
 * </p><p>
 * When a new session is created, the previous session is invalidated and any ongoing
 * media playback is stopped before the requested action is performed.  Any attempt
 * to use an invalidated session will result in an error.  (Protocol implementations
 * are encouraged to aggressively discard information associated with invalidated sessions
 * since it is no longer of use.)
 * </p><p>
 * Each session is identified by a unique session id that may be used to control
 * the session using actions such as pause, resume, stop and end session.
 * </p>
 *
 * <h4>Media items</h4>
 * <p>
 * Each successful {@link #ACTION_PLAY play} or {@link #ACTION_ENQUEUE enqueue} action
 * returns a unique media item id that an application can use to monitor and control
 * playback.  The media item id may be passed to other actions such as
 * {@link #ACTION_SEEK seek} or {@link #ACTION_GET_STATUS get status}.  It will also appear
 * as a parameter in status update broadcasts to identify the associated playback request.
 * </p><p>
 * Each media item is scoped to the session in which it was created.  Therefore media item
 * ids are only ever used together with session ids.  Media item ids are meaningless
 * on their own.  When the session is invalidated, all of its media items are also
 * invalidated.
 * </p>
 *
 * <h4>The playback queue</h4>
 * <p>
 * Each session has its own playback queue that consists of the media items that
 * are pending, playing, buffering or paused.  Items are added to the queue when
 * a playback request is issued.  Items are removed from the queue when they are no
 * longer eligible for playback (enter terminal states).
 * </p><p>
 * As described in the {@link MediaItemStatus} class, media items initially
 * start in a pending state, transition to the playing (or buffering or paused) state
 * during playback, and end in a finished, canceled, invalidated or error state.
 * Once the current item enters a terminal state, playback proceeds on to the
 * next item.
 * </p><p>
 * The application should determine whether the route supports queuing by checking
 * whether the {@link #ACTION_ENQUEUE} action is declared in the route's control filter
 * using {@link MediaRouter.RouteInfo#supportsControlRequest RouteInfo.supportsControlRequest}.
 * </p><p>
 * If the {@link #ACTION_ENQUEUE} action is supported by the route, then the route promises
 * to allow at least two items (possibly more) to be enqueued at a time.  Enqueued items play
 * back to back one after the other as the previous item completes.  Ideally there should
 * be no audible pause between items for standard audio content types.
 * </p><p>
 * If the {@link #ACTION_ENQUEUE} action is not supported by the route, then the queue
 * effectively contains at most one item at a time.  Each play action has the effect of
 * clearing the queue and resetting its state before the next item is played.
 * </p>
 *
 * <h4>Impact of pause, resume, stop and play actions on the playback queue</h4>
 * <p>
 * The pause, resume and stop actions affect the session's whole queue.  Pause causes
 * the playback queue to be suspended no matter which item is currently playing.
 * Resume reverses the effects of pause.  Stop clears the queue and also resets
 * the pause flag just like resume.
 * </p><p>
 * As described earlier, the play action has the effect of clearing the queue
 * and completely resetting its state (like the stop action) then enqueuing a
 * new media item to be played immediately.  Play is therefore equivalent
 * to stop followed by an action to enqueue an item.
 * </p><p>
 * The play action is also special in that it can be used to create new sessions.
 * An application with simple needs may find that it only needs to use play
 * (and occasionally stop) to control playback.
 * </p>
 *
 * <h4>Resolving conflicts between applications</h4>
 * <p>
 * When an application has a valid session, it is essentially in control of remote playback
 * on the route.  No other application can view or modify the remote playback state
 * of that application's session without knowing its id.
 * </p><p>
 * However, other applications can perform actions that have the effect of stopping
 * playback and invalidating the current session.  When this occurs, the former application
 * will be informed that it has lost control by way of individual media item status
 * update broadcasts that indicate that its queued media items have become
 * {@link MediaItemStatus#PLAYBACK_STATE_INVALIDATED invalidated}.  This broadcast
 * implies that playback was terminated abnormally by an external cause.
 * </p><p>
 * Applications should handle conflicts conservatively to allow other applications to
 * smoothly assume control over the route.  When a conflict occurs, the currently playing
 * application should release its session and allow the new application to use the
 * route until such time as the user intervenes to take over the route again and begin
 * a new playback session.
 * </p>
 *
 * <h4>Basic actions</h4>
 * <p>
 * The following basic actions must be supported (all or nothing) by all remote
 * playback routes.  These actions form the basis of the remote playback protocol
 * and are required in all implementations.
 * </p><ul>
 * <li>{@link #ACTION_PLAY Play}: Starts playing content specified by a given Uri
 * and returns a new media item id to describe the request.  Implicitly creates a new
 * session if no session id was specified as a parameter.
 * <li>{@link #ACTION_SEEK Seek}: Sets the content playback position of a specific media item.
 * <li>{@link #ACTION_GET_STATUS Get status}: Gets the status of a media item
 * including the item's current playback position and progress.
 * <li>{@link #ACTION_PAUSE Pause}: Pauses playback of the queue.
 * <li>{@link #ACTION_RESUME Resume}: Resumes playback of the queue.
 * <li>{@link #ACTION_STOP Stop}: Stops playback, clears the queue, and resets the
 * pause state.
 * </ul>
 *
 * <h4>Queue actions</h4>
 * <p>
 * The following queue actions must be supported (all or nothing) by remote
 * playback routes that offer optional queuing capabilities.
 * </p><ul>
 * <li>{@link #ACTION_ENQUEUE Enqueue}: Enqueues content specified by a given Uri
 * and returns a new media item id to describe the request.  Implicitly creates a new
 * session if no session id was specified as a parameter.
 * <li>{@link #ACTION_REMOVE Remove}: Removes a specified media item from the queue.
 * </ul>
 *
 * <h4>Session actions</h4>
 * <p>
 * The following session actions must be supported (all or nothing) by remote
 * playback routes that offer optional session management capabilities.
 * </p><ul>
 * <li>{@link #ACTION_START_SESSION Start session}: Starts a new session explicitly.
 * <li>{@link #ACTION_GET_SESSION_STATUS Get session status}: Gets the status of a session.
 * <li>{@link #ACTION_END_SESSION End session}: Ends a session explicitly.
 * </ul>
 *
 * <h4>Implementation note</h4>
 * <p>
 * Implementations of the remote playback protocol must implement <em>all</em> of the
 * documented actions, parameters and results.  Note that the documentation is written from
 * the perspective of a client of the protocol.  In particular, whenever a parameter
 * is described as being "optional", it is only from the perspective of the client.
 * Compliant media route provider implementations of this protocol must support all
 * of the features described herein.
 * </p>
 */
public final class MediaControlIntent {
    /* Route categories. */

    /**
     * Media control category: Live audio.
     * <p>
     * A route that supports live audio routing will allow the media audio stream
     * to be sent to supported destinations.  This can include internal speakers or
     * audio jacks on the device itself, A2DP devices, and more.
     * </p><p>
     * When a live audio route is selected, audio routing is transparent to the application.
     * All audio played on the media stream will be routed to the selected destination.
     * </p><p>
     * Refer to the class documentation for details about live audio routes.
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
     * </p><p>
     * Refer to the class documentation for details about live video routes.
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
     * </p><p>
     * Refer to the class documentation for details about remote playback routes.
     * </p>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     */
    public static final String CATEGORY_REMOTE_PLAYBACK =
            "android.media.intent.category.REMOTE_PLAYBACK";

    /* Remote playback actions that affect individual items. */

    /**
     * Remote playback media control action: Play media item.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to start playing content with
     * the {@link Uri} specified in the {@link Intent}'s {@link Intent#getData() data uri}.
     * The action returns a media session id and media item id which can be used
     * to control playback using other remote playback actions.
     * </p><p>
     * Once initiated, playback of the specified content will be managed independently
     * by the destination.  The application will receive status updates as the state
     * of the media item changes.
     * </p><p>
     * If the data uri specifies an HTTP or HTTPS scheme, then the destination is
     * responsible for following HTTP redirects to a reasonable depth of at least 3
     * levels as might typically be handled by a web browser.  If an HTTP error
     * occurs, then the destination should send a {@link MediaItemStatus status update}
     * back to the client indicating the {@link MediaItemStatus#PLAYBACK_STATE_ERROR error}
     * {@link MediaItemStatus#getPlaybackState() playback state}.
     * </p>
     *
     * <h3>One item at a time</h3>
     * <p>
     * Each successful play action <em>replaces</em> the previous play action.
     * If an item is already playing, then it is canceled, the session's playback queue
     * is cleared and the new item begins playing immediately (regardless of
     * whether the previously playing item had been paused).
     * </p><p>
     * Play is therefore equivalent to {@link #ACTION_STOP stop} followed by an action
     * to enqueue a new media item to be played immediately.
     * </p>
     *
     * <h3>Sessions</h3>
     * <p>
     * This request has the effect of implicitly creating a media session whenever the
     * application does not specify the {@link #EXTRA_SESSION_ID session id} parameter.
     * Because there can only be at most one valid session at a time, creating a new session
     * has the side-effect of invalidating any existing sessions and their media items,
     * then handling the playback request with a new session.
     * </p><p>
     * If the application specifies an invalid session id, then an error is returned.
     * When this happens, the application should assume that its session
     * is no longer valid.  To obtain a new session, the application may try again
     * and omit the session id parameter.  However, the application should
     * only retry requests due to an explicit action performed by the user,
     * such as the user clicking on a "play" button in the UI, since another
     * application may be trying to take control of the route and the former
     * application should try to stay out of its way.
     * </p><p>
     * For more information on sessions, queues and media items, please refer to the
     * class documentation.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(optional)</em>: Specifies the session id of the
     * session to which the playback request belongs.  If omitted, a new session
     * is created implicitly.
     * <li>{@link #EXTRA_ITEM_CONTENT_POSITION} <em>(optional)</em>: Specifies the initial
     * content playback position as a long integer number of milliseconds from
     * the beginning of the content.
     * <li>{@link #EXTRA_ITEM_METADATA} <em>(optional)</em>: Specifies metadata associated
     * with the content such as the title of a song.
     * <li>{@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER} <em>(optional)</em>: Specifies a
     * {@link PendingIntent} for a broadcast receiver that will receive status updates
     * about the media item.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(always returned)</em>: Specifies the session id of the
     * session that was affected by the request.  This will be a new session in
     * the case where no session id was supplied as a parameter.
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * <li>{@link #EXTRA_ITEM_ID} <em>(always returned)</em>: Specifies an opaque string identifier
     * to use to refer to the media item in subsequent requests such as
     * {@link #ACTION_GET_STATUS}.
     * <li>{@link #EXTRA_ITEM_STATUS} <em>(always returned)</em>: Specifies the initial status of
     * the new media item.
     * </ul>
     *
     * <h3>Status updates</h3>
     * <p>
     * If the client supplies an
     * {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER item status update receiver}
     * then the media route provider is responsible for sending status updates to the receiver
     * when significant media item state changes occur such as when playback starts or
     * stops.  The receiver will not be invoked for content playback position changes.
     * The application may retrieve the current playback position when necessary
     * using the {@link #ACTION_GET_STATUS} request.
     * </p><p>
     * Refer to {@link MediaItemStatus} for details.
     * </p>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if a session id was provided but is unknown or
     * no longer valid, if the item Uri or content type is not supported, or if
     * any other arguments are invalid.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
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
     *             // Playback may be controlled using the returned session and item id.
     *             String sessionId = data.getString(MediaControlIntent.EXTRA_SESSION_ID);
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
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     * @see #ACTION_PAUSE
     * @see #ACTION_RESUME
     * @see #ACTION_STOP
     */
    public static final String ACTION_PLAY = "android.media.intent.action.PLAY";

    /**
     * Remote playback media control action: Enqueue media item.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action works just like {@link #ACTION_PLAY play} except that it does
     * not clear the queue or reset the pause state when it enqueues the
     * new media item into the session's playback queue.  This action only
     * enqueues a media item with no other side-effects on the queue.
     * </p><p>
     * If the queue is currently empty and then the item will play immediately
     * (assuming the queue is not paused).  Otherwise, the item will play
     * after all earlier items in the queue have finished or been removed.
     * </p><p>
     * The enqueue action can be used to create new sessions just like play.
     * Its parameters and results are also the same.  Only the queuing behavior
     * is different.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String ACTION_ENQUEUE = "android.media.intent.action.ENQUEUE";

    /**
     * Remote playback media control action: Seek media item to a new playback position.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to modify the current playback position
     * of the specified media item.
     * </p><p>
     * This action only affects the playback position of the media item; not its playback state.
     * If the playback queue is paused, then seeking sets the position but the item
     * remains paused.  Likewise if the item is playing, then seeking will cause playback
     * to jump to the new position and continue playing from that point.  If the item has
     * not yet started playing, then the new playback position is remembered by the
     * queue and used as the item's initial content position when playback eventually begins.
     * </p><p>
     * If successful, the media item's playback position is changed.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of the session
     * to which the media item belongs.
     * <li>{@link #EXTRA_ITEM_ID} <em>(required)</em>: Specifies the media item id of
     * the media item to seek.
     * <li>{@link #EXTRA_ITEM_CONTENT_POSITION} <em>(required)</em>: Specifies the new
     * content position for playback as a long integer number of milliseconds from
     * the beginning of the content.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * <li>{@link #EXTRA_ITEM_STATUS} <em>(always returned)</em>: Specifies the new status of
     * the media item.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id or media item id are unknown
     * or no longer valid, if the content position is invalid, or if the media item
     * is in a terminal state.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_SEEK = "android.media.intent.action.SEEK";

    /**
     * Remote playback media control action: Get media item playback status
     * and progress information.
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
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of the session
     * to which the media item belongs.
     * <li>{@link #EXTRA_ITEM_ID} <em>(required)</em>: Specifies the media item id of
     * the media item to query.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * <li>{@link #EXTRA_ITEM_STATUS} <em>(always returned)</em>: Specifies the current status of
     * the media item.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id or media item id are unknown
     * or no longer valid.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #EXTRA_ITEM_STATUS_UPDATE_RECEIVER
     */
    public static final String ACTION_GET_STATUS = "android.media.intent.action.GET_STATUS";

    /**
     * Remote playback media control action: Remove media item from session's queue.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action asks a remote playback route to remove the specified media item
     * from the session's playback queue.  If the current item is removed, then
     * playback will proceed to the next media item (assuming the queue has not been
     * paused).
     * </p><p>
     * This action does not affect the pause state of the queue.  If the queue was paused
     * then it remains paused (even if it is now empty) until a resume, stop or play
     * action is issued that causes the pause state to be cleared.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of the session
     * to which the media item belongs.
     * <li>{@link #EXTRA_ITEM_ID} <em>(required)</em>: Specifies the media item id of
     * the media item to remove.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * <li>{@link #EXTRA_ITEM_STATUS} <em>(always returned)</em>: Specifies the new status of
     * the media item.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id or media item id are unknown
     * or no longer valid, or if the media item is in a terminal state (and therefore
     * no longer in the queue).
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_REMOVE = "android.media.intent.action.REMOVE";

    /* Remote playback actions that affect the whole playback queue. */

    /**
     * Remote playback media control action: Pause media playback.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes the playback queue of the specified session to be paused.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of the session
     * whose playback queue is to be paused.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id is unknown or no longer valid.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_RESUME
     */
    public static final String ACTION_PAUSE = "android.media.intent.action.PAUSE";

    /**
     * Remote playback media control action: Resume media playback (unpause).
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes the playback queue of the specified session to be resumed.
     * Reverses the effects of {@link #ACTION_PAUSE}.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of the session
     * whose playback queue is to be resumed.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id is unknown or no longer valid.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #ACTION_PAUSE
     */
    public static final String ACTION_RESUME = "android.media.intent.action.RESUME";

    /**
     * Remote playback media control action: Stop media playback (clear queue and unpause).
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to stop playback, cancel and remove
     * all media items from the session's media item queue and, reset the queue's
     * pause state.
     * </p><p>
     * If successful, the status of all media items in the queue is set to
     * {@link MediaItemStatus#PLAYBACK_STATE_CANCELED canceled} and a status update is sent
     * to the appropriate status update receivers indicating the new status of each item.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of
     * the session whose playback queue is to be stopped (cleared and unpaused).
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id is unknown or no longer valid.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_STOP = "android.media.intent.action.STOP";

    /**
     * Remote playback media control action: Start session.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to invalidate the current session
     * and start a new session.  The new session initially has an empty queue.
     * </p><p>
     * If successful, the status of all media items in the previous session's queue is set to
     * {@link MediaItemStatus#PLAYBACK_STATE_INVALIDATED invalidated} and a status update
     * is sent to the appropriate status update receivers indicating the new status
     * of each item.  The previous session becomes no longer valid and the new session
     * takes control of the route.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS_UPDATE_RECEIVER} <em>(optional)</em>: Specifies a
     * {@link PendingIntent} for a broadcast receiver that will receive status updates
     * about the media session.
     * <li>{@link #EXTRA_MESSAGE_RECEIVER} <em>(optional)</em>: Specifies a
     * {@link PendingIntent} for a broadcast receiver that will receive messages from
     * the media session.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(always returned)</em>: Specifies the session id of the
     * session that was started by the request.  This will always be a brand new session
     * distinct from any other previously created sessions.
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(always returned)</em>: Specifies the
     * status of the media session.
     * </ul>
     *
     * <h3>Status updates</h3>
     * <p>
     * If the client supplies a
     * {@link #EXTRA_SESSION_STATUS_UPDATE_RECEIVER status update receiver}
     * then the media route provider is responsible for sending status updates to the receiver
     * when significant media session state changes occur such as when the session's
     * queue is paused or resumed or when the session is terminated or invalidated.
     * </p><p>
     * Refer to {@link MediaSessionStatus} for details.
     * </p>
     *
     * <h3>Custom messages</h3>
     * <p>
     * If the client supplies a {@link #EXTRA_MESSAGE_RECEIVER message receiver}
     * then the media route provider is responsible for sending messages to the receiver
     * when the session has any messages to send.
     * </p><p>
     * Refer to {@link #EXTRA_MESSAGE} for details.
     * </p>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session could not be created.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_START_SESSION = "android.media.intent.action.START_SESSION";

    /**
     * Remote playback media control action: Get media session status information.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action asks a remote playback route to provide updated status information
     * about the specified media session.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of the
     * session whose status is to be retrieved.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(always returned)</em>: Specifies the
     * current status of the media session.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id is unknown or no longer valid.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     * @see #EXTRA_SESSION_STATUS_UPDATE_RECEIVER
     */
    public static final String ACTION_GET_SESSION_STATUS =
            "android.media.intent.action.GET_SESSION_STATUS";

    /**
     * Remote playback media control action: End session.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to end the specified session.
     * The session becomes no longer valid and the route ceases to be under control
     * of the session.
     * </p><p>
     * If successful, the status of the session is set to
     * {@link MediaSessionStatus#SESSION_STATE_ENDED} and a status update is sent to
     * the session's status update receiver.
     * </p><p>
     * Additionally, the status of all media items in the queue is set to
     * {@link MediaItemStatus#PLAYBACK_STATE_CANCELED canceled} and a status update is sent
     * to the appropriate status update receivers indicating the new status of each item.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of
     * the session to end.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(always returned)</em>: Specifies the
     * status of the media session.
     * </ul>
     *
     * <h3>Errors</h3>
     * <p>
     * This action returns an error if the session id is unknown or no longer valid.
     * In other words, it is an error to attempt to end a session other than the
     * current session.
     * </p><ul>
     * <li>{@link #EXTRA_ERROR_CODE} <em>(optional)</em>: Specifies the cause of the error.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_END_SESSION = "android.media.intent.action.END_SESSION";

    /**
     * Custom media control action: Send {@link #EXTRA_MESSAGE}.
     * <p>
     * This action asks a route to handle a message described by EXTRA_MESSAGE.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of the session
     * to which will handle this message.
     * <li>{@link #EXTRA_MESSAGE} <em>(required)</em>: Specifies the message to send.
     * </ul>
     *
     * <h3>Result data</h3>
     * Any messages defined by each media route provider.
     *
     * <h3>Errors</h3>
     * Any error messages defined by each media route provider.
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     */
    public static final String ACTION_SEND_MESSAGE = "android.media.intent.action.SEND_MESSAGE";

    /* Extras and related constants. */

    /**
     * Bundle extra: Media session id.
     * <p>
     * An opaque unique identifier that identifies the remote playback media session.
     * </p><p>
     * Used with various actions to specify the id of the media session to be controlled.
     * </p><p>
     * Included in broadcast intents sent to
     * {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER item status update receivers} to identify
     * the session to which the item in question belongs.
     * </p><p>
     * Included in broadcast intents sent to
     * {@link #EXTRA_SESSION_STATUS_UPDATE_RECEIVER session status update receivers} to identify
     * the session.
     * </p><p>
     * The value is a unique string value generated by the media route provider
     * to represent one particular media session.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     * @see #ACTION_PAUSE
     * @see #ACTION_RESUME
     * @see #ACTION_STOP
     * @see #ACTION_START_SESSION
     * @see #ACTION_GET_SESSION_STATUS
     * @see #ACTION_END_SESSION
     */
    public static final String EXTRA_SESSION_ID =
            "android.media.intent.extra.SESSION_ID";

    /**
     * Bundle extra: Media session status.
     * <p>
     * Returned as a result from media session actions such as {@link #ACTION_START_SESSION},
     * {@link #ACTION_PAUSE}, and {@link #ACTION_GET_SESSION_STATUS}
     * to describe the status of the specified media session.
     * </p><p>
     * Included in broadcast intents sent to
     * {@link #EXTRA_SESSION_STATUS_UPDATE_RECEIVER session status update receivers} to provide
     * updated status information.
     * </p><p>
     * The value is a {@link android.os.Bundle} of data that can be converted into
     * a {@link MediaSessionStatus} object using
     * {@link MediaSessionStatus#fromBundle MediaSessionStatus.fromBundle}.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     * @see #ACTION_PAUSE
     * @see #ACTION_RESUME
     * @see #ACTION_STOP
     * @see #ACTION_START_SESSION
     * @see #ACTION_GET_SESSION_STATUS
     * @see #ACTION_END_SESSION
     */
    public static final String EXTRA_SESSION_STATUS =
            "android.media.intent.extra.SESSION_STATUS";

    /**
     * Bundle extra: Media session status update receiver.
     * <p>
     * Used with {@link #ACTION_START_SESSION} to specify a {@link PendingIntent} for a
     * broadcast receiver that will receive status updates about the media session.
     * </p><p>
     * Whenever the status of the media session changes, the media route provider will
     * send a broadcast to the pending intent with extras that identify the session
     * id and its updated status.
     * </p><p>
     * The value is a {@link PendingIntent}.
     * </p>
     *
     * <h3>Broadcast extras</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of
     * the session.
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(required)</em>: Specifies the status of the
     * session as a bundle that can be decoded into a {@link MediaSessionStatus} object.
     * </ul>
     *
     * @see #ACTION_START_SESSION
     */
    public static final String EXTRA_SESSION_STATUS_UPDATE_RECEIVER =
            "android.media.intent.extra.SESSION_STATUS_UPDATE_RECEIVER";

    /**
     * Bundle extra: Media message receiver.
     * <p>
     * Used with {@link #ACTION_START_SESSION} to specify a {@link PendingIntent} for a
     * broadcast receiver that will receive messages from the media session.
     * </p><p>
     * When the media session has a message to send, the media route provider will
     * send a broadcast to the pending intent with extras that identify the session
     * id and its message.
     * </p><p>
     * The value is a {@link PendingIntent}.
     * </p>
     *
     * <h3>Broadcast extras</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of
     * the session.
     * <li>{@link #EXTRA_MESSAGE} <em>(required)</em>: Specifies the message from
     * the session as a bundle object.
     * </ul>
     *
     * @see #ACTION_START_SESSION
     */
    public static final String EXTRA_MESSAGE_RECEIVER =
            "android.media.intent.extra.MESSAGE_RECEIVER";

    /**
     * Bundle extra: Media item id.
     * <p>
     * An opaque unique identifier returned as a result from {@link #ACTION_PLAY} or
     * {@link #ACTION_ENQUEUE} that represents the media item that was created by the
     * playback request.
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
     * @see #ACTION_ENQUEUE
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_ITEM_ID =
            "android.media.intent.extra.ITEM_ID";

    /**
     * Bundle extra: Media item status.
     * <p>
     * Returned as a result from media item actions such as {@link #ACTION_PLAY},
     * {@link #ACTION_ENQUEUE}, {@link #ACTION_SEEK}, and {@link #ACTION_GET_STATUS}
     * to describe the status of the specified media item.
     * </p><p>
     * Included in broadcast intents sent to
     * {@link #EXTRA_ITEM_STATUS_UPDATE_RECEIVER item status update receivers} to provide
     * updated status information.
     * </p><p>
     * The value is a {@link android.os.Bundle} of data that can be converted into
     * a {@link MediaItemStatus} object using
     * {@link MediaItemStatus#fromBundle MediaItemStatus.fromBundle}.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_ENQUEUE
     * @see #ACTION_SEEK
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_ITEM_STATUS =
            "android.media.intent.extra.ITEM_STATUS";

    /**
     * Long extra: Media item content position.
     * <p>
     * Used with {@link #ACTION_PLAY} or {@link #ACTION_ENQUEUE} to specify the
     * starting playback position.
     * </p><p>
     * Used with {@link #ACTION_SEEK} to set a new playback position.
     * </p><p>
     * The value is a long integer number of milliseconds from the beginning of the content.
     * <p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_ENQUEUE
     * @see #ACTION_SEEK
     */
    public static final String EXTRA_ITEM_CONTENT_POSITION =
            "android.media.intent.extra.ITEM_POSITION";

    /**
     * Bundle extra: Media item metadata.
     * <p>
     * Used with {@link #ACTION_PLAY} or {@link #ACTION_ENQUEUE} to specify metadata
     * associated with the content of a media item.
     * </p><p>
     * The value is a {@link android.os.Bundle} of metadata key-value pairs as defined
     * in {@link MediaItemMetadata}.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_ENQUEUE
     */
    public static final String EXTRA_ITEM_METADATA =
            "android.media.intent.extra.ITEM_METADATA";

    /**
     * Bundle extra: HTTP request headers.
     * <p>
     * Used with {@link #ACTION_PLAY} or {@link #ACTION_ENQUEUE} to specify HTTP request
     * headers to be included when fetching to the content indicated by the media
     * item's data Uri.
     * </p><p>
     * This extra may be used to provide authentication tokens and other
     * parameters to the server separately from the media item's data Uri.
     * </p><p>
     * The value is a {@link android.os.Bundle} of string based key-value pairs
     * that describe the HTTP request headers.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_ENQUEUE
     */
    public static final String EXTRA_ITEM_HTTP_HEADERS =
            "android.media.intent.extra.HTTP_HEADERS";

    /**
     * Bundle extra: Media item status update receiver.
     * <p>
     * Used with {@link #ACTION_PLAY} or {@link #ACTION_ENQUEUE} to specify
     * a {@link PendingIntent} for a
     * broadcast receiver that will receive status updates about a particular
     * media item.
     * </p><p>
     * Whenever the status of the media item changes, the media route provider will
     * send a broadcast to the pending intent with extras that identify the session
     * to which the item belongs, the session status, the item's id
     * and the item's updated status.
     * </p><p>
     * The same pending intent and broadcast receiver may be shared by any number of
     * media items since the broadcast intent includes the media session id
     * and media item id.
     * </p><p>
     * The value is a {@link PendingIntent}.
     * </p>
     *
     * <h3>Broadcast extras</h3>
     * <ul>
     * <li>{@link #EXTRA_SESSION_ID} <em>(required)</em>: Specifies the session id of
     * the session to which the item in question belongs.
     * <li>{@link #EXTRA_SESSION_STATUS} <em>(optional, old implementations may
     * omit this key)</em>: Specifies the status of the media session.
     * <li>{@link #EXTRA_ITEM_ID} <em>(required)</em>: Specifies the media item id of the
     * media item in question.
     * <li>{@link #EXTRA_ITEM_STATUS} <em>(required)</em>: Specifies the status of the
     * item as a bundle that can be decoded into a {@link MediaItemStatus} object.
     * </ul>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_ENQUEUE
     */
    public static final String EXTRA_ITEM_STATUS_UPDATE_RECEIVER =
            "android.media.intent.extra.ITEM_STATUS_UPDATE_RECEIVER";

    /**
     * Bundle extra: Message.
     * <p>
     * Used with {@link #ACTION_SEND_MESSAGE}, and included in broadcast intents sent to
     * {@link #EXTRA_MESSAGE_RECEIVER message receivers} to describe a message between a
     * session and a media route provider.
     * </p><p>
     * The value is a {@link android.os.Bundle}.
     * </p>
     */
    public static final String EXTRA_MESSAGE = "android.media.intent.extra.MESSAGE";

    /**
     * Integer extra: Error code.
     * <p>
     * Used with all media control requests to describe the cause of an error.
     * This extra may be omitted when the error is unknown.
     * </p><p>
     * The value is one of: {@link #ERROR_UNKNOWN}, {@link #ERROR_UNSUPPORTED_OPERATION},
     * {@link #ERROR_INVALID_SESSION_ID}, {@link #ERROR_INVALID_ITEM_ID}.
     * </p>
     */
    public static final String EXTRA_ERROR_CODE = "android.media.intent.extra.ERROR_CODE";

    /**
     * Error code: An unknown error occurred.
     *
     * @see #EXTRA_ERROR_CODE
     */
    public static final int ERROR_UNKNOWN = 0;

    /**
     * Error code: The operation is not supported.
     *
     * @see #EXTRA_ERROR_CODE
     */
    public static final int ERROR_UNSUPPORTED_OPERATION = 1;

    /**
     * Error code: The session id specified in the request was invalid.
     *
     * @see #EXTRA_ERROR_CODE
     */
    public static final int ERROR_INVALID_SESSION_ID = 2;

    /**
     * Error code: The item id specified in the request was invalid.
     *
     * @see #EXTRA_ERROR_CODE
     */
    public static final int ERROR_INVALID_ITEM_ID = 3;

    private MediaControlIntent() {
    }
}
