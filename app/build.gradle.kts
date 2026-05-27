/*
 *  Copyright (C) 2026 Corvinoo
 *  This file is part of Heion Cloudless Assistant
 *
 * Heion Cloudless Assistant is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Heion Cloudless Assistant is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Heion Cloudless Assistant. If not, see <https://www.gnu.org/licenses/>.
 *
 * This program is subject to additional terms, experimental software disclaimers,
 * and trademark limitations pursuant to Section 7 of the GNU GPLv3.
 * See the README and first-launch notice for details.
 */
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

aboutLibraries {
    // Path to the custom library definitions
    configPath = "config/aboutlibraries"
}

android {
    namespace = "me.robin.heion"
    compileSdk = 35

    defaultConfig {
        applicationId = "me.robin.heion"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                abiFilters("arm64-v8a")
                cppFlags += listOf("-std=c++17", "-O3", "-DNDEBUG")
            }
        }

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            // enables code stripping and obfuscation
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            ndk {
                debugSymbolLevel = "none"
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.activity:activity-compose:1.9.2")

    // Import the Compose BOM to manage library versions
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))

    // Add the specific library providing ui.graphics.Color
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    // AI Edge / LiteRT (Gemma 4, LiteRT LM)
    implementation("com.google.ai.edge.litertlm:litertlm-android:0.11.0")

    // Markdown rendering for responses
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tasklist:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")


    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.22.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("org.robolectric:robolectric:4.12.1")


    // FOSS listing
    implementation("com.mikepenz:aboutlibraries:12.2.4")
    implementation("com.mikepenz:aboutlibraries-core:12.2.4")
    implementation("com.mikepenz:aboutlibraries-compose-m3:12.2.4")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
