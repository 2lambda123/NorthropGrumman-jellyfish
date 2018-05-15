package com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.DefaultParameter;
import com.ngc.seaside.jellyfish.api.DefaultParameterCollection;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.cli.command.test.service.MockedBuildManagementService;
import com.ngc.seaside.jellyfish.cli.command.test.service.MockedJavaServiceGenerationService;
import com.ngc.seaside.jellyfish.cli.command.test.service.MockedPackageNamingService;
import com.ngc.seaside.jellyfish.cli.command.test.service.MockedProjectNamingService;
import com.ngc.seaside.jellyfish.cli.command.test.service.MockedScenarioService;
import com.ngc.seaside.jellyfish.cli.command.test.service.MockedTemplateService;
import com.ngc.seaside.jellyfish.cli.command.test.service.MockedTransportConfigurationService;
import com.ngc.seaside.jellyfish.service.buildmgmt.api.IBuildManagementService;
import com.ngc.seaside.jellyfish.service.codegen.api.IJavaServiceGenerationService;
import com.ngc.seaside.jellyfish.service.config.api.dto.HttpMethod;
import com.ngc.seaside.jellyfish.service.config.api.dto.zeromq.ConnectionType;
import com.ngc.seaside.jellyfish.service.name.api.IPackageNamingService;
import com.ngc.seaside.jellyfish.service.name.api.IProjectNamingService;
import com.ngc.seaside.jellyfish.service.scenario.api.IScenarioService;
import com.ngc.seaside.jellyfish.utilities.command.JellyfishCommandPhase;
import com.ngc.seaside.systemdescriptor.model.api.ISystemDescriptor;
import com.ngc.seaside.systemdescriptor.model.impl.basic.Package;
import com.ngc.seaside.systemdescriptor.model.impl.basic.data.Data;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.DataReferenceField;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.Model;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.scenario.Scenario;
import com.ngc.seaside.systemdescriptor.model.impl.basic.model.scenario.ScenarioStep;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Optional;

import static com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.CreateJavaServiceGeneratedConfigCommand.CONFIG_BUILD_TEMPLATE_SUFFIX;
import static com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.CreateJavaServiceGeneratedConfigCommand.CONFIG_GENERATED_BUILD_TEMPLATE_SUFFIX;
import static com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.multicast.MulticastTransportProviderConfigDto.MULTICAST_TEMPLATE;
import static com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.spark.SparkTransportProviderConfigDto.SPARK_TEMPLATE;
import static com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.zeromq.ZeroMqTransportProviderConfigDto.ZEROMQ_TEMPLATE;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateJavaServiceGeneratedConfigCommandIT {

   @Rule
   public final TemporaryFolder outputDirectory = new TemporaryFolder();

   private CreateJavaServiceGeneratedConfigCommand command;

   @Mock
   private IJellyFishCommandOptions jellyFishCommandOptions;

   private DefaultParameterCollection parameters;

   @Mock
   private ILogService logService;

   private MockedTemplateService templateService;

   private IProjectNamingService projectService = new MockedProjectNamingService();

   private IPackageNamingService packageService = new MockedPackageNamingService();

   private IBuildManagementService buildManagementService = new MockedBuildManagementService();

   private MockedTransportConfigurationService transportConfigService = new MockedTransportConfigurationService();

   private IJavaServiceGenerationService generateService = new MockedJavaServiceGenerationService(packageService);

   private IScenarioService scenarioService = new MockedScenarioService();

   /**
    *
    * @return Pub Sub Model used for testing
    */
   public static Model newPubSubModelForTesting() {
      Data trackEngagementStatus = new Data("TrackEngagementStatus");
      Data trackPriority = new Data("TrackPriority");

      Scenario calculateTrackPriority = new Scenario("calculateTrackPriority");

      ScenarioStep step = new ScenarioStep();
      step.setKeyword("receiving");
      step.getParameters().add("trackEngagementStatus");
      step.setParent(calculateTrackPriority);
      calculateTrackPriority.setWhens(Collections.singletonList(step));

      step = new ScenarioStep();
      step.setKeyword("willPublish");
      step.getParameters().add("trackPriority");
      step.setParent(calculateTrackPriority);
      calculateTrackPriority.setThens(Collections.singletonList(step));

      Model model = new Model("EngagementTrackPriorityService");
      model.addInput(new DataReferenceField("trackEngagementStatus").setParent(model).setType(trackEngagementStatus));
      model.addOutput(new DataReferenceField("trackPriority").setParent(model).setType(trackPriority));
      model.addScenario(calculateTrackPriority);
      calculateTrackPriority.setParent(model);

      Package p = new Package("com.ngc.seaside.threateval");
      p.addModel(model);

      return model;
   }

   /**
    *
    * @return Response Model used for testing
    */
   public static Model newRequestResponseModelForTesting() {
      Data trackPriorityRequest = new Data("TrackPriorityRequest");
      Data trackPriorityResponse = new Data("TrackPriorityResponse");

      Scenario getTrackPriorities = new Scenario("getTrackPriorities");

      ScenarioStep step = new ScenarioStep();
      step.setKeyword("receivingRequest");
      step.getParameters().add("trackPriorityRequest");
      step.setParent(getTrackPriorities);
      getTrackPriorities.setWhens(Collections.singletonList(step));

      step = new ScenarioStep();
      step.setKeyword("willRespond");
      step.getParameters().add("with");
      step.getParameters().add("trackPriorityResponse");
      step.setParent(getTrackPriorities);
      getTrackPriorities.setThens(Collections.singletonList(step));

      Model model = new Model("TrackPriorityService");
      model.addInput(new DataReferenceField("trackPriorityRequest").setParent(model).setType(trackPriorityRequest));
      model.addOutput(new DataReferenceField("trackPriorityResponse").setParent(model).setType(trackPriorityResponse));
      model.addScenario(getTrackPriorities);
      getTrackPriorities.setParent(model);

      Package p = new Package("com.ngc.seaside.threateval");
      p.addModel(model);

      return model;
   }

   @Before
   public void setup() throws Throwable {
      outputDirectory.newFile("settings.gradle");

      templateService = new MockedTemplateService()
            .useRealPropertyService()
            .setTemplateDirectory(
                  CreateJavaServiceGeneratedConfigCommand.class.getPackage().getName() + "-"
                  + CONFIG_GENERATED_BUILD_TEMPLATE_SUFFIX,
                  Paths.get("src", "main", "templates", CONFIG_GENERATED_BUILD_TEMPLATE_SUFFIX))
            .setTemplateDirectory(
                  CreateJavaServiceGeneratedConfigCommand.class.getPackage().getName() + "-"
                  + CONFIG_BUILD_TEMPLATE_SUFFIX,
                  Paths.get("src", "main", "templates", CONFIG_BUILD_TEMPLATE_SUFFIX));

      ISystemDescriptor systemDescriptor = mock(ISystemDescriptor.class);
      when(systemDescriptor.findModel("com.ngc.seaside.threateval.EngagementTrackPriorityService"))
            .thenReturn(Optional.of(newPubSubModelForTesting()));
      when(systemDescriptor.findModel("com.ngc.seaside.threateval.TrackPriorityService"))
            .thenReturn(Optional.of(newRequestResponseModelForTesting()));

      parameters = new DefaultParameterCollection();
      when(jellyFishCommandOptions.getParameters()).thenReturn(parameters);
      when(jellyFishCommandOptions.getSystemDescriptor()).thenReturn(systemDescriptor);

      command = new CreateJavaServiceGeneratedConfigCommand();
      command.setLogService(logService);
      command.setProjectNamingService(projectService);
      command.setPackageNamingService(packageService);
      command.setTemplateService(templateService);
      command.setBuildManagementService(buildManagementService);
      command.setJavaServiceGenerationService(generateService);
      command.setTransportConfigurationService(transportConfigService);
      command.setScenarioService(scenarioService);
   }

   @Test
   public void multicast() throws Throwable {
      addGeneratedConfigTemplatePath(MULTICAST_TEMPLATE);

      transportConfigService.addMulticastConfiguration("trackEngagementStatus", "224.5.6.7",
                                                       61000, "127.0.0.1", "127.0.0.1");
      transportConfigService.addMulticastConfiguration("trackEngagementStatus", "224.5.6.7",
                                                       61001, "127.0.0.1", "127.0.0.1");

      run(
            CreateJavaServiceGeneratedConfigCommand.MODEL_PROPERTY,
            "com.ngc.seaside.threateval.EngagementTrackPriorityService",
            CreateJavaServiceGeneratedConfigCommand.DEPLOYMENT_MODEL_PROPERTY, "",
            CreateJavaServiceGeneratedConfigCommand.OUTPUT_DIRECTORY_PROPERTY,
            outputDirectory.getRoot().getAbsolutePath(),
            CommonParameters.PHASE.getName(), JellyfishCommandPhase.DEFERRED);

      Path
            projectDir =
            outputDirectory.getRoot().toPath()
                  .resolve("com.ngc.seaside.threateval.engagementtrackpriorityservice.config");
      Path
            srcDir =
            projectDir.resolve(Paths.get("src", "main", "java", "com", "ngc", "seaside", "threateval",
                                         "engagementtrackpriorityservice", "config"));

      Path buildFile = projectDir.resolve("build.generated.gradle");
      Path configurationFile = srcDir.resolve("EngagementTrackPriorityServiceTransportConfiguration.java");
      Path multicastFile = srcDir.resolve("EngagementTrackPriorityServiceMulticastConfiguration.java");

      assertTrue(Files.isRegularFile(buildFile));
      assertTrue(Files.isRegularFile(configurationFile));
      assertTrue(Files.isRegularFile(multicastFile));
   }

   @Test
   public void rest() throws Throwable {
      addGeneratedConfigTemplatePath(SPARK_TEMPLATE);

      transportConfigService.addRestConfiguration("trackPriorityRequest", "localhost", "0.0.0.0", 52412,
                                                  "/trackPriorityRequest", "application/x-protobuf", HttpMethod.POST);

      run(CreateJavaServiceGeneratedConfigCommand.MODEL_PROPERTY,
            "com.ngc.seaside.threateval.TrackPriorityService",
            CreateJavaServiceGeneratedConfigCommand.DEPLOYMENT_MODEL_PROPERTY, "",
            CreateJavaServiceGeneratedConfigCommand.OUTPUT_DIRECTORY_PROPERTY,
            outputDirectory.getRoot().getAbsolutePath(),
            CommonParameters.PHASE.getName(), JellyfishCommandPhase.DEFERRED);

      Path projectDir =
            outputDirectory.getRoot()
                  .toPath()
                  .resolve("com.ngc.seaside.threateval.trackpriorityservice.config");
      Path srcDir =
            projectDir.resolve(Paths.get("src", "main", "java", "com", "ngc", "seaside", "threateval",
                                         "trackpriorityservice", "config"));

      Path buildFile = projectDir.resolve("build.generated.gradle");
      Path configurationFile = srcDir.resolve("TrackPriorityServiceTransportConfiguration.java");
      Path restFile = srcDir.resolve("TrackPriorityServiceSparkConfiguration.java");

      assertTrue(Files.isRegularFile(buildFile));
      assertTrue(Files.isRegularFile(configurationFile));
      assertTrue(Files.isRegularFile(restFile));
   }

   @Test
   public void zeromq() throws Throwable {
      addGeneratedConfigTemplatePath(ZEROMQ_TEMPLATE);

      transportConfigService.addZeroMqTcpConfiguration("trackEngagementStatus",
                                                       ConnectionType.SOURCE_BINDS_TARGET_CONNECTS,
                                                       "*",
                                                       "localhost",
                                                       1000);

      transportConfigService.addZeroMqTcpConfiguration("trackPriorityRequest",
                                                       ConnectionType.SOURCE_BINDS_TARGET_CONNECTS,
                                                       "*",
                                                       "localhost",
                                                       1001);

      run(
            CreateJavaServiceGeneratedConfigCommand.MODEL_PROPERTY,
            "com.ngc.seaside.threateval.EngagementTrackPriorityService",
            CreateJavaServiceGeneratedConfigCommand.DEPLOYMENT_MODEL_PROPERTY,
            "",
            CreateJavaServiceGeneratedConfigCommand.OUTPUT_DIRECTORY_PROPERTY,
            outputDirectory.getRoot().getAbsolutePath(),
            CommonParameters.PHASE.getName(),
            JellyfishCommandPhase.DEFERRED);

      Path projectDir = outputDirectory.getRoot().toPath().resolve(
            "com.ngc.seaside.threateval.engagementtrackpriorityservice.config");
      Path srcDir = projectDir.resolve(Paths.get("src",
                                                 "main",
                                                 "java",
                                                 "com",
                                                 "ngc",
                                                 "seaside",
                                                 "threateval",
                                                 "engagementtrackpriorityservice",
                                                 "config"));

      Path buildFile = projectDir.resolve("build.generated.gradle");
      Path configurationFile = srcDir.resolve("EngagementTrackPriorityServiceTransportConfiguration.java");
      Path multicastFile = srcDir.resolve("EngagementTrackPriorityServiceZeroMqConfiguration.java");

      assertTrue(Files.isRegularFile(buildFile));
      assertTrue(Files.isRegularFile(configurationFile));
      assertTrue(Files.isRegularFile(multicastFile));

      run(
            CreateJavaServiceGeneratedConfigCommand.MODEL_PROPERTY,
            "com.ngc.seaside.threateval.TrackPriorityService",
            CreateJavaServiceGeneratedConfigCommand.DEPLOYMENT_MODEL_PROPERTY,
            "",
            CreateJavaServiceGeneratedConfigCommand.OUTPUT_DIRECTORY_PROPERTY,
            outputDirectory.getRoot().getAbsolutePath(),
            CommonParameters.PHASE.getName(),
            JellyfishCommandPhase.DEFERRED);

      projectDir = outputDirectory.getRoot().toPath().resolve("com.ngc.seaside.threateval.trackpriorityservice.config");
      srcDir = projectDir.resolve(
            Paths.get("src", "main", "java", "com", "ngc", "seaside", "threateval", "trackpriorityservice", "config"));

      buildFile = projectDir.resolve("build.generated.gradle");
      configurationFile = srcDir.resolve("TrackPriorityServiceTransportConfiguration.java");
      multicastFile = srcDir.resolve("TrackPriorityServiceZeroMqConfiguration.java");

      assertTrue(Files.isRegularFile(buildFile));
      assertTrue(Files.isRegularFile(configurationFile));
      assertTrue(Files.isRegularFile(multicastFile));
   }

   private void run(Object... args) {
      for (int i = 0; i < args.length; i += 2) {
         String parameterName = args[i].toString();
         Object parameterValue = args[i + 1];
         parameters.addParameter(new DefaultParameter<>(parameterName, parameterValue));
      }

      command.run(jellyFishCommandOptions);
   }

   private void addGeneratedConfigTemplatePath(String template) {
      String templateSuffix = template.substring(template.lastIndexOf('-') + 1);
      templateService.setTemplateDirectory(template,
                                           Paths.get("src",
                                                     "main",
                                                     "templates",
                                                     templateSuffix));
   }
}