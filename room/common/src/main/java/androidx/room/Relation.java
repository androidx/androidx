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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A convenience annotation which can be used in a Pojo to automatically fetch relation entities.
 * When the Pojo is returned from a query, all of its relations are also fetched by Room.
 *
 * <pre>
 * {@literal @}Entity
 * public class Pet {
 *     {@literal @} PrimaryKey
 *     int id;
 *     int userId;
 *     String name;
 *     // other fields
 * }
 * public class UserNameAndAllPets {
 *   public int id;
 *   public String name;
 *   {@literal @}Relation(parentColumn = "id", entityColumn = "userId")
 *   public List&lt;Pet&gt; pets;
 * }
 *
 * {@literal @}Dao
 * public interface UserPetDao {
 *     {@literal @}Query("SELECT id, name from User")
 *     public List&lt;UserNameAndAllPets&gt; loadUserAndPets();
 * }
 * </pre>
 * <p>
 * The type of the field annotated with {@code Relation} must be a {@link java.util.List} or
 * {@link java.util.Set}. By default, the {@link Entity} type is inferred from the return type.
 * If you would like to return a different object, you can specify the {@link #entity()} property
 * in the annotation.
 * <pre>
 * public class User {
 *     int id;
 *     // other fields
 * }
 * public class PetNameAndId {
 *     int id;
 *     String name;
 * }
 * public class UserAllPets {
 *   {@literal @}Embedded
 *   public User user;
 *   {@literal @}Relation(parentColumn = "id", entityColumn = "userId", entity = Pet.class)
 *   public List&lt;PetNameAndId&gt; pets;
 * }
 * {@literal @}Dao
 * public interface UserPetDao {
 *     {@literal @}Query("SELECT * from User")
 *     public List&lt;UserAllPets&gt; loadUserAndPets();
 * }
 * </pre>
 * <p>
 * In the example above, {@code PetNameAndId} is a regular Pojo but all of fields are fetched
 * from the {@code entity} defined in the {@code @Relation} annotation (<i>Pet</i>).
 * {@code PetNameAndId} could also define its own relations all of which would also be fetched
 * automatically.
 * <p>
 * If you would like to specify which columns are fetched from the child {@link Entity}, you can
 * use {@link #projection()} property in the {@code Relation} annotation.
 * <pre>
 * public class UserAndAllPets {
 *   {@literal @}Embedded
 *   public User user;
 *   {@literal @}Relation(parentColumn = "id", entityColumn = "userId", entity = Pet.class,
 *           projection = {"name"})
 *   public List&lt;String&gt; petNames;
 * }
 * </pre>
 * <p>
 * Note that {@code @Relation} annotation can be used only in Pojo classes, an {@link Entity} class
 * cannot have relations. This is a design decision to avoid common pitfalls in {@link Entity}
 * setups. You can read more about it in the main Room documentation. When loading data, you can
 * simply work around this limitation by creating Pojo classes that extend the {@link Entity}.
 * <p>
 * Note that the {@code @Relation} annotated field cannot be a constructor parameter, it must be
 * public or have a public setter.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface Relation {
    /**
     * The entity to fetch the item from. You don't need to set this if the entity matches the
     * type argument in the return type.
     *
     * @return The entity to fetch from. By default, inherited from the return type.
     */
    Class entity() default Object.class;

    /**
     * Reference field in the parent Pojo.
     * <p>
     * If you would like to access to a sub item of a {@link Embedded}d field, you can use
     * the {@code .} notation.
     * <p>
     * For instance, if you have a {@link Embedded}d field named {@code user} with a sub field
     * {@code id}, you can reference it via {@code user.id}.
     * <p>
     * This value will be matched against the value defined in {@link #entityColumn()}.
     *
     * @return The field reference in the parent object.
     */
    String parentColumn();

    /**
     * The field path to match in the {@link #entity()}. This value will be matched against the
     * value defined in {@link #parentColumn()}.
     */
    String entityColumn();

    /**
     * If sub fields should be fetched from the entity, you can specify them using this field.
     * <p>
     * By default, inferred from the the return type.
     *
     * @return The list of columns to be selected from the {@link #entity()}.
     */
    String[] projection() default {};
}
