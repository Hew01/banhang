pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()

        maven{
            setUrl("https://maven.cashfree.com/release")
            content {
                includeGroup("com.cashfree.pg")
            }
        }
    }
}


rootProject.name = "banhangs" // Đặt tên cho dự án
include(":app") // Bao gồm module 'app'
