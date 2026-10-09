pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/nexus/content/groups/public/")
            isAllowInsecureProtocol = true
        }
        maven {
            url = uri("https://maven.aliyun.com/nexus/content/repositories/google")
            isAllowInsecureProtocol = true
        }
        maven {
            url = uri("https://dl.google.com/dl/android/maven2/")
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("https://maven.aliyun.com/nexus/content/groups/public/")
            isAllowInsecureProtocol = true
        }
        maven {
            url = uri("https://maven.aliyun.com/nexus/content/repositories/google")
            isAllowInsecureProtocol = true
        }
        maven {
            url = uri("https://dl.google.com/dl/android/maven2/")
        }
    }
}

rootProject.name = "BillReconciler"
include(":app")
