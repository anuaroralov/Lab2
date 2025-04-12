import java.io.FileInputStream
import java.util.Properties

fun loadLocalProperties(rootDir: java.io.File): Properties {
    val properties = Properties()
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
        try {
            FileInputStream(localPropertiesFile).use { fis ->
                properties.load(fis)
            }
        } catch (e: Exception) {
            println("Warning: Could not load local.properties file: ${e.message}")
        }
    } else {
        println("Warning: local.properties file not found at ${localPropertiesFile.absolutePath}")
    }
    return properties
}

val localProperties = loadLocalProperties(rootDir)
val gprUsername = localProperties.getProperty("gpr.user", "")
val gprPassword = localProperties.getProperty("gpr.key", "")

val githubPackagesUrl = "https://maven.pkg.github.com/Yerassyl1234/AndroidLab2"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Lab2"
include(":app")
