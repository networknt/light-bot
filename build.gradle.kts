plugins {
    base
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

allprojects {
    group = "com.networknt.light-bot"
    version = "1.0"
    repositories {
        mavenLocal() // mavenLocal must be added first.
        jcenter()
    }
}

dependencies {
    // Make the root project archives configuration depend on every sub-project
    subprojects.forEach {
        archives(it)
    }
}

