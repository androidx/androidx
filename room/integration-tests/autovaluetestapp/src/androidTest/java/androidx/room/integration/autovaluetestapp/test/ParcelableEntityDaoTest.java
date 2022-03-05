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

import android.os.Bundle;

import androidx.room.integration.autovaluetestapp.vo.ParcelableEntity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParcelableEntityDaoTest extends TestDatabaseTest {

    @Test
    @SuppressWarnings("deprecation")
    public void readWrite_toFromBundle() {
        ParcelableEntity entity = ParcelableEntity.create("value");
        Bundle bundle = new Bundle();
        bundle.putParcelable("entity", entity);
        ParcelableEntity loaded = bundle.getParcelable("entity");
        assertThat(loaded, is(entity));
    }

    @Test
    public void readWrite_listOfEntities() {
        List<ParcelableEntity> entities = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            entities.add(ParcelableEntity.create("value" + i));
        }
        mParcelableEntityDao.insertAll(entities);

        List<ParcelableEntity> loaded = mParcelableEntityDao.getAll();
        assertThat(entities, is(loaded));
    }
}
