package androidx.datastore.core

import kotlinx.io.Input
import kotlinx.io.Output

interface Codec<T> {
    /**
     * Value to return if there is no data on disk.
     */
    public val defaultValue: T

    /**
     * Unmarshal object from stream.
     *
     * @param input the Input with the data to deserialize
     */
    public suspend fun readFrom(input: Input): T

    /**
     *  Marshal object to a stream. Closing the provided OutputStream is a no-op.
     *
     *  @param t the data to write to output
     *  @output the Output to serialize data to
     */
    public suspend fun writeTo(t: T, output: Output)

}