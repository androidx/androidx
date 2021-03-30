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

package androidx.core.google.shortcuts;

import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_DESCRIPTION_KEY;
import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_LABEL_KEY;
import static androidx.core.google.shortcuts.ShortcutUtils.SHORTCUT_URL_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class ShortcutInfoChangeListenerImplTest {
    private static final String TEST_PACKAGE = "com.test.package";

    private FirebaseAppIndex mFirebaseAppIndex;
    private FirebaseUserActions mFirebaseUserActions;
    private Context mContext;
    private ShortcutInfoChangeListenerImpl mShortcutInfoChangeListener;

    @Before
    public void setUp() {
        mFirebaseAppIndex = mock(FirebaseAppIndex.class);
        mFirebaseUserActions = mock(FirebaseUserActions.class);
        mContext = mock(Context.class);
        mShortcutInfoChangeListener = new ShortcutInfoChangeListenerImpl(
                mContext, mFirebaseAppIndex, mFirebaseUserActions);

        when(mContext.getPackageName()).thenReturn(TEST_PACKAGE);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_publicIntent_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);
        when(mFirebaseAppIndex.update(any())).thenReturn(Tasks.forResult(null));

        Intent intent = Intent.parseUri("http://www.google.com", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .setIcon(IconCompat.createWithContentUri("content://abc"))
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new Indexable.Builder()
                .setName("short label")
                .setId("publicIntent")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .put(SHORTCUT_LABEL_KEY, "short label")
                .put(SHORTCUT_DESCRIPTION_KEY, "long label")
                .put(SHORTCUT_URL_KEY, ShortcutUtils.getIndexableShortcutUrl(mContext, intent))
                .setImage("content://abc")
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutAdded_updateSuccess_reportUsage() throws Exception {
        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);
        Task<Void> result = Tasks.forResult(null);
        when(mFirebaseAppIndex.update(any())).thenReturn(result);

        Intent intent = Intent.parseUri("http://www.google.com", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .setIcon(IconCompat.createWithContentUri("content://abc"))
                .build();

        mShortcutInfoChangeListener.onShortcutAdded(Collections.singletonList(shortcut));

        // Sleep to make sure the asynchronous call finishes. Since the method is mocked this
        // should be really quick.
        Thread.sleep(100);
        verify(mFirebaseUserActions).end(actionCaptor.capture());

        Action action = actionCaptor.getValue();
        Action expectedAction = new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject("", ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
        assertThat(action.toString()).isEqualTo(expectedAction.toString());
    }

    @Test
    @SmallTest
    public void onShortcutAdded_updateError_doNotReportUsage() throws Exception {
        when(mFirebaseAppIndex.update(any()))
                .thenReturn(Tasks.forException(new RuntimeException()));

        Intent intent = Intent.parseUri("http://www.google.com", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .setIcon(IconCompat.createWithContentUri("content://abc"))
                .build();

        mShortcutInfoChangeListener.onShortcutAdded(Collections.singletonList(shortcut));

        // Sleep to make sure the asynchronous call finishes. Since the method is mocked this
        // should be really quick.
        Thread.sleep(100);
        verify(mFirebaseUserActions, never()).end(any());
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_withCapabilityBinding_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);
        when(mFirebaseAppIndex.update(any())).thenReturn(Tasks.forResult(null));

        Intent intent = Intent.parseUri("http://www.google.com", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .addCapabilityBinding(
                        "actions.intent.START_EXERCISE",
                        ImmutableMap.of("exercise.name", ImmutableList.of("start running",
                                "start jogging")))
                .addCapabilityBinding(
                        "actions.intent.STOP_EXERCISE",
                        ImmutableMap.of("exercise.name", ImmutableList.of("stop running",
                                "stop jogging")))
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable.Builder expectedBuilder = new Indexable.Builder()
                .setName("short label")
                .setId("publicIntent")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .put(SHORTCUT_LABEL_KEY, "short label")
                .put(SHORTCUT_DESCRIPTION_KEY, "long label")
                .put(SHORTCUT_URL_KEY, ShortcutUtils.getIndexableShortcutUrl(mContext, intent));
        // The order of isPartOf field matters during comparison. However since the order is not
        // deterministic because the data is stored in maps and sets in ShortcutInfoCompat, we
        // check for all possible orderings to make the test more reliable.
        Indexable expected1 = expectedBuilder
                .setIsPartOf(
                        new Indexable.Builder()
                                .setId("actions.intent.STOP_EXERCISE/exercise.name")
                                .setName("stop running")
                                .setAlternateName("stop jogging"),
                        new Indexable.Builder()
                                .setId("actions.intent.START_EXERCISE/exercise.name")
                                .setName("start running")
                                .setAlternateName("start jogging"))
                .build();
        Indexable expected2 = expectedBuilder
                .setIsPartOf(
                        new Indexable.Builder()
                                .setId("actions.intent.START_EXERCISE/exercise.name")
                                .setName("start running")
                                .setAlternateName("start jogging"),
                        new Indexable.Builder()
                                .setId("actions.intent.STOP_EXERCISE/exercise.name")
                                .setName("stop running")
                                .setAlternateName("stop jogging"))
                .build();
        assertThat(allValues).hasSize(1);
        assertThat(allValues).containsAnyOf(expected1, expected2);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_withCapabilityBindingNoParams_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);
        when(mFirebaseAppIndex.update(any())).thenReturn(Tasks.forResult(null));

        Intent intent = Intent.parseUri("http://www.google.com", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .addCapabilityBinding(
                        "actions.intent.TWEET", null)
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new Indexable.Builder()
                .setName("short label")
                .setId("publicIntent")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .put(SHORTCUT_LABEL_KEY, "short label")
                .put(SHORTCUT_DESCRIPTION_KEY, "long label")
                .put(SHORTCUT_URL_KEY, ShortcutUtils.getIndexableShortcutUrl(mContext, intent))
                .setIsPartOf(new Indexable.Builder().setId("actions.intent.TWEET"))
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_privateIntent_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);
        when(mFirebaseAppIndex.update(any())).thenReturn(Tasks.forResult(null));

        String privateIntentUri = "#Intent;component=androidx.core.google.shortcuts.test/androidx"
                + ".core.google.shortcuts.TrampolineActivity;end";
        Intent intent = Intent.parseUri(privateIntentUri, 0);

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "privateIntent")
                .setShortLabel("short label")
                .setIntent(intent)
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new Indexable.Builder()
                .setName("short label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "privateIntent"))
                .setId("privateIntent")
                .put("shortcutLabel", "short label")
                .put("shortcutUrl", ShortcutUtils.getIndexableShortcutUrl(mContext, intent))
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutAdded_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);
        when(mFirebaseAppIndex.update(any())).thenReturn(Tasks.forResult(null));

        Intent intent = Intent.parseUri("http://www.google.com", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "intent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .setIcon(IconCompat.createWithContentUri("content://abc"))
                .build();

        mShortcutInfoChangeListener.onShortcutAdded(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new Indexable.Builder()
                .setName("short label")
                .setId("intent")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "intent"))
                .put(SHORTCUT_LABEL_KEY, "short label")
                .put(SHORTCUT_DESCRIPTION_KEY, "long label")
                .put(SHORTCUT_URL_KEY, ShortcutUtils.getIndexableShortcutUrl(mContext, intent))
                .setImage("content://abc")
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutRemoved_removeFromAppIndex() {
        ArgumentCaptor<String> urlsCaptor = ArgumentCaptor.forClass(String.class);

        mShortcutInfoChangeListener.onShortcutRemoved(Arrays.asList("id1", "id2"));

        verify(mFirebaseAppIndex, only()).remove(urlsCaptor.capture());
        List<String> urls = urlsCaptor.getAllValues();
        assertThat(urls).containsExactly(
                ShortcutUtils.getIndexableUrl(mContext, "id1"),
                ShortcutUtils.getIndexableUrl(mContext, "id2"));
    }

    @Test
    @SmallTest
    public void onAllShortcutRemoved_removeFromAppIndex() {
        mShortcutInfoChangeListener.onAllShortcutsRemoved();
        verify(mFirebaseAppIndex, only()).removeAll();
    }

    @Test
    @SmallTest
    public void onShortcutUsageReported_savesToUserActions() {
        ArgumentCaptor<Action> actionCaptor = ArgumentCaptor.forClass(Action.class);

        mShortcutInfoChangeListener.onShortcutUsageReported(Arrays.asList("id1", "id2"));

        verify(mFirebaseUserActions, times(2)).end(actionCaptor.capture());
        List<Action> actions = actionCaptor.getAllValues();
        List<String> actionsString =
                actions.stream().map(Object::toString).collect(Collectors.toList());
        Action expectedAction1 = new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject("", ShortcutUtils.getIndexableUrl(mContext, "id1"))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
        Action expectedAction2 = new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject("", ShortcutUtils.getIndexableUrl(mContext, "id2"))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
        // Action has no equals comparator, so instead we compare their string forms.
        assertThat(actionsString).containsExactly(expectedAction1.toString(),
                expectedAction2.toString());
    }
}
