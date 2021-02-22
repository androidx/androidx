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

package androidx.textclassifier;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.UserManager;

import androidx.core.app.RemoteActionCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.textclassifier.LegacyTextClassifier.MatchMakerImpl;
import androidx.textclassifier.LegacyTextClassifier.MatchMakerImpl.PermissionsChecker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Unit tests for {@link MatchMakerImpl}.
 */
@LargeTest
@RunWith(AndroidJUnit4.class)
public final class MatchMakerImplTest {

    private static final ResolveInfo RESOLVE_INFO = new ResolveInfo();
    static {
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = "test.action.package";
        activityInfo.name = "TestAction";
        activityInfo.applicationInfo = new ApplicationInfo();
        RESOLVE_INFO.activityInfo = activityInfo;
    }

    private Context mContext;
    private PackageManager mPackageManager;
    private Bundle mUserRestrictions;
    private PermissionsChecker mPermissionsChecker;
    private MatchMaker mMatchMaker;

    private String mBrowse;
    private String mEmail;
    private String mAdd;
    private String mDial;
    private String mSms;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mPackageManager = mock(PackageManager.class);
        when(mPackageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(RESOLVE_INFO);
        mUserRestrictions = new Bundle();
        mPermissionsChecker = new PermissionsChecker() {
            @Override
            public boolean hasPermission(ActivityInfo info) {
                return true;
            }
        };
        mMatchMaker = new MatchMakerImpl(
                mContext, mPackageManager, mUserRestrictions, mPermissionsChecker);

        mBrowse = mContext.getString(R.string.browse);
        mEmail = mContext.getString(R.string.email);
        mAdd = mContext.getString(R.string.add_contact);
        mDial = mContext.getString(R.string.dial);
        mSms = mContext.getString(R.string.sms);
    }

    @Test
    public void getUrlActions() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_URL, "www.android.com");

        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo(mBrowse);
    }

    @Test
    public void getEmailActions() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_EMAIL, "email@android.com");

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).getTitle()).isEqualTo(mEmail);
        assertThat(actions.get(1).getTitle()).isEqualTo(mAdd);
    }

    @Test
    public void getPhoneActions() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_PHONE, "(987) 654-3210");

        assertThat(actions).hasSize(3);
        assertThat(actions.get(0).getTitle()).isEqualTo(mDial);
        assertThat(actions.get(1).getTitle()).isEqualTo(mAdd);
        assertThat(actions.get(2).getTitle()).isEqualTo(mSms);
    }

    @Test
    public void unsupportedEntityType() throws Exception {
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_ADDRESS, "1 Android way");

        assertThat(actions).isEmpty();
    }

    @Test
    public void noMatchingApp() throws Exception {
        final PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.resolveActivity(any(Intent.class), anyInt())).thenReturn(null);

        mMatchMaker = new MatchMakerImpl(
                mContext, packageManager, mUserRestrictions, mPermissionsChecker);
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_URL, "www.android.com");

        assertThat(actions).isEmpty();
    }

    @Test
    public void noMatchingActivity() throws Exception {
        final PackageManager packageManager = mock(PackageManager.class);
        when(packageManager.resolveActivity(any(Intent.class), anyInt()))
                .thenReturn(new ResolveInfo());

        mMatchMaker = new MatchMakerImpl(
                mContext, packageManager, mUserRestrictions, mPermissionsChecker);
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_URL, "www.android.com");

        assertThat(actions).isEmpty();
    }

    @Test
    public void noPermsission() throws Exception {
        final PermissionsChecker permissionsChecker = new PermissionsChecker() {
            @Override
            public boolean hasPermission(ActivityInfo info) {
                return false;
            }
        };

        mMatchMaker = new MatchMakerImpl(
                mContext, mPackageManager, mUserRestrictions, permissionsChecker);
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_URL, "www.android.com");

        assertThat(actions).isEmpty();
    }

    @Test
    public void disallowCalls() throws Exception {
        final Bundle userRestrictions = new Bundle();
        userRestrictions.putBoolean(UserManager.DISALLOW_OUTGOING_CALLS, true);

        mMatchMaker = new MatchMakerImpl(
                mContext, mPackageManager, userRestrictions, mPermissionsChecker);
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_PHONE, "(987) 654-3210");

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).getTitle()).isEqualTo(mAdd);
        assertThat(actions.get(1).getTitle()).isEqualTo(mSms);
    }

    @Test
    public void disallowSms() throws Exception {
        final Bundle userRestrictions = new Bundle();
        userRestrictions.putBoolean(UserManager.DISALLOW_SMS, true);

        mMatchMaker = new MatchMakerImpl(
                mContext, mPackageManager, userRestrictions, mPermissionsChecker);
        final List<RemoteActionCompat> actions =
                mMatchMaker.getActions(TextClassifier.TYPE_PHONE, "(987) 654-3210");

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0).getTitle()).isEqualTo(mDial);
        assertThat(actions.get(1).getTitle()).isEqualTo(mAdd);
    }
}
