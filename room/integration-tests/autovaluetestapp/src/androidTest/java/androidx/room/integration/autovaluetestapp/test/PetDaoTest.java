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

import androidx.room.integration.autovaluetestapp.vo.EmbeddedAutoValue;
import androidx.room.integration.autovaluetestapp.vo.Pet;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PetDaoTest extends TestDatabaseTest {

    @Test
    public void read_embeddedAtuoValue() {
        Pet.Cat catEntity = Pet.Cat.create(1, "Tom");
        mPetDao.insert(catEntity);
        EmbeddedAutoValue loaded = mPetDao.getCatAndSomething(1);
        assertThat(loaded.getCat(), is(catEntity));
    }
}
