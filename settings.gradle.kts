plugins {
  id("com.gradle.develocity") version "4.5.1"
}

rootProject.name = "gha-pr-comment-permissions-demo"

develocity {
  buildScan {
    // Publish to the free scans.gradle.com service so this repro needs no secrets
    // and works out of the box on a fork.
    termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
    termsOfUseAgree = "yes"

    val isCI = providers.environmentVariable("CI").isPresent
    uploadInBackground = !isCI
    publishing.onlyIf { true }
  }
}
