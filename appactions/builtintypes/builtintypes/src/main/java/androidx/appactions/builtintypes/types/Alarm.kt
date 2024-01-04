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

import androidx.appactions.builtintypes.properties.DisambiguatingDescription
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
import kotlin.jvm.JvmField
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
  public override fun toBuilder(): Builder<*>

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
    public override fun build(): Alarm

    /** Sets the `alarmSchedule`. */
    @Suppress("DocumentExceptions")
    public fun setAlarmSchedule(schedule: Schedule?): Self = throw NotImplementedError()

    /** Sets the `isAlarmEnabled`. */
    @Suppress("DocumentExceptions")
    public fun setAlarmEnabled(@Suppress("AutoBoxing") boolean: Boolean?): Self =
      throw NotImplementedError()

    /** Sets the `disambiguatingDescription` to a canonical [DisambiguatingDescriptionValue]. */
    public fun setDisambiguatingDescription(canonicalValue: DisambiguatingDescriptionValue): Self =
      setDisambiguatingDescription(DisambiguatingDescription(canonicalValue))
  }

  /**
   * A canonical value that may be assigned to [Alarm.disambiguatingDescription].
   *
   * Represents an open enum. See [Companion] for the different possible variants. More variants may
   * be added over time.
   */
  @Document(
    name = "bit:Alarm:DisambiguatingDescriptionValue",
    parent = [DisambiguatingDescription.CanonicalValue::class],
  )
  public class DisambiguatingDescriptionValue
  private constructor(
    textValue: String,
  ) : DisambiguatingDescription.CanonicalValue(textValue) {
    public override fun toString(): String = """Alarm.DisambiguatingDescriptionValue($textValue)"""

    public companion object {
      @JvmField
      public val FAMILY_BELL: DisambiguatingDescriptionValue =
        DisambiguatingDescriptionValue("FamilyBell")
    }
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
 *   val foo: String,
 *   val bars: List<Int>,
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
    Self : AbstractAlarm<Self, Builder>, Builder : AbstractAlarm.Builder<Builder, Self>>
internal constructor(
  public final override val namespace: String,
  public final override val alarmSchedule: Schedule?,
  @get:Suppress("AutoBoxing") public final override val isAlarmEnabled: Boolean?,
  public final override val disambiguatingDescription: DisambiguatingDescription?,
  public final override val identifier: String,
  public final override val name: Name?,
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
  ) : this(
    alarm.namespace,
    alarm.alarmSchedule,
    alarm.isAlarmEnabled,
    alarm.disambiguatingDescription,
    alarm.identifier,
    alarm.name
  )

  /** Returns a concrete [Builder] with the additional, non-[Alarm] properties copied over. */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  public final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setNamespace(namespace)
      .setAlarmSchedule(alarmSchedule)
      .setAlarmEnabled(isAlarmEnabled)
      .setDisambiguatingDescription(disambiguatingDescription)
      .setIdentifier(identifier)
      .setName(name)

  public final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (namespace != other.namespace) return false
    if (alarmSchedule != other.alarmSchedule) return false
    if (isAlarmEnabled != other.isAlarmEnabled) return false
    if (disambiguatingDescription != other.disambiguatingDescription) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  public final override fun hashCode(): Int =
    Objects.hash(
      namespace,
      alarmSchedule,
      isAlarmEnabled,
      disambiguatingDescription,
      identifier,
      name,
      additionalProperties
    )

  public final override fun toString(): String {
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
    if (disambiguatingDescription != null) {
      attributes["disambiguatingDescription"] =
        disambiguatingDescription.toString(includeWrapperName = false)
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
   * class MyAlarm :
   *   : AbstractAlarm<
   *     MyAlarm,
   *     MyAlarm.Builder>(...) {
   *
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

    private var disambiguatingDescription: DisambiguatingDescription? = null

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

    public final override fun build(): Built =
      buildFromAlarm(
        AlarmImpl(
          namespace,
          alarmSchedule,
          isAlarmEnabled,
          disambiguatingDescription,
          identifier,
          name
        )
      )

    public final override fun setNamespace(namespace: String): Self {
      this.namespace = namespace
      return this as Self
    }

    public final override fun setAlarmSchedule(schedule: Schedule?): Self {
      this.alarmSchedule = schedule
      return this as Self
    }

    public final override fun setAlarmEnabled(@Suppress("AutoBoxing") boolean: Boolean?): Self {
      this.isAlarmEnabled = boolean
      return this as Self
    }

    public final override fun setDisambiguatingDescription(
      disambiguatingDescription: DisambiguatingDescription?
    ): Self {
      this.disambiguatingDescription = disambiguatingDescription
      return this as Self
    }

    public final override fun setIdentifier(text: String): Self {
      this.identifier = text
      return this as Self
    }

    public final override fun setName(name: Name?): Self {
      this.name = name
      return this as Self
    }

    @Suppress("BuilderSetStyle")
    public final override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || this::class.java != other::class.java) return false
      other as Self
      if (namespace != other.namespace) return false
      if (alarmSchedule != other.alarmSchedule) return false
      if (isAlarmEnabled != other.isAlarmEnabled) return false
      if (disambiguatingDescription != other.disambiguatingDescription) return false
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    public final override fun hashCode(): Int =
      Objects.hash(
        namespace,
        alarmSchedule,
        isAlarmEnabled,
        disambiguatingDescription,
        identifier,
        name,
        additionalProperties
      )

    @Suppress("BuilderSetStyle")
    public final override fun toString(): String {
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
      if (disambiguatingDescription != null) {
        attributes["disambiguatingDescription"] =
          disambiguatingDescription!!.toString(includeWrapperName = false)
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
    disambiguatingDescription: DisambiguatingDescription?,
    identifier: String,
    name: Name?,
  ) : super(namespace, alarmSchedule, isAlarmEnabled, disambiguatingDescription, identifier, name)

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
