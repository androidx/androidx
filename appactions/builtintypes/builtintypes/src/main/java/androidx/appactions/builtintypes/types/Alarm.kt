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
import androidx.appsearch.`annotation`.Document
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
 * An alarm set to go off at a specified schedule.
 *
 * See https://schema.googleapis.com/Alarm for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [AbstractAlarm] if you need to extend this type.
 */
@Document(
  name = "bit:Alarm",
  parent = [Thing::class],
)
public interface Alarm : Thing {
  /**
   * Associates an Alarm with a Schedule.
   *
   * See https://schema.googleapis.com/alarmSchedule for more context.
   */
  @get:Document.DocumentProperty
  public val alarmSchedule: Schedule?
    get() = null

  /**
   * Specifies if the alarm enabled or disabled.
   *
   * Should be left unset in contexts where there is no notion of enabled/disabled alarms.
   *
   * See https://schema.googleapis.com/isAlarmEnabled for more context.
   */
  @get:Document.BooleanProperty
  @get:Suppress("AutoBoxing")
  public val isAlarmEnabled: Boolean?
    get() = null

  /** Converts this [Alarm] to its builder with all the properties copied over. */
  override fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder]. */
    @JvmStatic @Document.BuilderProducer public fun Builder(): Builder<*> = AlarmImpl.Builder()
  }

  /**
   * Builder for [Alarm].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [AbstractAlarm.Builder] if you need to extend this builder.
   */
  public interface Builder<Self : Builder<Self>> : Thing.Builder<Self> {
    /** Returns a built [Alarm]. */
    override fun build(): Alarm

    /** Sets the `alarmSchedule`. */
    @Suppress("DocumentExceptions")
    public fun setAlarmSchedule(schedule: Schedule?): Self = throw NotImplementedError()

    /** Sets the `isAlarmEnabled`. */
    @Suppress("DocumentExceptions")
    public fun setAlarmEnabled(@Suppress("AutoBoxing") boolean: Boolean?): Self =
      throw NotImplementedError()
  }
}

/**
 * An abstract implementation of [Alarm].
 *
 * Allows for extension like:
 * ```kt
 * @Document(
 *   name = "MyAlarm",
 *   parent = [Alarm::class],
 * )
 * class MyAlarm internal constructor(
 *   alarm: Alarm,
 *   @Document.StringProperty val foo: String,
 *   @Document.LongProperty val bars: List<Int>,
 * ) : AbstractAlarm<
 *   MyAlarm,
 *   MyAlarm.Builder
 * >(alarm) {
 *
 *   // No need to implement equals(), hashCode(), toString() or toBuilder()
 *
 *   override val selfTypeName =
 *     "MyAlarm"
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
 *     AbstractAlarm.Builder<
 *       Builder,
 *       MyAlarm> {...}
 * }
 * ```
 *
 * Also see [AbstractAlarm.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class AbstractAlarm<
  Self : AbstractAlarm<Self, Builder>,
  Builder : AbstractAlarm.Builder<Builder, Self>
>
internal constructor(
  final override val namespace: String,
  final override val alarmSchedule: Schedule?,
  @get:Suppress("AutoBoxing") final override val isAlarmEnabled: Boolean?,
  final override val identifier: String,
  final override val name: Name?,
) : Alarm {
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

  /** A copy-constructor that copies over properties from another [Alarm] instance. */
  public constructor(
    alarm: Alarm
  ) : this(alarm.namespace, alarm.alarmSchedule, alarm.isAlarmEnabled, alarm.identifier, alarm.name)

  /** Returns a concrete [Builder] with the additional, non-[Alarm] properties copied over. */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setNamespace(namespace)
      .setAlarmSchedule(alarmSchedule)
      .setAlarmEnabled(isAlarmEnabled)
      .setIdentifier(identifier)
      .setName(name)

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (namespace != other.namespace) return false
    if (alarmSchedule != other.alarmSchedule) return false
    if (isAlarmEnabled != other.isAlarmEnabled) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  final override fun hashCode(): Int =
    Objects.hash(namespace, alarmSchedule, isAlarmEnabled, identifier, name, additionalProperties)

  final override fun toString(): String {
    val attributes = mutableMapOf<String, String>()
    if (namespace.isNotEmpty()) {
      attributes["namespace"] = namespace
    }
    if (alarmSchedule != null) {
      attributes["alarmSchedule"] = alarmSchedule.toString()
    }
    if (isAlarmEnabled != null) {
      attributes["isAlarmEnabled"] = isAlarmEnabled.toString()
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
   * An abstract implementation of [Alarm.Builder].
   *
   * Allows for extension like:
   * ```kt
   * @Document(...)
   * class MyAlarm :
   *   : AbstractAlarm<
   *     MyAlarm,
   *     MyAlarm.Builder>(...) {
   *
   *   @Document.BuilderProducer
   *   class Builder
   *   : AbstractAlarm.Builder<
   *       Builder,
   *       MyAlarm
   *   >() {
   *
   *     // No need to implement equals(), hashCode(), toString() or build()
   *
   *     private var foo: String? = null
   *     private val bars = mutableListOf<Int>()
   *
   *     override val selfTypeName =
   *       "MyAlarm.Builder"
   *
   *     override val additionalProperties: Map<String, Any?>
   *       get() = mapOf("foo" to foo, "bars" to bars)
   *
   *     override fun buildFromAlarm(
   *       alarm: Alarm
   *     ): MyAlarm {
   *       return MyAlarm(
   *         alarm,
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
   * Also see [AbstractAlarm].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<Self : Builder<Self, Built>, Built : AbstractAlarm<Built, Self>> :
    Alarm.Builder<Self> {
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

    private var alarmSchedule: Schedule? = null

    @get:Suppress("AutoBoxing") private var isAlarmEnabled: Boolean? = null

    private var identifier: String = ""

    private var name: Name? = null

    /**
     * Builds a concrete [Built] instance, given a built [Alarm].
     *
     * Subclasses should override this method to build a concrete [Built] instance that holds both
     * the [Alarm]-specific properties and the subclass specific [additionalProperties].
     *
     * See the sample code in the documentation of this class for more context.
     */
    @Suppress("BuilderSetStyle") protected abstract fun buildFromAlarm(alarm: Alarm): Built

    final override fun build(): Built =
      buildFromAlarm(AlarmImpl(namespace, alarmSchedule, isAlarmEnabled, identifier, name))

    final override fun setNamespace(namespace: String): Self {
      this.namespace = namespace
      return this as Self
    }

    final override fun setAlarmSchedule(schedule: Schedule?): Self {
      this.alarmSchedule = schedule
      return this as Self
    }

    final override fun setAlarmEnabled(@Suppress("AutoBoxing") boolean: Boolean?): Self {
      this.isAlarmEnabled = boolean
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
      if (alarmSchedule != other.alarmSchedule) return false
      if (isAlarmEnabled != other.isAlarmEnabled) return false
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    final override fun hashCode(): Int =
      Objects.hash(namespace, alarmSchedule, isAlarmEnabled, identifier, name, additionalProperties)

    @Suppress("BuilderSetStyle")
    final override fun toString(): String {
      val attributes = mutableMapOf<String, String>()
      if (namespace.isNotEmpty()) {
        attributes["namespace"] = namespace
      }
      if (alarmSchedule != null) {
        attributes["alarmSchedule"] = alarmSchedule!!.toString()
      }
      if (isAlarmEnabled != null) {
        attributes["isAlarmEnabled"] = isAlarmEnabled!!.toString()
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

private class AlarmImpl : AbstractAlarm<AlarmImpl, AlarmImpl.Builder> {
  protected override val selfTypeName: String
    get() = "Alarm"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    namespace: String,
    alarmSchedule: Schedule?,
    isAlarmEnabled: Boolean?,
    identifier: String,
    name: Name?,
  ) : super(namespace, alarmSchedule, isAlarmEnabled, identifier, name)

  public constructor(alarm: Alarm) : super(alarm)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  public class Builder : AbstractAlarm.Builder<Builder, AlarmImpl>() {
    protected override val selfTypeName: String
      get() = "Alarm.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromAlarm(alarm: Alarm): AlarmImpl =
      alarm as? AlarmImpl ?: AlarmImpl(alarm)
  }
}
