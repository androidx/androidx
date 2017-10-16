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

package android.arch.persistence.room.integration.testapp.test;

import android.arch.persistence.room.integration.testapp.vo.Address;
import android.arch.persistence.room.integration.testapp.vo.Coordinates;
import android.arch.persistence.room.integration.testapp.vo.Pet;
import android.arch.persistence.room.integration.testapp.vo.School;
import android.arch.persistence.room.integration.testapp.vo.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TestUtil {
    public static User[] createUsersArray(int... ids) {
        User[] result = new User[ids.length];
        for (int i = 0; i < ids.length; i++) {
            result[i] = createUser(ids[i]);
        }
        return result;
    }

    public static List<User> createUsersList(int... ids) {
        List<User> result = new ArrayList<>();
        for (int id : ids) {
            result.add(createUser(id));
        }
        return result;
    }

    public static User createUser(int id) {
        User user = new User();
        user.setId(id);
        user.setName(UUID.randomUUID().toString());
        user.setLastName(UUID.randomUUID().toString());
        user.setAge((int) (10 + Math.random() * 50));
        user.setCustomField(UUID.randomUUID().toString());
        user.setBirthday(new Date());
        return user;
    }

    public static Pet createPet(int id) {
        Pet pet = new Pet();
        pet.setPetId(id);
        pet.setName(UUID.randomUUID().toString());
        return pet;
    }

    public static Pet[] createPetsForUser(int uid, int petStartId, int count) {
        Pet[] pets = new Pet[count];
        for (int i = 0; i < count; i++) {
            Pet pet = createPet(petStartId++);
            pet.setUserId(uid);
            pets[i] = pet;
        }
        return pets;
    }

    public static School createSchool(int id, int managerId) {
        School school = new School();
        school.setId(id);
        school.setName(UUID.randomUUID().toString());
        school.setManager(createUser(managerId));
        school.setAddress(createAddress());
        return school;
    }

    private static Address createAddress() {
        Address address = new Address();
        address.setCoordinates(createCoordinates());
        address.setPostCode((int) (Math.random() * 1000 + 1000));
        address.setState(UUID.randomUUID().toString().substring(0, 2));
        address.setStreet(UUID.randomUUID().toString());
        return address;
    }

    private static Coordinates createCoordinates() {
        Coordinates coordinates = new Coordinates();
        coordinates.lat = Math.random();
        coordinates.lng = Math.random();
        return coordinates;
    }
}
