plugins {
    java
}

dependencies {
    compile("org.slf4j:slf4j-api:1.7.25")
    compile("com.networknt:service:1.5.33")
    compile("com.networknt:client:1.5.33")
    compile("io.undertow:undertow-core:2.0.16.Final")
    compile("com.jayway.jsonpath:json-path:2.3.0")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.8")
    testCompile("junit:junit:4.12")
    testCompile("ch.qos.logback:logback-classic:1.2.3")
}
