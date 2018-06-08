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
import androidx.arch.core.util.Function;

/**
 * Transformation methods for {@link LiveData}.
 * <p>
 * These methods permit functional composition and delegation of {@link LiveData} instances. The
 * transformations are calculated lazily, and will run only when the returned {@link LiveData} is
 * observed. Lifecycle behavior is propagated from the input {@code source} {@link LiveData} to the
 * returned one.
 */
@SuppressWarnings("WeakerAccess")
public class Transformations {

    private Transformations() {
    }

    /**
     * Returns a {@code LiveData} mapped from the input {@code source} {@code LiveData} by applying
     * {@code mapFunction} to each value set on {@code source}.
     * <p>
     * This method is analogous to {@link io.reactivex.Observable#map}.
     * <p>
     * {@code transform} will be executed on the main thread.
     * <p>
     * Here is an example mapping a simple {@code User} struct in a {@code LiveData} to a
     * {@code LiveData} containing their full name as a {@code String}.
     *
     * <pre>
     * LiveData<User> userLiveData = ...;
     * LiveData<String> userFullNameLiveData =
     *     Transformations.map(
     *         userLiveData,
     *         user -> user.firstName + user.lastName);
     * });
     * </pre>
     *
     * @param source      the {@code LiveData} to map from
     * @param mapFunction a function to apply to each value set on {@code source} in order to set
     *                    it
     *                    on the output {@code LiveData}
     * @param <X>         the generic type parameter of {@code source}
     * @param <Y>         the generic type parameter of the returned {@code LiveData}
     * @return a LiveData mapped from {@code source} to type {@code <Y>} by applying
     * {@code mapFunction} to each value set.
     */
    @MainThread
    public static <X, Y> LiveData<Y> map(
            @NonNull LiveData<X> source,
            @NonNull final Function<X, Y> mapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<X>() {
            @Override
            public void onChanged(@Nullable X x) {
                result.setValue(mapFunction.apply(x));
            }
        });
        return result;
    }

    /**
     * Returns a {@code LiveData} mapped from the input {@code source} {@code LiveData} by applying
     * {@code switchMapFunction} to each value set on {@code source}.
     * <p>
     * The returned {@code LiveData} delegates to the most recent {@code LiveData} created by
     * calling {@code switchMapFunction} with the most recent value set to {@code source}, without
     * changing the reference. In this way, {@code switchMapFunction} can change the 'backing'
     * {@code LiveData} transparently to any observer registered to the {@code LiveData} returned
     * by {@code switchMap()}.
     * <p>
     * Note that when the backing {@code LiveData} is switched, no further values from the older
     * {@code LiveData} will be set to the output {@code LiveData}. In this way, the method is
     * analogous to {@link io.reactivex.Observable#switchMap}.
     * <p>
     * {@code switchMapFunction} will be executed on the main thread.
     * <p>
     * Here is an example class that holds a typed-in name of a user
     * {@code String} (such as from an {@code EditText}) in a {@link MutableLiveData} and
     * returns a {@code LiveData} containing a List of {@code User} objects for users that have
     * that name. It populates that {@code LiveData} by requerying a repository-pattern object
     * each time the typed name changes.
     * <p>
     * This {@code ViewModel} would permit the observing UI to update "live" as the user ID text
     * changes.
     *
     * <pre>
     * class UserViewModel extends AndroidViewModel {
     *     MutableLiveData<String> nameQueryLiveData = ...
     *
     *     LiveData<List<String>> getUsersWithNameLiveData() {
     *         return Transformations.switchMap(
     *             nameQueryLiveData,
     *                 name -> myDataSource.getUsersWithNameLiveData(name));
     *     }
     *
     *     void setNameQuery(String name) {
     *         this.nameQueryLiveData.setValue(name);
     *     }
     * }
     * </pre>
     *
     * @param source            the {@code LiveData} to map from
     * @param switchMapFunction a function to apply to each value set on {@code source} to create a
     *                          new delegate {@code LiveData} for the returned one
     * @param <X>               the generic type parameter of {@code source}
     * @param <Y>               the generic type parameter of the returned {@code LiveData}
     * @return a LiveData mapped from {@code source} to type {@code <Y>} by delegating
     * to the LiveData returned by applying {@code switchMapFunction} to each
     * value set
     */
    @MainThread
    public static <X, Y> LiveData<Y> switchMap(
            @NonNull LiveData<X> source,
            @NonNull final Function<X, LiveData<Y>> switchMapFunction) {
        final MediatorLiveData<Y> result = new MediatorLiveData<>();
        result.addSource(source, new Observer<X>() {
            LiveData<Y> mSource;

            @Override
            public void onChanged(@Nullable X x) {
                LiveData<Y> newLiveData = switchMapFunction.apply(x);
                if (mSource == newLiveData) {
                    return;
                }
                if (mSource != null) {
                    result.removeSource(mSource);
                }
                mSource = newLiveData;
                if (mSource != null) {
                    result.addSource(mSource, new Observer<Y>() {
                        @Override
                        public void onChanged(@Nullable Y y) {
                            result.setValue(y);
                        }
                    });
                }
            }
        });
        return result;
    }
}
