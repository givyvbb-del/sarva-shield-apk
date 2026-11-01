allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)

    val manifestFile = file("${project.projectDir}/src/main/AndroidManifest.xml")
    var extractedPackage: String? = null
    if (manifestFile.exists()) {
        val manifestContent = manifestFile.readText()
        val pkgRegex = Regex("""package\s*=\s*["']([^"']+)["']""")
        val match = pkgRegex.find(manifestContent)
        if (match != null) {
            extractedPackage = match.groupValues[1]
            val cleanedManifest = manifestContent.replace(pkgRegex, "")
            if (cleanedManifest != manifestContent) {
                manifestFile.writeText(cleanedManifest)
            }
        }
    }

    afterEvaluate {
        if (project.hasProperty("android")) {
            val androidExt = project.extensions.findByName("android")
            if (androidExt is com.android.build.gradle.BaseExtension) {
                androidExt.compileSdkVersion(36)
                androidExt.defaultConfig.minSdkVersion(21)
                androidExt.compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
            if (androidExt is com.android.build.gradle.LibraryExtension) {
                if (androidExt.namespace == null) {
                    androidExt.namespace = extractedPackage ?: when (project.name) {
                        "flutter_bluetooth_serial" -> "io.github.edufolly.flutterbluetoothserial"
                        else -> "com.example.${project.name.replace('-', '_')}"
                    }
                }
            }
        }
    }
}

subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
