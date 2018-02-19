plugins {
    application
}

application {
    mainClassName = "com.networknt.bot.cli.Cli"
}

dependencies {
    compile(project(":exec-core"))
    compile(project(":exec-develop"))
    compile(project(":exec-version"))
    compile(project(":exec-release"))
    compile(project(":exec-dockerhub"))
    compile(project(":exec-upgrade"))
    compile(project(":exec-regex-replace"))
    compile("com.networknt:email:1.5.6")
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
    from(configurations.runtime.map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks["jar"] as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}
