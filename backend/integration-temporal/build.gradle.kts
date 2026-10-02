plugins {
    kotlin("jvm")
    id("io.spring.dependency-management")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5")
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":integration-kafka"))
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("io.temporal:temporal-sdk:1.25.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework:spring-context")
}
