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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.collection.SimpleArrayMap;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.integration.testapp.ISampleDatabaseService;
import androidx.room.integration.testapp.SampleDatabaseService;
import androidx.room.integration.testapp.database.Customer;
import androidx.room.integration.testapp.database.CustomerDao;
import androidx.room.integration.testapp.database.Description;
import androidx.room.integration.testapp.database.Product;
import androidx.room.integration.testapp.database.SampleDatabase;
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
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MultiInstanceInvalidationTest {

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

    private ISampleDatabaseService mService;

    private final ArrayList<RoomDatabase> mDatabases = new ArrayList<>();
    private final SimpleArrayMap<LiveData<List<Customer>>, Observer<List<Customer>>> mObservers =
            new SimpleArrayMap<>();

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(SampleDatabaseService.DATABASE_NAME);
    }

    @After
    public void tearDown() {
        for (int i = 0, size = mObservers.size(); i < size; i++) {
            final LiveData<List<Customer>> liveData = mObservers.keyAt(i);
            final Observer<List<Customer>> observer = mObservers.valueAt(i);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                    liveData.removeObserver(observer));
        }
        if (mService != null) {
            serviceRule.unbindService();
        }
        for (int i = 0, size = mDatabases.size(); i < size; i++) {
            mDatabases.get(i).close();
        }
    }

    @Test
    public void invalidateInAnotherInstance() throws Exception {
        final SampleDatabase db1 = openDatabase(true);
        final SampleDatabase db2 = openDatabase(true);

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final CountDownLatch changed1 = prepareLiveDataObserver(db1).first;

        db2.getCustomerDao().insert(CUSTOMER_1);

        assertTrue(invalidated1.await(3, TimeUnit.SECONDS));
        assertTrue(changed1.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void invalidateInAnotherInstanceFts() throws Exception {
        final SampleDatabase db1 = openDatabase(true);
        final SampleDatabase db2 = openDatabase(true);

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

        assertTrue(changed.await(3, TimeUnit.SECONDS));
        List<Product> result = db1.getProductDao().getProductsWithDescription("candy");
        assertThat(result.size(), is(1));
        assertThat(result.get(0), is(theProduct));
    }

    @Test
    public void invalidationInAnotherInstance_noMultiInstanceInvalidation() throws Exception {
        final SampleDatabase db1 = openDatabase(false);
        final SampleDatabase db2 = openDatabase(false);

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final CountDownLatch changed1 = prepareLiveDataObserver(db1).first;
        final CountDownLatch invalidated2 = prepareTableObserver(db2);
        final CountDownLatch changed2 = prepareLiveDataObserver(db2).first;

        db2.getCustomerDao().insert(CUSTOMER_1);

        assertTrue(invalidated2.await(3, TimeUnit.SECONDS));
        assertTrue(changed2.await(3, TimeUnit.SECONDS));

        assertFalse(invalidated1.await(300, TimeUnit.MILLISECONDS));
        assertFalse(changed1.await(300, TimeUnit.MILLISECONDS));
        assertThat(db1.getCustomerDao().countCustomers(), is(1));
    }

    @Test
    public void invalidationInAnotherInstance_mixed() throws Exception {
        final SampleDatabase db1 = openDatabase(false);
        final SampleDatabase db2 = openDatabase(true); // Enabled only on one side

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final CountDownLatch changed1 = prepareLiveDataObserver(db1).first;
        final CountDownLatch invalidated2 = prepareTableObserver(db2);
        final CountDownLatch changed2 = prepareLiveDataObserver(db2).first;

        db2.getCustomerDao().insert(CUSTOMER_1);

        assertTrue(invalidated2.await(3, TimeUnit.SECONDS));
        assertTrue(changed2.await(3, TimeUnit.SECONDS));

        assertFalse(invalidated1.await(300, TimeUnit.MILLISECONDS));
        assertFalse(changed1.await(300, TimeUnit.MILLISECONDS));
        assertThat(db1.getCustomerDao().countCustomers(), is(1));
    }

    @Test
    public void invalidationInAnotherInstance_closed() throws Exception {
        final SampleDatabase db1 = openDatabase(true);
        final SampleDatabase db2 = openDatabase(true);
        final SampleDatabase db3 = openDatabase(true);

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final Pair<CountDownLatch, CountDownLatch> changed1 = prepareLiveDataObserver(db1);
        final CountDownLatch invalidated2 = prepareTableObserver(db2);
        final Pair<CountDownLatch, CountDownLatch> changed2 = prepareLiveDataObserver(db2);
        final CountDownLatch invalidated3 = prepareTableObserver(db3);
        final Pair<CountDownLatch, CountDownLatch> changed3 = prepareLiveDataObserver(db3);

        db2.getCustomerDao().insert(CUSTOMER_1);

        assertTrue(invalidated1.await(3, TimeUnit.SECONDS));
        assertTrue(changed1.first.await(3, TimeUnit.SECONDS));
        assertTrue(invalidated2.await(3, TimeUnit.SECONDS));
        assertTrue(changed2.first.await(3, TimeUnit.SECONDS));
        assertTrue(invalidated3.await(3, TimeUnit.SECONDS));
        assertTrue(changed3.first.await(3, TimeUnit.SECONDS));

        db3.close();
        db2.getCustomerDao().insert(CUSTOMER_2);

        assertTrue(changed1.second.await(3, TimeUnit.SECONDS));
        assertTrue(changed2.second.await(3, TimeUnit.SECONDS));
        assertFalse(changed3.second.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void invalidationCausesNoLoop() throws Exception {
        final SampleDatabase db1 = openDatabase(true);
        final SampleDatabase db2 = openDatabase(true);

        final CountDownLatch invalidated1 = prepareTableObserver(db1);
        final CountDownLatch changed1 = prepareLiveDataObserver(db1).first;

        db2.getInvalidationTracker().notifyObserversByTableNames("Customer");

        assertFalse(invalidated1.await(300, TimeUnit.MILLISECONDS));
        assertFalse(changed1.await(300, TimeUnit.MILLISECONDS));
    }

    @Test
    public void reopen() throws Exception {
        final SampleDatabase db1 = openDatabase(true);
        final Product product = new Product();
        product.setId(1);
        product.setName("A");
        db1.getProductDao().insert(product);
        db1.close();
        final SampleDatabase db2 = openDatabase(true);
        final CountDownLatch invalidated2 = prepareTableObserver(db2);
        final CountDownLatch changed2 = prepareLiveDataObserver(db2).first;
        db2.getCustomerDao().insert(CUSTOMER_1);
        assertTrue(invalidated2.await(3, TimeUnit.SECONDS));
        assertTrue(changed2.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void invalidatedByAnotherProcess() throws Exception {
        bindTestService();
        final SampleDatabase db = openDatabase(true);
        assertThat(db.getCustomerDao().countCustomers(), is(0));

        final CountDownLatch invalidated = prepareTableObserver(db);
        final CountDownLatch changed = prepareLiveDataObserver(db).first;

        mService.insertCustomer(CUSTOMER_1.getId(), CUSTOMER_1.getName(), CUSTOMER_1.getLastName());

        assertTrue(invalidated.await(3, TimeUnit.SECONDS));
        assertTrue(changed.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void invalidateAnotherProcess() throws Exception {
        bindTestService();
        final SampleDatabase db = openDatabase(true);
        db.getCustomerDao().insert(CUSTOMER_1);
        assertTrue(mService.waitForCustomer(CUSTOMER_1.getId(), CUSTOMER_1.getName(),
                CUSTOMER_1.getLastName()));
    }

    // TODO: b/72877822 Better performance measurement
    @Ignore
    @Test
    public void performance_oneByOne() {
        final List<Customer> customers = generateCustomers(100);
        final long[] elapsed = measureSeveralTimesEach(false, customers);
        final String message = createMessage(elapsed);
        assertThat(message, (double) elapsed[1], is(lessThan(elapsed[0] * 1.2)));
    }

    // TODO: b/72877822 Better performance measurement
    @Ignore
    @Test
    public void performance_bulk() {
        final List<Customer> customers = generateCustomers(10000);
        final long[] elapsed = measureSeveralTimesEach(true, customers);
        final String message = createMessage(elapsed);
        assertThat(message, (double) elapsed[1], is(lessThan(elapsed[0] * 1.2)));
    }

    private static String createMessage(long[] elapsed) {
        return "Without multi-instance invalidation: " + elapsed[0] + " ms, "
                + "with multi-instance invalidation: " + elapsed[1] + " ms.";
    }

    private List<Customer> generateCustomers(int count) {
        final ArrayList<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final Customer customer = new Customer();
            int id = i + 1;
            customer.setId(id);
            customer.setName("Name" + id);
            customer.setLastName("LastName" + id);
            customers.add(customer);
        }
        return customers;
    }

    private long[] measureSeveralTimesEach(boolean bulk, List<Customer> customers) {
        final int n = 10;
        final long[] results1 = new long[n];
        final long[] results2 = new long[n];
        for (int i = 0; i < n; i++) {
            results2[i] = measure(true, bulk, customers);
            results1[i] = measure(false, bulk, customers);
        }

        // Median
        Arrays.sort(results1);
        Arrays.sort(results2);
        final long result1 = results1[results1.length / 2];
        final long result2 = results2[results2.length / 2];

        return new long[]{result1, result2};
    }


    @SuppressWarnings("deprecation")
    private long measure(boolean multiInstanceInvalidation, boolean bulk,
            List<Customer> customers) {
        final Context context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(SampleDatabaseService.DATABASE_NAME);
        final SampleDatabase db = openDatabase(multiInstanceInvalidation);
        final CustomerDao dao = db.getCustomerDao();
        final InvalidationTracker.Observer observer = new InvalidationTracker.Observer("Customer") {
            @Override
            public void onInvalidated(Set<String> tables) {
                // This observer is only for creating triggers.
            }
        };
        db.getInvalidationTracker().addObserver(observer);
        final long start = SystemClock.currentThreadTimeMillis();
        if (bulk) {
            db.beginTransaction();
        }
        try {
            for (int i = 0, size = customers.size(); i < size; i++) {
                dao.insert(customers.get(i));
            }
            if (bulk) {
                db.setTransactionSuccessful();
            }
        } finally {
            if (bulk) {
                db.endTransaction();
            }
        }
        long elapsed = SystemClock.currentThreadTimeMillis() - start;
        db.getInvalidationTracker().removeObserver(observer);
        return elapsed;
    }

    private SampleDatabase openDatabase(boolean multiInstanceInvalidation) {
        final Context context = ApplicationProvider.getApplicationContext();
        final RoomDatabase.Builder<SampleDatabase> builder = Room
                .databaseBuilder(context, SampleDatabase.class,
                        SampleDatabaseService.DATABASE_NAME);
        if (multiInstanceInvalidation) {
            builder.enableMultiInstanceInvalidation();
        }
        final SampleDatabase db = builder.build();
        mDatabases.add(db);
        return db;
    }

    private void bindTestService() throws TimeoutException {
        final Context context = ApplicationProvider.getApplicationContext();
        mService = ISampleDatabaseService.Stub.asInterface(
                serviceRule.bindService(new Intent(context, SampleDatabaseService.class)));
    }

    private CountDownLatch prepareTableObserver(SampleDatabase db) {
        final CountDownLatch invalidated = new CountDownLatch(1);
        db.getInvalidationTracker()
                .addObserver(new InvalidationTracker.Observer("Customer", "Product") {
                    @Override
                    public void onInvalidated(Set<String> tables) {
                        assertThat(tables, hasSize(1));
                        assertThat(tables, hasItem("Customer"));
                        invalidated.countDown();
                    }
                });
        return invalidated;
    }

    private Pair<CountDownLatch, CountDownLatch> prepareLiveDataObserver(SampleDatabase db)
            throws InterruptedException {
        final CountDownLatch initialized = new CountDownLatch(1);
        final CountDownLatch changedFirst = new CountDownLatch(1);
        final CountDownLatch changedSecond = new CountDownLatch(1);
        final Observer<List<Customer>> observer = customers -> {
            if (customers == null || customers.isEmpty()) {
                initialized.countDown();
            } else if (changedFirst.getCount() > 0) {
                assertThat(customers, hasSize(1));
                assertThat(customers, hasItem(CUSTOMER_1));
                changedFirst.countDown();
            } else {
                assertThat(customers, hasSize(2));
                assertThat(customers, hasItem(CUSTOMER_1));
                assertThat(customers, hasItem(CUSTOMER_2));
                changedSecond.countDown();
            }
        };
        final LiveData<List<Customer>> customers = db.getCustomerDao().all();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                customers.observeForever(observer));
        mObservers.put(customers, observer);
        // Make sure that this observer is ready before inserting an item in another instance.
        initialized.await(3, TimeUnit.SECONDS);
        return new Pair<>(changedFirst, changedSecond);
    }
}
