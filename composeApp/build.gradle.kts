import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import kotlin.io.path.div
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.serialization)
    alias(libs.plugins.buildKonfig)
}

val appResourcesPath = rootDir.toPath() / "assets"

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation("com.adamglin:compose-shadow:2.0.4")
            implementation("media.kamel:kamel-image-default:1.0.6")

            implementation("io.github.vinceglb:filekit-core:0.10.0-beta04")
            implementation("io.github.vinceglb:filekit-dialogs:0.10.0-beta04")
            implementation("io.github.vinceglb:filekit-dialogs-compose:0.10.0-beta04")
            implementation("io.github.vinceglb:filekit-coil:0.10.0-beta04")
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

val appVersion = "1.0.0"
buildkonfig {
    packageName = "ru.raspberry.launcher"
     objectName = "AppConfig"
    // exposeObjectWithName = "YourAwesomePublicConfig"

    defaultConfigs {
        buildConfigField(STRING, "version", appVersion)
    }
}
compose.desktop {
    application {
        mainClass = "ru.raspberry.launcher.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Raspberry Launcher"
            packageVersion = appVersion
            vendor = "Raspberry(Pi)Craft"
            macOS {
                iconFile.set(project.layout.projectDirectory.file("icons/icon.icns"))
            }
            windows {
                iconFile.set(project.layout.projectDirectory.file("icons/icon.ico"))
            }
            linux {
                iconFile.set(project.layout.projectDirectory.file("icons/icon.png"))
            }
            appResourcesRootDir = appResourcesPath.toFile()
        }
    }
}
