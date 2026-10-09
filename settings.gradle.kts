pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
        maven {
            url = uri("https://maven.aliyun.com/nexus/content/groups/public/")
            isAllowInsecureProtocol = true
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven {
            url = uri("https://maven.aliyun.com/nexus/content/groups/public/")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "BillReconciler"
include(":app")
