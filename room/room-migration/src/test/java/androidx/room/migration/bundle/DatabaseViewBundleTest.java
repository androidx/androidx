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

package androidx.room.migration.bundle;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DatabaseViewBundleTest {

    @Test
    public void basic() {
        DatabaseViewBundle bundle = new DatabaseViewBundle("abc", "def");
        DatabaseViewBundle other = new DatabaseViewBundle("abc", "def");
        assertThat(bundle.isSchemaEqual(other), is(true));
        assertThat(bundle.getViewName(), is(equalTo("abc")));
        assertThat(bundle.getCreateSql(), is(equalTo("def")));
    }
}
