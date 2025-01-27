plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    id("com.ncorti.ktfmt.gradle") version "0.21.0"
}

dependencies {
}

sourceSets {
    main { kotlin.setSrcDirs(listOf("main")) }
    test { kotlin.setSrcDirs(listOf("test")) }
}

ktfmt {
    kotlinLangStyle()
    manageTrailingCommas = false
    maxWidth = 120
}
