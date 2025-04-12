pluginManagement {
    val properties = java.util.Properties()
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
        try {
            java.io.FileInputStream(localPropertiesFile).use { fis ->
                properties.load(fis)
            }
        } catch (e: Exception) {
            println("Warning: Could not load local.properties file: ${e.message}")
        }
    } else {
        println("Warning: local.properties file not found at ${localPropertiesFile.absolutePath}")
    }

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
        maven {
            name = "GitHubPackages_Plugins"
            url = uri("https://maven.pkg.github.com/anuaroralov/Lab2")
            credentials {
                username = properties.getProperty("gpr.user", "")
                password = properties.getProperty("gpr.key", "")
            }
        }
    }
}
dependencyResolutionManagement {
    val properties = java.util.Properties()
    val localPropertiesFile = rootDir.resolve("local.properties")
    if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
        try {
            java.io.FileInputStream(localPropertiesFile).use { fis ->
                properties.load(fis)
            }
        } catch (e: Exception) {
            println("Warning: Could not load local.properties file: ${e.message}")
        }
    } else {
        println("Warning: local.properties file not found at ${localPropertiesFile.absolutePath}")
    }

    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            name = "GitHubPackages_Deps"
            url = uri("https://maven.pkg.github.com/anuaroralov/Lab2")
            credentials {
                username = properties.getProperty("gpr.user", "")
                password = properties.getProperty("gpr.key", "")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "Lab2"
include(":app")
include(":chatlibrary")
