/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.support.v4.media.routing;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Display;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class MediaRouterJellybeanMr1 extends MediaRouterJellybean {
    private static final String TAG = "MediaRouterJellybeanMr1";

    public static Object createCallback(Callback callback) {
        return new CallbackProxy<Callback>(callback);
    }

    public static final class RouteInfo {
        public static boolean isEnabled(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).isEnabled();
        }

        public static Display getPresentationDisplay(Object routeObj) {
            return ((android.media.MediaRouter.RouteInfo)routeObj).getPresentationDisplay();
        }
    }

    public static interface Callback extends MediaRouterJellybean.Callback {
        public void onRoutePresentationDisplayChanged(Object routeObj);
    }

    /**
     * Workaround the fact that the version of MediaRouter.addCallback() that accepts a
     * flag to perform an active scan does not exist in JB MR1 so we need to force
     * wifi display scans directly through the DisplayManager.
     * Do not use on JB MR2 and above.
     */
    public static final class ActiveScanWorkaround implements Runnable {
        // Time between wifi display scans when actively scanning in milliseconds.
        private static final int WIFI_DISPLAY_SCAN_INTERVAL = 15000;

        private final DisplayManager mDisplayManager;
        private final Handler mHandler;
        private Method mScanWifiDisplaysMethod;

        private boolean mActivelyScanningWifiDisplays;

        public ActiveScanWorkaround(Context context, Handler handler) {
            if (Build.VERSION.SDK_INT != 17) {
                throw new UnsupportedOperationException();
            }

            mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
            mHandler = handler;
            try {
                mScanWifiDisplaysMethod = DisplayManager.class.getMethod("scanWifiDisplays");
            } catch (NoSuchMethodException ex) {
            }
        }

        public void setActiveScanRouteTypes(int routeTypes) {
            // On JB MR1, there is no API to scan wifi display routes.
            // Instead we must make a direct call into the DisplayManager to scan
            // wifi displays on this version but only when live video routes are requested.
            // See also the JellybeanMr2Impl implementation of this method.
            // This was fixed in JB MR2 by adding a new overload of addCallback() to
            // enable active scanning on request.
            if ((routeTypes & MediaRouterJellybean.ROUTE_TYPE_LIVE_VIDEO) != 0) {
                if (!mActivelyScanningWifiDisplays) {
                    if (mScanWifiDisplaysMethod != null) {
                        mActivelyScanningWifiDisplays = true;
                        mHandler.post(this);
                    } else {
                        Log.w(TAG, "Cannot scan for wifi displays because the "
                                + "DisplayManager.scanWifiDisplays() method is "
                                + "not available on this device.");
                    }
                }
            } else {
                if (mActivelyScanningWifiDisplays) {
                    mActivelyScanningWifiDisplays = false;
                    mHandler.removeCallbacks(this);
                }
            }
        }

        @Override
        public void run() {
            if (mActivelyScanningWifiDisplays) {
                try {
                    mScanWifiDisplaysMethod.invoke(mDisplayManager);
                } catch (IllegalAccessException ex) {
                    Log.w(TAG, "Cannot scan for wifi displays.", ex);
                } catch (InvocationTargetException ex) {
                    Log.w(TAG, "Cannot scan for wifi displays.", ex);
                }
                mHandler.postDelayed(this, WIFI_DISPLAY_SCAN_INTERVAL);
            }
        }
    }

    /**
     * Workaround the fact that the isConnecting() method does not exist in JB MR1.
     * Do not use on JB MR2 and above.
     */
    public static final class IsConnectingWorkaround {
        private Method mGetStatusCodeMethod;
        private int mStatusConnecting;

        public IsConnectingWorkaround() {
            if (Build.VERSION.SDK_INT != 17) {
                throw new UnsupportedOperationException();
            }

            try {
                Field statusConnectingField =
                        android.media.MediaRouter.RouteInfo.class.getField("STATUS_CONNECTING");
                mStatusConnecting = statusConnectingField.getInt(null);
                mGetStatusCodeMethod =
                        android.media.MediaRouter.RouteInfo.class.getMethod("getStatusCode");
            } catch (NoSuchFieldException ex) {
            } catch (NoSuchMethodException ex) {
            } catch (IllegalAccessException ex) {
            }
        }

        public boolean isConnecting(Object routeObj) {
            android.media.MediaRouter.RouteInfo route =
                    (android.media.MediaRouter.RouteInfo)routeObj;

            if (mGetStatusCodeMethod != null) {
                try {
                    int statusCode = (Integer)mGetStatusCodeMethod.invoke(route);
                    return statusCode == mStatusConnecting;
                } catch (IllegalAccessException ex) {
                } catch (InvocationTargetException ex) {
                }
            }

            // Assume not connecting.
            return false;
        }
    }

    static class CallbackProxy<T extends Callback>
            extends MediaRouterJellybean.CallbackProxy<T> {
        public CallbackProxy(T callback) {
            super(callback);
        }

        @Override
        public void onRoutePresentationDisplayChanged(android.media.MediaRouter router,
                android.media.MediaRouter.RouteInfo route) {
            mCallback.onRoutePresentationDisplayChanged(route);
        }
    }
}
