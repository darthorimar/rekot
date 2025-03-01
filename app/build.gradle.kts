buildscript {
    repositories {
        mavenCentral()
        google()
    }
}

plugins {
    kotlin("jvm") version "2.1.0"
    application
    java
    id("com.ncorti.ktfmt.gradle") version "0.21.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
    maven("https://www.jetbrains.com/intellij-repository/releases")
    mavenCentral()
    mavenLocal()
}

val kotlinVersion = "2.1.255-SNAPSHOT"
val intellijVersion = "233.13135.128"

dependencies {
    implementation(project(":config"))
    implementation("com.googlecode.lanterna:lanterna:3.1.3")
    implementation("io.insert-koin:koin-core:4.0.0")

    listOf(
            "com.jetbrains.intellij.platform:util-rt",
            "com.jetbrains.intellij.platform:util-class-loader",
            "com.jetbrains.intellij.platform:util-text-matching",
            "com.jetbrains.intellij.platform:util",
            "com.jetbrains.intellij.platform:util-base",
            "com.jetbrains.intellij.platform:util-xml-dom",
            "com.jetbrains.intellij.platform:core",
            "com.jetbrains.intellij.platform:core-impl",
            "com.jetbrains.intellij.platform:extensions",
            "com.jetbrains.intellij.platform:diagnostic",
            "com.jetbrains.intellij.java:java-frontback-psi",
            "com.jetbrains.intellij.java:java-frontback-psi-impl",
            "com.jetbrains.intellij.java:java-psi",
            "com.jetbrains.intellij.java:java-psi-impl",
        )
        .forEach {
            implementation("$it:$intellijVersion") { isTransitive = false }
            implementation("$it:$intellijVersion:sources") { isTransitive = false }
        }

    listOf(
            "org.jetbrains.kotlin:analysis-api-k2-for-ide",
            "org.jetbrains.kotlin:analysis-api-for-ide",
            "org.jetbrains.kotlin:low-level-api-fir-for-ide",
            "org.jetbrains.kotlin:analysis-api-platform-interface-for-ide",
            "org.jetbrains.kotlin:symbol-light-classes-for-ide",
            "org.jetbrains.kotlin:analysis-api-standalone-for-ide",
            "org.jetbrains.kotlin:analysis-api-impl-base-for-ide",
            "org.jetbrains.kotlin:kotlin-compiler-common-for-ide",
            "org.jetbrains.kotlin:kotlin-compiler-fir-for-ide",
            "org.jetbrains.kotlin:kotlin-compiler-fe10-for-ide",
            "org.jetbrains.kotlin:kotlin-compiler-ir-for-ide",
            "org.jetbrains.kotlin:kotlin-compiler-cli-for-ide",
            "org.jetbrains.kotlin:kotlin-scripting-common",
            "org.jetbrains.kotlin:kotlin-scripting-jvm",
            "org.jetbrains.kotlin:kotlin-scripting-compiler-impl",
            "org.jetbrains.kotlin:kotlin-script-runtime",
            "org.jetbrains.kotlin:kotlin-scripting-compiler",
        )
        .forEach {
            implementation("$it:$kotlinVersion") { isTransitive = false }
            implementation("$it:$kotlinVersion:sources") { isTransitive = false }
        }

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.google.guava:guava:33.2.0-jre")
    implementation("one.util:streamex:0.7.2")
    implementation("org.jetbrains.intellij.deps:asm-all:9.0")
    implementation("org.codehaus.woodstox:stax2-api:4.2.1") { isTransitive = false }
    implementation("com.fasterxml:aalto-xml:1.3.0") { isTransitive = false }
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("org.jetbrains.intellij.deps.jna:jna:5.9.0.26") { isTransitive = false }
    implementation("org.jetbrains.intellij.deps.jna:jna-platform:5.9.0.26") { isTransitive = false }
    implementation("org.jetbrains.intellij.deps:trove4j:1.0.20200330") { isTransitive = false }
    implementation("org.jetbrains.intellij.deps:log4j:1.2.17.2") { isTransitive = false }
    implementation("org.jetbrains.intellij.deps:jdom:2.0.6") { isTransitive = false }
    implementation("io.javaslang:javaslang:2.0.6")
    implementation("javax.inject:javax.inject:1")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.10")
    implementation("org.lz4:lz4-java:1.7.1") { isTransitive = false }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4") { isTransitive = false }
    implementation("org.jetbrains.intellij.deps.fastutil:intellij-deps-fastutil:8.5.11-18") { isTransitive = false }
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("org.apache.commons:commons-text:1.13.0")

    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest.extensions:kotest-extensions-koin:1.3.0")
    testImplementation("io.insert-koin:koin-test:4.0.0")
}

tasks.shadowJar {
    archiveBaseName.set("rekot")
    archiveVersion.set("0.1.2")
    archiveClassifier.set("")
    isZip64 = true
}

application { mainClass = "me.darthorimar.rekot.MainKt" }

kotlin {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.analysis.api.KaPlatformInterface")
        optIn.add("org.jetbrains.kotlin.analysis.api.KaImplementationDetail")
        optIn.add("org.jetbrains.kotlin.analysis.api.KaExperimentalApi")
        optIn.add("org.jetbrains.kotlin.analysis.api.KaIdeApi")
        optIn.add("kotlin.ExperimentalUnsignedTypes")
        optIn.add("kotlin.contracts.ExperimentalContracts")
        freeCompilerArgs.add("-Xcontext-receivers")
        freeCompilerArgs.add("-Xwhen-guards")
    }
}

sourceSets {
    main { kotlin.setSrcDirs(listOf("main")) }
    test { kotlin.setSrcDirs(listOf("test")) }
}

val buildProd by tasks.creating {
    dependsOn("shadowJar")
}


ktfmt {
    kotlinLangStyle()
    manageTrailingCommas = false
    maxWidth = 120
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
}
