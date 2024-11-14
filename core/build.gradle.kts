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
    type.set("IU")

    plugins.set(listOf(
        "idea.plugin.protoeditor",
//        "org.jetbrains.plugins.yaml:231.8770.3"
    ))
}

dependencies{
    implementation("org.asynchttpclient:async-http-client:2.12.2")
    {
        exclude("org.slf4j")
    }
    implementation("org.json:json:20230227")
    implementation("com.squareup.okhttp:okhttp:2.7.5")
    compileOnly("org.projectlombok:lombok:1.18.34")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("213")
        untilBuild.set("241.*")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

