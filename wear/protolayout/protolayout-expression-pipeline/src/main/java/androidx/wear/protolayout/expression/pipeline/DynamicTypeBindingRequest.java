/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.expression.pipeline;

import android.icu.util.ULocale;
import androidx.annotation.NonNull;
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
   *     UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicFloatInternal(
      @NonNull DynamicFloat floatSource, @NonNull DynamicTypeValueReceiver<Float> consumer) {
    return new DynamicFloatBindingRequest(floatSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicBuilders.DynamicFloat}
   * for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on the
   * given {@link Executor}.
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
    return new DynamicFloatBindingRequestWithExecutor(floatSource, executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicInt32} for future
   * binding.
   *
   * @param int32Source The given integer dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *     UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicInt32Internal(
      @NonNull DynamicInt32 int32Source, @NonNull DynamicTypeValueReceiver<Integer> consumer) {
    return new DynamicInt32BindingRequest(int32Source, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicBuilders.DynamicInt32}
   * for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on the
   * given {@link Executor}.
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
    return new DynamicInt32BindingRequestWithExecutor(int32Source, executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicColor} for future
   * binding.
   *
   * @param colorSource The given color dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *     UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicColorInternal(
      @NonNull DynamicColor colorSource, @NonNull DynamicTypeValueReceiver<Integer> consumer) {
    return new DynamicColorBindingRequest(colorSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicBuilders.DynamicColor}
   * for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on the
   * given {@link Executor}.
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
    return new DynamicColorBindingRequestWithExecutor(colorSource, executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicBool} for future
   * binding.
   *
   * @param boolSource The given boolean dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *     UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicBoolInternal(
      @NonNull DynamicBool boolSource, @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
    return new DynamicBoolBindingRequest(boolSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicBuilders.DynamicBool}
   * for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on the
   * given {@link Executor}.
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
    return new DynamicBoolBindingRequestWithExecutor(boolSource, executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicString} for future
   * binding.
   *
   * @param stringSource The given String dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *     UI thread.
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
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on the
   * given {@link Executor}.
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
    return new DynamicStringBindingRequestWithExecutor(stringSource, locale, executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicDuration} for future
   * binding.
   *
   * @param durationSource The given durations dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *     UI thread.
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
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on the
   * given {@link Executor}.
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
    return new DynamicDurationBindingRequestWithExecutor(durationSource, executor, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link DynamicInstant} for future
   * binding.
   *
   * @param instantSource The given instant dynamic type that should be evaluated.
   * @param consumer The registered consumer for results of the evaluation. It will be called from
   *     UI thread.
   */
  @NonNull
  @RestrictTo(Scope.LIBRARY_GROUP)
  public static DynamicTypeBindingRequest forDynamicInstantInternal(
      @NonNull DynamicInstant instantSource, @NonNull DynamicTypeValueReceiver<Instant> consumer) {
    return new DynamicInstantBindingRequest(instantSource, consumer);
  }

  /**
   * Creates a {@link DynamicTypeBindingRequest} from the given {@link
   * DynamicBuilders.DynamicInstant} for binding.
   *
   * <p>Results of evaluation will be sent through the given {@link DynamicTypeValueReceiver} on the
   * given {@link Executor}.
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
    return new DynamicInstantBindingRequestWithExecutor(instantSource, executor, consumer);
  }

  private static class DynamicFloatBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicFloat mFloatSource;
    @NonNull private final DynamicTypeValueReceiver<Float> mConsumer;

    DynamicFloatBindingRequest(
        @NonNull DynamicFloat floatSource, @NonNull DynamicTypeValueReceiver<Float> consumer) {
      mFloatSource = floatSource;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mFloatSource, mConsumer);
    }
  }

  private static class DynamicInt32BindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicInt32 mInt32Source;
    @NonNull private final DynamicTypeValueReceiver<Integer> mConsumer;

    DynamicInt32BindingRequest(
        @NonNull DynamicInt32 int32Source, @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mInt32Source = int32Source;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mInt32Source, mConsumer);
    }
  }

  private static class DynamicColorBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicColor mColorSource;
    @NonNull private final DynamicTypeValueReceiver<Integer> mConsumer;

    DynamicColorBindingRequest(
        @NonNull DynamicColor colorSource, @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mColorSource = colorSource;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mColorSource, mConsumer);
    }
  }

  private static class DynamicBoolBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBool mBoolSource;
    @NonNull private final DynamicTypeValueReceiver<Boolean> mConsumer;

    DynamicBoolBindingRequest(
        @NonNull DynamicBool boolSource, @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
      mBoolSource = boolSource;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mBoolSource, mConsumer);
    }
  }

  private static class DynamicStringBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicString mStringSource;
    @NonNull private final ULocale mLocale;
    @NonNull private final DynamicTypeValueReceiver<String> mConsumer;

    DynamicStringBindingRequest(
        @NonNull DynamicString stringSource,
        @NonNull ULocale locale,
        @NonNull DynamicTypeValueReceiver<String> consumer) {
      mStringSource = stringSource;
      mLocale = locale;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mStringSource, mLocale, mConsumer);
    }
  }

  private static class DynamicDurationBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicDuration mDurationSource;
    @NonNull private final DynamicTypeValueReceiver<Duration> mConsumer;

    DynamicDurationBindingRequest(
        @NonNull DynamicDuration durationSource,
        @NonNull DynamicTypeValueReceiver<Duration> consumer) {
      mDurationSource = durationSource;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mDurationSource, mConsumer);
    }
  }

  private static class DynamicInstantBindingRequest extends DynamicTypeBindingRequest {

    @NonNull private final DynamicInstant mInstantSource;
    @NonNull private final DynamicTypeValueReceiver<Instant> mConsumer;

    DynamicInstantBindingRequest(
        @NonNull DynamicInstant instantSource,
        @NonNull DynamicTypeValueReceiver<Instant> consumer) {
      mInstantSource = instantSource;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mInstantSource, mConsumer);
    }
  }

  private static class DynamicFloatBindingRequestWithExecutor extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBuilders.DynamicFloat mFloatSource;
    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Float> mConsumer;

    DynamicFloatBindingRequestWithExecutor(
        @NonNull DynamicBuilders.DynamicFloat floatSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Float> consumer) {
      mFloatSource = floatSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mFloatSource, mExecutor, mConsumer);
    }
  }

  private static class DynamicInt32BindingRequestWithExecutor extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBuilders.DynamicInt32 mInt32Source;
    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Integer> mConsumer;

    DynamicInt32BindingRequestWithExecutor(
        @NonNull DynamicBuilders.DynamicInt32 int32Source,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mInt32Source = int32Source;
      mExecutor = executor;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mInt32Source, mExecutor, mConsumer);
    }
  }

  private static class DynamicColorBindingRequestWithExecutor extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBuilders.DynamicColor mColorSource;
    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Integer> mConsumer;

    DynamicColorBindingRequestWithExecutor(
        @NonNull DynamicBuilders.DynamicColor colorSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Integer> consumer) {
      mColorSource = colorSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mColorSource, mExecutor, mConsumer);
    }
  }

  private static class DynamicBoolBindingRequestWithExecutor extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBuilders.DynamicBool mBoolSource;
    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Boolean> mConsumer;

    DynamicBoolBindingRequestWithExecutor(
        @NonNull DynamicBuilders.DynamicBool boolSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Boolean> consumer) {
      mBoolSource = boolSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mBoolSource, mExecutor, mConsumer);
    }
  }

  private static class DynamicStringBindingRequestWithExecutor extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBuilders.DynamicString mStringSource;
    @NonNull private final ULocale mLocale;
    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<String> mConsumer;

    DynamicStringBindingRequestWithExecutor(
        @NonNull DynamicBuilders.DynamicString stringSource,
        @NonNull ULocale locale,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<String> consumer) {
      mStringSource = stringSource;
      mExecutor = executor;
      mConsumer = consumer;
      this.mLocale = locale;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mStringSource, mLocale, mExecutor, mConsumer);
    }
  }

  private static class DynamicDurationBindingRequestWithExecutor extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBuilders.DynamicDuration mDurationSource;
    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Duration> mConsumer;

    DynamicDurationBindingRequestWithExecutor(
        @NonNull DynamicBuilders.DynamicDuration durationSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Duration> consumer) {
      mDurationSource = durationSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mDurationSource, mExecutor, mConsumer);
    }
  }

  private static class DynamicInstantBindingRequestWithExecutor extends DynamicTypeBindingRequest {

    @NonNull private final DynamicBuilders.DynamicInstant mInstantSource;
    @NonNull private final Executor mExecutor;
    @NonNull private final DynamicTypeValueReceiver<Instant> mConsumer;

    DynamicInstantBindingRequestWithExecutor(
        @NonNull DynamicBuilders.DynamicInstant instantSource,
        @NonNull Executor executor,
        @NonNull DynamicTypeValueReceiver<Instant> consumer) {
      mInstantSource = instantSource;
      mExecutor = executor;
      mConsumer = consumer;
    }

    @Override
    BoundDynamicTypeImpl callBindOn(DynamicTypeEvaluator evaluator) {
      return evaluator.bindInternal(mInstantSource, mExecutor, mConsumer);
    }
  }
}
