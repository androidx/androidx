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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * ViewModel is a class that is responsible for preparing and managing the data for
 * an {@link android.app.Activity Activity} or a {@link androidx.fragment.app.Fragment Fragment}.
 * It also handles the communication of the Activity / Fragment with the rest of the application
 * (e.g. calling the business logic classes).
 * <p>
 * A ViewModel is always created in association with a scope (a fragment or an activity) and will
 * be retained as long as the scope is alive. E.g. if it is an Activity, until it is
 * finished.
 * <p>
 * In other words, this means that a ViewModel will not be destroyed if its owner is destroyed for a
 * configuration change (e.g. rotation). The new owner instance just re-connects to the existing model.
 * <p>
 * The purpose of the ViewModel is to acquire and keep the information that is necessary for an
 * Activity or a Fragment. The Activity or the Fragment should be able to observe changes in the
 * ViewModel. ViewModels usually expose this information via {@link LiveData} or Android Data
 * Binding. You can also use any observability construct from your favorite framework.
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
 *         final UserModel viewModel = new ViewModelProvider(this).get(UserModel.class);
 *         viewModel.getUser().observe(this, new Observer&lt;User&gt;() {
 *             {@literal @}Override
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
 *         UserModel userModel = new ViewModelProvider(requireActivity()).get(UserModel.class);
 *     }
 * }
 * </pre>
 * </>
 */
public abstract class ViewModel {
    // Can't use ConcurrentHashMap, because it can lose values on old apis (see b/37042460)
    @Nullable
    private final Map<String, Object> mBagOfTags = new HashMap<>();
    @Nullable
    private final Set<Closeable> mCloseables = new LinkedHashSet<>();
    private volatile boolean mCleared = false;

    /**
     * Construct a new ViewModel instance.
     * <p>
     * You should <strong>never</strong> manually construct a ViewModel outside of a
     * {@link ViewModelProvider.Factory}.
     */
    public ViewModel() {
    }

    /**
     * Construct a new ViewModel instance. Any {@link Closeable} objects provided here
     * will be closed directly before {@link #onCleared()} is called.
     * <p>
     * You should <strong>never</strong> manually construct a ViewModel outside of a
     * {@link ViewModelProvider.Factory}.
     */
    public ViewModel(@NonNull Closeable... closeables) {
        mCloseables.addAll(Arrays.asList(closeables));
    }

    /**
     * Add a new {@link Closeable} object that will be closed directly before
     * {@link #onCleared()} is called.
     * <p>
     * If onCleared() has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     * <p>
     * @param closeable The object that should be {@link Closeable#close() closed} directly before
     *                  {@link #onCleared()} is called.
     */
    public void addCloseable(@NonNull Closeable closeable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (mCleared) {
            closeWithRuntimeException(closeable);
            return;
        }

        // As this method is final, it will still be called on mock objects even
        // though mCloseables won't actually be created...we'll just not do anything
        // in that case.
        if (mCloseables != null) {
            synchronized (mCloseables) {
                mCloseables.add(closeable);
            }
        }
    }

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
        // We need the same null check here
        if (mCloseables != null) {
            synchronized (mCloseables) {
                for (Closeable closeable : mCloseables) {
                    closeWithRuntimeException(closeable);
                }
            }
            mCloseables.clear();
        }
        onCleared();
    }

    /**
     * Add a new {@link Closeable} object that will be closed directly before
     * {@link #onCleared()} is called.
     * <p>
     * If onCleared() has already been called, the closeable will not be added,
     * and will instead be closed immediately.
     * <p>
     * @param key A key that allows you to retrieve the closeable passed in by using the same
     *            key with {@link #getCloseable(String)}
     * @param closeable The object that should be {@link Closeable#close() closed} directly before
     *                  {@link #onCleared()} is called.
     */
    public final void addCloseable(@NonNull String key, @NonNull Closeable closeable) {
        // Although no logic should be done after user calls onCleared(), we will
        // ensure that if it has already been called, the closeable attempting to
        // be added will be closed immediately to ensure there will be no leaks.
        if (mCleared) {
            closeWithRuntimeException(closeable);
            return;
        }

        // As this method is final, it will still be called on mock objects even
        // though mCloseables won't actually be created...we'll just not do anything
        // in that case.
        if (mBagOfTags != null) {
            synchronized (mBagOfTags) {
                mBagOfTags.put(key, closeable);
            }
        }
    }

    /**
     * Returns the closeable previously added with {@link #addCloseable(String, Closeable)}
     * with the given key.
     * @param key The key that was used to add the Closeable.
     */
    @SuppressWarnings({"TypeParameterUnusedInFormals", "unchecked"})
    @Nullable
    public final <T extends Closeable> T getCloseable(@NonNull String key) {
        if (mBagOfTags == null) {
            return null;
        }
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
