plugins {
    java
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("com.networknt:service:2.3.3-SNAPSHOT")
    implementation("com.networknt:client:2.3.3-SNAPSHOT")
    implementation("io.undertow:undertow-core:2.3.18.Final")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("org.zeroturnaround:zt-exec:1.12")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}
