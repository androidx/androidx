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

package android.support.v4.media;

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
     * <li>{@link #EXTRA_QUEUE_BEHAVIOR} specifies when the content should be played.
     * <li>{@link #EXTRA_STREAM_START_POSITION} specifies the start position of the
     * content stream to play in seconds.
     * <li>{@link #EXTRA_STREAM_END_POSITION} specifies the end position of the
     * content stream to play in seconds.
     * <li>{@link #EXTRA_STREAM_METADATA} specifies metadata associated with the
     * content stream such as the title of a song.
     * </ul>
     *
     * <h3>Result data</h3>
     * <ul>
     * <li>(none)
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
     *                 // request succeeded
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
     * Integer extra: Stream start position.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify the starting playback position in
     * seconds from the beginning of the content stream to be played.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_STREAM_START_POSITION =
            "android.media.intent.extra.STREAM_START_POSITION";

    /**
     * Integer extra: Stream end position.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify the ending playback position in
     * seconds from the beginning of the content stream to be played.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_STREAM_END_POSITION =
            "android.media.intent.extra.STREAM_END_POSITION";

    /**
     * Bundle extra: Stream metadata.
     * <p>
     * Used with {@link #ACTION_PLAY} to specify a {@link android.os.Bundle} of metadata that
     * describes the media content stream to be played.  The valid metadata keys are
     * defined in {@link MediaStreamMetadata}.
     * </p>
     *
     * @see #ACTION_PLAY
     */
    public static final String EXTRA_STREAM_METADATA =
            "android.media.intent.extra.STREAM_METADATA";

    private MediaControlIntent() {
    }
}
