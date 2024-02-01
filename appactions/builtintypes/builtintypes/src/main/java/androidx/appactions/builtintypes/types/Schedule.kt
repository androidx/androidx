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

import androidx.appactions.builtintypes.properties.ByDay
import androidx.appactions.builtintypes.properties.EndDate
import androidx.appactions.builtintypes.properties.EndTime
import androidx.appactions.builtintypes.properties.ExceptDate
import androidx.appactions.builtintypes.properties.Name
import androidx.appactions.builtintypes.properties.RepeatFrequency
import androidx.appactions.builtintypes.properties.StartDate
import androidx.appactions.builtintypes.properties.StartTime
import androidx.appsearch.`annotation`.Document
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Objects
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.NotImplementedError
import kotlin.String
import kotlin.Suppress
import kotlin.collections.Iterable
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.emptyList
import kotlin.collections.emptyMap
import kotlin.collections.isNotEmpty
import kotlin.collections.joinToString
import kotlin.collections.map
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.plusAssign
import kotlin.collections.toList
import kotlin.jvm.JvmStatic

/**
 * A schedule defines a repeating time period used to describe a regularly occurring `Event`. At a
 * minimum a schedule will specify `repeatFrequency` which describes the interval between
 * occurrences of the event. Additional information can be provided to specify the schedule more
 * precisely. This includes identifying the day(s) of the week or month when the recurring event
 * will take place, in addition to its start and end time. Schedules may also have start and end
 * dates to indicate when they are active, e.g. to define a limited calendar of events.
 *
 * See https://schema.org/Schedule for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [AbstractSchedule] if you need to extend this type.
 */
@Document(
  name = "bit:Schedule",
  parent = [Intangible::class],
)
public interface Schedule : Intangible {
  /**
   * Defines the day(s) of the week on which a recurring Event takes place.
   *
   * See https://schema.org/byDay for more context.
   */
  @get:Document.DocumentProperty
  public val byDays: List<ByDay>
    get() = emptyList()

  /**
   * Defines the month(s) of the year on which a recurring Event takes place. Specified as an
   * Integer between 1-12. January is 1.
   *
   * See https://schema.org/byMonth for more context.
   */
  @get:Document.LongProperty
  public val byMonths: List<Long>
    get() = emptyList()

  /**
   * Defines the day(s) of the month on which a recurring Event takes place. Specified as an Integer
   * between 1-31.
   *
   * See https://schema.org/byMonthDay for more context.
   */
  @get:Document.LongProperty
  public val byMonthDays: List<Long>
    get() = emptyList()

  /**
   * Defines the week(s) of the month on which a recurring Event takes place. Specified as an
   * Integer between 1-5. For clarity, byMonthWeek is best used in conjunction with byDay to
   * indicate concepts like the first and third Mondays of a month.
   *
   * See https://schema.org/byMonthWeek for more context.
   */
  @get:Document.LongProperty
  public val byMonthWeeks: List<Long>
    get() = emptyList()

  /**
   * The end date and time of the item.
   *
   * See https://schema.org/endDate for more context.
   */
  @get:Document.DocumentProperty
  public val endDate: EndDate?
    get() = null

  /**
   * The endTime of something.
   *
   * For a reserved event or service (e.g. `FoodEstablishmentReservation`), the time that it is
   * expected to end. For actions that span a period of time, when the action was performed. E.g.
   * John wrote a book from January to *December*. For media, including audio and video, it's the
   * time offset of the end of a clip within a larger file.
   *
   * See https://schema.org/endTime for more context.
   */
  @get:Document.DocumentProperty
  public val endTime: EndTime?
    get() = null

  /**
   * Defines a `Date` or `DateTime` during which a scheduled `Event` will not take place. The
   * property allows exceptions to a `Schedule` to be specified. If an exception is specified as a
   * `DateTime` then only the event that would have started at that specific date and time should be
   * excluded from the schedule. If an exception is specified as a `Date` then any event that is
   * scheduled for that 24 hour period should be excluded from the schedule. This allows a whole day
   * to be excluded from the schedule without having to itemise every scheduled event.
   *
   * See https://schema.org/exceptDate for more context.
   */
  @get:Document.DocumentProperty
  public val exceptDate: ExceptDate?
    get() = null

  /**
   * Defines the number of times a recurring `Event` will take place.
   *
   * See https://schema.org/repeatCount for more context.
   */
  @get:Document.LongProperty
  @get:Suppress("AutoBoxing")
  public val repeatCount: Long?
    get() = null

  /**
   * Defines the frequency at which `Event`s will occur according to a schedule `Schedule`. The
   * intervals between events should be defined as a `Duration` of time.
   *
   * See https://schema.org/repeatFrequency for more context.
   */
  @get:Document.DocumentProperty
  public val repeatFrequency: RepeatFrequency?
    get() = null

  /**
   * Indicates the timezone for which the time(s) indicated in the `Schedule` are given. The value
   * provided should be among those listed in the IANA Time Zone Database.
   *
   * See https://schema.org/scheduleTimezone for more context.
   */
  @get:Document.StringProperty
  public val scheduleTimezone: String?
    get() = null

  /**
   * The start date and time of the item.
   *
   * See https://schema.org/startDate for more context.
   */
  @get:Document.DocumentProperty
  public val startDate: StartDate?
    get() = null

  /**
   * The startTime of something.
   *
   * For a reserved event or service (e.g. `FoodEstablishmentReservation`), the time that it is
   * expected to start. For actions that span a period of time, when the action was performed. E.g.
   * John wrote a book from *January* to December. For media, including audio and video, it's the
   * time offset of the start of a clip within a larger file.
   *
   * See https://schema.org/startTime for more context.
   */
  @get:Document.DocumentProperty
  public val startTime: StartTime?
    get() = null

  /** Converts this [Schedule] to its builder with all the properties copied over. */
  override fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder]. */
    @JvmStatic @Document.BuilderProducer public fun Builder(): Builder<*> = ScheduleImpl.Builder()
  }

  /**
   * Builder for [Schedule].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [AbstractSchedule.Builder] if you need to extend this builder.
   */
  public interface Builder<Self : Builder<Self>> : Intangible.Builder<Self> {
    /** Returns a built [Schedule]. */
    override fun build(): Schedule

    /** Appends [DayOfWeek] as a value to `byDays`. */
    public fun addByDay(dayOfWeek: DayOfWeek): Self = addByDay(ByDay(dayOfWeek))

    /** Appends a value to `byDays`. */
    @Suppress("DocumentExceptions")
    public fun addByDay(byDay: ByDay): Self = throw NotImplementedError()

    /** Appends multiple values to `byDays`. */
    @Suppress("DocumentExceptions")
    public fun addByDays(values: Iterable<ByDay>): Self = throw NotImplementedError()

    /** Clears `byDays`. */
    @Suppress("DocumentExceptions") public fun clearByDays(): Self = throw NotImplementedError()

    /** Appends a value to `byMonths`. */
    @Suppress("DocumentExceptions")
    public fun addByMonth(integer: Long): Self = throw NotImplementedError()

    /** Appends multiple values to `byMonths`. */
    @Suppress("DocumentExceptions")
    public fun addByMonths(values: Iterable<Long>): Self = throw NotImplementedError()

    /** Clears `byMonths`. */
    @Suppress("DocumentExceptions") public fun clearByMonths(): Self = throw NotImplementedError()

    /** Appends a value to `byMonthDays`. */
    @Suppress("DocumentExceptions")
    public fun addByMonthDay(integer: Long): Self = throw NotImplementedError()

    /** Appends multiple values to `byMonthDays`. */
    @Suppress("DocumentExceptions")
    public fun addByMonthDays(values: Iterable<Long>): Self = throw NotImplementedError()

    /** Clears `byMonthDays`. */
    @Suppress("DocumentExceptions")
    public fun clearByMonthDays(): Self = throw NotImplementedError()

    /** Appends a value to `byMonthWeeks`. */
    @Suppress("DocumentExceptions")
    public fun addByMonthWeek(integer: Long): Self = throw NotImplementedError()

    /** Appends multiple values to `byMonthWeeks`. */
    @Suppress("DocumentExceptions")
    public fun addByMonthWeeks(values: Iterable<Long>): Self = throw NotImplementedError()

    /** Clears `byMonthWeeks`. */
    @Suppress("DocumentExceptions")
    public fun clearByMonthWeeks(): Self = throw NotImplementedError()

    /** Sets the `endDate` to [LocalDate]. */
    public fun setEndDate(date: LocalDate): Self = setEndDate(EndDate(date))

    /** Sets the `endDate`. */
    @Suppress("DocumentExceptions")
    public fun setEndDate(endDate: EndDate?): Self = throw NotImplementedError()

    /** Sets the `endTime` to [LocalTime]. */
    public fun setEndTime(time: LocalTime): Self = setEndTime(EndTime(time))

    /** Sets the `endTime`. */
    @Suppress("DocumentExceptions")
    public fun setEndTime(endTime: EndTime?): Self = throw NotImplementedError()

    /** Sets the `exceptDate` to [LocalDate]. */
    public fun setExceptDate(date: LocalDate): Self = setExceptDate(ExceptDate(date))

    /** Sets the `exceptDate` to [LocalDateTime]. */
    public fun setExceptDate(localDateTime: LocalDateTime): Self =
      setExceptDate(ExceptDate(localDateTime))

    /** Sets the `exceptDate` to [Instant]. */
    public fun setExceptDate(instant: Instant): Self = setExceptDate(ExceptDate(instant))

    /** Sets the `exceptDate`. */
    @Suppress("DocumentExceptions")
    public fun setExceptDate(exceptDate: ExceptDate?): Self = throw NotImplementedError()

    /** Sets the `repeatCount`. */
    @Suppress("DocumentExceptions")
    public fun setRepeatCount(@Suppress("AutoBoxing") integer: Long?): Self =
      throw NotImplementedError()

    /** Sets the `repeatFrequency` to [Duration]. */
    public fun setRepeatFrequency(duration: Duration): Self =
      setRepeatFrequency(RepeatFrequency(duration))

    /** Sets the `repeatFrequency`. */
    @Suppress("DocumentExceptions")
    public fun setRepeatFrequency(repeatFrequency: RepeatFrequency?): Self =
      throw NotImplementedError()

    /** Sets the `scheduleTimezone`. */
    @Suppress("DocumentExceptions")
    public fun setScheduleTimezone(text: String?): Self = throw NotImplementedError()

    /** Sets the `startDate` to [LocalDate]. */
    public fun setStartDate(date: LocalDate): Self = setStartDate(StartDate(date))

    /** Sets the `startDate`. */
    @Suppress("DocumentExceptions")
    public fun setStartDate(startDate: StartDate?): Self = throw NotImplementedError()

    /** Sets the `startTime` to [LocalTime]. */
    public fun setStartTime(time: LocalTime): Self = setStartTime(StartTime(time))

    /** Sets the `startTime`. */
    @Suppress("DocumentExceptions")
    public fun setStartTime(startTime: StartTime?): Self = throw NotImplementedError()
  }
}

/**
 * An abstract implementation of [Schedule].
 *
 * Allows for extension like:
 * ```kt
 * @Document(
 *   name = "MySchedule",
 *   parent = [Schedule::class],
 * )
 * class MySchedule internal constructor(
 *   schedule: Schedule,
 *   @Document.StringProperty val foo: String,
 *   @Document.LongProperty val bars: List<Int>,
 * ) : AbstractSchedule<
 *   MySchedule,
 *   MySchedule.Builder
 * >(schedule) {
 *
 *   // No need to implement equals(), hashCode(), toString() or toBuilder()
 *
 *   override val selfTypeName =
 *     "MySchedule"
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
 *     AbstractSchedule.Builder<
 *       Builder,
 *       MySchedule> {...}
 * }
 * ```
 *
 * Also see [AbstractSchedule.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class AbstractSchedule<
  Self : AbstractSchedule<Self, Builder>,
  Builder : AbstractSchedule.Builder<Builder, Self>
>
internal constructor(
  final override val namespace: String,
  final override val byDays: List<ByDay>,
  final override val byMonths: List<Long>,
  final override val byMonthDays: List<Long>,
  final override val byMonthWeeks: List<Long>,
  final override val endDate: EndDate?,
  final override val endTime: EndTime?,
  final override val exceptDate: ExceptDate?,
  @get:Suppress("AutoBoxing") final override val repeatCount: Long?,
  final override val repeatFrequency: RepeatFrequency?,
  final override val scheduleTimezone: String?,
  final override val startDate: StartDate?,
  final override val startTime: StartTime?,
  final override val identifier: String,
  final override val name: Name?,
) : Schedule {
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

  /** A copy-constructor that copies over properties from another [Schedule] instance. */
  public constructor(
    schedule: Schedule
  ) : this(
    schedule.namespace,
    schedule.byDays,
    schedule.byMonths,
    schedule.byMonthDays,
    schedule.byMonthWeeks,
    schedule.endDate,
    schedule.endTime,
    schedule.exceptDate,
    schedule.repeatCount,
    schedule.repeatFrequency,
    schedule.scheduleTimezone,
    schedule.startDate,
    schedule.startTime,
    schedule.identifier,
    schedule.name
  )

  /** Returns a concrete [Builder] with the additional, non-[Schedule] properties copied over. */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setNamespace(namespace)
      .addByDays(byDays)
      .addByMonths(byMonths)
      .addByMonthDays(byMonthDays)
      .addByMonthWeeks(byMonthWeeks)
      .setEndDate(endDate)
      .setEndTime(endTime)
      .setExceptDate(exceptDate)
      .setRepeatCount(repeatCount)
      .setRepeatFrequency(repeatFrequency)
      .setScheduleTimezone(scheduleTimezone)
      .setStartDate(startDate)
      .setStartTime(startTime)
      .setIdentifier(identifier)
      .setName(name)

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (namespace != other.namespace) return false
    if (byDays != other.byDays) return false
    if (byMonths != other.byMonths) return false
    if (byMonthDays != other.byMonthDays) return false
    if (byMonthWeeks != other.byMonthWeeks) return false
    if (endDate != other.endDate) return false
    if (endTime != other.endTime) return false
    if (exceptDate != other.exceptDate) return false
    if (repeatCount != other.repeatCount) return false
    if (repeatFrequency != other.repeatFrequency) return false
    if (scheduleTimezone != other.scheduleTimezone) return false
    if (startDate != other.startDate) return false
    if (startTime != other.startTime) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  final override fun hashCode(): Int =
    Objects.hash(
      namespace,
      byDays,
      byMonths,
      byMonthDays,
      byMonthWeeks,
      endDate,
      endTime,
      exceptDate,
      repeatCount,
      repeatFrequency,
      scheduleTimezone,
      startDate,
      startTime,
      identifier,
      name,
      additionalProperties
    )

  final override fun toString(): String {
    val attributes = mutableMapOf<String, String>()
    if (namespace.isNotEmpty()) {
      attributes["namespace"] = namespace
    }
    if (byDays.isNotEmpty()) {
      attributes["byDays"] = byDays.map { it.toString(includeWrapperName = false) }.toString()
    }
    if (byMonths.isNotEmpty()) {
      attributes["byMonths"] = byMonths.toString()
    }
    if (byMonthDays.isNotEmpty()) {
      attributes["byMonthDays"] = byMonthDays.toString()
    }
    if (byMonthWeeks.isNotEmpty()) {
      attributes["byMonthWeeks"] = byMonthWeeks.toString()
    }
    if (endDate != null) {
      attributes["endDate"] = endDate.toString(includeWrapperName = false)
    }
    if (endTime != null) {
      attributes["endTime"] = endTime.toString(includeWrapperName = false)
    }
    if (exceptDate != null) {
      attributes["exceptDate"] = exceptDate.toString(includeWrapperName = false)
    }
    if (repeatCount != null) {
      attributes["repeatCount"] = repeatCount.toString()
    }
    if (repeatFrequency != null) {
      attributes["repeatFrequency"] = repeatFrequency.toString(includeWrapperName = false)
    }
    if (scheduleTimezone != null) {
      attributes["scheduleTimezone"] = scheduleTimezone
    }
    if (startDate != null) {
      attributes["startDate"] = startDate.toString(includeWrapperName = false)
    }
    if (startTime != null) {
      attributes["startTime"] = startTime.toString(includeWrapperName = false)
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
   * An abstract implementation of [Schedule.Builder].
   *
   * Allows for extension like:
   * ```kt
   * @Document(...)
   * class MySchedule :
   *   : AbstractSchedule<
   *     MySchedule,
   *     MySchedule.Builder>(...) {
   *
   *   @Document.BuilderProducer
   *   class Builder
   *   : AbstractSchedule.Builder<
   *       Builder,
   *       MySchedule
   *   >() {
   *
   *     // No need to implement equals(), hashCode(), toString() or build()
   *
   *     private var foo: String? = null
   *     private val bars = mutableListOf<Int>()
   *
   *     override val selfTypeName =
   *       "MySchedule.Builder"
   *
   *     override val additionalProperties: Map<String, Any?>
   *       get() = mapOf("foo" to foo, "bars" to bars)
   *
   *     override fun buildFromSchedule(
   *       schedule: Schedule
   *     ): MySchedule {
   *       return MySchedule(
   *         schedule,
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
   * Also see [AbstractSchedule].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<
    Self : Builder<Self, Built>,
    Built : AbstractSchedule<Built, Self>
  > : Schedule.Builder<Self> {
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

    private val byDays: MutableList<ByDay> = mutableListOf()

    private val byMonths: MutableList<Long> = mutableListOf()

    private val byMonthDays: MutableList<Long> = mutableListOf()

    private val byMonthWeeks: MutableList<Long> = mutableListOf()

    private var endDate: EndDate? = null

    private var endTime: EndTime? = null

    private var exceptDate: ExceptDate? = null

    @get:Suppress("AutoBoxing") private var repeatCount: Long? = null

    private var repeatFrequency: RepeatFrequency? = null

    private var scheduleTimezone: String? = null

    private var startDate: StartDate? = null

    private var startTime: StartTime? = null

    private var identifier: String = ""

    private var name: Name? = null

    /**
     * Builds a concrete [Built] instance, given a built [Schedule].
     *
     * Subclasses should override this method to build a concrete [Built] instance that holds both
     * the [Schedule]-specific properties and the subclass specific [additionalProperties].
     *
     * See the sample code in the documentation of this class for more context.
     */
    @Suppress("BuilderSetStyle") protected abstract fun buildFromSchedule(schedule: Schedule): Built

    final override fun build(): Built =
      buildFromSchedule(
        ScheduleImpl(
          namespace,
          byDays.toList(),
          byMonths.toList(),
          byMonthDays.toList(),
          byMonthWeeks.toList(),
          endDate,
          endTime,
          exceptDate,
          repeatCount,
          repeatFrequency,
          scheduleTimezone,
          startDate,
          startTime,
          identifier,
          name
        )
      )

    final override fun setNamespace(namespace: String): Self {
      this.namespace = namespace
      return this as Self
    }

    final override fun addByDay(byDay: ByDay): Self {
      byDays += byDay
      return this as Self
    }

    final override fun addByDays(values: Iterable<ByDay>): Self {
      byDays += values
      return this as Self
    }

    final override fun clearByDays(): Self {
      byDays.clear()
      return this as Self
    }

    final override fun addByMonth(integer: Long): Self {
      byMonths += integer
      return this as Self
    }

    final override fun addByMonths(values: Iterable<Long>): Self {
      byMonths += values
      return this as Self
    }

    final override fun clearByMonths(): Self {
      byMonths.clear()
      return this as Self
    }

    final override fun addByMonthDay(integer: Long): Self {
      byMonthDays += integer
      return this as Self
    }

    final override fun addByMonthDays(values: Iterable<Long>): Self {
      byMonthDays += values
      return this as Self
    }

    final override fun clearByMonthDays(): Self {
      byMonthDays.clear()
      return this as Self
    }

    final override fun addByMonthWeek(integer: Long): Self {
      byMonthWeeks += integer
      return this as Self
    }

    final override fun addByMonthWeeks(values: Iterable<Long>): Self {
      byMonthWeeks += values
      return this as Self
    }

    final override fun clearByMonthWeeks(): Self {
      byMonthWeeks.clear()
      return this as Self
    }

    final override fun setEndDate(endDate: EndDate?): Self {
      this.endDate = endDate
      return this as Self
    }

    final override fun setEndTime(endTime: EndTime?): Self {
      this.endTime = endTime
      return this as Self
    }

    final override fun setExceptDate(exceptDate: ExceptDate?): Self {
      this.exceptDate = exceptDate
      return this as Self
    }

    final override fun setRepeatCount(@Suppress("AutoBoxing") integer: Long?): Self {
      this.repeatCount = integer
      return this as Self
    }

    final override fun setRepeatFrequency(repeatFrequency: RepeatFrequency?): Self {
      this.repeatFrequency = repeatFrequency
      return this as Self
    }

    final override fun setScheduleTimezone(text: String?): Self {
      this.scheduleTimezone = text
      return this as Self
    }

    final override fun setStartDate(startDate: StartDate?): Self {
      this.startDate = startDate
      return this as Self
    }

    final override fun setStartTime(startTime: StartTime?): Self {
      this.startTime = startTime
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
      if (byDays != other.byDays) return false
      if (byMonths != other.byMonths) return false
      if (byMonthDays != other.byMonthDays) return false
      if (byMonthWeeks != other.byMonthWeeks) return false
      if (endDate != other.endDate) return false
      if (endTime != other.endTime) return false
      if (exceptDate != other.exceptDate) return false
      if (repeatCount != other.repeatCount) return false
      if (repeatFrequency != other.repeatFrequency) return false
      if (scheduleTimezone != other.scheduleTimezone) return false
      if (startDate != other.startDate) return false
      if (startTime != other.startTime) return false
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    final override fun hashCode(): Int =
      Objects.hash(
        namespace,
        byDays,
        byMonths,
        byMonthDays,
        byMonthWeeks,
        endDate,
        endTime,
        exceptDate,
        repeatCount,
        repeatFrequency,
        scheduleTimezone,
        startDate,
        startTime,
        identifier,
        name,
        additionalProperties
      )

    @Suppress("BuilderSetStyle")
    final override fun toString(): String {
      val attributes = mutableMapOf<String, String>()
      if (namespace.isNotEmpty()) {
        attributes["namespace"] = namespace
      }
      if (byDays.isNotEmpty()) {
        attributes["byDays"] = byDays.map { it.toString(includeWrapperName = false) }.toString()
      }
      if (byMonths.isNotEmpty()) {
        attributes["byMonths"] = byMonths.toString()
      }
      if (byMonthDays.isNotEmpty()) {
        attributes["byMonthDays"] = byMonthDays.toString()
      }
      if (byMonthWeeks.isNotEmpty()) {
        attributes["byMonthWeeks"] = byMonthWeeks.toString()
      }
      if (endDate != null) {
        attributes["endDate"] = endDate!!.toString(includeWrapperName = false)
      }
      if (endTime != null) {
        attributes["endTime"] = endTime!!.toString(includeWrapperName = false)
      }
      if (exceptDate != null) {
        attributes["exceptDate"] = exceptDate!!.toString(includeWrapperName = false)
      }
      if (repeatCount != null) {
        attributes["repeatCount"] = repeatCount!!.toString()
      }
      if (repeatFrequency != null) {
        attributes["repeatFrequency"] = repeatFrequency!!.toString(includeWrapperName = false)
      }
      if (scheduleTimezone != null) {
        attributes["scheduleTimezone"] = scheduleTimezone!!
      }
      if (startDate != null) {
        attributes["startDate"] = startDate!!.toString(includeWrapperName = false)
      }
      if (startTime != null) {
        attributes["startTime"] = startTime!!.toString(includeWrapperName = false)
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

private class ScheduleImpl : AbstractSchedule<ScheduleImpl, ScheduleImpl.Builder> {
  protected override val selfTypeName: String
    get() = "Schedule"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    namespace: String,
    byDays: List<ByDay>,
    byMonths: List<Long>,
    byMonthDays: List<Long>,
    byMonthWeeks: List<Long>,
    endDate: EndDate?,
    endTime: EndTime?,
    exceptDate: ExceptDate?,
    repeatCount: Long?,
    repeatFrequency: RepeatFrequency?,
    scheduleTimezone: String?,
    startDate: StartDate?,
    startTime: StartTime?,
    identifier: String,
    name: Name?,
  ) : super(
    namespace,
    byDays,
    byMonths,
    byMonthDays,
    byMonthWeeks,
    endDate,
    endTime,
    exceptDate,
    repeatCount,
    repeatFrequency,
    scheduleTimezone,
    startDate,
    startTime,
    identifier,
    name
  )

  public constructor(schedule: Schedule) : super(schedule)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  public class Builder : AbstractSchedule.Builder<Builder, ScheduleImpl>() {
    protected override val selfTypeName: String
      get() = "Schedule.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromSchedule(schedule: Schedule): ScheduleImpl =
      schedule as? ScheduleImpl ?: ScheduleImpl(schedule)
  }
}
