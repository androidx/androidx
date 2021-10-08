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

package androidx.core.os;

import android.os.Build;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link android.os.UserHandle} in a backwards compatible
 * fashion.
 */
@RequiresApi(17)
public class UserHandleCompat {

    @Nullable
    private static Method sGetUserIdMethod;
    @Nullable
    private static Constructor<UserHandle> sUserHandleConstructor;

    private UserHandleCompat() {
    }

    /**
     * Returns the user handle for a given uid.
     */
    @NonNull
    public static UserHandle getUserHandleForUid(int uid) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.getUserHandleForUid(uid);
        } else {
            try {
                Integer userId = (Integer) getGetUserIdMethod().invoke(null, uid);
                return getUserHandleConstructor().newInstance(userId);
            } catch (NoSuchMethodException e) {
                Error error = new NoSuchMethodError();
                error.initCause(e);
                throw error;
            } catch (IllegalAccessException e) {
                Error error = new IllegalAccessError();
                error.initCause(e);
                throw error;
            } catch (InstantiationException e) {
                Error error = new InstantiationError();
                error.initCause(e);
                throw error;
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @RequiresApi(24)
    private static class Api24Impl {

        private Api24Impl() {
        }

        @NonNull
        static UserHandle getUserHandleForUid(int uid) {
            return UserHandle.getUserHandleForUid(uid);
        }
    }

    private static Method getGetUserIdMethod() throws NoSuchMethodException {
        if (sGetUserIdMethod == null) {
            sGetUserIdMethod = UserHandle.class.getDeclaredMethod("getUserId", int.class);
            sGetUserIdMethod.setAccessible(true);
        }

        return sGetUserIdMethod;
    }

    private static Constructor<UserHandle> getUserHandleConstructor() throws NoSuchMethodException {
        if (sUserHandleConstructor == null) {
            sUserHandleConstructor = UserHandle.class.getDeclaredConstructor(int.class);
            sUserHandleConstructor.setAccessible(true);
        }

        return sUserHandleConstructor;
    }
}
