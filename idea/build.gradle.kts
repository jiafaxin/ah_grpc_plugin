plugins {
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.autohome"

version = rootProject.version

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.1.2")
    type.set("IU")

    plugins.set(listOf(
        "com.intellij.java",
        "org.jetbrains.idea.maven",
        "idea.plugin.protoeditor"
    ))
}

dependencies{
    implementation(project(":core"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("243.*")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

