package com.starter.springboot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "info")
@Validated
public class AppInfoProperties {

    @Valid
    private App app = new App();

    @Valid
    private System system = new System();

    @Valid
    private Contact contact = new Contact();

    @Valid
    private License license = new License();

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }

    public System getSystem() {
        return system;
    }

    public void setSystem(System system) {
        this.system = system;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public License getLicense() {
        return license;
    }

    public void setLicense(License license) {
        this.license = license;
    }

    public static class App {
        @NotBlank
        private String name;
        @NotBlank
        private String description;
        @NotBlank
        private String version;
        @NotBlank
        private String javaVersion;
        @NotBlank
        private String springBootVersion;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getJavaVersion() { return javaVersion; }
        public void setJavaVersion(String javaVersion) { this.javaVersion = javaVersion; }
        public String getSpringBootVersion() { return springBootVersion; }
        public void setSpringBootVersion(String springBootVersion) { this.springBootVersion = springBootVersion; }
    }

    public static class System {
        @NotBlank
        private String os;
        @NotBlank
        private String architecture;

        // getters and setters
        public String getOs() { return os; }
        public void setOs(String os) { this.os = os; }
        public String getArchitecture() { return architecture; }
        public void setArchitecture(String architecture) { this.architecture = architecture; }
    }

    public static class Contact {
        @NotBlank
        private String name;
        @NotBlank
        private String email;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class License {
        @NotBlank
        private String name;
        @NotBlank
        private String url;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}