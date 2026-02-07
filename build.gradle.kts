plugins {
    base
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

allprojects {
    group = "com.networknt.light-bot"
    version = "1.0"
    repositories {
        mavenLocal() // mavenLocal must be added first.
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    tasks.withType<Test> {
        useJUnit()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    
    // Configure all test tasks to not fail when no tests are discovered
    afterEvaluate {
        tasks.withType<Test>().configureEach {
            val testTask = this
            doFirst {
                // Check if there are any test classes with actual test methods
                val hasTests = testTask.testClassesDirs.files.any { dir ->
                    dir.walkTopDown().any { file ->
                        file.name.endsWith(".class")
                    }
                }
                if (!hasTests) {
                    testTask.exclude("**/*")
                }
            }
        }
    }
}
