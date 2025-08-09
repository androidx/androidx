/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.telephony;

import static android.telephony.SubscriptionManager.DEFAULT_SUBSCRIPTION_ID;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.annotation.SuppressLint;
import android.os.Build.VERSION;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link TelephonyManager}.
 */
public class TelephonyManagerCompat {

    private static Method sGetSubIdMethod;

    /**
     * Returns the IMEI (International Mobile Equipment Identity) associated with the
     * subscription id of the given TelephonyManager, or null if not available.
     *
     * <p>Below Android 10, this API requires any of:
     * <ul>
     *     <li>the caller holds the READ_PHONE_STATE permission</li>
     *     <li>the caller has carrier privileges (see
     *     {@link TelephonyManager#hasCarrierPrivileges()})</li>
     * </ul>
     *
     * <p>On Android 10 and above, this API requires any of:
     * <ul>
     *     <li>the caller holds the READ_PRIVILEGED_PHONE_STATE permission</li>
     *     <li>the caller is the device or profile owner and holds the READ_PHONE_STATE
     *     permission</li>
     *     <li>the caller has carrier privileges (see
     *     {@link TelephonyManager#hasCarrierPrivileges()})</li>
     *     <li>the caller is the default SMS role holder (see
     *     {@link android.app.role.RoleManager#isRoleHeld(String)})</li>
     *     <li>the caller holds the USE_ICC_AUTH_WITH_DEVICE_IDENTIFIER permission</li>
     * </ul>
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
    public static @Nullable String getImei(@NonNull TelephonyManager telephonyManager) {
        if (VERSION.SDK_INT >= 26) {
            return Api26Impl.getImei(telephonyManager);
        } else {
            // below Android O the telephony manager has a severe bug (b/137114239) where many
            // methods do not properly respect the subscription id and always use the default
            // subscription id. so if we have a non-default subscription id, we need to do the work
            // ourselves...
            int subId = getSubscriptionId(telephonyManager);
            if (subId != DEFAULT_SUBSCRIPTION_ID && subId != INVALID_SUBSCRIPTION_ID) {
                int slotIndex = SubscriptionManagerCompat.getSlotIndex(subId);
                return telephonyManager.getDeviceId(slotIndex);
            }
        }

        return telephonyManager.getDeviceId();
    }

    /**
     * Return the subscription ID the TelephonyManager was created with (via
     * {@link TelephonyManager#createForSubscriptionId(int)}) if applicable, and otherwise the
     * default subscription ID.
     */
    @SuppressLint("SoonBlockedPrivateApi")
    public static int getSubscriptionId(@NonNull TelephonyManager telephonyManager) {
        if (VERSION.SDK_INT >= 30) {
            return Api30Impl.getSubscriptionId(telephonyManager);
        } else {
            try {
                if (sGetSubIdMethod == null) {
                    sGetSubIdMethod = TelephonyManager.class.getDeclaredMethod("getSubId");
                    sGetSubIdMethod.setAccessible(true);
                }

                Integer subId = (Integer) sGetSubIdMethod.invoke(telephonyManager);
                if (subId != null && subId != INVALID_SUBSCRIPTION_ID) {
                    return subId;
                }
            } catch (InvocationTargetException ignored) {
            } catch (IllegalAccessException ignored) {
            } catch (NoSuchMethodException ignored) {
            }
        }

        return DEFAULT_SUBSCRIPTION_ID;
    }

    private TelephonyManagerCompat() {}

    @RequiresApi(30)
    private static class Api30Impl {
        private Api30Impl() {}

        static int getSubscriptionId(TelephonyManager telephonyManager) {
            return telephonyManager.getSubscriptionId();
        }
    }

    @RequiresApi(26)
    private static class Api26Impl {
        private Api26Impl() {}

        @SuppressLint("MissingPermission")
        @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
        static @Nullable String getImei(TelephonyManager telephonyManager) {
            return telephonyManager.getImei();
        }
    }
}
