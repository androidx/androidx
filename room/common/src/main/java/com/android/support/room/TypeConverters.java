/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.support.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies additional type converters that Room can use. The TypeConverter is added to the scope
 * of the element so if you put it on a class / interface, all methods / fields in that class will
 * be able to use the converters.
 * <ul>
 * <li>If you put it on a @Database, all Daos and Entities in that database will be able to use it.
 * <li>If you put it on a @Dao, all methods in the Dao will be able to use it.
 * <li>If you put it on an @Entity, all fields of the Entity will be able to use it.
 * <li>If you put it on a Pojo, all fields of the Pojo will be able to use it.
 * <li>If you put it on a Field, only that field will be able to use it.
 * <li>If you put it on a Dao method, all parameters of the method will be able to use it.
 * <li>If you put it on a Dao method parameter, just that field will be able to use it.
 *
 * @see TypeConverter
 */
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.SOURCE)
public @interface TypeConverters {
    /**
     * The list of type converter classes. If converter methods are not static, Room will create
     * and instance of these classes.
     * @return The list of classes that contains the converter methods.
     */
    Class<?>[] value();
}
