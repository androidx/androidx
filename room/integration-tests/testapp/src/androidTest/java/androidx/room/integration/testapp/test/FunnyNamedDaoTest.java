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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.room.integration.testapp.vo.FunnyNamedEntity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FunnyNamedDaoTest extends TestDatabaseTest {
    @Rule
    public CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    @Test
    public void readWrite() {
        FunnyNamedEntity entity = new FunnyNamedEntity(1, "a");
        mFunnyNamedDao.insert(entity);
        FunnyNamedEntity loaded = mFunnyNamedDao.load(1);
        assertThat(loaded, is(entity));
    }

    @Test
    public void update() {
        FunnyNamedEntity entity = new FunnyNamedEntity(1, "a");
        mFunnyNamedDao.insert(entity);
        entity.setValue("b");
        mFunnyNamedDao.update(entity);
        FunnyNamedEntity loaded = mFunnyNamedDao.load(1);
        assertThat(loaded.getValue(), is("b"));
    }

    @Test
    public void delete() {
        FunnyNamedEntity entity = new FunnyNamedEntity(1, "a");
        mFunnyNamedDao.insert(entity);
        assertThat(mFunnyNamedDao.load(1), notNullValue());
        mFunnyNamedDao.delete(entity);
        assertThat(mFunnyNamedDao.load(1), nullValue());
    }

    @Test
    public void observe() throws TimeoutException, InterruptedException {
        final FunnyNamedEntity[] item = new FunnyNamedEntity[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> mFunnyNamedDao.observableOne(2).observeForever(
                        funnyNamedEntity -> item[0] = funnyNamedEntity));

        FunnyNamedEntity entity = new FunnyNamedEntity(1, "a");
        mFunnyNamedDao.insert(entity);
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
        assertThat(item[0], nullValue());

        final FunnyNamedEntity entity2 = new FunnyNamedEntity(2, "b");
        mFunnyNamedDao.insert(entity2);
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
        assertThat(item[0], is(entity2));

        final FunnyNamedEntity entity3 = new FunnyNamedEntity(2, "c");
        mFunnyNamedDao.update(entity3);
        mExecutorRule.drainTasks(1, TimeUnit.MINUTES);
        assertThat(item[0], is(entity3));
    }
}
