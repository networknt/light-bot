plugins {
    application
}

application {
    mainClassName = "com.networknt.bot.cli.Cli"
}

dependencies {
    compile(project(":exec-core"))
    compile(project(":exec-develop-build"))
    compile(project(":exec-version-upgrade"))
    compile(project(":exec-release-maven"))
    compile(project(":exec-release-docker"))
    compile(project(":exec-regex-replace"))
    compile(project(":exec-create-branch"))
    compile(project(":exec-merge-branch"))
    compile(project(":exec-gitrepo-sync"))
    compile("com.networknt:email-sender:1.6.4")
    compile("com.beust:jcommander:1.72")
    compile("ch.qos.logback:logback-classic:1.2.3")
}

val fatJar = task("fatJar", type = Jar::class) {
    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Title"] = "Gradle Jar File Example"
        attributes["Implementation-Version"] = version
        attributes["Main-Class"] = "com.networknt.bot.cli.Cli"
    }
    from(configurations.runtime.get().map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks["jar"] as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
