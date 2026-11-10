plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "net.thunderbird.feature.antispam"
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
