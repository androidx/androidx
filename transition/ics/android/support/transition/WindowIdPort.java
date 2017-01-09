/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.transition;

import android.annotation.TargetApi;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;


/**
 * Backport of WindowId.
 *
 * <p>Since the use of WindowId in Transition API is limited to identifying windows, we can just
 * wrap a window token and use it as an identifier.</p>
 */
@RequiresApi(14)
@TargetApi(14)
class WindowIdPort {

    private final IBinder mToken;

    private WindowIdPort(IBinder token) {
        mToken = token;
    }

    static WindowIdPort getWindowId(@NonNull View view) {
        return new WindowIdPort(view.getWindowToken());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WindowIdPort && ((WindowIdPort) obj).mToken.equals(this.mToken);
    }

}
