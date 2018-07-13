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

package androidx.room.integration.autovaluetestapp.test;

import static androidx.test.espresso.matcher.ViewMatchers.assertThat;

import static org.hamcrest.CoreMatchers.is;

import androidx.room.integration.autovaluetestapp.vo.Person;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PersonDaoTest extends TestDatabaseTest {

    @Test
    public void readWrite() {
        Person entity = Person.create(1, "1stName", "lastName");
        mPersonDao.insert(entity);
        Person loaded = mPersonDao.getPerson(1);
        assertThat(loaded, is(entity));
    }

    @Test
    public void readWrite_listOfEntities() {
        List<Person> entities = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            entities.add(Person.create(i, "name" + i, "lastName"));
        }
        mPersonDao.insertAll(entities);

        List<Person> loaded = mPersonDao.getAllPersons();
        assertThat(entities, is(loaded));
    }
}
