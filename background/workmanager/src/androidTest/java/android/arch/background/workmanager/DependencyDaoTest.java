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

package android.arch.background.workmanager;

import static android.arch.background.workmanager.Work.STATUS_BLOCKED;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import android.arch.background.workmanager.model.Dependency;
import android.arch.background.workmanager.model.DependencyDao;
import android.arch.background.workmanager.model.WorkSpec;
import android.arch.background.workmanager.model.WorkSpecDao;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class DependencyDaoTest extends DatabaseTest {
    private static final String TEST_PREREQUISITE_ID = "TestPrerequisiteId";
    private DependencyDao mDependencyDao;

    @Before
    public void setUp() {
        mDependencyDao = mDatabase.dependencyDao();
    }

    @Test
    @SmallTest
    public void getWorkSpecIdsWithSinglePrerequisite() {
        final String expectedWorkSpecId = "a";
        final Dependency[] dependencies = new Dependency[] {
                new Dependency(expectedWorkSpecId, TEST_PREREQUISITE_ID),
                new Dependency("b", TEST_PREREQUISITE_ID),
                new Dependency("b", "y"),
                new Dependency("c", "z")
        };

        insertDependenciesWithWorkSpecs(dependencies);
        List<String> resultWorkSpecIds =
                mDependencyDao.getWorkSpecIdsWithSinglePrerequisite(TEST_PREREQUISITE_ID);
        assertThat(resultWorkSpecIds, hasSize(1));
        assertThat(resultWorkSpecIds.get(0), is(expectedWorkSpecId));
    }

    @Test
    @LargeTest
    public void getWorkSpecIdsWithSinglePrerequisite_BigDb() {
        final int numDependencies = 100; // Can be changed to test against a larger table.
        final Dependency[] dependencies = new Dependency[numDependencies];
        final Dependency expectedDep1 = new Dependency("expected" + 0, TEST_PREREQUISITE_ID);
        final Dependency expectedDep2 = new Dependency("expected" + 1, TEST_PREREQUISITE_ID);

        dependencies[0] = expectedDep1;
        dependencies[1] = expectedDep2;
        for (int i = 2; i < numDependencies; i += 2) {
            String otherWorkSpecId = "other" + i;
            dependencies[i] = new Dependency(otherWorkSpecId, TEST_PREREQUISITE_ID);
            dependencies[i + 1] = new Dependency(otherWorkSpecId, "otherPrerequisite" + i);
        }
        insertDependenciesWithWorkSpecs(dependencies);
        List<String> resultWorkSpecIds =
                    mDependencyDao.getWorkSpecIdsWithSinglePrerequisite(TEST_PREREQUISITE_ID);
        assertThat(resultWorkSpecIds, hasSize(2));
        assertThat(resultWorkSpecIds,
                containsInAnyOrder(expectedDep1.getWorkSpecId(), expectedDep2.getWorkSpecId()));
    }

    @Test
    @SmallTest
    public void deleteDependenciesWithPrerequisite() {
        final Dependency[] dependencies = new Dependency[] {
                new Dependency("a", TEST_PREREQUISITE_ID),
                new Dependency("b", TEST_PREREQUISITE_ID),
                new Dependency("b", "y"),
                new Dependency("c", "z")
        };

        insertDependenciesWithWorkSpecs(dependencies);
        // Note that this does NOT set unblocked dependencies to ENQUEUED (and this method does not
        // test for that).
        mDependencyDao.deleteDependenciesWithPrerequisite(TEST_PREREQUISITE_ID);
        List<Dependency> resultDependencies = mDependencyDao.getAllDependencies();
        assertThat(resultDependencies, is(notNullValue()));
        for (Dependency dependency : resultDependencies) {
            assertThat(dependency.getPrerequisiteId(), is(not(TEST_PREREQUISITE_ID)));
        }
    }

    /**
     * Inserts {@link Dependency}s with corresponding {@link WorkSpec}s.
     * {@link Dependency}'s foreign key constraint enforces that there is a corresponding
     * {@link WorkSpec}s with {@link Dependency#mPrerequisiteId} and {@link Dependency#mWorkSpecId}.
     */
    private void insertDependenciesWithWorkSpecs(Dependency... dependencies) {
        // Prevent re-inserting work specs with the same IDs.
        Set<String> workSpecIds = new HashSet<>();
        for (Dependency dependency : dependencies) {
            workSpecIds.add(dependency.getPrerequisiteId());
            workSpecIds.add(dependency.getWorkSpecId());
        }

        WorkSpecDao workSpecDao = mDatabase.workSpecDao();
        mDatabase.beginTransaction();
        try {
            for (String workSpecId : workSpecIds) {
                workSpecDao.insertWorkSpec(new WorkSpec(workSpecId));
            }
            for (Dependency dependency : dependencies) {
                workSpecDao.setStatus(STATUS_BLOCKED, dependency.getWorkSpecId());
                mDependencyDao.insertDependency(dependency);
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }
}
