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

package androidx.core.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class JobIntentServiceTest {
    static final String TAG = "JobIntentServiceTest";

    static final int JOB_ID = 0x1000;

    static final Object sLock = new Object();
    static CountDownLatch sReadyToRunLatch;
    static CountDownLatch sServiceWaitingLatch;
    static CountDownLatch sServiceStoppedLatch;
    static CountDownLatch sWaitCompleteLatch;
    static CountDownLatch sServiceFinishedLatch;

    static boolean sFinished;
    static ArrayList<Intent> sFinishedWork;
    static String sFinishedErrorMsg;
    static String sLastServiceState;

    Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getContext();
    }

    public static final class TestIntentItem implements Parcelable {
        public static final int FLAG_WAIT = 1 << 0;
        public static final int FLAG_STOPPED_AFTER_WAIT = 1 << 1;

        public final Intent intent;
        public final TestIntentItem[] subitems;
        public final int flags;
        public final Uri[] requireUrisGranted;
        public final Uri[] requireUrisNotGranted;

        public TestIntentItem(Intent intent) {
            this.intent = intent;
            subitems = null;
            flags = 0;
            requireUrisGranted = null;
            requireUrisNotGranted = null;
        }

        public TestIntentItem(Intent intent, int flags) {
            this.intent = intent;
            subitems = null;
            this.flags = flags;
            intent.putExtra("flags", flags);
            requireUrisGranted = null;
            requireUrisNotGranted = null;
        }

        public TestIntentItem(Intent intent, TestIntentItem[] subitems) {
            this.intent = intent;
            this.subitems = subitems;
            intent.putExtra("subitems", subitems);
            flags = 0;
            requireUrisGranted = null;
            requireUrisNotGranted = null;
        }

        public TestIntentItem(Intent intent, Uri[] requireUrisGranted,
                Uri[] requireUrisNotGranted) {
            this.intent = intent;
            subitems = null;
            flags = 0;
            this.requireUrisGranted = requireUrisGranted;
            this.requireUrisNotGranted = requireUrisNotGranted;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("TestIntentItem { ");
            sb.append(intent);
            sb.append(" }");
            return sb.toString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            intent.writeToParcel(parcel, flags);
            parcel.writeTypedArray(subitems, flags);
            parcel.writeInt(flags);
        }

        TestIntentItem(Parcel parcel) {
            intent = Intent.CREATOR.createFromParcel(parcel);
            subitems = parcel.createTypedArray(CREATOR);
            flags = parcel.readInt();
            requireUrisGranted = null;
            requireUrisNotGranted = null;
        }

        public static final Parcelable.Creator<TestIntentItem> CREATOR =
                new Parcelable.Creator<TestIntentItem>() {

                    public TestIntentItem createFromParcel(Parcel source) {
                        return new TestIntentItem(source);
                    }

                    public TestIntentItem[] newArray(int size) {
                        return new TestIntentItem[size];
                    }
                };
    }

    static void initStatics() {
        synchronized (sLock) {
            sReadyToRunLatch = new CountDownLatch(1);
            sServiceWaitingLatch = new CountDownLatch(1);
            sServiceStoppedLatch = new CountDownLatch(1);
            sWaitCompleteLatch = new CountDownLatch(1);
            sServiceFinishedLatch = new CountDownLatch(1);
            sFinished = false;
            sFinishedWork = null;
            sFinishedErrorMsg = null;
        }
    }

    static void allowServiceToRun() {
        sReadyToRunLatch.countDown();
    }

    static void serviceReportWaiting() {
        sServiceWaitingLatch.countDown();
    }

    static void ensureServiceWaiting() {
        try {
            if (!sServiceWaitingLatch.await(10, TimeUnit.SECONDS)) {
                fail("Timed out waiting for wait, service state " + sLastServiceState);
            }
        } catch (InterruptedException e) {
            fail("Interrupted waiting for service to wait: " + e);
        }
    }

    static void serviceReportStopped() {
        sServiceStoppedLatch.countDown();
    }

    static void ensureServiceStopped() {
        try {
            if (!sServiceStoppedLatch.await(10, TimeUnit.SECONDS)) {
                fail("Timed out waiting for stop, service state " + sLastServiceState);
            }
        } catch (InterruptedException e) {
            fail("Interrupted waiting for service to stop: " + e);
        }
    }

    static void allowServiceToResumeFromWait() {
        sWaitCompleteLatch.countDown();
    }

    static void finishServiceExecution(ArrayList<Intent> work, String errorMsg) {
        synchronized (sLock) {
            if (!sFinished) {
                sFinishedWork = work;
                sFinishedErrorMsg = errorMsg;
                sServiceFinishedLatch.countDown();
            }
        }
    }

    static void updateServiceState(String msg) {
        synchronized (sLock) {
            sLastServiceState = msg;
        }
    }

    void waitServiceFinish() {
        try {
            if (!sServiceFinishedLatch.await(10, TimeUnit.SECONDS)) {
                synchronized (sLock) {
                    if (sFinishedErrorMsg != null) {
                        fail("Timed out waiting for finish, service state " + sLastServiceState
                                + ", had error: " + sFinishedErrorMsg);
                    }
                    fail("Timed out waiting for finish, service state " + sLastServiceState);
                }
            }
        } catch (InterruptedException e) {
            fail("Interrupted waiting for service to finish: " + e);
        }
        synchronized (sLock) {
            if (sFinishedErrorMsg != null) {
                fail(sFinishedErrorMsg);
            }
        }
    }

    public static class TargetService extends JobIntentService {
        final ArrayList<Intent> mReceivedWork = new ArrayList<>();

        @Override
        public void onCreate() {
            super.onCreate();
            updateServiceState("Creating: " + this);
            Log.i(TAG, "Creating: " + this);
            Log.i(TAG, "Waiting for ready to run...");
            try {
                if (!sReadyToRunLatch.await(10, TimeUnit.SECONDS)) {
                    finishServiceExecution(null, "Timeout waiting for ready");
                }
            } catch (InterruptedException e) {
                finishServiceExecution(null, "Interrupted waiting for ready: " + e);
            }
            updateServiceState("Past ready to run");
            Log.i(TAG, "Running!");
        }

        @Override
        protected void onHandleWork(@Nullable Intent intent) {
            Log.i(TAG, "Handling work: " + intent);
            updateServiceState("Handling work: " + intent);
            mReceivedWork.add(intent);
            intent.setExtrasClassLoader(TestIntentItem.class.getClassLoader());
            int flags = intent.getIntExtra("flags", 0);
            if ((flags & TestIntentItem.FLAG_WAIT) != 0) {
                serviceReportWaiting();
                try {
                    if (!sWaitCompleteLatch.await(10, TimeUnit.SECONDS)) {
                        finishServiceExecution(null, "Timeout waiting for wait complete");
                    }
                } catch (InterruptedException e) {
                    finishServiceExecution(null, "Interrupted waiting for wait complete: " + e);
                }
                if ((flags & TestIntentItem.FLAG_STOPPED_AFTER_WAIT) != 0) {
                    if (!isStopped()) {
                        finishServiceExecution(null, "Service not stopped after waiting");
                    }
                }
            }
            Parcelable[] subitems = intent.getParcelableArrayExtra("subitems");
            if (subitems != null) {
                for (Parcelable pitem : subitems) {
                    JobIntentService.enqueueWork(this, TargetService.class,
                            JOB_ID, ((TestIntentItem) pitem).intent);
                }
            }
        }

        @Override
        public boolean onStopCurrentWork() {
            serviceReportStopped();
            return super.onStopCurrentWork();
        }

        @Override
        public void onDestroy() {
            Log.i(TAG, "Destroying: " + this);
            updateServiceState("Destroying: " + this);
            finishServiceExecution(mReceivedWork, null);
            super.onDestroy();
        }
    }

    private boolean intentEquals(Intent i1, Intent i2) {
        if (i1 == i2) {
            return true;
        }
        if (i1 == null || i2 == null) {
            return false;
        }
        return i1.filterEquals(i2);
    }

    private void compareIntents(TestIntentItem[] expected, ArrayList<Intent> received) {
        if (received == null) {
            fail("Didn't receive any expected work.");
        }
        ArrayList<TestIntentItem> expectedArray = new ArrayList<>();
        for (int i = 0; i < expected.length; i++) {
            expectedArray.add(expected[i]);
        }

        ComponentName serviceComp = new ComponentName(mContext, TargetService.class.getName());

        for (int i = 0; i < received.size(); i++) {
            Intent r = received.get(i);
            if (i < expected.length && expected[i].subitems != null) {
                TestIntentItem[] sub = expected[i].subitems;
                for (int j = 0; j < sub.length; j++) {
                    expectedArray.add(sub[j]);
                }
            }
            if (i >= expectedArray.size()) {
                fail("Received more than " + expected.length + " work items, first extra is "
                        + r);
            }
            if (r.getComponent() != null) {
                // Intents we get back from the compat service will have a component... make
                // sure that is correct, and then erase it so the intentEquals() will pass.
                assertEquals(serviceComp, r.getComponent());
                r.setComponent(null);
            }
            if (!intentEquals(r, expectedArray.get(i).intent)) {
                fail("Received intent #" + i + " " + r + " but expected " + expected[i]);
            }
        }
        if (received.size() < expected.length) {
            fail("Received only " + received.size() + " work items, but expected "
                    + expected.length);
        }
    }

    /**
     * Test simple case of enqueueing one piece of work.
     */
    @MediumTest
    @Test
    public void testEnqueueOne() throws Throwable {
        initStatics();

        TestIntentItem[] items = new TestIntentItem[] {
                new TestIntentItem(new Intent("FIRST")),
        };

        for (TestIntentItem item : items) {
            JobIntentService.enqueueWork(mContext, TargetService.class, JOB_ID, item.intent);
        }
        allowServiceToRun();

        waitServiceFinish();
        compareIntents(items, sFinishedWork);
    }

    /**
     * Test case of enqueueing multiple pieces of work.
     */
    @MediumTest
    @Test
    public void testEnqueueMultiple() throws Throwable {
        initStatics();

        TestIntentItem[] items = new TestIntentItem[] {
                new TestIntentItem(new Intent("FIRST")),
                new TestIntentItem(new Intent("SECOND")),
                new TestIntentItem(new Intent("THIRD")),
                new TestIntentItem(new Intent("FOURTH")),
        };

        for (TestIntentItem item : items) {
            JobIntentService.enqueueWork(mContext, TargetService.class, JOB_ID, item.intent);
        }
        allowServiceToRun();

        waitServiceFinish();
        compareIntents(items, sFinishedWork);
    }

    /**
     * Test case of enqueueing multiple pieces of work.
     */
    @MediumTest
    @Test
    public void testEnqueueSubWork() throws Throwable {
        initStatics();

        TestIntentItem[] items = new TestIntentItem[] {
                new TestIntentItem(new Intent("FIRST")),
                new TestIntentItem(new Intent("SECOND")),
                new TestIntentItem(new Intent("THIRD"), new TestIntentItem[] {
                        new TestIntentItem(new Intent("FIFTH")),
                        new TestIntentItem(new Intent("SIXTH")),
                        new TestIntentItem(new Intent("SEVENTH")),
                        new TestIntentItem(new Intent("EIGTH")),
                }),
                new TestIntentItem(new Intent("FOURTH")),
        };

        for (TestIntentItem item : items) {
            JobIntentService.enqueueWork(mContext, TargetService.class, JOB_ID, item.intent);
        }
        allowServiceToRun();

        waitServiceFinish();
        compareIntents(items, sFinishedWork);
    }

    /**
     * Test case of job stopping while it is doing work.
     */
    @MediumTest
    @Test
    @RequiresApi(26)
    public void testStopWhileWorking() throws Throwable {
        if (Build.VERSION.SDK_INT < 26) {
            // This test only makes sense when running on top of JobScheduler.
            return;
        }

        initStatics();

        TestIntentItem[] items = new TestIntentItem[] {
                new TestIntentItem(new Intent("FIRST"),
                        TestIntentItem.FLAG_WAIT | TestIntentItem.FLAG_STOPPED_AFTER_WAIT),
        };

        for (TestIntentItem item : items) {
            JobIntentService.enqueueWork(mContext, TargetService.class, JOB_ID, item.intent);
        }
        allowServiceToRun();
        ensureServiceWaiting();

        // At this point we will make the job stop...  this isn't normally how this would
        // happen with an IntentJobService, and doing it this way breaks re-delivery of
        // work, but we have CTS tests for the underlying redlivery mechanism.
        ((JobScheduler) mContext.getApplicationContext().getSystemService(
                Context.JOB_SCHEDULER_SERVICE)).cancel(JOB_ID);
        ensureServiceStopped();

        allowServiceToResumeFromWait();

        waitServiceFinish();
        compareIntents(items, sFinishedWork);
    }
}
