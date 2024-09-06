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

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.testing.CountingTaskExecutorRule;
import androidx.collection.SimpleArrayMap;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.ISampleDatabaseService;
import androidx.room.integration.testapp.SampleDatabaseService;
import androidx.room.integration.testapp.database.Customer;
import androidx.room.integration.testapp.database.Description;
import androidx.room.integration.testapp.database.Product;
import androidx.room.integration.testapp.database.SampleDatabase;
import androidx.room.integration.testapp.database.SampleFtsDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ServiceTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MultiInstanceInvalidationTest {

    @Rule
    public TestName testName = new TestName();

    private static final Customer CUSTOMER_1 = new Customer();
    private static final Customer CUSTOMER_2 = new Customer();

    static {
        CUSTOMER_1.setId(1);
        CUSTOMER_1.setName("John");
        CUSTOMER_1.setLastName("Doe");
        CUSTOMER_2.setId(2);
        CUSTOMER_2.setName("Jane");
        CUSTOMER_2.setLastName("Doe");
    }

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    @Rule
    public final CountingTaskExecutorRule mExecutorRule = new CountingTaskExecutorRule();

    private ISampleDatabaseService mService;

    private String mDatabaseName;

    private final ArrayList<RoomDatabase> mDatabases = new ArrayList<>();
    private final SimpleArrayMap<LiveData<List<Customer>>, Observer<List<Customer>>> mObservers =
            new SimpleArrayMap<>();

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        // use a separate database file for each test because we are not fully capable of closing
        // and deleting a database connection in a multi-process setup
        mDatabaseName = "multi-process-" + testName.getMethodName() + ".db";
        context.deleteDatabase(mDatabaseName);
    }

    @After
    public void tearDown() throws InterruptedException, TimeoutException {
        for (int i = 0, size = mObservers.size(); i < size; i++) {
            final LiveData<List<Customer>> liveData = mObservers.keyAt(i);
            final Observer<List<Customer>> observer = mObservers.valueAt(i);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    liveData.removeObserver(observer));
        }
        if (mService != null) {
            serviceRule.unbindService();
        }
        for (RoomDatabase db : mDatabases) {
            db.close();
        }
        mExecutorRule.drainTasks(3, TimeUnit.SECONDS);
        assertWithMessage("Executor isIdle")
                .that(mExecutorRule.isIdle())
                .isTrue();
    }

    @Test
    public void invalidateInAnotherInstance() throws Exception {
        final SampleDatabase db1 = openDatabase(true);
        final SampleDatabase db2 = openDatabase(true);

        final CountDownLatch invalidated1 = prepareTableObserver(db1);

        db2.getCustomerDao().insert(CUSTOMER_1);

        assertWithMessage("Observer invalidation")
                .that(invalidated1.await(3, TimeUnit.SECONDS))
                .isTrue();
    }

    @Test
    public void invalidateInAnotherInstanceFts() throws Exception {
        final SampleFtsDatabase db1 = openFtsDatabase(true);
        final SampleFtsDatabase db2 = openFtsDatabase(true);

        Product theProduct = new Product(1, "Candy");
        db2.getProductDao().insert(theProduct);
        db2.getProductDao().addDescription(new Description(1, "Delicious candy."));

        final CountDownLatch changed = new CountDownLatch(1);
        db1.getInvalidationTracker().addObserver(new InvalidationTracker.Observer("Description") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                changed.countDown();
            }
        });

        db2.getProductDao().addDescription(new Description(1, "Wonderful candy."));

        assertWithMessage("Observer invalidation")
                .that(changed.await(3, TimeUnit.SECONDS))
                .isTrue();

        List<Product> result = db1.getProductDao().getProductsWithDescription("candy");
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(theProduct);
    }

    @Test
    public void invalidationInAnotherInstance_noMultiInstanceInvalidation() throws Exception {
        final SampleDatabase db1 = openDatabase(false);
        final SampleDatabase db2 = openDatabase(false);

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final CountDownLatch invalidated2 = prepareTableObserver(db2);

        db2.getCustomerDao().insert(CUSTOMER_1);

        assertWithMessage("Observer invalidation")
                .that(invalidated1.await(200, TimeUnit.MILLISECONDS))
                .isFalse();

        assertWithMessage("Observer invalidation")
                .that(invalidated2.await(3, TimeUnit.SECONDS))
                .isTrue();
    }

    @Test
    public void invalidationInAnotherInstance_mixed() throws Exception {
        final SampleDatabase db1 = openDatabase(false);
        final SampleDatabase db2 = openDatabase(true); // Enabled only on one side

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final CountDownLatch invalidated2 = prepareTableObserver(db2);

        db2.getCustomerDao().insert(CUSTOMER_1);

        assertWithMessage("Observer invalidation")
                .that(invalidated1.await(200, TimeUnit.MILLISECONDS))
                .isFalse();

        assertWithMessage("Observer invalidation")
                .that(invalidated2.await(3, TimeUnit.SECONDS))
                .isTrue();
    }

    @Test
    @Ignore // Flaky test, b/363246309.
    public void invalidationInAnotherInstance_closed() throws Exception {
        final SampleDatabase db1 = openDatabase(true);
        final SampleDatabase db2 = openDatabase(true);
        final SampleDatabase db3 = openDatabase(true);

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final CountDownLatch invalidated2 = prepareTableObserver(db2);
        final CountDownLatch invalidated3 = prepareTableObserver(db3);

        db3.close();
        db2.getCustomerDao().insert(CUSTOMER_1);

        assertWithMessage("Observer invalidation")
                .that(invalidated1.await(3, TimeUnit.SECONDS))
                .isTrue();
        assertWithMessage("Observer invalidation")
                .that(invalidated2.await(3, TimeUnit.SECONDS))
                .isTrue();
        assertWithMessage("Observer invalidation")
                .that(invalidated3.await(200, TimeUnit.MILLISECONDS))
                .isFalse();
    }

    @Test
    public void invalidatedByAnotherProcess() throws Exception {
        bindTestService();

        final SampleDatabase db = openDatabase(true);
        assertThat(db.getCustomerDao().countCustomers()).isEqualTo(0);

        final CountDownLatch invalidated = prepareTableObserver(db);

        mService.insertCustomer(CUSTOMER_1.getId(), CUSTOMER_1.getName(), CUSTOMER_1.getLastName());

        assertWithMessage("Observer invalidation")
                .that(invalidated.await(3, TimeUnit.SECONDS))
                .isTrue();
    }

    @Test
    public void invalidateAnotherProcess() throws Exception {
        bindTestService();

        final SampleDatabase db = openDatabase(true);

        ArchTaskExecutor.getIOThreadExecutor().execute(
                () -> {
                    // This sleep is needed to wait for the IPC of waitForCustomer() to
                    // reach the other process and to let the observer in the other process
                    // subscribe to invalidation. If we insert before the other process registers
                    // the observer then we'll miss the observer.
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        // no-op
                    }
                    db.getCustomerDao().insert(CUSTOMER_1);
                }
        );

        boolean awaitResult = mService.waitForCustomer(
                CUSTOMER_1.getId(), CUSTOMER_1.getName(), CUSTOMER_1.getLastName());
        assertWithMessage(
                "Observer invalidation in another process")
                .that(awaitResult)
                .isTrue();
    }

    private SampleDatabase openDatabase(boolean multiInstanceInvalidation) {
        final Context context = ApplicationProvider.getApplicationContext();
        final RoomDatabase.Builder<SampleDatabase> builder = Room
                .databaseBuilder(context, SampleDatabase.class, mDatabaseName);
        if (multiInstanceInvalidation) {
            builder.enableMultiInstanceInvalidation();
        }
        final SampleDatabase db = builder.build();
        mDatabases.add(db);
        return db;
    }

    private SampleFtsDatabase openFtsDatabase(boolean multiInstanceInvalidation) {
        final Context context = ApplicationProvider.getApplicationContext();
        final RoomDatabase.Builder<SampleFtsDatabase> builder = Room
                .databaseBuilder(context, SampleFtsDatabase.class, mDatabaseName);
        if (multiInstanceInvalidation) {
            builder.enableMultiInstanceInvalidation();
        }
        final SampleFtsDatabase db = builder.build();
        mDatabases.add(db);
        return db;
    }

    private void bindTestService() throws TimeoutException {
        final Context context = ApplicationProvider.getApplicationContext();
        mService = ISampleDatabaseService.Stub.asInterface(
                serviceRule.bindService(SampleDatabaseService.intentFor(
                        context,
                        mDatabaseName
                )));
    }

    private CountDownLatch prepareTableObserver(SampleDatabase db) {
        final CountDownLatch invalidated = new CountDownLatch(1);
        db.getInvalidationTracker()
                .addObserver(new InvalidationTracker.Observer("Customer", "Product") {
                    @Override
                    public void onInvalidated(@NonNull Set<String> tables) {
                        assertThat(tables).hasSize(1);
                        assertThat(tables).contains("Customer");
                        invalidated.countDown();
                    }
                });
        return invalidated;
    }
}
