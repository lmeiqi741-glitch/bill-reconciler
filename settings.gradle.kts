pluginManagement {
    repositories {
        maven {
            url = uri("http://maven.aliyun.com/nexus/content/groups/public/")
            isAllowInsecureProtocol = true
        }
        maven {
            url = uri("http://maven.aliyun.com/nexus/content/repositories/google")
            isAllowInsecureProtocol = true
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("http://maven.aliyun.com/nexus/content/groups/public/")
            isAllowInsecureProtocol = true
        }
        maven {
            url = uri("http://maven.aliyun.com/nexus/content/repositories/google")
            isAllowInsecureProtocol = true
        }
    }
}

rootProject.name = "BillReconciler"
include(":app")
