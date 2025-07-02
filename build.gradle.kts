plugins {
    java
    application
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

group = "net.minestom.jam"
version = "1.0"
application.mainClass = "net.minestom.jam.Main"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom-snapshots:b39badc77b")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}