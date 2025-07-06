plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.tuyen12081707"
version = "1.0"                 // Tốt nhất nên gắn tag version



gradlePlugin {
    plugins {
        create("generateDimensPlugin") {
            id = "com.panda.generatedimens"
            implementationClass = "com.panda.dimens_plugin.GenerateDimensPlugin"
        }
    }
}
