plugins {
	java
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "bpmn.pedido.app"
version = "0.0.1-SNAPSHOT"
description = "consultar pedido"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

var openFeignVersion = "13.6"
var logbackVersion = "8.0"
var jacksonBom2Version = "2.21.1"
var jacksonAnnotationsVersion = "2.21"
var jacksonBom3Version = "3.1.0"

dependencyManagement {
	dependencies {
		dependency("com.fasterxml.jackson.core:jackson-core:${jacksonBom2Version}")
		dependency("com.fasterxml.jackson.core:jackson-databind:${jacksonBom2Version}")
		dependency("com.fasterxml.jackson.core:jackson-annotations:${jacksonAnnotationsVersion}")
		dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:${jacksonBom2Version}")
		dependency("tools.jackson.core:jackson-core:${jacksonBom3Version}")
		dependency("tools.jackson.core:jackson-databind:${jacksonBom3Version}")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("io.micrometer:micrometer-tracing-bridge-brave")
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("net.logstash.logback:logstash-logback-encoder:${logbackVersion}")
	implementation("io.github.openfeign:feign-core:${openFeignVersion}")
	implementation("io.github.openfeign:feign-jackson:${openFeignVersion}")
	implementation("io.github.openfeign:feign-slf4j:${openFeignVersion}")

	runtimeOnly("org.postgresql:postgresql")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
