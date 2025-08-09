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

import static android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import android.os.Build;
import android.telephony.SubscriptionManager;

import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link SubscriptionManager}.
 */
public class SubscriptionManagerCompat {

    private static Method sGetSlotIndexMethod;

    /**
     * Returns the slot index associated with the subscription id.
     */
    public static int getSlotIndex(int subId) {
        if (subId == INVALID_SUBSCRIPTION_ID) {
            return INVALID_SIM_SLOT_INDEX;
        }

        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Impl.getSlotIndex(subId);
        } else {
            try {
                if (sGetSlotIndexMethod == null) {
                    if (Build.VERSION.SDK_INT >= 26) {
                        sGetSlotIndexMethod = SubscriptionManager.class.getDeclaredMethod(
                                "getSlotIndex", int.class);
                    } else {
                        sGetSlotIndexMethod = SubscriptionManager.class.getDeclaredMethod(
                                "getSlotId", int.class);
                    }
                    sGetSlotIndexMethod.setAccessible(true);
                }

                Integer slotIdx = (Integer) sGetSlotIndexMethod.invoke(null, subId);
                if (slotIdx != null) {
                    return slotIdx;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException ignored) {
            } catch (InvocationTargetException ignored) {
            }

            return INVALID_SIM_SLOT_INDEX;
        }
    }

    private SubscriptionManagerCompat() {}

    @RequiresApi(29)
    private static class Api29Impl {
        private Api29Impl() {}

        static int getSlotIndex(int subId) {
            return SubscriptionManager.getSlotIndex(subId);
        }
    }
}
