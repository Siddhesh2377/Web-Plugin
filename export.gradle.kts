/**
 * export.gradle.kts – Optimized Plugin packager tasks
 * -------------------------------------------------------------
 * Apply after your module's `plugins { id("com.android.library") }` block:
 *
 * ```kotlin
 * plugins { id("com.android.library") }
 * apply(from = rootProject.file("export.gradle.kts"))
 * ```
 */

import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.util.Properties

// -------------------------------------------------------------------------
//  Cached helper utilities
// -------------------------------------------------------------------------
val sdkDir: File by lazy {
    val lp = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    lp.getProperty("sdk.dir")?.let { return@lazy File(it) }
    System.getenv("ANDROID_SDK_ROOT")?.let { return@lazy File(it) }
    System.getenv("ANDROID_HOME")?.let { return@lazy File(it) }
    error("Android SDK not found. Set sdk.dir in local.properties or ANDROID_SDK_ROOT.")
}

val buildToolsDir: File by lazy {
    val dir = File(sdkDir, "build-tools")
    dir.listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.name }
        ?: error("No build-tools found in $dir")
}

// -------------------------------------------------------------------------
//  Cached android extension properties via reflection
// -------------------------------------------------------------------------
val androidExt by lazy { extensions.getByName("android") }

val compileSdkInt: Int by lazy {
    val method = androidExt.javaClass.methods.firstOrNull { it.name == "getCompileSdk" }
        ?: error("getCompileSdk not found on android extension; ensure AGP 7.0+")
    (method.invoke(androidExt) as Number).toInt()
}

val namespaceStr: String by lazy {
    androidExt.javaClass.methods.firstOrNull { it.name == "getNamespace" }
        ?.invoke(androidExt) as? String
        ?: "com.dark.${project.name.replace('-', '_')}"
}

// -------------------------------------------------------------------------
//  Common paths (providers for lazy evaluation and caching)
// -------------------------------------------------------------------------
val d8Exe = provider {
    File(buildToolsDir, if (OperatingSystem.current().isWindows) "d8.bat" else "d8")
}

val androidJar = provider {
    File(sdkDir, "platforms/android-$compileSdkInt/android.jar")
}

val manifestSrc = layout.projectDirectory.file("src/main/Manifest.json")
val tmpDir = layout.buildDirectory.dir("tmp/pluginDex")
val dexOutDir = layout.buildDirectory.dir("outputs/pluginDex")
val pluginOutDir = layout.buildDirectory.dir("outputs/plugin")

// -------------------------------------------------------------------------
//  Task configuration
// -------------------------------------------------------------------------
val extractClassesJar = tasks.register<Copy>("extractClassesJar") {
    dependsOn("assembleRelease")

    val aarFile = layout.buildDirectory.file("outputs/aar/${project.name}-release.aar")
    from(zipTree(aarFile))
    include("classes.jar")
    into(tmpDir)

    // Enable incremental build
    outputs.upToDateWhen { tmpDir.get().asFile.resolve("classes.jar").exists() }
}

val makeDex = tasks.register<Exec>("makeDex") {
    dependsOn(extractClassesJar)

    val classesJar = tmpDir.get().asFile.resolve("classes.jar")
    val dexFile = dexOutDir.map { it.asFile.resolve("classes.dex") }

    inputs.file(classesJar)
    inputs.file(androidJar)
    outputs.file(dexFile)

    doFirst {
        val d8 = d8Exe.get()
        val androidJarFile = androidJar.get()

        require(d8.exists()) { "d8 not found: $d8" }
        require(androidJarFile.exists()) { "android.jar not found for compileSdk $compileSdkInt" }

        dexOutDir.get().asFile.mkdirs()

        val kotlinStdlib = configurations.named("releaseCompileClasspath").get()
            .firstOrNull { it.name.startsWith("kotlin-stdlib") }
            ?: error("kotlin-stdlib not found in releaseCompileClasspath")

        commandLine(
            d8.absolutePath,
            "--release",
            "--min-api", "26",
            "--lib", androidJarFile.absolutePath,
            "--output", dexOutDir.get().asFile.absolutePath,
            classesJar.absolutePath,
            kotlinStdlib.absolutePath
        )
    }
}

val packDexJar = tasks.register<Zip>("packDexJar") {
    dependsOn(makeDex)

    archiveFileName.set("plugin.dex.jar")
    destinationDirectory.set(dexOutDir)

    from(dexOutDir) { include("classes.dex") }

    // Enable incremental build
    inputs.file(dexOutDir.map { it.asFile.resolve("classes.dex") })
    outputs.file(dexOutDir.map { it.asFile.resolve("plugin.dex.jar") })
}

val copyManifest = tasks.register<Copy>("copyManifest") {
    from(manifestSrc)
    into(pluginOutDir)

    inputs.file(manifestSrc)
    outputs.file(pluginOutDir.map { it.asFile.resolve("Manifest.json") })

    doFirst {
        check(manifestSrc.asFile.exists()) {
            "Manifest.json not found at ${manifestSrc.asFile}. Place it at src/main/Manifest.json."
        }
    }
}

val packagePluginZip = tasks.register<Zip>("packagePluginZip") {
    dependsOn(packDexJar, copyManifest)

    group = "build"
    description = "Builds a distributable plugin ZIP."

    archiveFileName.set("${project.name}-plugin.zip")
    destinationDirectory.set(pluginOutDir)

    from(dexOutDir) { include("plugin.dex.jar") }
    from(pluginOutDir) { include("Manifest.json") }

    eachFile { path = name }
    includeEmptyDirs = false

    // Enable build cache
    inputs.file(dexOutDir.map { it.asFile.resolve("plugin.dex.jar") })
    inputs.file(pluginOutDir.map { it.asFile.resolve("Manifest.json") })
}

// -------------------------------------------------------------------------
//  Convenience aliases
// -------------------------------------------------------------------------
tasks.register("buildPluginDexJar") {
    group = "build"
    description = "Builds plugin.dex.jar (jar containing classes.dex)."
    dependsOn(packDexJar)
}

tasks.register("buildPluginZip") {
    group = "build"
    description = "Builds the final plugin zip (dex + manifest)."
    dependsOn(packagePluginZip)
}