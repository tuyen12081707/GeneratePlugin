plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.panda"
version = "1.0"                 // Tốt nhất nên gắn tag version

repositories {
    google()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("generateDimensPlugin") {
            id = "com.panda.generatedimens"
            implementationClass = "com.panda.dimens_plugin.GenerateDimensPlugin"
        }
    }
}
