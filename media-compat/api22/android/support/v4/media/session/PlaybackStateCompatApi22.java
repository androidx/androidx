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

import java.util.ArrayList;
import java.util.List;

class PlaybackStateCompatApi22 {
    public static Bundle getExtras(Object stateObj) {
        return ((PlaybackState)stateObj).getExtras();
    }

    public static Object newInstance(int state, long position, long bufferedPosition,
            float speed, long actions, CharSequence errorMessage, long updateTime,
            List<Object> customActions,
            long activeItemId, Bundle extras) {
        PlaybackState.Builder stateObj = new PlaybackState.Builder();
        stateObj.setState(state, position, speed, updateTime);
        stateObj.setBufferedPosition(bufferedPosition);
        stateObj.setActions(actions);
        stateObj.setErrorMessage(errorMessage);
        for (Object customAction : customActions) {
            stateObj.addCustomAction((PlaybackState.CustomAction) customAction);
        }
        stateObj.setActiveQueueItemId(activeItemId);
        stateObj.setExtras(extras);
        return stateObj.build();
    }
}
