/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.protolayout.expression.pipeline;

import android.icu.util.ULocale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import androidx.wear.protolayout.expression.DynamicBuilders;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicBool;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicColor;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicDuration;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicFloat;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInstant;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicInt32;
import androidx.wear.protolayout.expression.proto.DynamicProto.DynamicString;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executor;

/**
 * Holds the parameters needed by {@link DynamicTypeEvaluator#bind}. It can be used as follows:
 *
 * <pre>{@code
 * DynamicTypeBindingRequest request = DynamicTypeBindingRequest.forDynamicInt32(source,consumer);
 * BoundDynamicType boundType = evaluator.bind(request);
 * }</pre>
 */
public abstract class DynamicTypeBindingRequest {

  private DynamicTypeBindingRequest() {}

  abstract BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator);

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicFloat} for future
   * binding.
   *
   * @param floatSource The given float dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *   UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicFloatInternal(
      @NonNull DynamicFloat floatSource, @NonNull DynamicTypeValueReceiver<Float> consumer) {
    return new DynamicFloatBindingRequest(floatSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicFloat} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
   * the given {@link Executor}.
   *
   * @param floatSource The given float dynamic type that should be evaluated.
   * @param executor The Executor to run the consumer on.
   * @param consumer The registered consumer for results of the evaluation.
   */
  @NonNull
  public static DynamicTypeBindingRequest forDynamicFloat(
      @NonNull DynamicBuilders.DynamicFloat floatSource,
      @NonNull Executor executor,
      @NonNull DynamicTypeValueReceiver<Float> consumer) {
    return new DynamicFloatBindingRequest(
        floatSource.toDynamicFloatProto(), executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicInt32} for future
   * binding.
   *
   * @param int32Source The given integer dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *   UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicInt32Internal(
      @NonNull DynamicInt32 int32Source,
      @NonNull DynamicTypeValueReceiver<Integer> consumer) {
    return new DynamicInt32BindingRequest(int32Source, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicInt32} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
   * the given {@link Executor}.
   *
   * @param int32Source The given integer dynamic type that should be evaluated.
   * @param executor The Executor to run the consumer on.
   * @param consumer The registered consumer for results of the evaluation.
   */
  @NonNull
  public static DynamicTypeBindingRequest forDynamicInt32(
      @NonNull DynamicBuilders.DynamicInt32 int32Source,
      @NonNull Executor executor,
      @NonNull DynamicTypeValueReceiver<Integer> consumer) {
    return new DynamicInt32BindingRequest(
        int32Source.toDynamicInt32Proto(), executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicColor} for future
   * binding.
   *
   * @param colorSource The given color dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *   UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicColorInternal(
      @NonNull DynamicColor colorSource,
      @NonNull DynamicTypeValueReceiver<Integer> consumer) {
    return new DynamicColorBindingRequest(colorSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicColor} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
   * the given {@link Executor}.
   *
   * @param colorSource The given color dynamic type that should be evaluated.
   * @param executor The Executor to run the consumer on.
   * @param consumer The registered consumer for results of the evaluation.
   */
  @NonNull
  public static DynamicTypeBindingRequest forDynamicColor(
      @NonNull DynamicBuilders.DynamicColor colorSource,
      @NonNull Executor executor,
      @NonNull DynamicTypeValueReceiver<Integer> consumer) {
    return new DynamicColorBindingRequest(
        colorSource.toDynamicColorProto(), executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicBool} for future
   * binding.
   *
   * @param boolSource The given boolean dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *   UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicBoolInternal(
      @NonNull DynamicBool boolSource, @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
    return new DynamicBoolBindingRequest(boolSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicBool} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
   * the given {@link Executor}.
   *
   * @param boolSource The given boolean dynamic type that should be evaluated.
   * @param executor The Executor to run the consumer on.
   * @param consumer The registered consumer for results of the evaluation.
   */
  @NonNull
  public static DynamicTypeBindingRequest forDynamicBool(
      @NonNull DynamicBuilders.DynamicBool boolSource,
      @NonNull Executor executor,
      @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
    return new DynamicBoolBindingRequest(boolSource.toDynamicBoolProto(), executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicString} for future
   * binding.
   *
   * @param stringSource The given String dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *   UI thread.
   * @param locale The locale used for the given String source.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicStringInternal(
      @NonNull DynamicString stringSource,
      @NonNull ULocale locale,
      @NonNull DynamicTypeValueReceiver<String> consumer) {
    return new DynamicStringBindingRequest(stringSource, locale, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicString} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
   * the given {@link Executor}.
   *
   * @param stringSource The given String dynamic type that should be evaluated.
   * @param locale The locale used for the given String source.
   * @param executor The Executor to run the consumer on.
   * @param consumer The registered consumer for results of the evaluation.
   */
  @NonNull
  public static DynamicTypeBindingRequest forDynamicString(
      @NonNull DynamicBuilders.DynamicString stringSource,
      @NonNull ULocale locale,
      @NonNull Executor executor,
      @NonNull DynamicTypeValueReceiver<String> consumer) {
    return new DynamicStringBindingRequest(
        stringSource.toDynamicStringProto(), locale, executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicDuration} for future
   * binding.
   *
   * @param durationSource The given durations dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *   UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicDurationInternal(
      @NonNull DynamicDuration durationSource,
      @NonNull DynamicTypeValueReceiver<Duration> consumer) {
    return new DynamicDurationBindingRequest(durationSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicDuration} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
   * the given {@link Executor}.
   *
   * @param durationSource The given duration dynamic type that should be evaluated.
   * @param executor The Executor to run the consumer on.
   * @param consumer The registered consumer for results of the evaluation.
   */
  @NonNull
  public static DynamicTypeBindingRequest forDynamicDuration(
      @NonNull DynamicBuilders.DynamicDuration durationSource,
      @NonNull Executor executor,
      @NonNull DynamicTypeValueReceiver<Duration> consumer) {
    return new DynamicDurationBindingRequest(
        durationSource.toDynamicDurationProto(), executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicInstant} for future
   * binding.
   *
   * @param instantSource The given instant dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *   UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicInstantInternal(
      @NonNull DynamicInstant instantSource,
      @NonNull DynamicTypeValueReceiver<Instant> consumer) {
    return new DynamicInstantBindingRequest(instantSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicInstant} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on
   * the given {@link Executor}.
   *
   * @param instantSource The given instant dynamic type that should be evaluated.
   * @param executor The Executor to run the consumer on.
   * @param consumer The registered consumer for results of the evaluation.
   */
  @NonNull
  public static DynamicTypeBindingRequest forDynamicInstant(
      @NonNull DynamicBuilders.DynamicInstant instantSource,
      @NonNull Executor executor,
      @NonNull DynamicTypeValueReceiver<Instant> consumer) {
    return new DynamicInstantBindingRequest(
        instantSource.toDynamicInstantProto(), executor, consumer);
  }

  @NonNull
  private static <T> DynamicTypeValueReceiverOnExecutor<T> createReceiver(
      @Nullable Executor executor, @NonNull DynamicTypeValueReceiver<T> consumer) {
    if (executor != null) {
      return new DynamicTypeValueReceiverOnExecutor<>(executor, consumer);
    } else {
      return new DynamicTypeValueReceiverOnExecutor<>(consumer);
    }
  }

  private static class DynamicFloatBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicFloat mFloatSource;
    @Nullable private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Float> mConsumer;

    DynamicFloatBindingRequest(
        @NonNull DynamicFloat floatSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Float> consumer) {
      mFloatSource = floatSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    DynamicFloatBindingRequest(
        @NonNull DynamicFloat floatSource,
        @NonNull DynamicTypeValueReceiver<Float> consumer) {
      mFloatSource = floatSource;
      mConsumer = consumer;
      mExecutor = null;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mFloatSource, createReceiver(mExecutor, mConsumer));
    }
  }

  private static class DynamicInt32BindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicInt32 mInt32Source;
    @Nullable private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Integer> mConsumer;

    DynamicInt32BindingRequest(
        @NonNull DynamicInt32 int32Source,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mInt32Source = int32Source;
      mExecutor = executor;
      mConsumer = consumer;
    }

    DynamicInt32BindingRequest(
        @NonNull DynamicInt32 int32Source,
        @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mInt32Source = int32Source;
      mConsumer = consumer;
      mExecutor = null;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mInt32Source, createReceiver(mExecutor, mConsumer));
    }
  }

  private static class DynamicColorBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicColor mColorSource;
    @Nullable private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Integer> mConsumer;

    DynamicColorBindingRequest(
        @NonNull DynamicColor colorSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mColorSource = colorSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    DynamicColorBindingRequest(
        @NonNull DynamicColor colorSource,
        @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mColorSource = colorSource;
      mConsumer = consumer;
      mExecutor = null;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mColorSource, createReceiver(mExecutor, mConsumer));
    }
  }

  private static class DynamicBoolBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBool mBoolSource;
    @Nullable private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Boolean> mConsumer;

    DynamicBoolBindingRequest(
        @NonNull DynamicBool boolSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
      mBoolSource = boolSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    DynamicBoolBindingRequest(
        @NonNull DynamicBool boolSource,
        @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
      mBoolSource = boolSource;
      mConsumer = consumer;
      mExecutor = null;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mBoolSource, createReceiver(mExecutor, mConsumer));
    }
  }

  private static class DynamicStringBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicString mStringSource;
    @NonNull private final ULocale mLocale;
    @Nullable private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<String> mConsumer;

    DynamicStringBindingRequest(
        @NonNull DynamicString stringSource,
        @NonNull ULocale locale,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<String> consumer) {
      mStringSource = stringSource;
      mExecutor = executor;
      mConsumer = consumer;
      this.mLocale = locale;
    }

    DynamicStringBindingRequest(
        @NonNull DynamicString stringSource,
        @NonNull ULocale locale,
        @NonNull DynamicTypeValueReceiver<String> consumer) {
      mStringSource = stringSource;
      mConsumer = consumer;
      mLocale = locale;
      mExecutor = null;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(
          mStringSource, mLocale, createReceiver(mExecutor, mConsumer));
    }
  }

  private static class DynamicDurationBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicDuration mDurationSource;
    @Nullable private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Duration> mConsumer;

    DynamicDurationBindingRequest(
        @NonNull DynamicDuration durationSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Duration> consumer) {
      mDurationSource = durationSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    DynamicDurationBindingRequest(
        @NonNull DynamicDuration durationSource,
        @NonNull DynamicTypeValueReceiver<Duration> consumer) {
      mDurationSource = durationSource;
      mConsumer = consumer;
      mExecutor = null;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mDurationSource, createReceiver(mExecutor, mConsumer));
    }
  }

  private static class DynamicInstantBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicInstant mInstantSource;
    @Nullable private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Instant> mConsumer;

    DynamicInstantBindingRequest(
        @NonNull DynamicInstant instantSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Instant> consumer) {
      mInstantSource = instantSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    DynamicInstantBindingRequest(
        @NonNull DynamicInstant instantSource,
        @NonNull DynamicTypeValueReceiver<Instant> consumer) {
      mInstantSource = instantSource;
      mConsumer = consumer;
      mExecutor = null;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mInstantSource, createReceiver(mExecutor, mConsumer));
    }
  }

  /**
   * Wraps {@link DynamicTypeValueReceiver} and executes its methods on the given {@link
   * Executor}.
   */
  private static class DynamicTypeValueReceiverOnExecutor<T>
      implements DynamicTypeValueReceiverWithPreUpdate<T> {

    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<T> mConsumer;

    DynamicTypeValueReceiverOnExecutor(@NonNull DynamicTypeValueReceiver<T> consumer) {
      this(Runnable::run, consumer);
    }

    DynamicTypeValueReceiverOnExecutor(
        @NonNull Executor executor, @NonNull DynamicTypeValueReceiver<T> consumer) {
      this.mConsumer = consumer;
      this.mExecutor = executor;
    }

    /** This method is noop in this class. */
    @Override
    @SuppressWarnings("ExecutorTaskName")
    public void onPreUpdate() {}

    @Override
    @SuppressWarnings("ExecutorTaskName")
    public void onData(@NonNull T newData) {
      mExecutor.execute(() -> mConsumer.onData(newData));
    }

    @Override
    @SuppressWarnings("ExecutorTaskName")
    public void onInvalidated() {
      mExecutor.execute(mConsumer::onInvalidated);
    }
  }
}
