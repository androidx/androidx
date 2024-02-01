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
 * A parent type that serves as the umbrella for a number of types that represent the status of a
 * pending task.
 *
 * Prefer one of the subtypes in most contexts to represent a specific type of status e.g.
 * `UnsupportedOperationStatus`.
 *
 * See https://schema.googleapis.com/ExecutionStatus for context.
 *
 * Should not be directly implemented. More properties may be added over time. Instead consider
 * using [Companion.Builder] or see [AbstractExecutionStatus] if you need to extend this type.
 */
@Document(
  name = "bit:ExecutionStatus",
  parent = [Intangible::class],
)
public interface ExecutionStatus : Intangible {
  /** Converts this [ExecutionStatus] to its builder with all the properties copied over. */
  override fun toBuilder(): Builder<*>

  public companion object {
    /** Returns a default implementation of [Builder]. */
    @JvmStatic
    @Document.BuilderProducer
    public fun Builder(): Builder<*> = ExecutionStatusImpl.Builder()
  }

  /**
   * Builder for [ExecutionStatus].
   *
   * Should not be directly implemented. More methods may be added over time. See
   * [AbstractExecutionStatus.Builder] if you need to extend this builder.
   */
  public interface Builder<Self : Builder<Self>> : Intangible.Builder<Self> {
    /** Returns a built [ExecutionStatus]. */
    override fun build(): ExecutionStatus
  }
}

/**
 * An abstract implementation of [ExecutionStatus].
 *
 * Allows for extension like:
 * ```kt
 * @Document(
 *   name = "MyExecutionStatus",
 *   parent = [ExecutionStatus::class],
 * )
 * class MyExecutionStatus internal constructor(
 *   executionStatus: ExecutionStatus,
 *   @Document.StringProperty val foo: String,
 *   @Document.LongProperty val bars: List<Int>,
 * ) : AbstractExecutionStatus<
 *   MyExecutionStatus,
 *   MyExecutionStatus.Builder
 * >(executionStatus) {
 *
 *   // No need to implement equals(), hashCode(), toString() or toBuilder()
 *
 *   override val selfTypeName =
 *     "MyExecutionStatus"
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
 *     AbstractExecutionStatus.Builder<
 *       Builder,
 *       MyExecutionStatus> {...}
 * }
 * ```
 *
 * Also see [AbstractExecutionStatus.Builder].
 */
@Suppress("UNCHECKED_CAST")
public abstract class AbstractExecutionStatus<
  Self : AbstractExecutionStatus<Self, Builder>,
  Builder : AbstractExecutionStatus.Builder<Builder, Self>
>
internal constructor(
  final override val namespace: String,
  final override val identifier: String,
  final override val name: Name?,
) : ExecutionStatus {
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

  /** A copy-constructor that copies over properties from another [ExecutionStatus] instance. */
  public constructor(
    executionStatus: ExecutionStatus
  ) : this(executionStatus.namespace, executionStatus.identifier, executionStatus.name)

  /**
   * Returns a concrete [Builder] with the additional, non-[ExecutionStatus] properties copied over.
   */
  protected abstract fun toBuilderWithAdditionalPropertiesOnly(): Builder

  final override fun toBuilder(): Builder =
    toBuilderWithAdditionalPropertiesOnly()
      .setNamespace(namespace)
      .setIdentifier(identifier)
      .setName(name)

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.java != other::class.java) return false
    other as Self
    if (namespace != other.namespace) return false
    if (identifier != other.identifier) return false
    if (name != other.name) return false
    if (additionalProperties != other.additionalProperties) return false
    return true
  }

  final override fun hashCode(): Int =
    Objects.hash(namespace, identifier, name, additionalProperties)

  final override fun toString(): String {
    val attributes = mutableMapOf<String, String>()
    if (namespace.isNotEmpty()) {
      attributes["namespace"] = namespace
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
   * An abstract implementation of [ExecutionStatus.Builder].
   *
   * Allows for extension like:
   * ```kt
   * @Document(...)
   * class MyExecutionStatus :
   *   : AbstractExecutionStatus<
   *     MyExecutionStatus,
   *     MyExecutionStatus.Builder>(...) {
   *
   *   @Document.BuilderProducer
   *   class Builder
   *   : AbstractExecutionStatus.Builder<
   *       Builder,
   *       MyExecutionStatus
   *   >() {
   *
   *     // No need to implement equals(), hashCode(), toString() or build()
   *
   *     private var foo: String? = null
   *     private val bars = mutableListOf<Int>()
   *
   *     override val selfTypeName =
   *       "MyExecutionStatus.Builder"
   *
   *     override val additionalProperties: Map<String, Any?>
   *       get() = mapOf("foo" to foo, "bars" to bars)
   *
   *     override fun buildFromExecutionStatus(
   *       executionStatus: ExecutionStatus
   *     ): MyExecutionStatus {
   *       return MyExecutionStatus(
   *         executionStatus,
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
   * Also see [AbstractExecutionStatus].
   */
  @Suppress("StaticFinalBuilder")
  public abstract class Builder<
    Self : Builder<Self, Built>,
    Built : AbstractExecutionStatus<Built, Self>
  > : ExecutionStatus.Builder<Self> {
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

    private var identifier: String = ""

    private var name: Name? = null

    /**
     * Builds a concrete [Built] instance, given a built [ExecutionStatus].
     *
     * Subclasses should override this method to build a concrete [Built] instance that holds both
     * the [ExecutionStatus]-specific properties and the subclass specific [additionalProperties].
     *
     * See the sample code in the documentation of this class for more context.
     */
    @Suppress("BuilderSetStyle")
    protected abstract fun buildFromExecutionStatus(executionStatus: ExecutionStatus): Built

    final override fun build(): Built =
      buildFromExecutionStatus(ExecutionStatusImpl(namespace, identifier, name))

    final override fun setNamespace(namespace: String): Self {
      this.namespace = namespace
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
      if (identifier != other.identifier) return false
      if (name != other.name) return false
      if (additionalProperties != other.additionalProperties) return false
      return true
    }

    @Suppress("BuilderSetStyle")
    final override fun hashCode(): Int =
      Objects.hash(namespace, identifier, name, additionalProperties)

    @Suppress("BuilderSetStyle")
    final override fun toString(): String {
      val attributes = mutableMapOf<String, String>()
      if (namespace.isNotEmpty()) {
        attributes["namespace"] = namespace
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

private class ExecutionStatusImpl :
  AbstractExecutionStatus<ExecutionStatusImpl, ExecutionStatusImpl.Builder> {
  protected override val selfTypeName: String
    get() = "ExecutionStatus"

  protected override val additionalProperties: Map<String, Any?>
    get() = emptyMap()

  public constructor(
    namespace: String,
    identifier: String,
    name: Name?,
  ) : super(namespace, identifier, name)

  public constructor(executionStatus: ExecutionStatus) : super(executionStatus)

  protected override fun toBuilderWithAdditionalPropertiesOnly(): Builder = Builder()

  public class Builder : AbstractExecutionStatus.Builder<Builder, ExecutionStatusImpl>() {
    protected override val selfTypeName: String
      get() = "ExecutionStatus.Builder"

    protected override val additionalProperties: Map<String, Any?>
      get() = emptyMap()

    protected override fun buildFromExecutionStatus(
      executionStatus: ExecutionStatus
    ): ExecutionStatusImpl =
      executionStatus as? ExecutionStatusImpl ?: ExecutionStatusImpl(executionStatus)
  }
}
