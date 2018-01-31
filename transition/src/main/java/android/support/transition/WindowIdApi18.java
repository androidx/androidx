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

import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.WindowId;

@RequiresApi(18)
class WindowIdApi18 implements WindowIdImpl {

    private final WindowId mWindowId;

    WindowIdApi18(@NonNull View view) {
        mWindowId = view.getWindowId();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WindowIdApi18 && ((WindowIdApi18) o).mWindowId.equals(mWindowId);
    }

    @Override
    public int hashCode() {
        return mWindowId.hashCode();
    }
}
