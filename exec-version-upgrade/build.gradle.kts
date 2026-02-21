plugins {
    java
}

dependencies {
    implementation(project(":exec-core"))
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("com.networknt:config:2.3.3-SNAPSHOT")
    implementation("com.networknt:service:2.3.3-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")
}
