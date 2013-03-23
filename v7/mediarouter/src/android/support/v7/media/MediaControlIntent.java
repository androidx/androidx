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
     * Media control action: Play.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to start playing content with
     * the {@link Uri} specified in the {@link Intent}'s {@link Intent#getData() data uri}.
     * </p><p>
     * Once initiated, playback of the specified content will be queued and managed
     * independently by the destination.  The application will receive status
     * and progress updates as the content is played.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_QUEUE_BEHAVIOR}: specifies when the content should be played.
     * <li>{@link #EXTRA_STREAM_POSITION}: specifies the initial start position of the
     * content stream.
     * <li>{@link #EXTRA_STREAM_METADATA}: specifies metadata associated with the
     * content stream such as the title of a song.
     * <li>{@link #EXTRA_HTTP_HEADERS}: specifies HTTP headers to supply to the
     * server when requesting the content stream.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_STREAM_ID}: specifies a string identifier to use to refer
     * to the media stream in subsequent requests such as {@link #ACTION_SEEK}.
     * </ul>
     *
     * <h3>Example</h3>
     * <pre>
     * MediaRouter mediaRouter = MediaRouter.getInstance(context);
     * MediaRouter.RouteInfo route = mediaRouter.getSelectedRoute();
     * Intent intent = new Intent(MediaControlIntent.ACTION_PLAY);
     * intent.addCategory(MediaControlIntent.CATEGORY_REMOTE_PLAYBACK);
     * intent.setDataAndType("http://example.com/videos/movie.mp4", "video/mp4");
     * intent.putExtra(MediaControlIntent.EXTRA_QUEUE_BEHAVIOR,
     *         MediaControlIntent.QUEUE_BEHAVIOR_PLAY_NEXT);
     * if (route.supportsControlRequest(intent)) {
     *     MediaRouter.ControlRequestCallback callback = new MediaRouter.ControlRequestCallback() {
     *         public void onResult(int result, Bundle data) {
     *             if (result == REQUEST_SUCCEEDED) {
     *                 // The request succeeded.
     *                 // The content stream may be controlled using the returned stream id...
     *                 String id = data.getStringExtra(MediaControlIntent.EXTRA_STREAM_ID);
     *             }
     *         }
     *     };
     *     route.sendControlRequest(intent, callback);
     * }</pre>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_PLAY = "android.media.intent.action.PLAY";

    /**
     * Media control action: Seek to a new playback position.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to modify the current playback
     * position of the specified media stream.
     * </p><p>
     * This action should generally not affect the current playback state of the media stream.
     * If the stream is paused, then seeking should set the position but leave
     * the stream paused.  Likewise if the stream is playing, then seeking should
     * continue playing from the new position.  If the stream has not yet started
     * playing, then the new playback position should be remembered and used as the
     * initial position for the stream.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_STREAM_ID}: specifies the stream id of the stream to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * <li>{@link #EXTRA_STREAM_POSITION}: specifies the new position of the
     * content stream.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>(none)
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_SEEK = "android.media.intent.action.SEEK";

    /**
     * Media control action: Pause playback.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to pause playback of the
     * specified stream.
     * </p><p>
     * If the stream has not started playing yet, then the request to pause should
     * be remembered such that the stream will initially start in a paused state.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_STREAM_ID}: specifies the stream id of the stream to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>(none)
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_PAUSE = "android.media.intent.action.PAUSE";

    /**
     * Media control action: Resume playback (unpause).
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action causes a remote playback route to resume playback of the
     * specified stream.  Reverses the effects of {@link #ACTION_PAUSE}.
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_STREAM_ID}: specifies the stream id of the stream to be
     * controlled.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>(none)
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_RESUME = "android.media.intent.action.RESUME";

    /**
     * Media control action: Get status.
     * <p>
     * Used with routes that support {@link #CATEGORY_REMOTE_PLAYBACK remote playback}
     * media control.
     * </p><p>
     * This action asks a remote playback route to provide updated status information
     * about playback of the specified stream.
     * </p><p>
     * If the route caches this information, it must take care to ensure that
     * it is in sync with all previously completed control actions.  For example,
     * if an applications sends {@link #ACTION_PAUSE} to pause a content stream,
     * waits for the callback to indicate that the pause completed successfully,
     * then sends {@link #ACTION_GET_STATUS} to query the current status, it
     * must consistently observe that the stream is now paused (assuming no other
     * application has acted upon the content stream in the meantime).
     * </p>
     *
     * <h3>Request parameters</h3>
     * <ul>
     * <li>{@link #EXTRA_STREAM_ID}: specifies the stream id of the stream to be
     * queried.  This value was returned as a result from the
     * {@link #ACTION_PLAY play} action.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>{@link #EXTRA_STREAM_STATUS}: specifies the status of the stream.
     * </ul>
     *
     * @see MediaRouter.RouteInfo#sendControlRequest
     * @see #CATEGORY_REMOTE_PLAYBACK
     */
    public static final String ACTION_GET_STATUS = "android.media.intent.action.GET_STATUS";

    /**
     * Integer extra: Queue behavior.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify when the requested content should be
     * played.  The default is to play the content immediately.
     * </p><p>
     * The value must be one of {@link #QUEUE_BEHAVIOR_PLAY_NOW},
     * {@link #QUEUE_BEHAVIOR_PLAY_NEXT}, or {@link #QUEUE_BEHAVIOR_PLAY_LATER}.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_QUEUE_BEHAVIOR =
            "android.media.intent.extra.QUEUE_BEHAVIOR";

    /**
     * Value for {@link #EXTRA_QUEUE_BEHAVIOR}: Play now.
     * <p>
     * Requests that the new content play immediately, cancelling the currently playing
     * media item.  (This is the default queue behavior.)
     * </p>
     *
     * @see #EXTRA_QUEUE_BEHAVIOR
     */
    public final static int QUEUE_BEHAVIOR_PLAY_NOW = 0;

    /**
     * Value for {@link #EXTRA_QUEUE_BEHAVIOR}: Play next.
     * <p>
     * Requests that the new content be enqueued to play next after the currently playing
     * media item.
     * </p>
     *
     * @see #EXTRA_QUEUE_BEHAVIOR
     */
    public final static int QUEUE_BEHAVIOR_PLAY_NEXT = 1;

    /**
     * Value for {@link #EXTRA_QUEUE_BEHAVIOR}: Play later.
     * <p>
     * Requests that the new content be enqueued to play later after all other items
     * currently in the queue.
     * </p>
     *
     * @see #EXTRA_QUEUE_BEHAVIOR
     */
    public final static int QUEUE_BEHAVIOR_PLAY_LATER = 2;

    /**
     * Integer extra: Stream position.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify the starting playback position.
     * </p><p>
     * Used with {@link #ACTION_SEEK} to set a new playback position.
     * </p><p>
     * The value is an integer number of sections from the beginning of the
     * content stream.
     * <p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_SEEK
     */
    public static final String EXTRA_STREAM_POSITION =
            "android.media.intent.extra.STREAM_POSITION";

    /**
     * Bundle extra: Stream metadata.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify metadata associated with a
     * content stream.
     * </p><p>
     * The value is a {@link android.os.Bundle} of metadata keys and values as defined
     * in {@link MediaStreamMetadata}.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_STREAM_METADATA =
            "android.media.intent.extra.STREAM_METADATA";

    /**
     * Bundle extra: Stream id.
     * <p>
     * Returned as a result from {@link #ACTION_PLAY} to provide a unique id
     * for the requested content stream which may then be used to issue subsequent
     * requests to control that content stream.
     * </p><p>
     * Used with various actions to specify the id of the stream to be controlled.
     * </p><p>
     * The value is a unique string value generated by the media route provider
     * to represent one particular content stream.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_SEEK
     * @see #ACTION_PAUSE
     * @see #ACTION_RESUME
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_STREAM_ID =
            "android.media.intent.extra.STREAM_ID";

    /**
     * Bundle extra: Stream id.
     * <p>
     * Returned as a result from {@link #ACTION_GET_STATUS} to describe the
     * status of the route.
     * </p><p>
     * The value is a {@link android.os.Bundle} of status keys and values as defined
     * in {@link MediaStreamStatus}.
     * </p>
     *
     * @see #ACTION_PLAY
     * @see #ACTION_SEEK
     * @see #ACTION_PAUSE
     * @see #ACTION_RESUME
     * @see #ACTION_GET_STATUS
     */
    public static final String EXTRA_STREAM_STATUS =
            "android.media.intent.extra.STREAM_STATUS";

    /**
     * Bundle extra: HTTP headers.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify HTTP headers to be included when
     * connecting to the media stream indicated by the data Uri.
     * </p><p>
     * This extra may be used to provide authentication tokens and other
     * parameters to the server separately from the media stream's data Uri.
     * </p><p>
     * The value is a {@link android.os.Bundle} of string based key value pairs
     * that describe the HTTP headers.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_HTTP_HEADERS =
            "android.media.intent.extra.HTTP_HEADERS";

    private MediaControlIntent() {
    }
}
