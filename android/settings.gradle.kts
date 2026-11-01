pluginManagement {
    val flutterSdkPath =
        run {
            val properties = java.util.Properties()
            val localPropertiesFile = java.io.File(settingsDir, "local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { properties.load(it) }
            }
            val propPath = properties.getProperty("flutter.sdk")
            val envPath = System.getenv("FLUTTER_ROOT")
            val resolved = if (propPath != null && java.io.File(propPath).exists()) propPath else envPath
            require(resolved != null) { "flutter.sdk not set in local.properties and FLUTTER_ROOT env var not set" }
            resolved
        }

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}

// Auto-heal legacy Flutter plugins (flutter_bluetooth_serial-0.4.0) before Gradle evaluates their build.gradle
run {
    val depsFile = java.io.File(settingsDir, "../.flutter-plugins-dependencies")
    if (depsFile.exists()) {
        val text = depsFile.readText()
        val pathRegex = Regex(""""path"\s*:\s*"([^"]+)"""")
        for (match in pathRegex.findAll(text)) {
            val rawPath = match.groupValues[1].replace("\\\\", "/").replace("\\/", "/")
            val pluginDir = java.io.File(rawPath)
            if (pluginDir.name.startsWith("flutter_bluetooth_serial")) {
                val btBuildGradle = java.io.File(pluginDir, "android/build.gradle")
                if (btBuildGradle.exists()) {
                    btBuildGradle.writeText(
                        """
                        group = 'io.github.edufolly.flutterbluetoothserial'
                        version = '1.0-SNAPSHOT'

                        buildscript {
                            repositories {
                                google()
                                mavenCentral()
                            }
                            dependencies {
                                classpath 'com.android.tools.build:gradle:8.11.1'
                            }
                        }

                        rootProject.allprojects {
                            repositories {
                                google()
                                mavenCentral()
                            }
                        }

                        apply plugin: 'com.android.library'

                        android {
                            namespace = 'io.github.edufolly.flutterbluetoothserial'
                            compileSdk = 36

                            compileOptions {
                                sourceCompatibility = JavaVersion.VERSION_17
                                targetCompatibility = JavaVersion.VERSION_17
                            }

                            defaultConfig {
                                minSdk = 21
                            }
                        }
                        """.trimIndent() + "\n"
                    )
                }
                val btManifest = java.io.File(pluginDir, "android/src/main/AndroidManifest.xml")
                if (btManifest.exists()) {
                    val cleaned = btManifest.readText().replace(
                        Regex("""\s*package\s*=\s*["'][^"']+["']"""),
                        ""
                    )
                    btManifest.writeText(cleaned)
                }
            }
        }
    }
}

include(":app")
