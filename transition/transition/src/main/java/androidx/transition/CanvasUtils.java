/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.transition;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.os.Build;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class CanvasUtils {

    private static Method sReorderBarrierMethod;
    private static Method sInorderBarrierMethod;
    private static boolean sOrderMethodsFetched;

    /**
     * Enables Z support for the Canvas.
     *
     * IMPORTANT: This method doesn't work on Pie! It will thrown an exception instead
     */
    @SuppressLint("SoonBlockedPrivateApi")
    static void enableZ(@NonNull Canvas canvas, boolean enable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // no shadows yet added into a platform
        } else if (Build.VERSION.SDK_INT >= 29) {
            if (enable) {
                canvas.enableZ();
            } else {
                canvas.disableZ();
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // not on P's greylist, can't use reflection
            throw new IllegalStateException("This method doesn't work on Pie!");
        } else {
            if (!sOrderMethodsFetched) {
                try {
                    sReorderBarrierMethod = Canvas.class.getDeclaredMethod(
                            "insertReorderBarrier");
                    sReorderBarrierMethod.setAccessible(true);
                    sInorderBarrierMethod = Canvas.class.getDeclaredMethod(
                            "insertInorderBarrier");
                    sInorderBarrierMethod.setAccessible(true);
                } catch (NoSuchMethodException ignore) {
                    // Do nothing
                }
                sOrderMethodsFetched = true;
            }
            try {
                if (enable && sReorderBarrierMethod != null) {
                    sReorderBarrierMethod.invoke(canvas);
                }
                if (!enable && sInorderBarrierMethod != null) {
                    sInorderBarrierMethod.invoke(canvas);
                }
            } catch (IllegalAccessException ignore) {
                // Do nothing
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    private CanvasUtils() {
    }
}
