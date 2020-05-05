/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.sqlite.inspection;

import androidx.annotation.NonNull;
import androidx.inspection.InspectorEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

class SqlDelightInvalidation {
    private static final String SQLDELIGHT_QUERY_CLASS_NAME = "com.squareup.sqldelight.Query";
    private static final String SQLDELIGHT_NOTIFY_METHOD_NAME = "notifyDataChanged";

    private final InspectorEnvironment mEnvironment;
    private final Class<?> mQueryClass;
    private final Method mNotifyDataChangeMethod;

    SqlDelightInvalidation(InspectorEnvironment environment, Class<?> queryClass,
            Method notifyDataChangeMethod) {
        mQueryClass = queryClass;
        mEnvironment = environment;
        mNotifyDataChangeMethod = notifyDataChangeMethod;
    }

    @NonNull
    static SqlDelightInvalidation create(@NonNull InspectorEnvironment environment) {
        ClassLoader classLoader = SqlDelightInvalidation.class.getClassLoader();
        try {
            Class<?> queryClass = classLoader.loadClass(SQLDELIGHT_QUERY_CLASS_NAME);
            Method notifyMethod = queryClass.getMethod(SQLDELIGHT_NOTIFY_METHOD_NAME);
            return new SqlDelightInvalidation(environment, queryClass, notifyMethod);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return new SqlDelightInvalidation(environment, null, null);
        }
    }

    void triggerInvalidations() {
        if (mQueryClass == null || mNotifyDataChangeMethod == null) {
            return;
        }
        // invalidating all queries because we can't say which ones were actually affected.
        List<?> queries = mEnvironment.findInstances(mQueryClass);
        for (Object query: queries) {
            notifyDataChanged(query);
        }
    }

    private void notifyDataChanged(Object query) {
        try {
            mNotifyDataChangeMethod.invoke(query);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // ok it didn't work out for us,
            // in first version we don't have a special UI around it,
            // so we can't do much about it.
        }
    }
}
