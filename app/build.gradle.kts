import java.util.Properties
import java.io.FileInputStream
import java.io.FileOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
}

fun getVersionNameWithSuffix(): String {
    val propFile = file("version.properties")
    val props = Properties()
    if (propFile.exists()) {
        FileInputStream(propFile).use { props.load(it) }
    } else {
        props.setProperty("build_letter_index", "0")
        props.setProperty("base_version", "1.0.0")
    }
    
    val base = props.getProperty("base_version", "1.0.0")
    val index = props.getProperty("build_letter_index", "0").toInt()
    
    val suffix = StringBuilder()
    var n = index
    do {
        suffix.append(('a'.toInt() + (n % 26)).toChar())
        n = (n / 26) - 1
    } while (n >= 0)
    
    return "$base${suffix.reverse()}"
}

fun getVersionCodeValue(): Int {
    val propFile = file("version.properties")
    val props = Properties()
    if (propFile.exists()) {
        FileInputStream(propFile).use { props.load(it) }
        return props.getProperty("build_letter_index", "0").toInt() + 1
    }
    return 1
}

fun incrementVersionIndex() {
    val propFile = file("version.properties")
    val props = Properties()
    if (propFile.exists()) {
        FileInputStream(propFile).use { props.load(it) }
        val index = props.getProperty("build_letter_index", "0").toInt()
        props.setProperty("build_letter_index", (index + 1).toString())
        FileOutputStream(propFile).use { props.store(it, null) }
    }
}

android {
    namespace = "dev.abu.material3"
    compileSdk = 34

    val currentVersionName = getVersionNameWithSuffix()
    val currentVersionCode = getVersionCodeValue()

    defaultConfig {
        applicationId = "dev.abu.material3"
        minSdk = 24
        targetSdk = 34
        versionCode = currentVersionCode
        
        val customVersion = project.findProperty("customVersionName") as? String
        versionName = customVersion ?: currentVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "Material3-${variant.versionName}-${variant.name}.apk"
        }
    }

    // Increment index after build
    gradle.taskGraph.whenReady {
        if (hasTask(":app:assembleDebug") || hasTask(":app:assembleRelease")) {
            incrementVersionIndex()
        }
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.3.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("io.socket:socket.io-client:2.1.0")

    // Media3 (ExoPlayer)
    val media3Version = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.coil-kt:coil-compose:2.4.0")

    // InnerTube / OuterTune Dependencies
    val ktorVersion = "2.3.12" // Compatible with Kotlin 1.9.24
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.24.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("androidx.webkit:webkit:1.11.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.03"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
