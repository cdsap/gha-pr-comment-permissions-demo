tasks.register("hello") {
  // Flip with -Pfail=true to make the build fail, so you can also exercise
  // add-job-summary-as-pr-comment: 'on-failure'.
  val shouldFail = providers.gradleProperty("fail").getOrElse("false").toBoolean()

  doLast {
    if (shouldFail) {
      throw GradleException("Failing on purpose (-Pfail=true).")
    }
    println("Hello. This build published a Build Scan.")
  }
}
