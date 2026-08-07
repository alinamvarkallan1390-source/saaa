plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }
android { namespace="com.linkbridge.watch"; compileSdk=35
 defaultConfig { applicationId="com.linkbridge.watch"; minSdk=29; targetSdk=35; versionCode=1; versionName="1.0.0" }
 signingConfigs { create("release") { storeFile=file(System.getenv("KEYSTORE_PATH") ?: "../release.jks"); storePassword=System.getenv("KEYSTORE_PASSWORD"); keyAlias=System.getenv("KEY_ALIAS"); keyPassword=System.getenv("KEY_PASSWORD") } }
 buildTypes { getByName("release") { isMinifyEnabled=true; isShrinkResources=true; signingConfig=signingConfigs.getByName("release"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro") } }
 buildFeatures { compose=true }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies { implementation(project(":core")); implementation(platform(libs.compose.bom)); implementation(libs.compose.ui); implementation(libs.compose.preview); debugImplementation(libs.compose.tooling); implementation(libs.material3); implementation(libs.icons); implementation(libs.activity.compose); implementation(libs.lifecycle.runtime); implementation(libs.lifecycle.compose); implementation(libs.viewmodel.compose); implementation(libs.hilt.android); ksp(libs.hilt.compiler); implementation(libs.work.runtime) }
