import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

fun loadLocalProperties(rootDir: java.io.File): Properties {
    val properties = Properties()
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
        try {
            FileInputStream(localPropertiesFile).use { fis ->
                properties.load(fis)
            }
        } catch (e: Exception) {
            println("Warning: Could not load local.properties in chatlibrary: ${e.message}")
        }
    } else {
        println("Warning: local.properties file not found in chatlibrary context at ${localPropertiesFile.absolutePath}")
    }
    return properties
}
val localProps = loadLocalProperties(project.rootDir)
val gprUsername = localProps.getProperty("gpr.user", "")
val gprPassword = localProps.getProperty("gpr.key", "")

android {
    namespace = "com.example.chatlibrary"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.anuaroralov"
            artifactId = "chatlibrary"
            version = libs.versions.chatlibraryVersion.get()

            pom {
                name.set("chatlibrary")
                description.set("A simple Android chat library using WebSockets.")
                url.set("https://github.com/anuaroralov/Lab2")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("anuaroralov")
                        name.set("Anuar Oralov")
                    }
                }
            }
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/anuaroralov/Lab2")
            credentials {
                username = gprUsername
                password = gprPassword
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.logging)
    implementation(libs.okio)
}