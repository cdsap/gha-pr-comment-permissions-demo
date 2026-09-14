val greeting = providers.gradleProperty("greeting").getOrElse("Hello")

// A stand-in for a real check a PR can break: the kind of task that turns a
// build red without anyone touching the workflow files.
tasks.register("verifyGreeting") {
  doLast {
    if (greeting != "Hello") {
      throw GradleException("Unexpected greeting '$greeting'. Expected 'Hello'.")
    }
  }
}

tasks.register("hello") {
  dependsOn("verifyGreeting")

  // Flip with -Pfail=true to make the build fail, so you can also exercise
  // add-job-summary-as-pr-comment: 'on-failure'.
  val shouldFail = providers.gradleProperty("fail").getOrElse("false").toBoolean()

  doLast {
    if (shouldFail) {
      throw GradleException("Failing on purpose (-Pfail=true).")
    }
    println("$greeting. This build published a Build Scan.")
  }
}
