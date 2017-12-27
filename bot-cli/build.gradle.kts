plugins {
    application
}

application {
    mainClassName = "com.networknt.bot.cli.Cli"
}

dependencies {
    compile(project(":exec-core"))
    compile(project(":exec-develop"))
    compile("com.beust:jcommander:1.72")
}
