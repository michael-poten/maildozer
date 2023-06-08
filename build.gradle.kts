import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.3.1.RELEASE"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
}

group = "de.mroedel"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation ("org.springframework.shell:spring-shell-starter:2.0.1.RELEASE")
//	implementation ("jakarta.mail:jakarta.mail-api:2.1.1")
	implementation ("org.eclipse.angus:jakarta.mail:1.1.0")
//	implementation ("com.sun.mail:jakarta.mail:2.0.1")
	implementation ("com.sun.activation:javax.activation:1.2.0")
	implementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo.spring30x:4.5.2")
	implementation ("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation ("com.h2database:h2:1.4.193")
	implementation ("org.jsoup:jsoup:1.9.2")
	implementation ("com.sun.activation:jakarta.activation:2.0.1")
	testImplementation("org.springframework.boot:spring-boot-starter-test") {
		exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}
