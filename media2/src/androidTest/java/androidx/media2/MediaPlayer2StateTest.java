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
package androidx.media2;

import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_ATTACH_AUX_EFFECT;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_DESELECT_TRACK;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_LOOP_CURRENT;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_PAUSE;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_PLAY;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_PREPARE;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SEEK_TO;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SELECT_TRACK;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_AUDIO_ATTRIBUTES;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_AUDIO_SESSION_ID;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCE;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCES;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_PLAYER_VOLUME;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SET_SURFACE;
import static androidx.media2.MediaPlayer2.CALL_COMPLETED_SKIP_TO_NEXT;
import static androidx.media2.MediaPlayer2.CALL_STATUS_NO_ERROR;
import static androidx.media2.MediaPlayer2.PLAYER_STATE_ERROR;
import static androidx.media2.MediaPlayer2.PLAYER_STATE_IDLE;
import static androidx.media2.MediaPlayer2.PLAYER_STATE_PAUSED;
import static androidx.media2.MediaPlayer2.PLAYER_STATE_PLAYING;
import static androidx.media2.MediaPlayer2.PLAYER_STATE_PREPARED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import android.os.Build;
import android.util.Pair;

import androidx.media.AudioAttributesCompat;
import androidx.media2.MediaPlayer2.MediaPlayer2State;
import androidx.media2.MediaPlayer2.TrackInfo;
import androidx.media2.TestUtils.Monitor;
import androidx.media2.test.R;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;

@RunWith(Parameterized.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.KITKAT)
public class MediaPlayer2StateTest extends MediaPlayer2TestBase {
    private static final String LOG_TAG = "MediaPlayer2StateTest";

    // TODO: Underlying MediaPlayer1 implementation does not report an error when an operation is
    // triggered in an invalid state. e.g. MediaPlayer.getTrackInfo in the error state. Check the
    // cause and update javadoc of MediaPlayer1 or change the test case.
    private static final boolean CHECK_INVALID_STATE = false;

    // Used for testing case that operation is called before setDataSourceDesc().
    private static final int MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE = 400001;

    private static final MediaItem sDummyDataSource = new CallbackMediaItem.Builder(
            new DataSourceCallback() {
                @Override
                public int readAt(long position, byte[] buffer, int offset, int size)
                        throws IOException {
                    return -1;
                }

                @Override
                public long getSize() throws IOException {
                    return -1;  // Unknown size
                }

                @Override
                public void close() throws IOException {}
            })
            .build();

    private static final PlayerOperation sCloseOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.close();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;  // synchronous operation.
        }

        @Override
        public String toString() {
            return "close()";
        }
    };
    private static final PlayerOperation sPlayOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.play();
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_PLAY;
        }

        @Override
        public String toString() {
            return "play()";
        }
    };
    private static final PlayerOperation sPrepareOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.prepare();
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_PREPARE;
        }

        @Override
        public String toString() {
            return "prepare()";
        }
    };
    private static final PlayerOperation sPauseOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.pause();
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_PAUSE;
        }

        @Override
        public String toString() {
            return "pause()";
        }
    };
    private static final PlayerOperation sSkipToNextOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.skipToNext();
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SKIP_TO_NEXT;
        }

        @Override
        public String toString() {
            return "skipToNext()";
        }
    };
    private static final PlayerOperation sSeekToOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.seekTo(1000);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SEEK_TO;
        }

        @Override
        public String toString() {
            return "seekTo()";
        }
    };
    private static final PlayerOperation sGetCurrentPositionOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getCurrentPosition();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getCurrentPosition()";
        }
    };
    private static final PlayerOperation sGetDurationOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getDuration();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getDuration()";
        }
    };
    private static final PlayerOperation sGetBufferedPositionOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getBufferedPosition();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getBufferedPosition()";
        }
    };
    private static final PlayerOperation sGetStateOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getState();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getState()";
        }
    };
    private static final PlayerOperation sSetAudioAttributesOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setAudioAttributes(
                    new AudioAttributesCompat.Builder().setContentType(CONTENT_TYPE_MUSIC).build());
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_AUDIO_ATTRIBUTES;
        }

        @Override
        public String toString() {
            return "setAudioAttributes()";
        }
    };
    private static final PlayerOperation sGetAudioAttributesOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getAudioAttributes();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getAudioAttributes()";
        }
    };
    private static final PlayerOperation sSetDataSourceOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setMediaItem(sDummyDataSource);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_DATA_SOURCE;
        }

        @Override
        public String toString() {
            return "setMediaItem()";
        }
    };
    private static final PlayerOperation sSetNextDataSourceOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setNextMediaItem(sDummyDataSource);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_NEXT_DATA_SOURCE;
        }

        @Override
        public String toString() {
            return "setNextMediaItem()";
        }
    };
    private static final PlayerOperation sSetNextDataSourcesOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setNextMediaItems(Arrays.asList(sDummyDataSource));
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_NEXT_DATA_SOURCES;
        }

        @Override
        public String toString() {
            return "setNextMediaItems()";
        }
    };
    private static final PlayerOperation sLoopCurrentOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.loopCurrent(true);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_LOOP_CURRENT;
        }

        @Override
        public String toString() {
            return "loopCurrent()";
        }
    };
    private static final PlayerOperation sSetPlayerVolumeOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setPlayerVolume(0.5f);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_PLAYER_VOLUME;
        }

        @Override
        public String toString() {
            return "setPlayerVolume()";
        }
    };
    private static final PlayerOperation sGetPlayerVolumeOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getPlayerVolume();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getPlayerVolume()";
        }
    };
    private static final PlayerOperation sGetMaxPlayerVolumeOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getMaxPlayerVolume();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getMaxPlayerVolume()";
        }
    };
    private static final PlayerOperation sNotifyWhenCommandLabelReachedOperation =
            new PlayerOperation() {
                @Override
                public void doOperation(MediaPlayer2 player) {
                    player.notifyWhenCommandLabelReached(new Object());
                }

                @Override
                public Integer getCallCompleteCode() {
                    return CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED;
                }

                @Override
                public String toString() {
                    return "notifyWhenCommandLabelReached()";
                }
            };
    private static final PlayerOperation sSetSurfaceOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setSurface(null);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_SURFACE;
        }

        @Override
        public String toString() {
            return "setSurface()";
        }
    };
    private static final PlayerOperation sClearPendingCommandsOperation =
            new PlayerOperation() {
                @Override
                public void doOperation(MediaPlayer2 player) {
                    player.clearPendingCommands();
                }

                @Override
                public Integer getCallCompleteCode() {
                    return null;
                }

                @Override
                public String toString() {
                    return "clearPendingCommands()";
                }
            };
    private static final PlayerOperation sGetVideoWidthOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getVideoWidth();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getVideoWidth()";
        }
    };
    private static final PlayerOperation sGetVideoHeightOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getVideoHeight();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getVideoHeight()";
        }
    };
    private static final PlayerOperation sGetMetricsOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getMetrics();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getMetrics()";
        }
    };
    private static final PlayerOperation sSetPlaybackParamsOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setPlaybackParams(new PlaybackParams.Builder().setSpeed(1.0f).build());
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_PLAYBACK_PARAMS;
        }

        @Override
        public String toString() {
            return "setPlaybackParams()";
        }
    };
    private static final PlayerOperation sGetPlaybackParamsOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getPlaybackParams();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getPlaybackParams()";
        }
    };
    private static final PlayerOperation sGetTimestampOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getTimestamp();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getTimestamp()";
        }
    };
    private static final PlayerOperation sResetOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.reset();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "reset()";
        }
    };
    private static final PlayerOperation sSetAudioSessionIdOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setAudioSessionId(0);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_AUDIO_SESSION_ID;
        }

        @Override
        public String toString() {
            return "setAudioSessionId()";
        }
    };
    private static final PlayerOperation sGetAudioSessionIdOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getAudioSessionId();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getAudioSessionId()";
        }
    };
    private static final PlayerOperation sAttachAuxEffectOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.attachAuxEffect(0);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_ATTACH_AUX_EFFECT;
        }

        @Override
        public String toString() {
            return "attachAuxEffect()";
        }
    };
    private static final PlayerOperation sSetAuxEffectSendLevelOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setAuxEffectSendLevel(0.5f);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL;
        }

        @Override
        public String toString() {
            return "setAuxEffectSendLevel()";
        }
    };
    private static final PlayerOperation sGetTrackInfoOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getTrackInfo();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getTrackInfo()";
        }
    };
    private static final PlayerOperation sGetSelectedTrackOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.getSelectedTrack(TrackInfo.MEDIA_TRACK_TYPE_VIDEO);
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "getSelectedTrack()";
        }
    };
    private static final PlayerOperation sSelectTrackOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.selectTrack(1);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SELECT_TRACK;
        }

        @Override
        public String toString() {
            return "selectTrack()";
        }
    };
    private static final PlayerOperation sDeselectTrackOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.deselectTrack(1);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_DESELECT_TRACK;
        }

        @Override
        public String toString() {
            return "deselectTrack()";
        }
    };
    private static final PlayerOperation sSetEventCallbackOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setEventCallback(
                    Executors.newFixedThreadPool(1), new MediaPlayer2.EventCallback(){});
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "setEventCallback()";
        }
    };
    private static final PlayerOperation sClearEventCallbackOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.clearEventCallback();
        }

        @Override
        public Integer getCallCompleteCode() {
            return null;
        }

        @Override
        public String toString() {
            return "clearEventCallback()";
        }
    };

    private @MediaPlayer2State int mTestState;
    private PlayerOperation mTestOperation;
    private boolean mIsValidOperation;

    @Parameterized.Parameters(name = "{index}: operation={0} state={1} valid={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { sCloseOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sCloseOperation, PLAYER_STATE_IDLE, true },
                { sCloseOperation, PLAYER_STATE_PREPARED, true },
                { sCloseOperation, PLAYER_STATE_PAUSED, true },
                { sCloseOperation, PLAYER_STATE_PLAYING, true },
                { sCloseOperation, PLAYER_STATE_ERROR, true },

                { sPlayOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sPlayOperation, PLAYER_STATE_IDLE, false },
                { sPlayOperation, PLAYER_STATE_PREPARED, true },
                { sPlayOperation, PLAYER_STATE_PAUSED, true },
                { sPlayOperation, PLAYER_STATE_PLAYING, true },
                { sPlayOperation, PLAYER_STATE_ERROR, false },

                { sPrepareOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sPrepareOperation, PLAYER_STATE_IDLE, true },
                { sPrepareOperation, PLAYER_STATE_PREPARED, false },
                { sPrepareOperation, PLAYER_STATE_PAUSED, false },
                { sPrepareOperation, PLAYER_STATE_PLAYING, false },
                { sPrepareOperation, PLAYER_STATE_ERROR, false },

                { sPauseOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sPauseOperation, PLAYER_STATE_IDLE, false },
                { sPauseOperation, PLAYER_STATE_PREPARED, true },
                { sPauseOperation, PLAYER_STATE_PAUSED, true },
                { sPauseOperation, PLAYER_STATE_PLAYING, true },
                { sPauseOperation, PLAYER_STATE_ERROR, false },

                { sSkipToNextOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSkipToNextOperation, PLAYER_STATE_IDLE, true },
                { sSkipToNextOperation, PLAYER_STATE_PREPARED, true },
                { sSkipToNextOperation, PLAYER_STATE_PAUSED, true },
                { sSkipToNextOperation, PLAYER_STATE_PLAYING, true },
                { sSkipToNextOperation, PLAYER_STATE_ERROR, false },

                { sSeekToOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSeekToOperation, PLAYER_STATE_IDLE, false },
                { sSeekToOperation, PLAYER_STATE_PREPARED, true },
                { sSeekToOperation, PLAYER_STATE_PAUSED, true },
                { sSeekToOperation, PLAYER_STATE_PLAYING, true },
                { sSeekToOperation, PLAYER_STATE_ERROR, false },

                { sGetCurrentPositionOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sGetCurrentPositionOperation, PLAYER_STATE_IDLE, false },
                { sGetCurrentPositionOperation, PLAYER_STATE_PREPARED, true },
                { sGetCurrentPositionOperation, PLAYER_STATE_PAUSED, true },
                { sGetCurrentPositionOperation, PLAYER_STATE_PLAYING, true },
                { sGetCurrentPositionOperation, PLAYER_STATE_ERROR, false },

                { sGetDurationOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sGetDurationOperation, PLAYER_STATE_IDLE, false },
                { sGetDurationOperation, PLAYER_STATE_PREPARED, true },
                { sGetDurationOperation, PLAYER_STATE_PAUSED, true },
                { sGetDurationOperation, PLAYER_STATE_PLAYING, true },
                { sGetDurationOperation, PLAYER_STATE_ERROR, false },

                { sGetBufferedPositionOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sGetBufferedPositionOperation, PLAYER_STATE_IDLE, false },
                { sGetBufferedPositionOperation, PLAYER_STATE_PREPARED, true },
                { sGetBufferedPositionOperation, PLAYER_STATE_PAUSED, true },
                { sGetBufferedPositionOperation, PLAYER_STATE_PLAYING, true },
                { sGetBufferedPositionOperation, PLAYER_STATE_ERROR, false },

                { sGetStateOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetStateOperation, PLAYER_STATE_IDLE, true },
                { sGetStateOperation, PLAYER_STATE_PREPARED, true },
                { sGetStateOperation, PLAYER_STATE_PAUSED, true },
                { sGetStateOperation, PLAYER_STATE_PLAYING, true },
                { sGetStateOperation, PLAYER_STATE_ERROR, true },

                { sSetAudioAttributesOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetAudioAttributesOperation, PLAYER_STATE_IDLE, true },
                { sSetAudioAttributesOperation, PLAYER_STATE_PREPARED, true },
                { sSetAudioAttributesOperation, PLAYER_STATE_PAUSED, true },
                { sSetAudioAttributesOperation, PLAYER_STATE_PLAYING, true },
                { sSetAudioAttributesOperation, PLAYER_STATE_ERROR, false },

                { sSetDataSourceOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetDataSourceOperation, PLAYER_STATE_IDLE, false },
                { sSetDataSourceOperation, PLAYER_STATE_PREPARED, false },
                { sSetDataSourceOperation, PLAYER_STATE_PAUSED, false },
                { sSetDataSourceOperation, PLAYER_STATE_PLAYING, false },
                { sSetDataSourceOperation, PLAYER_STATE_ERROR, false },

                { sSetNextDataSourceOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSetNextDataSourceOperation, PLAYER_STATE_IDLE, true },
                { sSetNextDataSourceOperation, PLAYER_STATE_PREPARED, true },
                { sSetNextDataSourceOperation, PLAYER_STATE_PAUSED, true },
                { sSetNextDataSourceOperation, PLAYER_STATE_PLAYING, true },
                { sSetNextDataSourceOperation, PLAYER_STATE_ERROR, false },

                { sSetNextDataSourcesOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSetNextDataSourcesOperation, PLAYER_STATE_IDLE, true },
                { sSetNextDataSourcesOperation, PLAYER_STATE_PREPARED, true },
                { sSetNextDataSourcesOperation, PLAYER_STATE_PAUSED, true },
                { sSetNextDataSourcesOperation, PLAYER_STATE_PLAYING, true },
                { sSetNextDataSourcesOperation, PLAYER_STATE_ERROR, false },

                { sLoopCurrentOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sLoopCurrentOperation, PLAYER_STATE_IDLE, true },
                { sLoopCurrentOperation, PLAYER_STATE_PREPARED, true },
                { sLoopCurrentOperation, PLAYER_STATE_PAUSED, true },
                { sLoopCurrentOperation, PLAYER_STATE_PLAYING, true },
                { sLoopCurrentOperation, PLAYER_STATE_ERROR, false },

                { sSetPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetPlayerVolumeOperation, PLAYER_STATE_IDLE, true },
                { sSetPlayerVolumeOperation, PLAYER_STATE_PREPARED, true },
                { sSetPlayerVolumeOperation, PLAYER_STATE_PAUSED, true },
                { sSetPlayerVolumeOperation, PLAYER_STATE_PLAYING, true },
                { sSetPlayerVolumeOperation, PLAYER_STATE_ERROR, false },

                { sGetPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetPlayerVolumeOperation, PLAYER_STATE_IDLE, true },
                { sGetPlayerVolumeOperation, PLAYER_STATE_PREPARED, true },
                { sGetPlayerVolumeOperation, PLAYER_STATE_PAUSED, true },
                { sGetPlayerVolumeOperation, PLAYER_STATE_PLAYING, true },
                { sGetPlayerVolumeOperation, PLAYER_STATE_ERROR, false },

                { sGetMaxPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetMaxPlayerVolumeOperation, PLAYER_STATE_IDLE, true },
                { sGetMaxPlayerVolumeOperation, PLAYER_STATE_PREPARED, true },
                { sGetMaxPlayerVolumeOperation, PLAYER_STATE_PAUSED, true },
                { sGetMaxPlayerVolumeOperation, PLAYER_STATE_PLAYING, true },
                { sGetMaxPlayerVolumeOperation, PLAYER_STATE_ERROR, false },

                { sNotifyWhenCommandLabelReachedOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE,
                        true },
                { sNotifyWhenCommandLabelReachedOperation, PLAYER_STATE_IDLE, true },
                { sNotifyWhenCommandLabelReachedOperation, PLAYER_STATE_PREPARED, true },
                { sNotifyWhenCommandLabelReachedOperation, PLAYER_STATE_PAUSED, true },
                { sNotifyWhenCommandLabelReachedOperation, PLAYER_STATE_PLAYING, true },
                { sNotifyWhenCommandLabelReachedOperation, PLAYER_STATE_ERROR, true },

                { sSetSurfaceOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetSurfaceOperation, PLAYER_STATE_IDLE, true },
                { sSetSurfaceOperation, PLAYER_STATE_PREPARED, true },
                { sSetSurfaceOperation, PLAYER_STATE_PAUSED, true },
                { sSetSurfaceOperation, PLAYER_STATE_PLAYING, true },
                { sSetSurfaceOperation, PLAYER_STATE_ERROR, false },

                { sClearPendingCommandsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sClearPendingCommandsOperation, PLAYER_STATE_IDLE, true },
                { sClearPendingCommandsOperation, PLAYER_STATE_PREPARED, true },
                { sClearPendingCommandsOperation, PLAYER_STATE_PAUSED, true },
                { sClearPendingCommandsOperation, PLAYER_STATE_PLAYING, true },
                { sClearPendingCommandsOperation, PLAYER_STATE_ERROR, true },

                { sGetVideoWidthOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetVideoWidthOperation, PLAYER_STATE_IDLE, true },
                { sGetVideoWidthOperation, PLAYER_STATE_PREPARED, true },
                { sGetVideoWidthOperation, PLAYER_STATE_PAUSED, true },
                { sGetVideoWidthOperation, PLAYER_STATE_PLAYING, true },
                { sGetVideoWidthOperation, PLAYER_STATE_ERROR, false },

                { sGetVideoHeightOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetVideoHeightOperation, PLAYER_STATE_IDLE, true },
                { sGetVideoHeightOperation, PLAYER_STATE_PREPARED, true },
                { sGetVideoHeightOperation, PLAYER_STATE_PAUSED, true },
                { sGetVideoHeightOperation, PLAYER_STATE_PLAYING, true },
                { sGetVideoHeightOperation, PLAYER_STATE_ERROR, false },

                { sGetMetricsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetMetricsOperation, PLAYER_STATE_IDLE, true },
                { sGetMetricsOperation, PLAYER_STATE_PREPARED, true },
                { sGetMetricsOperation, PLAYER_STATE_PAUSED, true },
                { sGetMetricsOperation, PLAYER_STATE_PLAYING, true },
                { sGetMetricsOperation, PLAYER_STATE_ERROR, false },

                { sSetPlaybackParamsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetPlaybackParamsOperation, PLAYER_STATE_IDLE, true },
                { sSetPlaybackParamsOperation, PLAYER_STATE_PREPARED, true },
                { sSetPlaybackParamsOperation, PLAYER_STATE_PAUSED, true },
                { sSetPlaybackParamsOperation, PLAYER_STATE_PLAYING, true },
                { sSetPlaybackParamsOperation, PLAYER_STATE_ERROR, false },

                { sGetPlaybackParamsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sGetPlaybackParamsOperation, PLAYER_STATE_IDLE, true },
                { sGetPlaybackParamsOperation, PLAYER_STATE_PREPARED, true },
                { sGetPlaybackParamsOperation, PLAYER_STATE_PAUSED, true },
                { sGetPlaybackParamsOperation, PLAYER_STATE_PLAYING, true },
                { sGetPlaybackParamsOperation, PLAYER_STATE_ERROR, false },

                { sGetTimestampOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetTimestampOperation, PLAYER_STATE_IDLE, true },
                { sGetTimestampOperation, PLAYER_STATE_PREPARED, true },
                { sGetTimestampOperation, PLAYER_STATE_PAUSED, true },
                { sGetTimestampOperation, PLAYER_STATE_PLAYING, true },
                { sGetTimestampOperation, PLAYER_STATE_ERROR, false },

                { sResetOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sResetOperation, PLAYER_STATE_IDLE, true },
                { sResetOperation, PLAYER_STATE_PREPARED, true },
                { sResetOperation, PLAYER_STATE_PAUSED, true },
                { sResetOperation, PLAYER_STATE_PLAYING, true },
                { sResetOperation, PLAYER_STATE_ERROR, true },

                { sSetAudioSessionIdOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetAudioSessionIdOperation, PLAYER_STATE_IDLE, true },
                { sSetAudioSessionIdOperation, PLAYER_STATE_PREPARED, true },
                { sSetAudioSessionIdOperation, PLAYER_STATE_PAUSED, true },
                { sSetAudioSessionIdOperation, PLAYER_STATE_PLAYING, true },
                { sSetAudioSessionIdOperation, PLAYER_STATE_ERROR, false },

                { sGetAudioSessionIdOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetAudioSessionIdOperation, PLAYER_STATE_IDLE, true },
                { sGetAudioSessionIdOperation, PLAYER_STATE_PREPARED, true },
                { sGetAudioSessionIdOperation, PLAYER_STATE_PAUSED, true },
                { sGetAudioSessionIdOperation, PLAYER_STATE_PLAYING, true },
                { sGetAudioSessionIdOperation, PLAYER_STATE_ERROR, false },

                { sAttachAuxEffectOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sAttachAuxEffectOperation, PLAYER_STATE_IDLE, true },
                { sAttachAuxEffectOperation, PLAYER_STATE_PREPARED, true },
                { sAttachAuxEffectOperation, PLAYER_STATE_PAUSED, true },
                { sAttachAuxEffectOperation, PLAYER_STATE_PLAYING, true },
                { sAttachAuxEffectOperation, PLAYER_STATE_ERROR, false },

                { sSetAuxEffectSendLevelOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetAuxEffectSendLevelOperation, PLAYER_STATE_IDLE, true },
                { sSetAuxEffectSendLevelOperation, PLAYER_STATE_PREPARED, true },
                { sSetAuxEffectSendLevelOperation, PLAYER_STATE_PAUSED, true },
                { sSetAuxEffectSendLevelOperation, PLAYER_STATE_PLAYING, true },
                { sSetAuxEffectSendLevelOperation, PLAYER_STATE_ERROR, false },

                { sGetTrackInfoOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sGetTrackInfoOperation, PLAYER_STATE_IDLE, true },
                { sGetTrackInfoOperation, PLAYER_STATE_PREPARED, true },
                { sGetTrackInfoOperation, PLAYER_STATE_PAUSED, true },
                { sGetTrackInfoOperation, PLAYER_STATE_PLAYING, true },
                { sGetTrackInfoOperation, PLAYER_STATE_ERROR, false },

                { sGetSelectedTrackOperation, PLAYER_STATE_IDLE, true },
                { sGetSelectedTrackOperation, PLAYER_STATE_IDLE, true },
                { sGetSelectedTrackOperation, PLAYER_STATE_PREPARED, true },
                { sGetSelectedTrackOperation, PLAYER_STATE_PAUSED, true },
                { sGetSelectedTrackOperation, PLAYER_STATE_PLAYING, true },
                { sGetSelectedTrackOperation, PLAYER_STATE_ERROR, false },

                { sSelectTrackOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSelectTrackOperation, PLAYER_STATE_IDLE, false },
                { sSelectTrackOperation, PLAYER_STATE_PREPARED, true },
                { sSelectTrackOperation, PLAYER_STATE_PAUSED, true },
                { sSelectTrackOperation, PLAYER_STATE_PLAYING, true },
                { sSelectTrackOperation, PLAYER_STATE_ERROR, false },

                { sDeselectTrackOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sDeselectTrackOperation, PLAYER_STATE_IDLE, false },
                { sDeselectTrackOperation, PLAYER_STATE_PREPARED, true },
                { sDeselectTrackOperation, PLAYER_STATE_PAUSED, true },
                { sDeselectTrackOperation, PLAYER_STATE_PLAYING, true },
                { sDeselectTrackOperation, PLAYER_STATE_ERROR, false},

                { sSetEventCallbackOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetEventCallbackOperation, PLAYER_STATE_IDLE, true },
                { sSetEventCallbackOperation, PLAYER_STATE_PREPARED, true },
                { sSetEventCallbackOperation, PLAYER_STATE_PAUSED, true },
                { sSetEventCallbackOperation, PLAYER_STATE_PLAYING, true },
                { sSetEventCallbackOperation, PLAYER_STATE_ERROR, false },

                { sClearEventCallbackOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sClearEventCallbackOperation, PLAYER_STATE_IDLE, true },
                { sClearEventCallbackOperation, PLAYER_STATE_PREPARED, true },
                { sClearEventCallbackOperation, PLAYER_STATE_PAUSED, true },
                { sClearEventCallbackOperation, PLAYER_STATE_PLAYING, true },
                { sClearEventCallbackOperation, PLAYER_STATE_ERROR, false },
        });
    }

    public MediaPlayer2StateTest(
            PlayerOperation operation, int testState, boolean isValid) {
        mTestOperation = operation;
        mTestState = testState;
        mIsValidOperation = isValid;
    }

    private void setupPlayer() throws Exception {
        final Monitor onPauseCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PAUSE) {
                    onPauseCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PREPARE) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                }
            }

            @Override
            public void onError(MediaPlayer2 mp, MediaItem item, int what, int extra) {
                mOnErrorCalled.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        if (mTestState == PLAYER_STATE_ERROR) {
            DataSourceCallback invalidDataSource = new DataSourceCallback() {
                @Override
                public int readAt(long position, byte[] buffer, int offset, int size)
                        throws IOException {
                    return -1;
                }

                @Override
                public long getSize() throws IOException {
                    return -1;  // Unknown size
                }

                @Override
                public void close() throws IOException {}
            };
            mOnErrorCalled.reset();
            mPlayer.setMediaItem(new CallbackMediaItem.Builder(invalidDataSource)
                    .build());
            mPlayer.prepare();
            mOnErrorCalled.waitForSignal(1000);
            assertEquals(PLAYER_STATE_ERROR, mPlayer.getState());
            return;
        }

        if (mTestState == MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE) {
            mTestState = PLAYER_STATE_IDLE;
            return;
        }
        if (!checkLoadResource(R.raw.testvideo_with_2_subtitle_tracks)) {
            fail();
        }
        if (mTestOperation == sSkipToNextOperation) {
            MediaItem item = createDataSourceDesc(R.raw.testvideo);
            mPlayer.setNextMediaItem(item);
        }
        assertEquals(PLAYER_STATE_IDLE, mPlayer.getState());
        if (mTestState == PLAYER_STATE_IDLE) {
            return;
        }

        mPlayer.prepare();
        // TODO(b/80232248): The first time one of the tests reads from the resource preparation can
        // take ~ 1.5 seconds to complete with the pre-P implementation. Later calls take ~ 100 ms.
        // Find out why the first preparation is slow and reduce this timeout back to one second.
        mOnPrepareCalled.waitForSignal(2000);
        assertEquals(PLAYER_STATE_PREPARED, mPlayer.getState());
        if (mTestOperation == sDeselectTrackOperation) {
            mPlayer.selectTrack(1);
        }
        if (mTestState == PLAYER_STATE_PREPARED) {
            return;
        }

        mPlayer.play();
        mOnPlayCalled.waitForSignal(1000);
        assertEquals(PLAYER_STATE_PLAYING, mPlayer.getState());
        if (mTestState == PLAYER_STATE_PLAYING) {
            return;
        }

        mPlayer.pause();
        onPauseCalled.waitForSignal(1000);
        assertEquals(PLAYER_STATE_PAUSED, mPlayer.getState());
        if (mTestState == PLAYER_STATE_PAUSED) {
            return;
        }
        fail();
    }

    @Test
    @LargeTest
    public void testOperation() throws Exception {
        if (!CHECK_INVALID_STATE && !mIsValidOperation) {
            return;
        }
        setupPlayer();

        final List<Pair<Integer, Integer>> callCompletes = new ArrayList();
        final Monitor callCompleteCalled = new Monitor();
        final Monitor commandLabelReachedCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(
                    MediaPlayer2 mp, MediaItem item, int what, int status) {
                callCompletes.add(new Pair<Integer, Integer>(what, status));
                callCompleteCalled.signal();
            }

            @Override
            public void onCommandLabelReached(MediaPlayer2 mp, Object label) {
                callCompletes.add(new Pair<Integer, Integer>(
                            CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED,
                            CALL_STATUS_NO_ERROR));
                callCompleteCalled.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        commandLabelReachedCalled.reset();
        Object tag = new Object();
        mPlayer.notifyWhenCommandLabelReached(tag);
        commandLabelReachedCalled.waitForSignal(1000);

        callCompletes.clear();
        callCompleteCalled.reset();
        try {
            mTestOperation.doOperation(mPlayer);
        } catch (IllegalStateException e) {
            if (mTestOperation.getCallCompleteCode() != null || mIsValidOperation) {
                fail();
            }
        }
        if (mTestOperation.getCallCompleteCode() != null) {
            // asynchronous operation. Need to check call complete notification.
            callCompleteCalled.waitForSignal();
            assertEquals(mTestOperation.getCallCompleteCode(), callCompletes.get(0).first);
            if (mIsValidOperation) {
                assertEquals(CALL_STATUS_NO_ERROR, (int) callCompletes.get(0).second);
            } else {
                assertNotEquals(CALL_STATUS_NO_ERROR, (int) callCompletes.get(0).second);
            }
        } else if (!mIsValidOperation) {
            fail();
        }
    }

    interface PlayerOperation {
        void doOperation(MediaPlayer2 player);
        /* Expected call complete notification. {@code null} if operation is synchronous. */
        Integer getCallCompleteCode();
    }
}
