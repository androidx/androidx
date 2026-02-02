package arrow.core;

public sealed class Either<out L, out R> {
    public data class Left<out L>(val value: L) : Either<L, Nothing>()
    public data class Right<out R>(val value: R) : Either<Nothing, R>()
}