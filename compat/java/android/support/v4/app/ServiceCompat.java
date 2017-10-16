/*
 * Copyright (C) 2011 The Android Open Source Project
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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.Notification;
import android.app.Service;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Helper for accessing features in {@link android.app.Service}.
 */
public final class ServiceCompat {

    private ServiceCompat() {
        /* Hide constructor */
    }

    /**
     * Constant to return from {@link android.app.Service#onStartCommand}: if this
     * service's process is killed while it is started (after returning from
     * {@link android.app.Service#onStartCommand}), then leave it in the started
     * state but don't retain this delivered intent.  Later the system will try to
     * re-create the service.  Because it is in the started state, it will
     * guarantee to call {@link android.app.Service#onStartCommand} after creating
     * the new service instance; if there are not any pending start commands to be
     * delivered to the service, it will be called with a null intent
     * object, so you must take care to check for this.
     *
     * <p>This mode makes sense for things that will be explicitly started
     * and stopped to run for arbitrary periods of time, such as a service
     * performing background music playback.
     */
    public static final int START_STICKY = 1;

    /**
     * Flag for {@link #stopForeground(Service, int)}: if set, the notification previously provided
     * to {@link Service#startForeground(int, Notification)} will be removed.  Otherwise it
     * will remain until a later call (to {@link Service#startForeground(int, Notification)} or
     * {@link #stopForeground(Service, int)} removes it, or the service is destroyed.
     */
    public static final int STOP_FOREGROUND_REMOVE = 1<<0;

    /**
     * Flag for {@link #stopForeground(Service, int)}: if set, the notification previously provided
     * to {@link Service#startForeground(int, Notification)} will be detached from the service.
     * Only makes sense when {@link #STOP_FOREGROUND_REMOVE} is <b>not</b> set -- in this case, the
     * notification will remain shown, but be completely detached from the service and so no longer
     * changed except through direct calls to the
     * notification manager.
     * <p>
     * This flag will only work on
     * {@link android.os.Build.VERSION_CODES#N} and later. It doesn't have any effect on earlier
     * platform versions.
     */
    public static final int STOP_FOREGROUND_DETACH = 1<<1;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef(flag = true,
            value = {
                    STOP_FOREGROUND_REMOVE,
                    STOP_FOREGROUND_DETACH
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StopForegroundFlags {}

    /**
     * Remove the passed service from foreground state, allowing it to be killed if
     * more memory is needed.
     * @param flags Additional behavior options: {@link #STOP_FOREGROUND_REMOVE},
     * {@link #STOP_FOREGROUND_DETACH}.
     * @see Service#startForeground(int, Notification)
     */
    public static void stopForeground(Service service, @StopForegroundFlags int flags) {
        if (Build.VERSION.SDK_INT >= 24) {
            service.stopForeground(flags);
        } else {
            service.stopForeground((flags & ServiceCompat.STOP_FOREGROUND_REMOVE) != 0);
        }
    }
}
