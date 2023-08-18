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

package androidx.core.app;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

import android.app.Notification;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.os.Build;

import androidx.annotation.DoNotInline;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

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

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    @IntDef(flag = true,
            value = {
                    STOP_FOREGROUND_REMOVE,
                    STOP_FOREGROUND_DETACH
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface StopForegroundFlags {}

    private static final int FOREGROUND_SERVICE_TYPE_ALLOWED_SINCE_Q =
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;

    private static final int FOREGROUND_SERVICE_TYPE_ALLOWED_SINCE_U =
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            | ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;

    /**
     * {@link Service#startForeground(int, Notification, int)} with the third parameter
     * {@code foregroundServiceType} was added in {@link android.os.Build.VERSION_CODES#Q}.
     *
     * <p>Before SDK Version {@link android.os.Build.VERSION_CODES#Q}, this method call should call
     * {@link Service#startForeground(int, Notification)} without the {@code foregroundServiceType}
     * parameter.</p>
     *
     * <p>Beginning with SDK Version {@link android.os.Build.VERSION_CODES#Q}, the allowed
     * foregroundServiceType are:
     * <ul>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MANIFEST}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_NONE}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_DATA_SYNC}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_PHONE_CALL}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_CAMERA}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MICROPHONE}</li>
     * </ul>
     * </p>
     *
     * <p>Beginning with SDK Version {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE},
     * apps targeting SDK Version {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE} is not
     * allowed to use {@link ServiceInfo#FOREGROUND_SERVICE_TYPE_NONE}. The allowed
     * foregroundServiceType are:
     * <ul>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MANIFEST}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_DATA_SYNC}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_PHONE_CALL}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_LOCATION}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_CAMERA}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_MICROPHONE}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_HEALTH}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_SHORT_SERVICE}</li>
     *   <li>{@link ServiceInfo#FOREGROUND_SERVICE_TYPE_SPECIAL_USE}</li>
     * </ul>
     * </p>
     *
     * @see Service#startForeground(int, Notification)
     * @see Service#startForeground(int, Notification, int)
     */
    public static void startForeground(@NonNull Service service, int id,
            @NonNull Notification notification, int foregroundServiceType) {
        if (Build.VERSION.SDK_INT >= 34) {
            Api34Impl.startForeground(service, id, notification, foregroundServiceType);
        } else if (Build.VERSION.SDK_INT >= 29) {
            Api29Impl.startForeground(service, id, notification, foregroundServiceType);
        } else {
            service.startForeground(id, notification);
        }
    }

    /**
     * Remove the passed service from foreground state, allowing it to be killed if
     * more memory is needed.
     * @param service service to remove.
     * @param flags Additional behavior options: {@link #STOP_FOREGROUND_REMOVE},
     * {@link #STOP_FOREGROUND_DETACH}.
     * @see Service#startForeground(int, Notification)
     */
    @SuppressWarnings("deprecation")
    public static void stopForeground(@NonNull Service service, @StopForegroundFlags int flags) {
        if (Build.VERSION.SDK_INT >= 24) {
            Api24Impl.stopForeground(service, flags);
        } else {
            service.stopForeground((flags & ServiceCompat.STOP_FOREGROUND_REMOVE) != 0);
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        private Api24Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void stopForeground(Service service, int flags) {
            service.stopForeground(flags);
        }
    }

    @RequiresApi(29)
    static class Api29Impl {
        private Api29Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void startForeground(Service service, int id, Notification notification,
                int foregroundServiceType) {
            if (foregroundServiceType == FOREGROUND_SERVICE_TYPE_NONE
                    || foregroundServiceType == FOREGROUND_SERVICE_TYPE_MANIFEST) {
                service.startForeground(id, notification, foregroundServiceType);
            } else {
                service.startForeground(id, notification,
                        foregroundServiceType & FOREGROUND_SERVICE_TYPE_ALLOWED_SINCE_Q);
            }
        }
    }

    @RequiresApi(34)
    static class Api34Impl {
        private Api34Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static void startForeground(Service service, int id, Notification notification,
                int foregroundServiceType) {
            if (foregroundServiceType == FOREGROUND_SERVICE_TYPE_NONE
                    || foregroundServiceType == FOREGROUND_SERVICE_TYPE_MANIFEST) {
                service.startForeground(id, notification, foregroundServiceType);
            } else {
                service.startForeground(id, notification,
                        foregroundServiceType & FOREGROUND_SERVICE_TYPE_ALLOWED_SINCE_U);
            }
        }
    }

}
