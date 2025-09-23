import java.util.Properties
import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.jodli.coffeeshottimer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jodli.coffeeshottimer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.0"

        testInstrumentationRunner = "com.jodli.coffeeshottimer.HiltTestRunner"

        // Enable vector drawable support for older Android versions
        vectorDrawables.useSupportLibrary = true

        // Localization support
        androidResources.localeFilters.addAll(listOf("en", "de"))
    }

    // Signing configuration
    signingConfigs {
        create("release") {
            // Check for CI/CD environment variables first
            val signingKeyBase64 = System.getenv("SIGNING_KEY_BASE64")
            val keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties["keyAlias"]?.toString()
            val keyStorePassword = System.getenv("KEY_STORE_PASSWORD") ?: keystoreProperties["storePassword"]?.toString()
            val keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProperties["keyPassword"]?.toString()
            
            if (signingKeyBase64 != null && keyAlias != null && keyStorePassword != null && keyPassword != null) {
                // CI/CD signing using base64 encoded keystore
                val keystoreFile = File.createTempFile("keystore", ".jks")
                keystoreFile.writeBytes(Base64.getDecoder().decode(signingKeyBase64))
                storeFile = keystoreFile
                storePassword = keyStorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            } else if (keystoreProperties.containsKey("storeFile")) {
                // Local development signing using keystore.properties
                storeFile = file(keystoreProperties["storeFile"].toString())
                storePassword = keystoreProperties["storePassword"].toString()
                this.keyAlias = keystoreProperties["keyAlias"].toString()
                this.keyPassword = keystoreProperties["keyPassword"].toString()
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Use release signing configuration
            signingConfig = signingConfigs.getByName("release")

            // Performance optimizations
            isDebuggable = false
            isJniDebuggable = false
            renderscriptOptimLevel = 3

            // Set application name for release
            manifestPlaceholders["appName"] = "@string/app_name"

            // Packaging options for smaller APK
            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    excludes += "META-INF/DEPENDENCIES"
                    excludes += "META-INF/LICENSE"
                    excludes += "META-INF/LICENSE.txt"
                    excludes += "META-INF/license.txt"
                    excludes += "META-INF/NOTICE"
                    excludes += "META-INF/NOTICE.txt"
                    excludes += "META-INF/notice.txt"
                    excludes += "META-INF/ASL2.0"
                    excludes += "META-INF/*.kotlin_module"
                }
            }
        }

        debug {
            isMinifyEnabled = false
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("dev") {
            dimension = "version"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Coffee Shot Timer DEV")
        }
        create("prod") {
            dimension = "version"
            resValue("string", "app_name", "Coffee Shot Timer")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    // kotlinOptions deprecated: migrate to compilerOptions DSL below
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Room schema export configuration now via KSP

    // Test configuration
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.jvmArgs("-Xmx4096m", "-XX:MaxMetaspaceSize=1024m", "-XX:+UseG1GC")
                it.systemProperty("robolectric.enabledSdks", "28,29,30,31,32,33,34")
                it.maxHeapSize = "4096m"
            }
        }
    }

    applicationVariants.all {
        val flavor = this.flavorName
        val versionName = this.versionName
        
        this.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl

            // Clean version name by removing flavor suffixes
            val cleanVersion = versionName?.replace("-$flavor", "") ?: "0.0.0"

            when (flavor) {
                "dev" -> {
                    output.outputFileName = "coffee-shot-timer-dev-${cleanVersion}.apk"
                }
                "prod" -> {
                    output.outputFileName = "coffee-shot-timer-${cleanVersion}.apk"
                }
            }
        }
    }

    // KSP arguments
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // Configure AAB naming for bundle tasks
    tasks.whenTaskAdded {
        if (name.startsWith("bundle") && name.contains("Release")) {
            doLast {
                val flavor = when {
                    name.contains("Dev") -> "dev"
                    name.contains("Prod") -> "prod"
                    else -> "unknown"
                }
                val bundleDir = layout.buildDirectory.dir("outputs/bundle/${flavor}Release").get().getAsFile()
                if (bundleDir.exists()) {
                    bundleDir.listFiles()?.forEach { file ->
                        if (file.name.endsWith(".aab")) {
                            // Get version from defaultConfig, clean it up
                            val versionName = android.defaultConfig.versionName ?: "0.0.0"
                            val cleanVersion = versionName.replace("-$flavor", "")
                            
                            val newName = when (flavor) {
                                "dev" -> "coffee-shot-timer-dev-${cleanVersion}.aab"
                                "prod" -> "coffee-shot-timer-${cleanVersion}.aab"
                                else -> file.name
                            }
                            val newFile = File(bundleDir, newName)
                            if (file.renameTo(newFile)) {
                                println("Renamed AAB: ${file.name} -> $newName")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Migrate Kotlin compiler settings to the new compilerOptions DSL
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Media/EXIF support
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt dependencies
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil for image loading
    implementation(libs.coil.compose)
    implementation(libs.coil.base)

    // Core library desugaring for Java 8 time APIs
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Pull to refresh
    implementation("androidx.compose.material:material")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.rules)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    
    // Additional Android test dependencies
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:truth:1.5.0")
    androidTestImplementation("org.jetbrains.kotlin:kotlin-test:${libs.versions.kotlin.get()}")
    
    // Hilt testing dependencies
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    
    // Room testing dependencies
    androidTestImplementation(libs.androidx.room.testing)
    
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Detekt formatting plugin for ktlint rules
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detekt.get()}")
}

// Detekt configuration
detekt {
    // Point to the configuration file
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    
    // Enable baseline for suppressing existing issues
    baseline = file("$rootDir/config/detekt/baseline.xml")
    
    // Build upon the default detekt configuration
    buildUponDefaultConfig = true
    
    // Enable all rules by default
    allRules = false
    
    // Ignore test source sets
    ignoreFailures = false
}

// Configure all detekt tasks to use JVM target 11 and custom reports
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // Ensure every Detekt task uses the shared baseline and config, even on CI
    baseline.set(rootProject.layout.projectDirectory.file("config/detekt/baseline.xml"))
    config.setFrom(rootProject.layout.projectDirectory.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true

    // Set JVM target for compatibility
    jvmTarget = "11"
    
    // Performance optimizations for CI
    parallel = true
    setIncludes(listOf("**/*.kt", "**/*.kts"))
    setExcludes(listOf("**/build/**", "**/.*"))
    
    // Configure reports
    reports {
        // Enable XML report for GitHub Actions integration
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.xml"))
        
        // Enable HTML report for detailed developer review
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.html"))
        
        // Enable SARIF report for GitHub Security tab integration
        sarif.required.set(true)
        sarif.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt.sarif"))
        
        // Disable text and markdown reports to reduce noise
        txt.required.set(false)
        md.required.set(false)
    }
}
