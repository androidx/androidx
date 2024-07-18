package app.cash.paparazzi.agent

import net.bytebuddy.agent.ByteBuddyAgent
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class AgentTestRule : TestRule {
  override fun apply(
    base: Statement,
    description: Description
  ) = object : Statement() {
    override fun evaluate() {
      ByteBuddyAgent.install()
      InterceptorRegistrar.registerMethodInterceptors()
      // interceptors are statically retained until test process finishes, so no need to cleanup
      base.evaluate()
    }
  }
}
