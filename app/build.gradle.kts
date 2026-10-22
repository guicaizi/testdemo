plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.testdemo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.testdemo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Dagger2：注解在编译期由 dagger-compiler 生成 DaggerXxx 组件
    implementation(libs.dagger)
    implementation(libs.javax.inject)
    annotationProcessor(libs.dagger.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.androidx.junit)
}