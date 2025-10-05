import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.yaml.snakeyaml.Yaml
import kotlin.io.path.div

val tokensFile = rootProject.file("tokens.yaml")
val tokenMap = if (tokensFile.exists()) {
    val yaml = Yaml()
    @Suppress("UNCHECKED_CAST")
    yaml.load<Map<String, Map<String, String>>>(tokensFile.readText())
} else {
    emptyMap()
}
val inputKotlinFiles = tokenMap.mapNotNull { (_, entry) ->
    val filePath = entry["file"]
    filePath?.substringAfter("src/desktopMain/kotlin/")
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.yaml:snakeyaml:2.2") // версия может быть другой
    }
}

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
    repositories {
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir(
                layout.buildDirectory.dir("generated/tokenPatched")
            )
            inputKotlinFiles.forEach { kotlin.exclude(it) }
        }
        
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
            implementation(libs.bundles.jna)
            implementation("com.adamglin:compose-shadow:2.0.4")
            implementation("media.kamel:kamel-image-default:1.0.6")

            implementation("io.github.vinceglb:filekit-core:0.10.0-beta04")
            implementation("io.github.vinceglb:filekit-dialogs:0.10.0-beta04")
            implementation("io.github.vinceglb:filekit-dialogs-compose:0.10.0-beta04")
            implementation("io.github.vinceglb:filekit-coil:0.10.0-beta04")
            implementation(libs.kotlinx.datetime)
            implementation(libs.benwoodworth.knbt)

            // https://mvnrepository.com/artifact/com.github.JnCrMx/discord-game-sdk4j
            implementation("com.github.JnCrMx:discord-game-sdk4j:v1.0.0")
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

val appVersion = "1.2.0"
buildkonfig {
    packageName = "ru.raspberry.launcher"
    objectName = "AppConfig"

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
        buildTypes.release.proguard {
            isEnabled = false
        }
    }
}

@CacheableTask
abstract class PatchDesktopSources : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokensFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Input
    abstract val rootDirPath: Property<String>

    @TaskAction
    fun run() {
        val yaml = Yaml()
        val tokens = yaml.load<Map<String, Map<String, String>>>(tokensFile.get().asFile.reader())
        val rootDir = File(rootDirPath.get())
        tokens.forEach { (_, entry) ->
            println("Processing token: $name")
            val relativePath = entry["file"] ?: error("Missing 'file'")
            val from = entry["from"] ?: error("Missing 'from'")
            val to = entry["to"] ?: error("Missing 'to'")
            val mode = entry["mode"] ?: "replace"
            val pattern = entry["pattern"] ?: "simple"

            val srcFile = File(rootDir, relativePath)
            if (!srcFile.exists()) error("File not found: $relativePath")

            // Вырезаем src/desktopMain/..., сохраняем относительный путь
            val prefix = "composeApp/src/desktopMain/"
            val subPath = relativePath.substringAfter(prefix)
            val targetFile = File(outputDir.get().asFile, subPath)

            targetFile.parentFile.mkdirs()
            var text = srcFile.readText()
            val replacer = when (pattern) {
                "simple" -> Regex(Regex.escape(from))
                "regex" -> Regex(from)
                else -> error("Unknown pattern type: $pattern")
            }
            when (mode) {
                "replace" ->
                    text = text.replace(replacer, to)
                else -> error("Unknown mode: $mode")
            }
            targetFile.writeText(text)

            logger.lifecycle("Token $name: patched ${srcFile.path} → ${targetFile.path}")
        }
    }
}

val patchDesktopSources by tasks.registering(PatchDesktopSources::class) {
    tokensFile.set(rootProject.file("tokens.yaml"))
    inputFiles.setFrom(inputKotlinFiles)
    outputDir.set(layout.buildDirectory.dir("generated/tokenPatched"))
    rootDirPath.set(rootProject.rootDir.absolutePath)
}

tasks.named("compileKotlinDesktop") {
    dependsOn(patchDesktopSources)
}

