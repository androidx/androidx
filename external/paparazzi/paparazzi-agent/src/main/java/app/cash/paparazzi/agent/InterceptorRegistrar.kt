package app.cash.paparazzi.agent

import net.bytebuddy.ByteBuddy
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.matcher.ElementMatchers

object InterceptorRegistrar {
  private val byteBuddy = ByteBuddy()
  private val methodInterceptors = mutableListOf<() -> Unit>()

  fun addMethodInterceptor(
    receiver: Class<*>,
    methodName: String,
    interceptor: Class<*>
  ) = addMethodInterceptors(receiver, setOf(methodName to interceptor))

  fun addMethodInterceptors(
    receiver: Class<*>,
    methodNamesToInterceptors: Set<Pair<String, Class<*>>>
  ) {
    methodInterceptors += {
      var builder = byteBuddy
        .redefine(receiver)

      methodNamesToInterceptors.forEach {
        builder = builder
          .method(ElementMatchers.named(it.first))
          .intercept(MethodDelegation.to(it.second))
      }

      builder
        .make()
        .load(receiver.classLoader, ClassReloadingStrategy.fromInstalledAgent())
    }
  }

  fun registerMethodInterceptors() {
    methodInterceptors.forEach { it.invoke() }
  }

  fun clearMethodInterceptors() {
    methodInterceptors.clear()
  }
}
