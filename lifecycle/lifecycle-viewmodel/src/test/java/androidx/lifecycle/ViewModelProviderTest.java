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

package androidx.lifecycle;

import static androidx.lifecycle.ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory;
import androidx.lifecycle.viewmodel.CreationExtras;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ViewModelProviderTest {

    private ViewModelProvider mViewModelProvider;

    @Before
    public void setup() {
        mViewModelProvider = new ViewModelProvider(new ViewModelStore(), new NewInstanceFactory());
    }

    @Test
    public void twoViewModelsWithSameKey() {
        String key = "the_key";
        ViewModel1 vm1 = mViewModelProvider.get(key, ViewModel1.class);
        assertThat(vm1.mCleared, is(false));
        ViewModel2 vw2 = mViewModelProvider.get(key, ViewModel2.class);
        assertThat(vw2, notNullValue());
        assertThat(vm1.mCleared, is(true));
    }


    @Test
    public void localViewModel() {
        class VM extends ViewModel1 {
        }
        try {
            mViewModelProvider.get(VM.class);
            Assert.fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void twoViewModels() {
        ViewModel1 model1 = mViewModelProvider.get(ViewModel1.class);
        ViewModel2 model2 = mViewModelProvider.get(ViewModel2.class);
        assertThat(mViewModelProvider.get(ViewModel1.class), is(model1));
        assertThat(mViewModelProvider.get(ViewModel2.class), is(model2));
    }

    @Test
    public void testOwnedBy() {
        final ViewModelStore store = new ViewModelStore();
        ViewModelStoreOwner owner = () -> store;
        ViewModelProvider provider = new ViewModelProvider(owner, new NewInstanceFactory());
        ViewModel1 viewModel = provider.get(ViewModel1.class);
        assertThat(viewModel, is(provider.get(ViewModel1.class)));
    }

    @Test
    public void testCustomDefaultFactory() {
        final ViewModelStore store = new ViewModelStore();
        final CountingFactory factory = new CountingFactory();
        ViewModelStoreOwnerWithFactory owner = new ViewModelStoreOwnerWithFactory(store, factory);
        ViewModelProvider provider = new ViewModelProvider(owner);
        ViewModel1 viewModel = provider.get(ViewModel1.class);
        assertThat(viewModel, is(provider.get(ViewModel1.class)));
        assertThat(factory.mCalled, is(1));
    }

    @Test
    public void testKeyedFactory() {
        final ViewModelStore store = new ViewModelStore();
        ViewModelStoreOwner owner = () -> store;
        ViewModelProvider.Factory explicitlyKeyed = new ViewModelProvider.Factory() {
            @SuppressWarnings("unchecked")
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                    @NonNull CreationExtras extras) {
                String key = extras.get(VIEW_MODEL_KEY);
                assertThat(key, is("customkey"));
                return (T) new ViewModel1();
            }
        };

        ViewModelProvider provider = new ViewModelProvider(owner, explicitlyKeyed);
        provider.get("customkey", ViewModel1.class);

        ViewModelProvider.Factory implicitlyKeyed = new ViewModelProvider.Factory() {
            @SuppressWarnings("unchecked")
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass,
                    @NonNull CreationExtras extras) {
                String key = extras.get(VIEW_MODEL_KEY);
                assertThat(key, is(notNullValue()));
                return (T) new ViewModel1();
            }
        };
        new ViewModelProvider(owner, implicitlyKeyed).get("customkey", ViewModel1.class);
    }

    public static class ViewModelStoreOwnerWithFactory implements
            ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
        private final ViewModelStore mStore;
        private final ViewModelProvider.Factory mFactory;

        ViewModelStoreOwnerWithFactory(@NonNull ViewModelStore store,
                @NonNull ViewModelProvider.Factory factory) {
            mStore = store;
            mFactory = factory;
        }

        @NonNull
        @Override
        public ViewModelStore getViewModelStore() {
            return mStore;
        }

        @NonNull
        @Override
        public ViewModelProvider.Factory getDefaultViewModelProviderFactory() {
            return mFactory;
        }
    }

    public static class ViewModel1 extends ViewModel {
        boolean mCleared;

        @Override
        protected void onCleared() {
            mCleared = true;
        }
    }

    public static class ViewModel2 extends ViewModel {
    }

    public static class CountingFactory extends NewInstanceFactory {
        int mCalled = 0;

        @Override
        @NonNull
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            mCalled++;
            return super.create(modelClass);
        }
    }
}
