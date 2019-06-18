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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method in a {@link Dao} annotated class as an insert method.
 * <p>
 * The implementation of the method will insert its parameters into the database.
 * <p>
 * All of the parameters of the Insert method must either be classes annotated with {@link Entity}
 * or collections/array of it. However if the target entity is specified via {@link #entity()} then
 * the parameters can be of arbitrary POJO types that will be interpreted as partial entities.
 * <p>
 * Example:
 * <pre>
 * {@literal @}Dao
 * public interface MyDao {
 *     {@literal @}Insert(onConflict = OnConflictStrategy.REPLACE)
 *     public void insertUsers(User... users);
 *
 *     {@literal @}Insert
 *     public void insertBoth(User user1, User user2);
 *
 *     {@literal @}Insert
 *     public void insertWithFriends(User user, List&lt;User&gt; friends);
 *
 *     {@literal @}Insert(entity = User.class)
 *     public void insertUsername(Username username);
 * }
 * </pre>
 *
 * @see Update
 * @see Delete
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface Insert {

    /**
     * The target entity of the insert method.
     * <p>
     * When this is declared, the insert method parameters are interpreted as partial entities when
     * the type of the parameter differs from the target. The POJO class that represents the entity
     * must contain all of the non-null fields without default values of the target entity.
     * <p>
     * If the target entity contains a {@link PrimaryKey} that is auto generated, then the POJO
     * class doesn't need an equal primary key field, otherwise primary keys must also be present
     * in the POJO.
     * <p>
     * By default the target entity is interpreted by the method parameters.
     *
     * @return the target entity of the insert method or none if the method should use the
     *         parameter type entities.
     */
    Class<?> entity() default Object.class;

    /**
     * What to do if a conflict happens.
     * <p>
     * Use {@link OnConflictStrategy#ABORT} (default) to roll back the transaction on conflict.
     * Use {@link OnConflictStrategy#REPLACE} to replace the existing rows with the new rows.
     * Use {@link OnConflictStrategy#IGNORE} to keep the existing rows.
     *
     * @return How to handle conflicts. Defaults to {@link OnConflictStrategy#ABORT}.
     */
    @OnConflictStrategy
    int onConflict() default OnConflictStrategy.ABORT;
}
