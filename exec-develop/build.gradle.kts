plugins {
    java
}

dependencies {
    compile(project(":exec-core"))
    compile("org.slf4j:slf4j-api:1.7.25")
    compile("com.networknt:config:1.5.5")
    compile("com.networknt:service:1.5.5")
    compile("com.networknt:client:1.5.5")
    compile("com.networknt:email:1.5.5")
    compile("io.undertow:undertow-core:1.4.20.Final")
    testCompile("junit:junit:4.12")
    testCompile("ch.qos.logback:logback-classic:1.2.3")
}
