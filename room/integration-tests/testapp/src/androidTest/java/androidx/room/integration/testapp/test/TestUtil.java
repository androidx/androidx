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

package androidx.room.integration.testapp.test;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.integration.testapp.vo.Address;
import androidx.room.integration.testapp.vo.Coordinates;
import androidx.room.integration.testapp.vo.House;
import androidx.room.integration.testapp.vo.Mail;
import androidx.room.integration.testapp.vo.Pet;
import androidx.room.integration.testapp.vo.School;
import androidx.room.integration.testapp.vo.Toy;
import androidx.room.integration.testapp.vo.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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
        pet.setAdoptionDate(new Date());
        return pet;
    }

    public static Toy createToyForPet(Pet pet, int toyId) {
        Toy toy = new Toy();
        toy.setName("toy " + toyId);
        toy.setId(toyId);
        toy.setPetId(pet.getPetId());
        return toy;
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

    public static House[] createHousesForUser(int uid, int houseStartId, int count) {
        House[] houses = new House[count];
        for (int i = 0; i < count; i++) {
            houses[i] = new House(houseStartId++, uid, createAddress());
        }
        return houses;
    }

    public static Mail createMail(int id, String subject, String body) {
        Mail mail = new Mail();
        mail.rowId = id;
        mail.subject = subject;
        mail.body = body;
        mail.datetime = System.currentTimeMillis();
        return mail;
    }

    public static void observeOnMainThread(final LiveData liveData, final LifecycleOwner provider,
            final Observer observer) throws ExecutionException, InterruptedException {
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            //noinspection unchecked
            liveData.observe(provider, observer);
            return null;
        });
        ArchTaskExecutor.getInstance().executeOnMainThread(futureTask);
        futureTask.get();
    }

    public static void observeForeverOnMainThread(final LiveData liveData, final Observer observer)
            throws ExecutionException, InterruptedException {
        FutureTask<Void> futureTask = new FutureTask<>(() -> {
            //noinspection unchecked
            liveData.observeForever(observer);
            return null;
        });
        ArchTaskExecutor.getInstance().executeOnMainThread(futureTask);
        futureTask.get();
    }

    public static void forceGc() {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
    }

    private TestUtil() {
    }
}
