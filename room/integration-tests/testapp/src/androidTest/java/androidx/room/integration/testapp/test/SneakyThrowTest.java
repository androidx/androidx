/*
 * Copyright 2019 The Android Open Source Project
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.filters.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SmallTest
@RunWith(JUnit4.class)
public class SneakyThrowTest extends TestDatabaseTest {

    @Test
    public void testRuntimeException_catchRuntimeException() {
        try {
            mDatabase.runInTransaction(() -> {
                throw new IllegalStateException("Boom");
            });
            fail("runInTransaction should have thrown an exception");
        } catch (IllegalStateException e) {
            // no-op on purpose
        }
    }

    @Test
    public void testCheckedException_catchThrowable() {
        try {
            mDatabase.runInTransaction(() -> {
                JSONObject json = new JSONObject();
                return json.getString("key"); // method declares that it throws JSONException
            });
            fail("runInTransaction should have thrown an exception");
        } catch (Throwable e) {
            assertTrue(e instanceof JSONException);
        }
    }

    @Test
    public void testCheckedException_catchException() {
        try {
            mDatabase.runInTransaction(() -> {
                JSONObject json = new JSONObject();
                return json.getString("key"); // method declares that it throws JSONException
            });
            fail("runInTransaction should have thrown an exception");
        } catch (Exception e) {
            assertTrue(e instanceof JSONException);
        }
    }

    @Test
    public void testCheckedException_catchCheckedException() {
        try {
            // Must move the lambda to a method that declares throwing the checked exception,
            // otherwise compiler complains about 'exception not thrown in corresponding block', a
            // limitation of the sneaky throw technique.
            doJsonWork();
            fail("doJsonWork should have thrown an exception");
        } catch (JSONException e) {
            // no-op on purpose
        }
    }

    private void doJsonWork() throws JSONException {
        mDatabase.runInTransaction(() -> {
            JSONObject json = new JSONObject();
            return json.getString("key"); // method declares that it throws JSONException
        });
    }
}
