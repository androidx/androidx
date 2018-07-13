/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.room.integration.autovaluetestapp.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.integration.autovaluetestapp.vo.EmbeddedAutoValue;
import androidx.room.integration.autovaluetestapp.vo.Pet;

import java.util.List;

@Dao
public interface PetDao {

    @Insert
    void insert(Pet.Cat cat);

    @Query("SELECT id, kittyName FROM cat")
    List<Pet.Cat> getAllCats();

    @Insert
    void insert(Pet.Dog dog);

    @Query("SELECT id, doggoName FROM dog")
    List<Pet.Dog> getAllDogs();

    @Query("SELECT id, kittyName, 'food' as mSomething from cat where id = :catId")
    EmbeddedAutoValue getCatAndSomething(long catId);
}
