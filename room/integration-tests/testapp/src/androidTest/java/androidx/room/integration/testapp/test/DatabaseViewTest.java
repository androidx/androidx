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

package androidx.room.integration.testapp.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.DatabaseView;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Insert;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.Transaction;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class DatabaseViewTest {

    @Entity(
            foreignKeys = {
                    @ForeignKey(
                            entity = Employee.class,
                            childColumns = {"managerId"},
                            parentColumns = {"id"}),
                    @ForeignKey(
                            entity = Team.class,
                            childColumns = {"teamId"},
                            parentColumns = {"id"})},
            indices = {
                    @Index("managerId"),
                    @Index("teamId")})
    static class Employee {
        @PrimaryKey
        public long id;
        public String name;
        public Long managerId;
        public long teamId;

        Employee(long id, String name, Long managerId, long teamId) {
            this.id = id;
            this.name = name;
            this.managerId = managerId;
            this.teamId = teamId;
        }
    }

    @DatabaseView("SELECT"
            + "  employee.id"
            + ", employee.name"
            + ", employee.teamId"
            + ", manager.id AS manager_id"
            + ", manager.name AS manager_name"
            + ", manager.managerId AS manager_managerId"
            + ", manager.teamId AS manager_teamId"
            + " FROM Employee AS employee LEFT JOIN Employee AS manager"
            + " ON employee.managerId = manager.id")
    static class EmployeeWithManager {
        public long id;
        public String name;
        public long teamId;
        @Embedded(prefix = "manager_")
        public Employee manager;
    }

    @DatabaseView("SELECT"
            + "  employee.id"
            + ", employee.name"
            + ", employee.manager_id"
            + ", employee.manager_name"
            + ", employee.manager_managerId"
            + ", employee.manager_teamId"
            + ", team.id AS team_id"
            + ", team.name AS team_name"
            + ", team.departmentId AS team_departmentId"
            + ", team.departmentName AS team_departmentName"
            + " FROM EmployeeWithManager AS employee"
            + " LEFT JOIN TeamDetail AS team ON employee.teamId = team.id")
    static class EmployeeDetail {
        public long id;
        public String name;
        @Embedded(prefix = "manager_")
        public Employee manager;
        @Embedded(prefix = "team_")
        public TeamDetail team;
    }

    static class TeamWithMembers {
        @Embedded
        public TeamDetail teamDetail;
        @Relation(parentColumn = "id", entityColumn = "teamId")
        public List<EmployeeWithManager> members;
    }

    @Entity(
            foreignKeys = {
                    @ForeignKey(
                            entity = Department.class,
                            childColumns = {"departmentId"},
                            parentColumns = {"id"})},
            indices = {
                    @Index("departmentId")})
    static class Team {
        @PrimaryKey
        public long id;
        public long departmentId;
        public String name;

        Team(long id, long departmentId, String name) {
            this.id = id;
            this.departmentId = departmentId;
            this.name = name;
        }

        @NonNull
        @Override
        public String toString() {
            return name + " (" + id + ", " + departmentId + ")";
        }
    }

    @Entity
    static class Department {
        @PrimaryKey
        public long id;
        public String name;

        Department(long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @DatabaseView(
            "SELECT Team.id, Team.name, Team.departmentId, Department.name AS departmentName "
                    + "FROM Team INNER JOIN Department ON Team.departmentId = Department.id ")
    static class TeamDetail {
        public long id;
        public String name;
        public long departmentId;
        public String departmentName;
    }

    @DatabaseView(
            "SELECT * FROM Team "
                    + "INNER JOIN Department AS department_ "
                    + "ON Team.departmentId = department_.id"
    )
    static class TeamDetail2 {
        @Embedded
        public Team team;
        @Embedded(prefix = "department_")
        public Department department;
    }

    @DatabaseView("SELECT * FROM TeamDetail AS first_, TeamDetail AS second_ "
            + "WHERE first_.id <> second_.id")
    static class TeamPair {
        @Embedded(prefix = "first_")
        public TeamDetail first;
        @Embedded(prefix = "second_")
        public TeamDetail second;
    }

    @Dao
    interface EmployeeDao {
        @Insert
        long insert(Employee employee);

        @Query("UPDATE Employee SET managerId = :managerId WHERE id = :id")
        void updateReport(long id, long managerId);

        @Query("SELECT * FROM EmployeeWithManager WHERE id = :id")
        EmployeeWithManager withManagerById(long id);

        @Query("SELECT * FROM EmployeeDetail WHERE id = :id")
        EmployeeDetail detailById(long id);

        @Query("SELECT * FROM EmployeeDetail WHERE id = :id")
        LiveData<EmployeeDetail> liveDetailById(long id);
    }

    @Dao
    interface TeamDao {
        @Insert
        long insert(Team team);

        @Query("SELECT * FROM TeamDetail WHERE id = :id")
        TeamDetail detailById(long id);

        @Query("SELECT * FROM TeamDetail")
        LiveData<List<TeamDetail>> liveDetail();

        @Transaction
        @Query("SELECT * FROM TeamDetail WHERE id = :id")
        TeamWithMembers withMembers(long id);

        @Query("SELECT * FROM TeamDetail2 WHERE id = :id")
        TeamDetail2 detail2ById(long id);

        @Query("SELECT * FROM TeamPair WHERE first_id = :id")
        List<TeamPair> roundRobinById(long id);
    }

    @Dao
    interface DepartmentDao {
        @Insert
        long insert(Department department);

        @Query("UPDATE Department SET name = :name WHERE id = :id")
        void rename(long id, String name);
    }

    @Database(
            entities = {
                    Department.class,
                    Team.class,
                    Employee.class,
            },
            views = {
                    TeamDetail.class,
                    TeamDetail2.class,
                    TeamPair.class,
                    EmployeeWithManager.class,
                    EmployeeDetail.class,
            },
            version = 1,
            exportSchema = false)
    abstract static class CompanyDatabase extends RoomDatabase {

        abstract DepartmentDao department();

        abstract TeamDao team();

        abstract EmployeeDao employee();
    }

    @Rule
    public CountingTaskExecutorRule executorRule = new CountingTaskExecutorRule();

    private CompanyDatabase getDatabase() {
        final Context context = ApplicationProvider.getApplicationContext();
        return Room.inMemoryDatabaseBuilder(context, CompanyDatabase.class).build();
    }

    @Test
    @SmallTest
    public void basic() {
        final CompanyDatabase db = getDatabase();
        db.department().insert(new Department(1L, "Sales"));
        db.department().insert(new Department(2L, "IT"));
        db.team().insert(new Team(1L, 1L, "Books"));
        db.team().insert(new Team(2L, 2L, "Backend"));
        final TeamDetail team1 = db.team().detailById(1L);
        assertThat(team1.name, is(equalTo("Books")));
        assertThat(team1.departmentName, is(equalTo("Sales")));
        final TeamDetail team2 = db.team().detailById(2L);
        assertThat(team2.name, is(equalTo("Backend")));
        assertThat(team2.departmentName, is(equalTo("IT")));
    }

    @Test
    @SmallTest
    public void embedded() {
        final CompanyDatabase db = getDatabase();
        db.department().insert(new Department(1, "Sales"));
        db.team().insert(new Team(1L, 1L, "Books"));
        db.employee().insert(new Employee(1L, "CEO", null, 1L));
        db.employee().insert(new Employee(2L, "John", 1L, 1L));
        db.employee().insert(new Employee(3L, "Jane", 2L, 1L));
        assertThat(db.employee().withManagerById(1L).manager, is(nullValue()));
        assertThat(db.employee().withManagerById(2L).manager.name, is(equalTo("CEO")));
        assertThat(db.employee().withManagerById(3L).manager.name, is(equalTo("John")));
    }

    @Test
    @MediumTest
    public void liveData() throws TimeoutException, InterruptedException {
        final CompanyDatabase db = getDatabase();
        db.department().insert(new Department(1L, "Shop"));
        final LiveData<List<TeamDetail>> teams = db.team().liveDetail();
        @SuppressWarnings("unchecked") final Observer<List<TeamDetail>> observer =
                mock(Observer.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                teams.observeForever(observer));
        db.team().insert(new Team(1L, 1L, "Books"));
        executorRule.drainTasks(3000, TimeUnit.MILLISECONDS);
        verify(observer, timeout(300L).atLeastOnce()).onChanged(argThat((list) ->
                list.size() == 1
                        && list.get(0).departmentName.equals("Shop")
                        && list.get(0).name.equals("Books")));
        resetMock(observer);
        db.department().rename(1L, "Sales");
        executorRule.drainTasks(3000, TimeUnit.MILLISECONDS);
        verify(observer, timeout(300L).atLeastOnce()).onChanged(argThat((list) ->
                list.size() == 1
                        && list.get(0).departmentName.equals("Sales")
                        && list.get(0).name.equals("Books")));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                teams.removeObserver(observer));
    }

    @Test
    @SmallTest
    public void nested() {
        final CompanyDatabase db = getDatabase();
        db.department().insert(new Department(1L, "Shop"));
        db.team().insert(new Team(1L, 1L, "Books"));
        db.employee().insert(new Employee(1L, "CEO", null, 1L));
        db.employee().insert(new Employee(2L, "John", 1L, 1L));

        final EmployeeDetail employee = db.employee().detailById(2L);
        assertThat(employee.name, is(equalTo("John")));
        assertThat(employee.manager.name, is(equalTo("CEO")));
        assertThat(employee.team.name, is(equalTo("Books")));
        assertThat(employee.team.departmentName, is(equalTo("Shop")));
    }

    @Test
    @MediumTest
    public void nestedLive() throws TimeoutException, InterruptedException {
        final CompanyDatabase db = getDatabase();
        final LiveData<EmployeeDetail> employee = db.employee().liveDetailById(2L);
        @SuppressWarnings("unchecked") final Observer<EmployeeDetail> observer =
                mock(Observer.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                employee.observeForever(observer));

        db.department().insert(new Department(1L, "Shop"));
        db.team().insert(new Team(1L, 1L, "Books"));
        db.employee().insert(new Employee(1L, "CEO", null, 1L));
        db.employee().insert(new Employee(2L, "Jane", 1L, 1L));
        executorRule.drainTasks(3000, TimeUnit.MILLISECONDS);

        verify(observer, timeout(300L).atLeastOnce()).onChanged(argThat(e ->
                e != null
                        && e.name.equals("Jane")
                        && e.manager.name.equals("CEO")
                        && e.team.name.equals("Books")
                        && e.team.departmentName.equals("Shop")));

        resetMock(observer);
        db.runInTransaction(() -> {
            db.department().rename(1L, "Sales");
            db.employee().insert(new Employee(3L, "John", 1L, 1L));
            db.employee().updateReport(2L, 3L);
        });

        executorRule.drainTasks(3000, TimeUnit.MILLISECONDS);
        verify(observer, timeout(300L).atLeastOnce()).onChanged(argThat(e ->
                e != null
                        && e.name.equals("Jane")
                        && e.manager.name.equals("John")
                        && e.team.name.equals("Books")
                        && e.team.departmentName.equals("Sales")));

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                employee.removeObserver(observer));
    }

    @Test
    @MediumTest
    public void viewInRelation() {
        final CompanyDatabase db = getDatabase();
        db.department().insert(new Department(1L, "Shop"));
        db.team().insert(new Team(1L, 1L, "Books"));
        db.employee().insert(new Employee(1L, "CEO", null, 1L));
        db.employee().insert(new Employee(2L, "John", 1L, 1L));

        TeamWithMembers teamWithMembers = db.team().withMembers(1L);
        assertThat(teamWithMembers.teamDetail.name, is(equalTo("Books")));
        assertThat(teamWithMembers.teamDetail.departmentName, is(equalTo("Shop")));
        assertThat(teamWithMembers.members, hasSize(2));
        assertThat(teamWithMembers.members.get(0).name, is(equalTo("CEO")));
        assertThat(teamWithMembers.members.get(1).name, is(equalTo("John")));
        assertThat(teamWithMembers.members.get(1).manager.name, is(equalTo("CEO")));
    }

    @SuppressWarnings("unchecked")
    private static <T> void resetMock(T mock) {
        reset(mock);
    }

    @Test
    @MediumTest
    public void expandProjection() {
        final CompanyDatabase db = getDatabase();
        db.department().insert(new Department(3L, "Sales"));
        db.team().insert(new Team(5L, 3L, "Books"));
        final TeamDetail2 detail = db.team().detail2ById(5L);
        assertThat(detail.team.id, is(equalTo(5L)));
        assertThat(detail.team.name, is(equalTo("Books")));
        assertThat(detail.team.departmentId, is(equalTo(3L)));
        assertThat(detail.department.id, is(equalTo(3L)));
        assertThat(detail.department.name, is(equalTo("Sales")));
    }

    @Test
    @MediumTest
    public void expandProjection_embedView() {
        final CompanyDatabase db = getDatabase();
        db.department().insert(new Department(3L, "Sales"));
        db.team().insert(new Team(5L, 3L, "Books"));
        db.team().insert(new Team(7L, 3L, "Toys"));
        List<TeamPair> pairs = db.team().roundRobinById(5L);
        assertThat(pairs, hasSize(1));
        assertThat(pairs.get(0).first.name, is(equalTo("Books")));
        assertThat(pairs.get(0).first.departmentName, is(equalTo("Sales")));
        assertThat(pairs.get(0).second.name, is(equalTo("Toys")));
        assertThat(pairs.get(0).second.departmentName, is(equalTo("Sales")));
    }
}
