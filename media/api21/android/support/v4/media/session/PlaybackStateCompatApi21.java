/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.media.session;

import android.media.session.PlaybackState;
import android.os.Bundle;

import androidx.annotation.RequiresApi;

import java.util.List;

@RequiresApi(21)
class PlaybackStateCompatApi21 {
    public static int getState(Object stateObj) {
        return ((PlaybackState)stateObj).getState();
    }

    public static long getPosition(Object stateObj) {
        return ((PlaybackState)stateObj).getPosition();
    }

    public static long getBufferedPosition(Object stateObj) {
        return ((PlaybackState)stateObj).getBufferedPosition();
    }

    public static float getPlaybackSpeed(Object stateObj) {
        return ((PlaybackState)stateObj).getPlaybackSpeed();
    }

    public static long getActions(Object stateObj) {
        return ((PlaybackState)stateObj).getActions();
    }

    public static CharSequence getErrorMessage(Object stateObj) {
        return ((PlaybackState)stateObj).getErrorMessage();
    }

    public static long getLastPositionUpdateTime(Object stateObj) {
        return ((PlaybackState)stateObj).getLastPositionUpdateTime();
    }

    public static List<Object> getCustomActions(Object stateObj) {
        return (List)((PlaybackState)stateObj).getCustomActions();
    }

    public static long getActiveQueueItemId(Object stateObj) {
        return ((PlaybackState)stateObj).getActiveQueueItemId();
    }

    public static Object newInstance(int state, long position, long bufferedPosition,
            float speed, long actions, CharSequence errorMessage, long updateTime,
            List<Object> customActions,
            long activeItemId) {
        PlaybackState.Builder stateObj = new PlaybackState.Builder();
        stateObj.setState(state, position, speed, updateTime);
        stateObj.setBufferedPosition(bufferedPosition);
        stateObj.setActions(actions);
        stateObj.setErrorMessage(errorMessage);
        for (Object customAction : customActions) {
            stateObj.addCustomAction((PlaybackState.CustomAction) customAction);
        }
        stateObj.setActiveQueueItemId(activeItemId);
        return stateObj.build();
    }

    static final class CustomAction {
        public static String getAction(Object customActionObj) {
            return ((PlaybackState.CustomAction)customActionObj).getAction();
        }

        public static CharSequence getName(Object customActionObj) {
            return ((PlaybackState.CustomAction)customActionObj).getName();
        }

        public static int getIcon(Object customActionObj) {
            return ((PlaybackState.CustomAction)customActionObj).getIcon();
        }
        public static Bundle getExtras(Object customActionObj) {
            return ((PlaybackState.CustomAction)customActionObj).getExtras();
        }

        public static Object newInstance(String action, CharSequence name,
                int icon, Bundle extras) {
            PlaybackState.CustomAction.Builder customActionObj =
                    new PlaybackState.CustomAction.Builder(action, name, icon);
            customActionObj.setExtras(extras);
            return customActionObj.build();
        }

        private CustomAction() {
        }
    }

    private PlaybackStateCompatApi21() {
    }
}
