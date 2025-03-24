plugins {
    kotlin("jvm") version "2.1.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))  // Kotlin standard library
    implementation("com.google.code.gson:gson:2.8.8")  // Gson
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")  // OkHttp Logging Interceptor
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")  // Gson converter
    implementation("com.squareup.retrofit2:retrofit:2.9.0")  // Retrofit
    testImplementation(kotlin("test"))
    implementation("com.github.ajalt.clikt:clikt:4.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}