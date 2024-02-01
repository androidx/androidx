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
package androidx.appactions.builtintypes.types

import androidx.appactions.builtintypes.properties.Name
import androidx.appactions.builtintypes.serializers.DurationAsNanosSerializer
import androidx.appsearch.`annotation`.Document
import java.time.Duration
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.NotImplementedError
import kotlin.String
import kotlin.Suppress
import kotlin.collections.Map
import kotlin.collections.emptyMap
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableMapOf
import kotlin.collections.plusAssign
import kotlin.jvm.JvmStatic

/**
 * A timer to go off at a particular time.
 *
 * See https://schema.googleapis.com/Timer for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [AbstractTimer] if you need to extend this type.
 */
@Document(
  name = "bit:Timer",
  parent = [Thing::class],
)
public interface Timer : Thing {
  /**
   * The duration of the item (movie, audio recording, event, etc.).
   *
   * See https://schema.org/duration for more context.
   */
  @get:Document.LongProperty(serializer = DurationAsNanosSerializer::class)
  public val duration: Duration?
    get() = null

  /** Converts this [Timer] to its builder with all the properties copied over. */
  override fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder]. */
    @JvmStatic @Document.BuilderProducer public fun Builder(): Builder<*> = TimerImpl.Builder()
  }

  /**
   * Builder for [Timer].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [AbstractTimer.Builder] if you need to extend this builder.
   */
  public interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
    /** Returns a built [Timer]. */
    override fun build(): Timer

    /** Sets the `duration`. */
    @Suppress("DocumentExceptions")
    public fun setDuration(duration: Duration?): Self = throw NotImplementedError()
  }
}

/**
 * An abstract implementation of [Timer].
 *
 * Allows for extension like:
 * ```kt
 * @Document(
 *   name = "MyTimer",
 *   parent = [Timer::class],
 * )
 * class MyTimer internal constructor(
 *   timer: Timer,
 *   @Document.StringProperty val foo: String,
 *   @Document.LongProperty val bars: List<Int>,
 * ) : AbstractTimer<
 *   MyTimer,
 *   MyTimer.Builder
 * >(timer) {
 *
 *   // No need to implement equals(), hashCode(), toString() or toBuilder()
 *
 *   override val selfTypeName =
 *     "MyTimer"
 *
 *   override val additionalProperties: Map<String, Any?>
 *     get() = mapOf("foo" to foo, "bars" to bars)
 *
 *   override fun toBuilderWithAdditionalPropertiesOnly(): Builder {
 *     return Builder()
 *       .setFoo(foo)
 *       .addBars(bars)
 *   }
 *
 *   @Document.BuilderProducer
 *   class Builder :
 *     AbstractTimer.Builder<
 *       Builder,
 *       MyTimer> {...}
 * }
 * ```
 *
 * Also see [AbstractTimer.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class AbstractTimer<
  Self : AbstractTimer<Self, Builder>,
  Builder : AbstractTimer.Builder<Builder, Self>
>
internal constructor(
  final override val namespace: String,
  final override val duration: Duration?,
  final override val identifier: String,
  final override val name: Name?,
) : Timer {
  /**
   * Human readable name for the concrete [Self] class.
   *
   * Used in the [toString] output.
   */
  protected abstract val selfTypeName: String

  /**
   * The additional properties that exist on the concrete [Self] class.
   *
   * Used for equality comparison and computing the hash code.
   */
  protected abstract val additionalProperties: Map<String, Any?>

  /** A copy-constructor that copies over properties from another [Timer] instance. */
  public constructor(
    timer: Timer
  ) : this(timer.namespace, timer.duration, timer.identifier, timer.name)

  /** Returns a concrete [Builder] with the additional, non-[Timer] properties copied over. */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setNamespace(namespace)
      .setDuration(duration)
      .setIdentifier(identifier)
      .setName(name)

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (namespace != other.namespace) return false
    if (duration != other.duration) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  final override fun hashCode(): Int =
    Objects.hash(namespace, duration, identifier, name, additionalProperties)

  final override fun toString(): String {
    val attributes = mutableMapOf<String, String>()
    if (namespace.isNotEmpty()) {
      attributes["namespace"] = namespace
    }
    if (duration != null) {
      attributes["duration"] = duration.toString()
    }
    if (identifier.isNotEmpty()) {
      attributes["identifier"] = identifier
    }
    if (name != null) {
      attributes["name"] = name.toString(includeWrapperName = false)
    }
    attributes += additionalProperties.map { (k, v) -> k to v.toString() }
    val commaSeparated = attributes.entries.joinToString(separator = ", ") { (k, v) -> """$k=$v""" }
    return """$selfTypeName($commaSeparated)"""
  }

  /**
   * An abstract implementation of [Timer.Builder].
   *
   * Allows for extension like:
   * ```kt
   * @Document(...)
   * class MyTimer :
   *   : AbstractTimer<
   *     MyTimer,
   *     MyTimer.Builder>(...) {
   *
   *   @Document.BuilderProducer
   *   class Builder
   *   : AbstractTimer.Builder<
   *       Builder,
   *       MyTimer
   *   >() {
   *
   *     // No need to implement equals(), hashCode(), toString() or build()
   *
   *     private var foo: String? = null
   *     private val bars = mutableListOf<Int>()
   *
   *     override val selfTypeName =
   *       "MyTimer.Builder"
   *
   *     override val additionalProperties: Map<String, Any?>
   *       get() = mapOf("foo" to foo, "bars" to bars)
   *
   *     override fun buildFromTimer(
   *       timer: Timer
   *     ): MyTimer {
   *       return MyTimer(
   *         timer,
   *         foo,
   *         bars.toList()
   *       )
   *     }
   *
   *     fun setFoo(string: String): Builder {
   *       return apply { foo = string }
   *     }
   *
   *     fun addBar(int: Int): Builder {
   *       return apply { bars += int }
   *     }
   *
   *     fun addBars(values: Iterable<Int>): Builder {
   *       return apply { bars += values }
   *     }
   *   }
   * }
   * ```
   *
   * Also see [AbstractTimer].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<Self : Builder<Self, Built>, Built : AbstractTimer<Built, Self>> :
    Timer.Builder<Self> {
    /**
     * Human readable name for the concrete [Self] class.
     *
     * Used in the [toString] output.
     */
    @get:Suppress("GetterOnBuilder") protected abstract val selfTypeName: String

    /**
     * The additional properties that exist on the concrete [Self] class.
     *
     * Used for equality comparison and computing the hash code.
     */
    @get:Suppress("GetterOnBuilder") protected abstract val additionalProperties: Map<String, Any?>

    private var namespace: String = ""

    private var duration: Duration? = null

    private var identifier: String = ""

    private var name: Name? = null

    /**
     * Builds a concrete [Built] instance, given a built [Timer].
     *
     * Subclasses should override this method to build a concrete [Built] instance that holds both
     * the [Timer]-specific properties and the subclass specific [additionalProperties].
     *
     * See the sample code in the documentation of this class for more context.
     */
    @Suppress("BuilderSetStyle") protected abstract fun buildFromTimer(timer: Timer): Built

    final override fun build(): Built =
      buildFromTimer(TimerImpl(namespace, duration, identifier, name))

    final override fun setNamespace(namespace: String): Self {
      this.namespace = namespace
      return this as Self
    }

    final override fun setDuration(duration: Duration?): Self {
      this.duration = duration
      return this as Self
    }

    final override fun setIdentifier(text: String): Self {
      this.identifier = text
      return this as Self
    }

    final override fun setName(name: Name?): Self {
      this.name = name
      return this as Self
    }

    @Suppress("BuilderSetStyle")
    final override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || this::class.java != other::class.java) return false
      other as Self
      if (namespace != other.namespace) return false
      if (duration != other.duration) return false
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    final override fun hashCode(): Int =
      Objects.hash(namespace, duration, identifier, name, additionalProperties)

    @Suppress("BuilderSetStyle")
    final override fun toString(): String {
      val attributes = mutableMapOf<String, String>()
      if (namespace.isNotEmpty()) {
        attributes["namespace"] = namespace
      }
      if (duration != null) {
        attributes["duration"] = duration!!.toString()
      }
      if (identifier.isNotEmpty()) {
        attributes["identifier"] = identifier
      }
      if (name != null) {
        attributes["name"] = name!!.toString(includeWrapperName = false)
      }
      attributes += additionalProperties.map { (k, v) -> k to v.toString() }
      val commaSeparated =
        attributes.entries.joinToString(separator = ", ") { (k, v) -> """$k=$v""" }
      return """$selfTypeName($commaSeparated)"""
    }
  }
}

private class TimerImpl : AbstractTimer<TimerImpl, TimerImpl.Builder> {
  protected override val selfTypeName: String
    get() = "Timer"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    namespace: String,
    duration: Duration?,
    identifier: String,
    name: Name?,
  ) : super(namespace, duration, identifier, name)

  public constructor(timer: Timer) : super(timer)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  public class Builder : AbstractTimer.Builder<Builder, TimerImpl>() {
    protected override val selfTypeName: String
      get() = "Timer.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromTimer(timer: Timer): TimerImpl =
      timer as? TimerImpl ?: TimerImpl(timer)
  }
}
