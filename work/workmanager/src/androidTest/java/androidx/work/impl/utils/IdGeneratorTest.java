/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.work.impl.utils;

import static androidx.work.impl.utils.IdGenerator.INITIAL_ID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static java.lang.Integer.MAX_VALUE;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.WorkManagerImpl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = WorkManagerImpl.MIN_JOB_SCHEDULER_API_LEVEL)
public class IdGeneratorTest {
    private Integer mMockSharedPrefsNextId;
    private IdGenerator mIdGenerator;

    @Before
    public void setUp() {
        Context mMockContext = mock(Context.class);
        SharedPreferences.Editor mockEditor = createMockSharedPreferencesEditor();
        SharedPreferences mockSharedPrefs = createMockSharedPreferences(mockEditor);
        when(mMockContext.getSharedPreferences(
                eq(IdGenerator.PREFERENCE_FILE_KEY), anyInt()))
                .thenReturn(mockSharedPrefs);
        mIdGenerator = new IdGenerator(mMockContext);
    }

    @Test
    public void testNextId_returnsInitialIdWhenNoStoredNextId() {
        int nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(INITIAL_ID));
    }

    @Test
    public void testNextId_returnsStoredNextId() {
        int expectedId = 100;
        storeNextIdInSharedPrefs(expectedId);
        int nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(expectedId));
    }

    @Test
    public void testNextId_returnsInitialIdAfterReturningMaxInteger() {
        int expectedId = MAX_VALUE;
        storeNextIdInSharedPrefs(expectedId);
        int nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(MAX_VALUE));
        nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(INITIAL_ID));
    }

    @Test
    public void testNextId_belowMinRange() {
        storeNextIdInSharedPrefs(2);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(10));
    }

    @Test
    public void testNextId_aboveMaxRange() {
        storeNextIdInSharedPrefs(100);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(100));
    }

    @Test
    public void testNextId_aboveMaxRange2() {
        storeNextIdInSharedPrefs(110);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(10));
    }

    @Test
    public void testNextId_withinRange() {
        storeNextIdInSharedPrefs(20);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(20));
    }

    /**
     * Mocks setting a stored value in {@link SharedPreferences} for the next ID.
     *
     * @param nextId The next ID to store in {@link SharedPreferences}.
     */
    private void storeNextIdInSharedPrefs(int nextId) {
        mMockSharedPrefsNextId = nextId;
    }

    private SharedPreferences createMockSharedPreferences(SharedPreferences.Editor mockEditor) {
        final SharedPreferences mockSharedPreferences = mock(SharedPreferences.class);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockSharedPreferences.getInt(eq(IdGenerator.NEXT_JOB_SCHEDULER_ID_KEY), anyInt()))
                .thenAnswer(new Answer<Integer>() {
                    @Override
                    public Integer answer(InvocationOnMock invocation) throws Throwable {
                        int defValue = invocation.getArgument(1);
                        return (mMockSharedPrefsNextId == null) ? defValue : mMockSharedPrefsNextId;
                    }
                });
        return mockSharedPreferences;
    }

    private SharedPreferences.Editor createMockSharedPreferencesEditor() {
        final SharedPreferences.Editor mockEditor = mock(SharedPreferences.Editor.class);
        when(mockEditor.putInt(eq(IdGenerator.NEXT_JOB_SCHEDULER_ID_KEY), anyInt())).thenAnswer(
                new Answer<SharedPreferences.Editor>() {
                    @Override
                    public SharedPreferences.Editor answer(InvocationOnMock invocation)
                            throws Throwable {
                        mMockSharedPrefsNextId = invocation.getArgument(1);
                        return mockEditor;
                    }
                });
        return mockEditor;
    }
}
