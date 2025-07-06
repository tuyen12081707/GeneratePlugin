plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "https://github.com/tuyen12081707" // ⚠️ JitPack yêu cầu dạng này
version = "1.0.0"                          // Tốt nhất nên gắn tag version

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
