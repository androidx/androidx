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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.google.shortcuts.builders.CapabilityBuilder;
import androidx.core.google.shortcuts.builders.ParameterBuilder;
import androidx.core.google.shortcuts.builders.ShortcutBuilder;
import androidx.core.google.shortcuts.utils.ShortcutUtils;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.google.android.gms.appindex.Action;
import com.google.android.gms.appindex.AppIndex;
import com.google.android.gms.appindex.Indexable;
import com.google.android.gms.appindex.UserActions;
import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ShortcutInfoChangeListenerImplTest {
    private AppIndex mFirebaseAppIndex;
    private UserActions mFirebaseUserActions;
    private Context mContext;
    private ShortcutInfoChangeListenerImpl mShortcutInfoChangeListener;

    @Before
    public void setUp() {
        mFirebaseAppIndex = mock(AppIndex.class);
        mFirebaseUserActions = mock(UserActions.class);
        mContext = ApplicationProvider.getApplicationContext();
        mShortcutInfoChangeListener = new ShortcutInfoChangeListenerImpl(
                mContext, mFirebaseAppIndex, mFirebaseUserActions, null);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_publicIntent_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);

        Intent intent = Intent.parseUri("app://shortcut", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new ShortcutBuilder()
                .setId("publicIntent")
                .setShortcutLabel("short label")
                .setShortcutDescription("long label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .setShortcutUrl(ShortcutUtils.getIndexableShortcutUrl(mContext, intent, null))
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_withCapabilityBinding_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);

        Intent intent = Intent.parseUri("app://shortcut", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .addCapabilityBinding("actions.intent.START_EXERCISE", "exercise.name",
                        ImmutableList.of("start running", "start jogging"))
                .addCapabilityBinding("actions.intent.STOP_EXERCISE", "exercise.name",
                        ImmutableList.of("stop running", "stop jogging"))
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        ShortcutBuilder expectedBuilder = new ShortcutBuilder()
                .setId("publicIntent")
                .setShortcutLabel("short label")
                .setShortcutDescription("long label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .setShortcutUrl(ShortcutUtils.getIndexableShortcutUrl(mContext, intent, null));
        // The order of capability field matters during comparison. However since the order is not
        // deterministic because the data is stored in maps and sets in ShortcutInfoCompat, we
        // check for all possible orderings to make the test more reliable.
        Indexable expected1 = expectedBuilder
                .setCapability(
                        new CapabilityBuilder()
                                .setName("actions.intent.STOP_EXERCISE")
                                .setParameter(new ParameterBuilder()
                                        .setName("exercise.name")
                                        .setValue("stop running", "stop jogging")),
                        new CapabilityBuilder()
                                .setName("actions.intent.START_EXERCISE")
                                .setParameter(new ParameterBuilder()
                                        .setName("exercise.name")
                                        .setValue("start running", "start jogging")))
                .build();
        Indexable expected2 = expectedBuilder
                .setCapability(
                        new CapabilityBuilder()
                                .setName("actions.intent.START_EXERCISE")
                                .setParameter(new ParameterBuilder()
                                        .setName("exercise.name")
                                        .setValue("start running", "start jogging")),
                        new CapabilityBuilder()
                                .setName("actions.intent.STOP_EXERCISE")
                                .setParameter(new ParameterBuilder()
                                        .setName("exercise.name")
                                        .setValue("stop running", "stop jogging")))
                .build();
        assertThat(allValues).hasSize(1);
        assertThat(allValues).containsAnyOf(expected1, expected2);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_withCapabilityBindingNoParams_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);

        Intent intent = Intent.parseUri("app://shortcut", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .addCapabilityBinding("actions.intent.TWEET")
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new ShortcutBuilder()
                .setId("publicIntent")
                .setShortcutLabel("short label")
                .setShortcutDescription("long label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .setShortcutUrl(ShortcutUtils.getIndexableShortcutUrl(mContext, intent, null))
                .setCapability(new CapabilityBuilder().setName("actions.intent.TWEET"))
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_privateIntent_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);

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
        Indexable expected = new ShortcutBuilder()
                .setName("short label")
                .setId("privateIntent")
                .setShortcutLabel("short label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "privateIntent"))
                .setShortcutUrl(ShortcutUtils.getIndexableShortcutUrl(mContext, intent, null))
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutAdded_savesToAppIndex() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);

        Intent intent = Intent.parseUri("app://shortcut", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "intent")
                .setShortLabel("short label")
                .setLongLabel("long label")
                .setIntent(intent)
                .build();

        mShortcutInfoChangeListener.onShortcutAdded(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new ShortcutBuilder()
                .setName("short label")
                .setId("intent")
                .setShortcutLabel("short label")
                .setDescription("long label")
                .setShortcutDescription("long label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "intent"))
                .setShortcutUrl(ShortcutUtils.getIndexableShortcutUrl(mContext, intent, null))
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
        Action expectedAction1 = new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject("", ShortcutUtils.getIndexableUrl(mContext, "id1"))
                .build();
        Action expectedAction2 = new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject("", ShortcutUtils.getIndexableUrl(mContext, "id2"))
                .build();
        // Action has no equals comparator, so instead we compare their string forms.
        assertThat(convertActionsToString(actionCaptor.getAllValues())).containsExactly(
                expectedAction1.toString(),
                expectedAction2.toString());
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_withUriIcon_savesToAppIndexWithUri() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);

        Intent intent = Intent.parseUri("app://shortcut", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setIntent(intent)
                .setIcon(IconCompat.createWithContentUri(
                        "file:///data/user/0/com.example.myapp/files/ic_myicon.jpg"))
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new ShortcutBuilder()
                .setId("publicIntent")
                .setShortcutLabel("short label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .setShortcutUrl(ShortcutUtils.getIndexableShortcutUrl(mContext, intent, null))
                .setImage("file:///data/user/0/com.example.myapp/files/ic_myicon.jpg")
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    @Test
    @SmallTest
    public void onShortcutUpdated_withAdaptiveUriIcon_savesToAppIndexWithUri() throws Exception {
        ArgumentCaptor<Indexable> indexableCaptor = ArgumentCaptor.forClass(Indexable.class);

        Intent intent = Intent.parseUri("app://shortcut", 0);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(mContext, "publicIntent")
                .setShortLabel("short label")
                .setIntent(intent)
                .setIcon(IconCompat.createWithAdaptiveBitmapContentUri(
                        "file:///data/user/0/com.example.myapp/files/ic_myicon.jpg"))
                .build();

        mShortcutInfoChangeListener.onShortcutUpdated(Collections.singletonList(shortcut));

        verify(mFirebaseAppIndex, only()).update(indexableCaptor.capture());
        List<Indexable> allValues = indexableCaptor.getAllValues();
        Indexable expected = new ShortcutBuilder()
                .setId("publicIntent")
                .setShortcutLabel("short label")
                .setUrl(ShortcutUtils.getIndexableUrl(mContext, "publicIntent"))
                .setShortcutUrl(ShortcutUtils.getIndexableShortcutUrl(mContext, intent, null))
                .setImage("file:///data/user/0/com.example.myapp/files/ic_myicon.jpg")
                .build();
        assertThat(allValues).containsExactly(expected);
    }

    private List<String> convertActionsToString(List<Action> actions) {
        List<String> actionStrings = new ArrayList<>();
        for (Action action : actions) {
            actionStrings.add(action.toString());
        }
        return actionStrings;
    }
}
