plugins {
    application
}

application {
    mainClass.set("com.networknt.bot.cli.Cli")
}

dependencies {
    implementation(project(":exec-core"))
    implementation(project(":exec-develop-build"))
    implementation(project(":exec-version-upgrade"))
    implementation(project(":exec-release-maven"))
    implementation(project(":exec-release-docker"))
    implementation(project(":exec-regex-replace"))
    implementation(project(":exec-create-branch"))
    implementation(project(":exec-merge-branch"))
    implementation(project(":exec-gitrepo-sync"))
    implementation("com.networknt:email-sender:2.3.3-SNAPSHOT")
    implementation("com.beust:jcommander:1.82")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    testImplementation("junit:junit:4.13.2")
}

val fatJar = tasks.register<Jar>("fatJar") {
    archiveBaseName.set("${project.name}-fat")
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "com.networknt.bot.cli.Cli"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
}

tasks.build {
    dependsOn(fatJar)
}

tasks.test {
    useJUnit()
    outputs.upToDateWhen { true }
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Disable the test task failing when no tests are found
gradle.taskGraph.whenReady {
    tasks.withType<Test>().configureEach {
        if (inputs.sourceFiles.isEmpty) {
            enabled = false
        }
    }
}
