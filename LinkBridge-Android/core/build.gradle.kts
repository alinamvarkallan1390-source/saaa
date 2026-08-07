plugins { alias(libs.plugins.android.library); alias(libs.plugins.kotlin.android); alias(libs.plugins.ksp); alias(libs.plugins.hilt) }
android { namespace="com.linkbridge.core"; compileSdk=35
 defaultConfig { minSdk=26; consumerProguardFiles("consumer-rules.pro") }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }
}
dependencies { implementation(libs.androidx.core); implementation(libs.coroutines.android); implementation(libs.serialization.json); implementation(libs.hilt.android); ksp(libs.hilt.compiler); implementation(libs.room.runtime); implementation(libs.room.ktx); ksp(libs.room.compiler) }
