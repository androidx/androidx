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

package android.arch.background.workmanager.systemjob;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class SystemJobIdGeneratorTest {
    private Integer mMockSharedPrefsNextId;
    private SystemJobIdGenerator mIdGenerator;

    @Before
    public void setUp() {
        Context mMockContext = mock(Context.class);
        SharedPreferences.Editor mockEditor = createMockSharedPreferencesEditor();
        SharedPreferences mockSharedPrefs = createMockSharedPreferences(mockEditor);
        when(mMockContext.getSharedPreferences(
                eq(SystemJobIdGenerator.PREFERENCE_FILE_KEY), anyInt()))
                .thenReturn(mockSharedPrefs);
        mIdGenerator = new SystemJobIdGenerator(mMockContext);
    }

    @Test
    public void testNextId_returnsInitialIdWhenNoStoredNextId() {
        assertEquals(SystemJobIdGenerator.INITIAL_ID, mIdGenerator.nextId());
    }

    @Test
    public void testNextId_returnsStoredNextId() {
        int expectedId = 100;
        storeNextIdInSharedPrefs(expectedId);
        assertEquals(expectedId, mIdGenerator.nextId());
    }

    @Test
    public void testNextId_returnsInitialIdAfterReturningMaxInteger() {
        int expectedId = Integer.MAX_VALUE;
        storeNextIdInSharedPrefs(expectedId);
        assertEquals(expectedId, mIdGenerator.nextId());
        assertEquals(SystemJobIdGenerator.INITIAL_ID, mIdGenerator.nextId());
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
        when(mockSharedPreferences.getInt(eq(SystemJobIdGenerator.NEXT_ID_KEY), anyInt()))
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
        when(mockEditor.putInt(eq(SystemJobIdGenerator.NEXT_ID_KEY), anyInt())).thenAnswer(
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
