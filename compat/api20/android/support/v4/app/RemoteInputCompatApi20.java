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

package android.support.v4.app;

import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;

class RemoteInputCompatApi20 {
    static RemoteInputCompatBase.RemoteInput[] toCompat(RemoteInput[] srcArray,
            RemoteInputCompatBase.RemoteInput.Factory factory) {
        if (srcArray == null) {
            return null;
        }
        RemoteInputCompatBase.RemoteInput[] result = factory.newArray(srcArray.length);
        for (int i = 0; i < srcArray.length; i++) {
            RemoteInput src = srcArray[i];
            result[i] = factory.build(src.getResultKey(), src.getLabel(), src.getChoices(),
                    src.getAllowFreeFormInput(), src.getExtras());
        }
        return result;
    }

    static RemoteInput[] fromCompat(RemoteInputCompatBase.RemoteInput[] srcArray) {
        if (srcArray == null) {
            return null;
        }
        RemoteInput[] result = new RemoteInput[srcArray.length];
        for (int i = 0; i < srcArray.length; i++) {
            RemoteInputCompatBase.RemoteInput src = srcArray[i];
            result[i] = new RemoteInput.Builder(src.getResultKey())
                    .setLabel(src.getLabel())
                    .setChoices(src.getChoices())
                    .setAllowFreeFormInput(src.getAllowFreeFormInput())
                    .addExtras(src.getExtras())
                    .build();
        }
        return result;
    }

    static Bundle getResultsFromIntent(Intent intent) {
        return RemoteInput.getResultsFromIntent(intent);
    }

    static void addResultsToIntent(RemoteInputCompatBase.RemoteInput[] remoteInputs,
            Intent intent, Bundle results) {
        RemoteInput.addResultsToIntent(fromCompat(remoteInputs), intent, results);
    }
}
