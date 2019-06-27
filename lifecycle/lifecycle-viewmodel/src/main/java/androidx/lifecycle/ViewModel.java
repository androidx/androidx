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

package androidx.lifecycle;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * ViewModel is a class that is responsible for preparing and managing the data for
 * an {@link android.app.Activity Activity} or a {@link androidx.fragment.app.Fragment Fragment}.
 * It also handles the communication of the Activity / Fragment with the rest of the application
 * (e.g. calling the business logic classes).
 * <p>
 * A ViewModel is always created in association with a scope (an fragment or an activity) and will
 * be retained as long as the scope is alive. E.g. if it is an Activity, until it is
 * finished.
 * <p>
 * In other words, this means that a ViewModel will not be destroyed if its owner is destroyed for a
 * configuration change (e.g. rotation). The new instance of the owner will just re-connected to the
 * existing ViewModel.
 * <p>
 * The purpose of the ViewModel is to acquire and keep the information that is necessary for an
 * Activity or a Fragment. The Activity or the Fragment should be able to observe changes in the
 * ViewModel. ViewModels usually expose this information via {@link LiveData} or Android Data
 * Binding. You can also use any observability construct from you favorite framework.
 * <p>
 * ViewModel's only responsibility is to manage the data for the UI. It <b>should never</b> access
 * your view hierarchy or hold a reference back to the Activity or the Fragment.
 * <p>
 * Typical usage from an Activity standpoint would be:
 * <pre>
 * public class UserActivity extends Activity {
 *
 *     {@literal @}Override
 *     protected void onCreate(Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.user_activity_layout);
 *         final UserModel viewModel = ViewModelProviders.of(this).get(UserModel.class);
 *         viewModel.userLiveData.observer(this, new Observer<User>() {
 *            {@literal @}Override
 *             public void onChanged(@Nullable User data) {
 *                 // update ui.
 *             }
 *         });
 *         findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
 *             {@literal @}Override
 *             public void onClick(View v) {
 *                  viewModel.doAction();
 *             }
 *         });
 *     }
 * }
 * </pre>
 *
 * ViewModel would be:
 * <pre>
 * public class UserModel extends ViewModel {
 *     private final MutableLiveData&lt;User&gt; userLiveData = new MutableLiveData&lt;&gt;();
 *
 *     public LiveData&lt;User&gt; getUser() {
 *         return userLiveData;
 *     }
 *
 *     public UserModel() {
 *         // trigger user load.
 *     }
 *
 *     void doAction() {
 *         // depending on the action, do necessary business logic calls and update the
 *         // userLiveData.
 *     }
 * }
 * </pre>
 *
 * <p>
 * ViewModels can also be used as a communication layer between different Fragments of an Activity.
 * Each Fragment can acquire the ViewModel using the same key via their Activity. This allows
 * communication between Fragments in a de-coupled fashion such that they never need to talk to
 * the other Fragment directly.
 * <pre>
 * public class MyFragment extends Fragment {
 *     public void onStart() {
 *         UserModel userModel = ViewModelProviders.of(getActivity()).get(UserModel.class);
 *     }
 * }
 * </pre>
 * </>
 */
public abstract class ViewModel {
    // Can't use ConcurrentHashMap, because it can lose values on old apis (see b/37042460)
    @Nullable
    private final Map<String, Object> mBagOfTags = new HashMap<>();
    private volatile boolean mCleared = false;

    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     * <p>
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    @SuppressWarnings("WeakerAccess")
    protected void onCleared() {
    }

    @MainThread
    final void clear() {
        mCleared = true;
        // Since clear() is final, this method is still called on mock objects
        // and in those cases, mBagOfTags is null. It'll always be empty though
        // because setTagIfAbsent and getTag are not final so we can skip
        // clearing it
        if (mBagOfTags != null) {
            synchronized (mBagOfTags) {
                for (Object value : mBagOfTags.values()) {
                    // see comment for the similar call in setTagIfAbsent
                    closeWithRuntimeException(value);
                }
            }
        }
        onCleared();
    }

    /**
     * Sets a tag associated with this viewmodel and a key.
     * If the given {@code newValue} is {@link Closeable},
     * it will be closed once {@link #clear()}.
     * <p>
     * If a value was already set for the given key, this calls do nothing and
     * returns currently associated value, the given {@code newValue} would be ignored
     * <p>
     * If the ViewModel was already cleared then close() would be called on the returned object if
     * it implements {@link Closeable}. The same object may receive multiple close calls, so method
     * should be idempotent.
     */
    @SuppressWarnings("unchecked")
    <T> T setTagIfAbsent(String key, T newValue) {
        T previous;
        synchronized (mBagOfTags) {
            previous = (T) mBagOfTags.get(key);
            if (previous == null) {
                mBagOfTags.put(key, newValue);
            }
        }
        T result = previous == null ? newValue : previous;
        if (mCleared) {
            // It is possible that we'll call close() multiple times on the same object, but
            // Closeable interface requires close method to be idempotent:
            // "if the stream is already closed then invoking this method has no effect." (c)
            closeWithRuntimeException(result);
        }
        return result;
    }

    /**
     * Returns the tag associated with this viewmodel and the specified key.
     */
    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    <T> T getTag(String key) {
        synchronized (mBagOfTags) {
            return (T) mBagOfTags.get(key);
        }
    }

    private static void closeWithRuntimeException(Object obj) {
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
