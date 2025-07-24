plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "com.github.tuyen12081707"
version = "1.0.0"

gradlePlugin {
    plugins {
        create("generateDimensPlugin") {
            id = "com.panda.generatedimens"
            implementationClass = "com.panda.dimens_plugin.GenerateDimensPlugin"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("plugin") {
            from(components["java"])
            groupId = "com.github.tuyen12081707"
            artifactId = "dimens-plugin"
            version = "1.0.0"
        }
    }
}
