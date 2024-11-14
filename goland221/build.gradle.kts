plugins {
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.autohome"

version = rootProject.version

repositories {
    mavenCentral()
}


intellij {
    version.set("2023.1.2")
    type.set("GO")

    plugins.set(listOf(
        "org.jetbrains.plugins.go:231.9011.4",
        "idea.plugin.protoeditor:231.8109.91",
        "org.jetbrains.plugins.yaml:231.8770.3"
    ))
}

dependencies{
    implementation(project(":core"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("233.*")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

