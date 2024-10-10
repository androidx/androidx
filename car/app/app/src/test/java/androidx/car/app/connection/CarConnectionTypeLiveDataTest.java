/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.connection;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.robolectric.Shadows.shadowOf;

import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Build;

import androidx.lifecycle.Observer;
import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.internal.DoNotInstrument;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

/** Tests for {@link CarConnectionTypeLiveData}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CarConnectionTypeLiveDataTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private Observer<Integer> mMockObserver;

    private final Application mApplication = ApplicationProvider.getApplicationContext();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private CarConnectionTypeLiveData mCarConnectionTypeLiveData;
    private TestContentProvider mContentProvider;

    /**
     * Filters receivers to only ones created by the androidx.car group, filtering out for
     * example the ProfileInstallReceiver which *may* be present, based on inclusion of
     * profileinstaller (for example, in CI when tested against TOT dependencies).
     * <p>
     * To force its presence for local testing, add the following to car:app:app's build.gradle:
     * <pre>implementation("androidx.profileinstaller:profileinstaller:1.2.1")</pre>
     */
    private ImmutableList<ShadowApplication.Wrapper> getFilteredRegisteredReceivers() {
        return ImmutableList.copyOf(
                Iterables.filter(shadowOf(mApplication).getRegisteredReceivers(),
                        wrapper -> wrapper.broadcastReceiver.getClass().getName().startsWith(
                                "androidx.car.app.")
                ));
    }

    @Before
    public void setUp() {
        ProviderInfo info = new ProviderInfo();
        info.authority = CarConnectionTypeLiveData.CAR_CONNECTION_AUTHORITY;
        mContentProvider =
                Robolectric.buildContentProvider(TestContentProvider.class).create(info).get();

        // Starts with 1 broadcast receiver (for CarPendingIntent)
        assertThat(getFilteredRegisteredReceivers()).hasSize(1);

        mCarConnectionTypeLiveData = new CarConnectionTypeLiveData(mContext);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.S_V2)
    public void observe_belowT_registersBroadcastReceiverWithNoFlags() {
        shadowOf(mApplication).clearRegisteredReceivers();

        mCarConnectionTypeLiveData.observeForever(mMockObserver);

        ShadowApplication.Wrapper registeredReceiver =
                getFilteredRegisteredReceivers().get(0);
        assertThat(registeredReceiver.broadcastReceiver).isNotNull();
        assertThat(registeredReceiver.flags).isEqualTo(0);
    }

    @Test
    @Config(sdk = Build.VERSION_CODES.TIRAMISU)
    @Ignore // TODO(b/240591692): Remove @Ignore once Robolectric supports SDK 33
    public void observe_tAndAbove_registersBroadcastReceiverWithFlags() {
        shadowOf(mApplication).clearRegisteredReceivers();

        mCarConnectionTypeLiveData.observeForever(mMockObserver);

        ShadowApplication.Wrapper registeredReceiver =
                getFilteredRegisteredReceivers().get(0);
        assertThat(registeredReceiver.broadcastReceiver).isNotNull();
        assertThat(registeredReceiver.flags).isEqualTo(Context.RECEIVER_EXPORTED);
    }

    @Test
    public void getInstance_queriesContentProvider() {
        mCarConnectionTypeLiveData.observeForever(mMockObserver);
        assertThat(mContentProvider.mDidQueryContentProvider).isTrue();
    }

    @Test
    public void contentProviderQuery_wasProjecting() {
        mContentProvider.mIsProjecting = true;
        mCarConnectionTypeLiveData = new CarConnectionTypeLiveData(mContext);

        mCarConnectionTypeLiveData.observeForever(mMockObserver);
        ShadowLooper.runUiThreadTasks();
        verify(mMockObserver).onChanged(CarConnection.CONNECTION_TYPE_PROJECTION);
    }

    @Test
    public void contentProviderQuery_nullReturn() {
        mContentProvider.mReturnNull = true;
        mCarConnectionTypeLiveData = new CarConnectionTypeLiveData(mContext);

        mCarConnectionTypeLiveData.observeForever(mMockObserver);
        ShadowLooper.runUiThreadTasks();
        verify(mMockObserver).onChanged(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
    }

    @Test
    public void contentProviderQuery_noColumn() {
        mContentProvider.mReturnNoColumn = true;
        mCarConnectionTypeLiveData = new CarConnectionTypeLiveData(mContext);

        mCarConnectionTypeLiveData.observeForever(mMockObserver);
        ShadowLooper.runUiThreadTasks();
        verify(mMockObserver).onChanged(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
    }

    @Test
    public void contentProviderQuery_noRow() {
        mContentProvider.mReturnNoRow = true;
        mCarConnectionTypeLiveData = new CarConnectionTypeLiveData(mContext);

        mCarConnectionTypeLiveData.observeForever(mMockObserver);
        ShadowLooper.runUiThreadTasks();
        verify(mMockObserver).onChanged(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
    }

    @Test
    public void broadcastReceived_queriesAndSetsValue() {
        InOrder mocks = inOrder(mMockObserver);

        mCarConnectionTypeLiveData.observeForever(mMockObserver);
        ShadowLooper.runUiThreadTasks();

        ShadowApplication.Wrapper receiverWrapper = getFilteredRegisteredReceivers().get(1);

        mContentProvider.mIsProjecting = true;
        receiverWrapper.broadcastReceiver.onReceive(mContext,
                new Intent(CarConnection.ACTION_CAR_CONNECTION_UPDATED));
        ShadowLooper.runUiThreadTasks();

        mocks.verify(mMockObserver).onChanged(CarConnection.CONNECTION_TYPE_NOT_CONNECTED);
        mocks.verify(mMockObserver).onChanged(CarConnection.CONNECTION_TYPE_PROJECTION);
        mocks.verifyNoMoreInteractions();
    }

    @Test
    public void stopObserving_removedBroadcastReceiver() {
        assertThat(getFilteredRegisteredReceivers()).hasSize(1);

        mCarConnectionTypeLiveData.observeForever(mMockObserver);

        assertThat(getFilteredRegisteredReceivers()).hasSize(2);

        mCarConnectionTypeLiveData.removeObserver(mMockObserver);

        assertThat(getFilteredRegisteredReceivers()).hasSize(1);
    }

    private static class TestContentProvider extends ContentProvider {
        boolean mDidQueryContentProvider;
        boolean mIsProjecting;
        boolean mReturnNull;
        boolean mReturnNoColumn;
        boolean mReturnNoRow;

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public @Nullable Cursor query(@NonNull Uri uri, String @Nullable [] projection,
                @Nullable String selection, String @Nullable [] selectionArgs,
                @Nullable String sortOrder) {
            mDidQueryContentProvider = true;
            assertThat(projection).asList().containsExactly(CarConnection.CAR_CONNECTION_STATE);

            if (mReturnNull) {
                return null;
            }

            MatrixCursor cursor = new MatrixCursor(projection);

            if (mReturnNoRow) {
                return cursor;
            }

            MatrixCursor.RowBuilder rowBuilder = cursor.newRow();

            if (mReturnNoColumn) {
                return cursor;
            }
            rowBuilder.add(CarConnection.CAR_CONNECTION_STATE,
                    mIsProjecting ? CarConnection.CONNECTION_TYPE_PROJECTION :
                            CarConnection.CONNECTION_TYPE_NOT_CONNECTED);

            return cursor;
        }

        @Override
        public @Nullable String getType(@NonNull Uri uri) {
            return null;
        }

        @Override
        public @Nullable Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
            return null;
        }

        @Override
        public int delete(@NonNull Uri uri, @Nullable String selection,
                String @Nullable [] selectionArgs) {
            return 0;
        }

        @Override
        public int update(@NonNull Uri uri, @Nullable ContentValues values,
                @Nullable String selection, String @Nullable [] selectionArgs) {
            return 0;
        }
    }
}
