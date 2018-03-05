package com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice;

import com.ngc.seaside.jellyfish.service.buildmgmt.api.DependencyScope;
import com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice.config.DependenciesConfiguration;

import static com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice.config.DependenciesConfiguration.artifact;
import static com.ngc.seaside.jellyfish.service.buildmgmt.impl.buildmgmtservice.config.DependenciesConfiguration.currentJellyfishVersion;

public class DefaultDependenciesConfiguration {

   private DefaultDependenciesConfiguration() {
   }

   public static DependenciesConfiguration getConfig() {
      DependenciesConfiguration config = new DependenciesConfiguration();

      configureSeasideGradlePlugins(config);
      configureStarfish(config);
      configureJellyfish(config);
      configureBlocs(config);
      configureSonarQube(config);
      configureProtoBuffers(config);
      configureOsgi(config);
      configureGoogle(config);
      configureCucumber(config);
      configureUnitTesting(config);

      return config;
   }

   private static void configureSeasideGradlePlugins(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("seasidePluginsVersion")
            .version("2.3.0")
            .includes(artifact("gradle.plugins")
                            .groupId("com.ngc.seaside")
                            .scope(DependencyScope.BUILDSCRIPT));
   }

   private static void configureStarfish(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("starfishVersion")
            .version("2.1.0")
            .defaultGroupId("com.ngc.seaside")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("service.api"),
                      artifact("service.transport.api"),
                      artifact("service.correlation.impl.correlationservice"),
                      artifact("fault.impl.faultloggingservice"),
                      artifact("monitoring.impl.loggingmonitoringservice"),
                      artifact("service.transport.impl.defaulttransportservice"),
                      artifact("service.transport.impl.defaulttransportservice.module"),
                      artifact("service.transport.impl.testutils"),
                      artifact("service.transport.impl.topic.multicast"),
                      artifact("service.transport.impl.provider.multicast"),
                      artifact("service.transport.impl.provider.multicast.module"));
   }

   private static void configureJellyfish(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("jellyfishVersion")
            .version(currentJellyfishVersion())
            .defaultGroupId("com.ngc.seaside")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("guice.modules"),
                      artifact("jellyfish.cli.gradle.plugins").scope(DependencyScope.BUILDSCRIPT));
   }

   private static void configureBlocs(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("blocsPluginVersion")
            .version("0.5")
            .includes(artifact("gradle.plugin")
                            .groupId("com.ngc.blocs")
                            .scope(DependencyScope.BUILDSCRIPT));

      config.addGroup()
            .versionPropertyName("blocsCoreVersion")
            .version("2.2.0")
            .defaultGroupId("com.ngc.blocs")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("api"),
                      artifact("service.api"),
                      artifact("file.impl.common.fileutilities"),
                      artifact("jaxb.impl.common.jaxbutilities"),
                      artifact("component.impl.common.componentutilities"),
                      artifact("notification.impl.common.notificationsupport"),
                      artifact("properties.resource.impl.common.propertiesresource"),
                      artifact("xml.resource.impl.common.xmlresource"),
                      artifact("security.impl.common.securityutilities"),
                      artifact("service.thread.impl.common.threadservice"),
                      artifact("service.log.impl.common.logservice"),
                      artifact("service.event.impl.common.eventservice"),
                      artifact("service.resource.impl.common.resourceservice"),
                      artifact("service.framework.impl.common.frameworkmgmtservice"),
                      artifact("service.notification.impl.common.notificationservice"),
                      artifact("service.deployment.impl.common.autodeploymentservice"),
                      artifact("test.impl.common.testutilities"));
   }

   private static void configureSonarQube(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("sonarqubePluginVersion")
            .version("2.5")
            .includes(artifact("sonarqube-gradle-plugin")
                            .groupId("org.sonarsource.scanner.gradle")
                            .scope(DependencyScope.BUILDSCRIPT));
   }

   private static void configureProtoBuffers(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("protobufPluginVersion")
            .version("0.8.3")
            .includes(artifact("protobuf-gradle-plugin")
                            .groupId("com.google.protobuf")
                            .scope(DependencyScope.BUILDSCRIPT));

      config.addGroup()
            .versionPropertyName("protobufVersion")
            .version("3.2.0")
            .defaultGroupId("com.google.protobuf")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("protobuf-java"),
                      artifact("protoc"));
   }

   private static void configureOsgi(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("osgiVersion")
            .version("6.0.0")
            .defaultGroupId("org.osgi")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("osgi.core"),
                      artifact("osgi.enterprise"));
   }

   private static void configureGoogle(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("guavaVersion")
            .version("22.0")
            .includes(artifact("guava")
                            .groupId("com.google.guava")
                            .scope(DependencyScope.BUILD));

      config.addGroup()
            .versionPropertyName("guiceVersion")
            .version("4.1.0")
            .includes(artifact("guice")
                            .groupId("com.google.inject")
                            .scope(DependencyScope.BUILD));
   }

   private static void configureCucumber(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("cucumberVersion")
            .version("1.2.5")
            .defaultGroupId("info.cukes")
            .defaultScope(DependencyScope.BUILD)
            .includes(artifact("cucumber-java"),
                      artifact("cucumber-guice"));
   }

   private static void configureUnitTesting(DependenciesConfiguration config) {
      config.addGroup()
            .versionPropertyName("junitVersion")
            .version("4.12")
            .includes(artifact("junit")
                            .groupId("junit")
                            .scope(DependencyScope.TEST));

      config.addGroup()
            .versionPropertyName("mockitoVersion")
            .version("2.7.14")
            .includes(artifact("mockito-core")
                            .groupId("org.mockito")
                            .scope(DependencyScope.TEST));
   }
}
