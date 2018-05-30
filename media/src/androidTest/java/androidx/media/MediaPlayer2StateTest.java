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
package androidx.media;

import static androidx.media.AudioAttributesCompat.CONTENT_TYPE_MUSIC;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_ATTACH_AUX_EFFECT;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_DESELECT_TRACK;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_LOOP_CURRENT;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_NOTIFY_WHEN_COMMAND_LABEL_REACHED;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_PAUSE;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_PLAY;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_PREPARE;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SEEK_TO;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SELECT_TRACK;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_AUDIO_ATTRIBUTES;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_AUDIO_SESSION_ID;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_AUX_EFFECT_SEND_LEVEL;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_DATA_SOURCE;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCE;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_NEXT_DATA_SOURCES;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_PLAYBACK_PARAMS;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_PLAYER_VOLUME;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SET_SURFACE;
import static androidx.media.MediaPlayer2.CALL_COMPLETED_SKIP_TO_NEXT;
import static androidx.media.MediaPlayer2.CALL_STATUS_NO_ERROR;
import static androidx.media.MediaPlayer2.MEDIAPLAYER2_STATE_ERROR;
import static androidx.media.MediaPlayer2.MEDIAPLAYER2_STATE_IDLE;
import static androidx.media.MediaPlayer2.MEDIAPLAYER2_STATE_PAUSED;
import static androidx.media.MediaPlayer2.MEDIAPLAYER2_STATE_PLAYING;
import static androidx.media.MediaPlayer2.MEDIAPLAYER2_STATE_PREPARED;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import android.os.Build;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;
import android.util.Pair;

import androidx.media.MediaPlayer2.MediaPlayer2State;
import androidx.media.MediaPlayer2.TrackInfo;
import androidx.media.TestUtils.Monitor;
import androidx.media.test.R;

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
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
public class MediaPlayer2StateTest extends MediaPlayer2TestBase {
    private static final String LOG_TAG = "MediaPlayer2StateTest";

    // TODO: Underlying MediaPlayer1 implementation does not report an error when an operation is
    // triggered in an invalid state. e.g. MediaPlayer.getTrackInfo in the error state. Check the
    // cause and update javadoc of MediaPlayer1 or change the test case.
    private static final boolean CHECK_INVALID_STATE = false;

    // Used for testing case that operation is called before setDataSourceDesc().
    private static final int MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE = 400001;

    private static final DataSourceDesc sDummyDataSource = new DataSourceDesc.Builder()
            .setDataSource(
                    new Media2DataSource() {
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
            player.setDataSource(sDummyDataSource);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_DATA_SOURCE;
        }

        @Override
        public String toString() {
            return "setDataSource()";
        }
    };
    private static final PlayerOperation sSetNextDataSourceOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setNextDataSource(sDummyDataSource);
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_NEXT_DATA_SOURCE;
        }

        @Override
        public String toString() {
            return "setNextDataSource()";
        }
    };
    private static final PlayerOperation sSetNextDataSourcesOperation = new PlayerOperation() {
        @Override
        public void doOperation(MediaPlayer2 player) {
            player.setNextDataSources(Arrays.asList(sDummyDataSource));
        }

        @Override
        public Integer getCallCompleteCode() {
            return CALL_COMPLETED_SET_NEXT_DATA_SOURCES;
        }

        @Override
        public String toString() {
            return "setNextDataSources()";
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
            player.setPlaybackParams(new PlaybackParams2.Builder().setSpeed(1.0f).build());
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
            player.selectTrack(0);
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
    private PlayerOperation mTestOpertation;
    private boolean mIsValidOperation;

    @Parameterized.Parameters(name = "{index}: operation={0} state={1} valid={2}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { sCloseOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sCloseOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sCloseOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sCloseOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sCloseOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sCloseOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sPlayOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sPlayOperation, MEDIAPLAYER2_STATE_IDLE, false },
                { sPlayOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sPlayOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sPlayOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sPlayOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sPrepareOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sPrepareOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sPrepareOperation, MEDIAPLAYER2_STATE_PREPARED, false },
                { sPrepareOperation, MEDIAPLAYER2_STATE_PAUSED, false },
                { sPrepareOperation, MEDIAPLAYER2_STATE_PLAYING, false },
                { sPrepareOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sPauseOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sPauseOperation, MEDIAPLAYER2_STATE_IDLE, false },
                { sPauseOperation, MEDIAPLAYER2_STATE_PREPARED, false },
                { sPauseOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sPauseOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sPauseOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sSkipToNextOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSkipToNextOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSkipToNextOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSkipToNextOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSkipToNextOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSkipToNextOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sSeekToOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSeekToOperation, MEDIAPLAYER2_STATE_IDLE, false },
                { sSeekToOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSeekToOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSeekToOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSeekToOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sGetCurrentPositionOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetCurrentPositionOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetCurrentPositionOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetCurrentPositionOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetCurrentPositionOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetCurrentPositionOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sGetDurationOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetDurationOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetDurationOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetDurationOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetDurationOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetDurationOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sGetBufferedPositionOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetBufferedPositionOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetBufferedPositionOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetBufferedPositionOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetBufferedPositionOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetBufferedPositionOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sGetStateOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetStateOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetStateOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetStateOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetStateOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetStateOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sSetAudioAttributesOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetAudioAttributesOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetAudioAttributesOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetAudioAttributesOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetAudioAttributesOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetAudioAttributesOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sSetDataSourceOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetDataSourceOperation, MEDIAPLAYER2_STATE_IDLE, false },
                { sSetDataSourceOperation, MEDIAPLAYER2_STATE_PREPARED, false },
                { sSetDataSourceOperation, MEDIAPLAYER2_STATE_PAUSED, false },
                { sSetDataSourceOperation, MEDIAPLAYER2_STATE_PLAYING, false },
                { sSetDataSourceOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sSetNextDataSourceOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetNextDataSourceOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetNextDataSourceOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetNextDataSourceOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetNextDataSourceOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetNextDataSourceOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sSetNextDataSourcesOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetNextDataSourcesOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetNextDataSourcesOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetNextDataSourcesOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetNextDataSourcesOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetNextDataSourcesOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sLoopCurrentOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sLoopCurrentOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sLoopCurrentOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sLoopCurrentOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sLoopCurrentOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sLoopCurrentOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sSetPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetPlayerVolumeOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetPlayerVolumeOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetPlayerVolumeOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetPlayerVolumeOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetPlayerVolumeOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetPlayerVolumeOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetPlayerVolumeOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetPlayerVolumeOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetMaxPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetMaxPlayerVolumeOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetMaxPlayerVolumeOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetMaxPlayerVolumeOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetMaxPlayerVolumeOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetMaxPlayerVolumeOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sNotifyWhenCommandLabelReachedOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE,
                        true },
                { sNotifyWhenCommandLabelReachedOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sNotifyWhenCommandLabelReachedOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sNotifyWhenCommandLabelReachedOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sNotifyWhenCommandLabelReachedOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sNotifyWhenCommandLabelReachedOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sSetSurfaceOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetSurfaceOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetSurfaceOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetSurfaceOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetSurfaceOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetSurfaceOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sClearPendingCommandsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sClearPendingCommandsOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sClearPendingCommandsOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sClearPendingCommandsOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sClearPendingCommandsOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sClearPendingCommandsOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetVideoWidthOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetVideoWidthOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetVideoWidthOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetVideoWidthOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetVideoWidthOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetVideoWidthOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetVideoHeightOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetVideoHeightOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetVideoHeightOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetVideoHeightOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetVideoHeightOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetVideoHeightOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetMetricsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetMetricsOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetMetricsOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetMetricsOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetMetricsOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetMetricsOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sSetPlaybackParamsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetPlaybackParamsOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetPlaybackParamsOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetPlaybackParamsOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetPlaybackParamsOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetPlaybackParamsOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetPlaybackParamsOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sGetPlaybackParamsOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetPlaybackParamsOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetPlaybackParamsOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetPlaybackParamsOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetPlaybackParamsOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetTimestampOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetTimestampOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetTimestampOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetTimestampOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetTimestampOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetTimestampOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sResetOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sResetOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sResetOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sResetOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sResetOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sResetOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sSetAudioSessionIdOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetAudioSessionIdOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetAudioSessionIdOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetAudioSessionIdOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetAudioSessionIdOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetAudioSessionIdOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetAudioSessionIdOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sGetAudioSessionIdOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetAudioSessionIdOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetAudioSessionIdOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetAudioSessionIdOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetAudioSessionIdOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sAttachAuxEffectOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sAttachAuxEffectOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sAttachAuxEffectOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sAttachAuxEffectOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sAttachAuxEffectOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sAttachAuxEffectOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sSetAuxEffectSendLevelOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetAuxEffectSendLevelOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetAuxEffectSendLevelOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetAuxEffectSendLevelOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetAuxEffectSendLevelOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetAuxEffectSendLevelOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sGetTrackInfoOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sGetTrackInfoOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetTrackInfoOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetTrackInfoOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetTrackInfoOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetTrackInfoOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sGetSelectedTrackOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetSelectedTrackOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sGetSelectedTrackOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sGetSelectedTrackOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sGetSelectedTrackOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sGetSelectedTrackOperation, MEDIAPLAYER2_STATE_ERROR, false },

                { sSelectTrackOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, false },
                { sSelectTrackOperation, MEDIAPLAYER2_STATE_IDLE, false },
                { sSelectTrackOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSelectTrackOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSelectTrackOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSelectTrackOperation, MEDIAPLAYER2_STATE_ERROR, false },

                /* TODO: deselectTrack is failing due to error -38 from native side.
                         Debug more in the framework side.
                { sDeselectTrackOperation, MEDIAPLAYER2_STATE_IDLE, false },
                { sDeselectTrackOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sDeselectTrackOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sDeselectTrackOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sDeselectTrackOperation, MEDIAPLAYER2_STATE_ERROR, false},
                */

                { sSetEventCallbackOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sSetEventCallbackOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sSetEventCallbackOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sSetEventCallbackOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sSetEventCallbackOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sSetEventCallbackOperation, MEDIAPLAYER2_STATE_ERROR, true },

                { sClearEventCallbackOperation, MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE, true },
                { sClearEventCallbackOperation, MEDIAPLAYER2_STATE_IDLE, true },
                { sClearEventCallbackOperation, MEDIAPLAYER2_STATE_PREPARED, true },
                { sClearEventCallbackOperation, MEDIAPLAYER2_STATE_PAUSED, true },
                { sClearEventCallbackOperation, MEDIAPLAYER2_STATE_PLAYING, true },
                { sClearEventCallbackOperation, MEDIAPLAYER2_STATE_ERROR, true },
        });
    }

    public MediaPlayer2StateTest(
            PlayerOperation operation, int testState, boolean isValid) {
        mTestOpertation = operation;
        mTestState = testState;
        mIsValidOperation = isValid;
    }

    private void setupPlayer() throws Exception {
        final Monitor onPauseCalled = new Monitor();
        MediaPlayer2.EventCallback ecb = new MediaPlayer2.EventCallback() {
            @Override
            public void onCallCompleted(MediaPlayer2 mp, DataSourceDesc dsd, int what, int status) {
                if (what == MediaPlayer2.CALL_COMPLETED_PAUSE) {
                    onPauseCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PREPARE) {
                    mOnPrepareCalled.signal();
                } else if (what == MediaPlayer2.CALL_COMPLETED_PLAY) {
                    mOnPlayCalled.signal();
                }
            }

            @Override
            public void onError(MediaPlayer2 mp, DataSourceDesc dsd, int what, int extra) {
                mOnErrorCalled.signal();
            }
        };
        synchronized (mEventCbLock) {
            mEventCallbacks.add(ecb);
        }

        if (mTestState == MEDIAPLAYER2_STATE_ERROR) {
            Media2DataSource invalidDataSource = new Media2DataSource() {
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
            mPlayer.setDataSource(new DataSourceDesc.Builder()
                    .setDataSource(invalidDataSource)
                    .build());
            mPlayer.prepare();
            mOnErrorCalled.waitForSignal(1000);
            assertEquals(MEDIAPLAYER2_STATE_ERROR, mPlayer.getState());
            return;
        }

        if (mTestState == MEDIAPLAYER2_STATE_IDLE_NO_DATA_SOURCE) {
            mTestState = MEDIAPLAYER2_STATE_IDLE;
            return;
        }
        if (!checkLoadResource(R.raw.testvideo)) {
            fail();
        }
        if (mTestOpertation == sSkipToNextOperation) {
            DataSourceDesc dsd = createDataSourceDesc(R.raw.testvideo);
            mPlayer.setNextDataSource(dsd);
        }
        assertEquals(MEDIAPLAYER2_STATE_IDLE, mPlayer.getState());
        if (mTestState == MEDIAPLAYER2_STATE_IDLE) {
            return;
        }

        mPlayer.prepare();
        mOnPrepareCalled.waitForSignal(1000);
        assertEquals(MEDIAPLAYER2_STATE_PREPARED, mPlayer.getState());
        if (mTestOpertation == sDeselectTrackOperation) {
            mPlayer.selectTrack(1);
        }
        if (mTestState == MEDIAPLAYER2_STATE_PREPARED) {
            return;
        }

        mPlayer.play();
        mOnPlayCalled.waitForSignal(1000);
        assertEquals(MEDIAPLAYER2_STATE_PLAYING, mPlayer.getState());
        if (mTestState == MEDIAPLAYER2_STATE_PLAYING) {
            return;
        }

        mPlayer.pause();
        onPauseCalled.waitForSignal(1000);
        assertEquals(MEDIAPLAYER2_STATE_PAUSED, mPlayer.getState());
        if (mTestState == MEDIAPLAYER2_STATE_PAUSED) {
            return;
        }
        fail();
    }

    @Test
    @MediumTest
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
            public void onCallCompleted(MediaPlayer2 mp, DataSourceDesc dsd, int what, int status) {
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
            mTestOpertation.doOperation(mPlayer);
        } catch (IllegalStateException e) {
            if (mTestOpertation.getCallCompleteCode() != null || mIsValidOperation) {
                fail();
            }
        }
        if (mTestOpertation.getCallCompleteCode() != null) {
            // asynchronous operation. Need to check call complete notification.
            callCompleteCalled.waitForSignal();
            assertEquals(mTestOpertation.getCallCompleteCode(), callCompletes.get(0).first);
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
