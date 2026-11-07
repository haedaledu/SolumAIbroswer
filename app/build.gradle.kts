import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

dependencies {
    implementation("com.google.android.material:material:1.13.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun configuredValue(propertyName: String, environmentName: String, defaultValue: String): String =
    providers.gradleProperty(propertyName).orNull
        ?: localProperties.getProperty(propertyName)
        ?: providers.environmentVariable(environmentName).orNull
        ?: defaultValue

fun javaString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
        .replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.solum.browser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.solum.browser"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "AI_MODEL", javaString(configuredValue("AI_MODEL", "AI_MODEL", "gemini-3.5-flash-lite")))
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
