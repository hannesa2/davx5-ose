import java.io.FileInputStream
import java.util.Properties

/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

plugins {
    alias(libs.plugins.mikepenz.aboutLibraries)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val keystorePropertiesFile = rootProject.file("signing/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Android configuration
android {
    compileSdk = 35

    defaultConfig {
        applicationId = "at.bitfire.davdroid"

        versionCode = 404060000
        versionName = "4.4.6-alpha.1"

        setProperty("archivesBaseName", "davx5-ose-$versionName")
        buildConfigField("String", "GIT_REPOSITORY", "\"" + getGitOriginRemote() + "\"")

        minSdk = 24        // Android 7.0
        targetSdk = 35     // Android 15

        testInstrumentationRunner = "at.bitfire.davdroid.HiltTestRunner"
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    compileOptions {
        // required for
        // - dnsjava 3.x: java.nio.file.Path
        // - ical4android: time API
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    // Java namespace for our classes (not to be confused with Android package ID)
    namespace = "at.bitfire.davdroid"

    flavorDimensions += "distribution"
    productFlavors {
        create("ose") {
            dimension = "distribution"
            versionNameSuffix = "-ose"
        }
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDir("$projectDir/schemas")
        }
    }

    signingConfigs {
        register("debugCI") {
            storeFile = file("../signing/debug.keystore")
            storePassword = "android"
            keyPassword = "android"
            keyAlias = "androiddebugkey"
        }
        register("release") {
            storeFile = file("../signing/release.keystore")
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }
    buildTypes {
        debug {
            if (System.getenv("CI") == "true") { // Github action
                println("I run on Github and use for debug the RELEASE signing")
                signingConfig = signingConfigs.findByName("release")
            }
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            if (System.getenv("CI_SERVER") != null) { // gitlab
                println("I run on Gitlab and use RELEASE signing")
                signingConfig = signingConfigs.findByName("release")
            } else if (System.getenv("CI") == "true") { // Github
                println("I run on Github and use RELEASE signing")
                signingConfig = signingConfigs.findByName("release")
            } else if (file("../signing/release.keystore").exists()) {
                println("I use RELEASE signing")
                signingConfig = signingConfigs.findByName("release")
            } else {
                println("I run somewhere else and I use debug signing")
                signingConfig = signingConfigs.findByName("debugCI")
            }
            isMinifyEnabled = false
            proguardFiles.addAll(
                listOf(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    file("proguard-rules.pro"),
                ),
            )
        }
    }
    lint {
        disable += arrayOf("GoogleAppIndexingWarning", "ImpliedQuantity", "MissingQuantity", "MissingTranslation", "ExtraTranslation", "RtlEnabled", "RtlHardcoded", "Typos", "NullSafeMutableLiveData")
    }

    packaging {
        resources {
            excludes += arrayOf("META-INF/*.md")
        }
    }

    androidResources {
        generateLocaleConfig = true
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        managedDevices {
            localDevices {
                create("virtual") {
                    device = "Pixel 3"
                    apiLevel = 34
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

aboutLibraries {
    excludeFields = arrayOf("generated")
}

configurations {
    configureEach {
        // exclude modules which are in conflict with system libraries
        exclude(module="commons-logging")
        exclude(group="org.json", module="json")

        // Groovy requires SDK 26+, and it's not required, so exclude it
        exclude(group="org.codehaus.groovy")
    }
}

dependencies {
    implementation("com.github.hannesa2:githubAppUpdate:2.3.1")
    // core
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines)
    coreLibraryDesugaring(libs.android.desugaring)

    // Hilt
    implementation(libs.hilt.android.base)
    ksp(libs.androidx.hilt.compiler)
    ksp(libs.hilt.android.compiler)

    // support libs
    implementation(libs.androidx.activityCompose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.core)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.base)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.security)
    implementation(libs.androidx.work.base)

    // Jetpack Compose
    implementation(libs.compose.accompanist.permissions)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.materialIconsExtended)
    implementation(libs.compose.runtime.livedata)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.toolingPreview)

    // Glance Widgets
    implementation(libs.glance.base)
    implementation(libs.glance.material)

    // Jetpack Room
    implementation(libs.room.runtime)
    implementation(libs.room.base)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)

    // own libraries
    implementation(libs.bitfire.cert4android)
    implementation(libs.bitfire.dav4jvm) {
        exclude(group="junit")
    }
    implementation(libs.bitfire.ical4android)
    implementation(libs.bitfire.vcard4android)

    // third-party libs
    @Suppress("RedundantSuppression")
    implementation(libs.dnsjava)
    implementation(libs.guava)
    implementation(libs.mikepenz.aboutLibraries)
    implementation(libs.nsk90.kstatemachine)
    implementation(libs.okhttp.base)
    implementation(libs.okhttp.brotli)
    implementation(libs.okhttp.logging)
    implementation(libs.openid.appauth)
    implementation(libs.unifiedpush)

    // for tests
    androidTestImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.room.testing)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
}

@JvmOverloads
fun String.runCommand(workingDir: File = File("./")): String {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
        .directory(workingDir)
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    return proc.inputStream.bufferedReader().readText().trim()
}

fun getGitOriginRemote(): String {
    val process = "git remote -v".runCommand()
    val values = process.trim().split("\n")
    val foundLine = values.find {
        it.startsWith("origin") && it.endsWith("(push)")
    }
    return foundLine
        ?.replace("origin", "")
        ?.replace("(push)", "")
        ?.replace(".git", "")
        ?.trim()!!
}
