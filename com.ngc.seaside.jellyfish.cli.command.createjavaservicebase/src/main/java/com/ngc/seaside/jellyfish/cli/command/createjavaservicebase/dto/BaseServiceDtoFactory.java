package com.ngc.seaside.jellyfish.cli.command.createjavaservicebase.dto;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.ngc.blocs.service.event.api.IEvent;
import com.ngc.blocs.service.event.api.Subscriber;
import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.blocs.service.thread.api.ISubmittedLongLivingTask;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicebase.dto.TriggerDto.CompletenessDto;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicebase.dto.TriggerDto.EventDto;
import com.ngc.seaside.jellyfish.service.codegen.api.IDataFieldGenerationService;
import com.ngc.seaside.jellyfish.service.codegen.api.IJavaServiceGenerationService;
import com.ngc.seaside.jellyfish.service.codegen.api.dto.ClassDto;
import com.ngc.seaside.jellyfish.service.codegen.api.dto.EnumDto;
import com.ngc.seaside.jellyfish.service.codegen.api.dto.MethodDto;
import com.ngc.seaside.jellyfish.service.codegen.api.dto.PubSubMethodDto;
import com.ngc.seaside.jellyfish.service.codegen.api.dto.TypeDto;
import com.ngc.seaside.jellyfish.service.data.api.IDataService;
import com.ngc.seaside.jellyfish.service.name.api.IPackageNamingService;
import com.ngc.seaside.jellyfish.service.name.api.IProjectNamingService;
import com.ngc.seaside.jellyfish.service.scenario.api.IPublishSubscribeMessagingFlow;
import com.ngc.seaside.jellyfish.service.scenario.api.IScenarioService;
import com.ngc.seaside.jellyfish.service.scenario.correlation.api.ICorrelationDescription;
import com.ngc.seaside.jellyfish.service.scenario.correlation.api.ICorrelationExpression;
import com.ngc.seaside.systemdescriptor.model.api.INamedChild;
import com.ngc.seaside.systemdescriptor.model.api.IPackage;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataPath;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenario;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BaseServiceDtoFactory implements IBaseServiceDtoFactory {

   private ILogService logService;
   private IProjectNamingService projectService;
   private IPackageNamingService packageService;
   private IJavaServiceGenerationService generateService;
   private IScenarioService scenarioService;
   private IDataService dataService;
   private IDataFieldGenerationService dataFieldGenerationService;

   @Inject
   public BaseServiceDtoFactory(IProjectNamingService projectService,
                                IPackageNamingService packageService,
                                IJavaServiceGenerationService generateService,
                                IScenarioService scenarioService,
                                IDataService dataService,
                                IDataFieldGenerationService dataFieldGenerationService, ILogService logService) {
      this.projectService = projectService;
      this.packageService = packageService;
      this.generateService = generateService;
      this.scenarioService = scenarioService;
      this.dataService = dataService;
      this.dataFieldGenerationService = dataFieldGenerationService;
      this.logService = logService;
   }

   @Override
   public BaseServiceDto newDto(IJellyFishCommandOptions options, IModel model) {
      Set<String> projectDependencies = Collections.singleton(
         projectService.getEventsProjectName(options, model).getArtifactId());
      ClassDto<? extends MethodDto> interfaceDto = generateService.getServiceInterfaceDescription(options, model);
      ClassDto<? extends PubSubMethodDto> abstractClassDto = generateService.getBaseServiceDescription(options, model);
      EnumDto<?> topicsDto = generateService.getTransportTopicsDescription(options, model);

      BaseServiceDto dto = new BaseServiceDto();
      dto.setProjectDirectoryName(projectService.getBaseServiceProjectName(options, model).getDirectoryName());
      dto.setProjectDependencies(projectDependencies);
      dto.setAbstractClass(abstractClassDto);
      dto.setInterface(interfaceDto);
      dto.setExportedPackages(new LinkedHashSet<>(
         Arrays.asList(packageService.getServiceInterfacePackageName(options, model) + ".*",
            packageService.getServiceBaseImplementationPackageName(options, model) + ".*",
            packageService.getTransportTopicsPackageName(options, model) + ".*")));
      dto.setModel(model);
      dto.setTopicsEnum(topicsDto);

      setReceiveMethods(dto, options, model);
      setPublishMethods(dto, options, model);

      for (IScenario scenario : model.getScenarios()) {
         Optional<IPublishSubscribeMessagingFlow> flowOptional = scenarioService.getPubSubMessagingFlow(options,
            scenario);
         if (!flowOptional.isPresent()) {
            continue;
         }
         IPublishSubscribeMessagingFlow flow = flowOptional.get();

         Optional<BasicPubSubDto> pubSub = getBasicPubSubMethod(scenario, flow, dto, options);
         if (pubSub.isPresent()) {
            dto.getBasicPubSubMethods().add(pubSub.get());
            continue;
         }
         Optional<BasicPubSubDto> sink = getBasicSinkMethod(scenario, flow, dto, options);
         if (sink.isPresent()) {
            dto.getBasicSinkMethods().add(sink.get());
            continue;
         }
         Optional<TriggerDto> trigger = getTriggerRegistrationMethod(scenario, flow, dto, options);
         if (trigger.isPresent()) {
            dto.getTriggerRegistrationMethods().add(trigger.get());
         }
         Optional<CorrelationDto> correlation = getCorrelationMethod(scenario, flow, dto, options);
         if (correlation.isPresent()) {
            dto.getCorrelationMethods().add(correlation.get());
            continue;
         }
         Optional<ComplexScenarioDto> complex = getComplexScenario(scenario, flow, dto, options);
         if (complex.isPresent()) {
            dto.getComplexScenarios().add(complex.get());
            continue;
         }
         logService.warn(getClass(),
            "Could not handle the publish/subscribe steps for scenario %s:%s",
            model.getFullyQualifiedName(),
            scenario.getName());
      }

      return dto;
   }

   /**
    * Returns true if the model uses two types with the same name, but different packages (e.g., com.Data and org.Data).
    */
   private boolean hasDuplicateTypeNames(IJellyFishCommandOptions options, IModel model) {
      Set<String> names = new HashSet<>();
      Set<String> qualifiedNames = new HashSet<>();
      for (IDataReferenceField field : model.getInputs()) {
         TypeDto<?> dto = dataService.getEventClass(options, field.getType());
         if (qualifiedNames.add(dto.getFullyQualifiedName()) && !names.add(dto.getTypeName())) {
            return true;
         }
      }

      return false;
   }

   private void setReceiveMethods(BaseServiceDto dto, IJellyFishCommandOptions options, IModel model) {
      List<ReceiveDto> receiveDtos = new ArrayList<>();
      Set<String> methods = new HashSet<>();
      for (IDataReferenceField input : model.getInputs()) {
         List<String> basicScenarios = new ArrayList<>();
         ReceiveDto receive = new ReceiveDto();
         TypeDto<?> inputField = dataService.getEventClass(options, input.getType());
         receive.setEventType(inputField.getTypeName());

         receive.setTopic(inputField.getTypeName() + ".TOPIC_NAME");

         receive.setName("receive" + inputField.getTypeName().replace('.', '_'));
         boolean inScenario = false;
         for (IScenario scenario : model.getScenarios()) {
            Optional<IPublishSubscribeMessagingFlow> flowOptional = scenarioService.getPubSubMessagingFlow(options,
               scenario);

            if (!flowOptional.isPresent()) {
               continue;
            }

            IPublishSubscribeMessagingFlow flow = flowOptional.get();
            if (!flow.getInputs().contains(input)) {
               continue;
            }

            inScenario = true;

            // scenario has this input
            if (flow.getCorrelationDescription().isPresent()
               && !flow.getCorrelationDescription().get().getCompletenessExpressions().isEmpty()) {
               receive.setHasCorrelations(true);
            }

            // check for basic scenario
            if (flow.getInputs().size() == 1 && flow.getOutputs().size() <= 1) {
               basicScenarios.add("do" + StringUtils.capitalize(scenario.getName()));
            }
         }

         if (inScenario && methods.add(receive.getName())) {
            receive.setBasicScenarios(basicScenarios);
            receiveDtos.add(receive);
            dto.getAbstractClass().getImports().add(IEvent.class.getName());
            dto.getAbstractClass().getImports().add(Subscriber.class.getName());
            dto.getAbstractClass().getImports().add(Preconditions.class.getName());
         }
      }
      dto.setReceiveMethods(receiveDtos);
   }

   private void setPublishMethods(BaseServiceDto dto, IJellyFishCommandOptions options, IModel model) {
      List<PublishDto> publishDtos = new ArrayList<>();
      Set<String> methods = new HashSet<>();
      for (IDataReferenceField output : model.getOutputs()) {
         for (IScenario scenario : model.getScenarios()) {

            Optional<IPublishSubscribeMessagingFlow> flowOptional = scenarioService.getPubSubMessagingFlow(options,
               scenario);

            if (!flowOptional.isPresent()) {
               continue;
            }

            IPublishSubscribeMessagingFlow flow = flowOptional.get();
            if (!flow.getOutputs().contains(output)) {
               continue;
            }

            PublishDto publish = getPublishDto(output, dto, options);

            if (methods.add(publish.getName())) {
               publishDtos.add(publish);
            }
            break;
         }

      }
      dto.setPublishMethods(publishDtos);
   }

   private Optional<BasicPubSubDto> getBasicPubSubMethod(IScenario scenario, IPublishSubscribeMessagingFlow flow,
            BaseServiceDto dto, IJellyFishCommandOptions options) {

      // check for basic scenario
      if (flow.getInputs().size() != 1 || flow.getOutputs().size() != 1) {
         return Optional.empty();
      }

      BasicPubSubDto pubSub = new BasicPubSubDto();
      pubSub.setScenarioName(scenario.getName());
      pubSub.setName("do" + StringUtils.capitalize(scenario.getName()));

      pubSub.setInput(getInputDto(flow.getInputs().iterator().next(), dto, options));
      pubSub.setOutput(getPublishDto(flow.getOutputs().iterator().next(), dto, options));

      pubSub.setServiceMethod(scenario.getName());

      List<IOCorrelationDto> ioCorrelations = new ArrayList<>();

      if (flow.getCorrelationDescription().isPresent()) {
         pubSub.setInputOutputCorrelations(
            getInputOutputCorrelations(flow.getCorrelationDescription().get(), dto, options));
      }

      pubSub.setInputOutputCorrelations(ioCorrelations);

      return Optional.of(pubSub);
   }

   private Optional<BasicPubSubDto> getBasicSinkMethod(IScenario scenario, IPublishSubscribeMessagingFlow flow,
            BaseServiceDto dto, IJellyFishCommandOptions options) {

      // check for basic scenario
      if (flow.getInputs().size() != 1 || flow.getOutputs().size() != 0) {
         return Optional.empty();
      }

      BasicPubSubDto pubSub = new BasicPubSubDto();
      pubSub.setName("do" + StringUtils.capitalize(scenario.getName()));

      pubSub.setInput(getInputDto(flow.getInputs().iterator().next(), dto, options));

      pubSub.setServiceMethod(scenario.getName());
      return Optional.of(pubSub);
   }

   private Optional<CorrelationDto> getCorrelationMethod(IScenario scenario, IPublishSubscribeMessagingFlow flow,
            BaseServiceDto dto, IJellyFishCommandOptions options) {

      // Ignore scenarios with multiple outputs
      // Ignore scenarios without correlations
      if (flow.getOutputs().size() != 1 || !flow.getCorrelationDescription().isPresent()) {
         return Optional.empty();
      }

      // Ignore flows with inputs of the same type
      if (flow.getInputs().stream().map(input -> input.getType()).distinct().count() != flow.getInputs().size()) {
         return Optional.empty();
      }

      ICorrelationDescription description = flow.getCorrelationDescription().get();

      // Ignore scenarios without input-input correlations
      if (description.getCompletenessExpressions().isEmpty()) {
         return Optional.empty();
      }

      CorrelationDto correlation = new CorrelationDto();

      correlation.setName("do" + StringUtils.capitalize(scenario.getName()));
      correlation.setScenarioName(scenario.getName());
      correlation.setOutput(getPublishDto(flow.getOutputs().iterator().next(), dto, options));

      correlation.setServiceMethod(scenario.getName());

      correlation.setInputLogFormat(
         IntStream.range(0, flow.getInputs().size())
                  .mapToObj(i -> "%s")
                  .collect(Collectors.joining(", ")));

      correlation.setInputs(flow.getInputs()
                                .stream()
                                .map(field -> getInputDto(field, dto, options))
                                .map(input -> input.setCorrelationMethod(correlation.getName()))
                                .collect(Collectors.toList()));

      correlation.setInputOutputCorrelations(getInputOutputCorrelations(description, dto, options));

      ICorrelationExpression completeness = description.getCompletenessExpressions()
                                                       .iterator()
                                                       .next();
      switch (completeness.getCorrelationEventIdType()) {
      case DATA:
      case ENUM:
         INamedChild<IPackage> element = completeness.getCorrelationEventIdReferenceType();
         TypeDto<?> type = dataService.getEventClass(options, element);
         correlation.setCorrelationType(type.getTypeName());
         break;
      case BOOLEAN:
         correlation.setCorrelationType("Boolean");
         break;
      case FLOAT:
         correlation.setCorrelationType("Float");
         break;
      case INT:
         correlation.setCorrelationType("Integer");
         break;
      case STRING:
         correlation.setCorrelationType("String");
         break;
      }

      dto.getAbstractClass().getImports().add(Collection.class.getName());
      dto.getAbstractClass().getImports().add(ArrayList.class.getName());
      dto.getAbstractClass().getImports().add(Objects.class.getName());
      dto.getAbstractClass().getImports().add("com.ngc.seaside.service.correlation.api.ICorrelationService");
      dto.getAbstractClass().getImports().add("com.ngc.seaside.service.correlation.api.ICorrelationStatus");
      dto.getAbstractClass().getImports().add("com.ngc.seaside.service.correlation.api.ICorrelationTrigger");
      dto.getAbstractClass().getImports().add("com.ngc.blocs.requestmodel.api.IRequest");
      dto.getAbstractClass().getImports().add("com.ngc.blocs.requestmodel.api.Requests");
      dto.getAbstractClass().getImports().add("com.ngc.seaside.request.api.ServiceRequest");
      dto.getAbstractClass().getImports().add(Consumer.class.getName());
      dto.getInterface().getImports().add("com.ngc.seaside.service.correlation.api.ILocalCorrelationEvent");
      dto.getAbstractClass().getImports().add("com.ngc.seaside.service.correlation.api.ILocalCorrelationEvent");

      return Optional.of(correlation);

   }

   private Optional<TriggerDto> getTriggerRegistrationMethod(IScenario scenario, IPublishSubscribeMessagingFlow flow,
            BaseServiceDto dto, IJellyFishCommandOptions options) {

      // Ignore scenarios with multiple outputs
      // Ignore scenarios without correlations
      if (flow.getOutputs().size() != 1 || !flow.getCorrelationDescription().isPresent()) {
         return Optional.empty();
      }

      // Ignore flows with inputs of the same type
      if (flow.getInputs().stream().map(input -> input.getType()).distinct().count() != flow.getInputs().size()) {
         return Optional.empty();
      }

      ICorrelationDescription description = flow.getCorrelationDescription().get();

      // Ignore scenarios without input-input correlations
      if (description.getCompletenessExpressions().isEmpty()) {
         return Optional.empty();
      }

      TriggerDto trigger = new TriggerDto();

      trigger.setName("register" + StringUtils.capitalize(scenario.getName() + "Trigger"));
      trigger.setCorrelationMethod("do" + StringUtils.capitalize(scenario.getName()));

      trigger.setInputs(flow.getInputs()
                            .stream()
                            .map(field -> getInputDto(field, dto, options))
                            .map(input -> input.setCorrelationMethod(trigger.getCorrelationMethod()))
                            .collect(Collectors.toList()));

      trigger.setEventProducers(description.getCompletenessExpressions()
                                           .stream()
                                           .flatMap(expression -> Stream.of(expression.getLeftHandOperand(),
                                              expression.getRightHandOperand()))
                                           .distinct()
                                           .map(left -> {
                                              EventDto eventDto = new EventDto();
                                              eventDto.setGetterSnippet(
                                                 left.getElements()
                                                     .stream()
                                                     .map(
                                                        field -> dataFieldGenerationService.getEventsField(
                                                           options, field).getJavaGetterName() + "()")
                                                     .collect(Collectors.joining(".")));

                                              eventDto.setType(dataService.getEventClass(
                                                 options, left.getStart().getType()).getTypeName());
                                              return eventDto;
                                           })
                                           .collect(Collectors.toList()));

      trigger.setCompletionStatements(description.getCompletenessExpressions()
                                                 .stream()
                                                 .map(expression -> {
                                                    IDataPath left = expression.getLeftHandOperand();
                                                    IDataPath right = expression.getRightHandOperand();
                                                    CompletenessDto completenessDto = new CompletenessDto();
                                                    completenessDto.setInput1GetterSnippet(
                                                       getGetterSnippet(left, options));
                                                    completenessDto.setInput1Type(dataService.getEventClass(
                                                       options, left.getStart().getType()).getTypeName());
                                                    completenessDto.setInput2GetterSnippet(
                                                       getGetterSnippet(right, options));
                                                    completenessDto.setInput2Type(dataService.getEventClass(
                                                       options, right.getStart().getType()).getTypeName());
                                                    return completenessDto;
                                                 })
                                                 .collect(Collectors.toList()));

      ICorrelationExpression completeness = description.getCompletenessExpressions()
                                                       .iterator()
                                                       .next();
      switch (completeness.getCorrelationEventIdType()) {
      case DATA:
      case ENUM:
         INamedChild<IPackage> element = completeness.getCorrelationEventIdReferenceType();
         TypeDto<?> type = dataService.getEventClass(options, element);
         trigger.setTriggerType(type.getTypeName());
         break;
      case BOOLEAN:
         trigger.setTriggerType("Boolean");
         break;
      case FLOAT:
         trigger.setTriggerType("Float");
         break;
      case INT:
         trigger.setTriggerType("Integer");
         break;
      case STRING:
         trigger.setTriggerType("String");
         break;
      }

      return Optional.of(trigger);
   }

   private Optional<ComplexScenarioDto> getComplexScenario(IScenario scenario, IPublishSubscribeMessagingFlow flow,
            BaseServiceDto dto, IJellyFishCommandOptions options) {

      // Ignore simple flows
      if (flow.getInputs().size() == 1 && flow.getOutputs().size() <= 1) {
         return Optional.empty();
      }

      // Ignore flows with input-input correlations
      if (flow.getCorrelationDescription().isPresent()
         && !flow.getCorrelationDescription().get().getCompletenessExpressions().isEmpty()) {
         return Optional.empty();
      }

      // Ignore flows with inputs of the same type
      if (flow.getInputs().stream().map(input -> input.getType()).distinct().count() != flow.getInputs().size()) {
         return Optional.empty();
      }

      ComplexScenarioDto scenarioDto = new ComplexScenarioDto();
      scenarioDto.setName(scenario.getName());
      scenarioDto.setServiceMethod(scenario.getName());
      scenarioDto.setStartMethod("start" + StringUtils.capitalize(scenario.getName()));

      scenarioDto.setInputs(flow.getInputs()
                                .stream()
                                .map(field -> getInputDto(field, dto, options))
                                .collect(Collectors.toList()));

      scenarioDto.setOutputs(flow.getOutputs()
                                 .stream()
                                 .map(field -> getPublishDto(field, dto, options))
                                 .collect(Collectors.toList()));

      dto.getInterface().getImports().add(Consumer.class.getName());
      dto.getInterface().getImports().add(BlockingQueue.class.getName());

      dto.getAbstractClass().getImports().add(Collection.class.getName());
      dto.getAbstractClass().getImports().add(BlockingQueue.class.getName());
      dto.getAbstractClass().getImports().add(LinkedBlockingQueue.class.getName());
      dto.getAbstractClass().getImports().add(Queue.class.getName());
      dto.getAbstractClass().getImports().add(IdentityHashMap.class.getName());
      dto.getAbstractClass().getImports().add(Collections.class.getName());
      dto.getAbstractClass().getImports().add(Map.class.getName());
      dto.getAbstractClass().getImports().add(ConcurrentHashMap.class.getName());
      dto.getAbstractClass().getImports().add(ISubmittedLongLivingTask.class.getName());

      return Optional.of(scenarioDto);
   }

   private InputDto getInputDto(IDataReferenceField field, BaseServiceDto dto, IJellyFishCommandOptions options) {
      InputDto input = new InputDto();
      TypeDto<?> type = dataService.getEventClass(options, field.getType());
      if (hasDuplicateTypeNames(options, field.getParent())) {
         input.setType(type.getFullyQualifiedName());
      } else {
         input.setType(type.getTypeName());
         dto.getInterface().getImports().add(type.getFullyQualifiedName());
         dto.getAbstractClass().getImports().add(type.getFullyQualifiedName());
      }
      input.setFieldName(field.getName());
      return input;
   }

   private PublishDto getPublishDto(IDataReferenceField field, BaseServiceDto dto, IJellyFishCommandOptions options) {
      PublishDto output = new PublishDto();
      TypeDto<?> type = dataService.getEventClass(options, field.getType());
      if (hasDuplicateTypeNames(options, field.getParent())) {
         output.setType(type.getFullyQualifiedName());
         output.setTopic(type.getFullyQualifiedName() + ".TOPIC");
         output.setName("publish" + type.getFullyQualifiedName().replace('.', '_'));
      } else {
         output.setType(type.getTypeName());
         output.setTopic(type.getTypeName() + ".TOPIC");
         output.setName("publish" + type.getTypeName());
         dto.getAbstractClass().getImports().add(type.getFullyQualifiedName());
         dto.getInterface().getImports().add(type.getFullyQualifiedName());
      }
      dto.getAbstractClass().getImports().add(Preconditions.class.getName());
      output.setFieldName(field.getName());
      return output;
   }

   private String getSetterSnippet(IDataPath path, IJellyFishCommandOptions options) {
      List<IDataField> elements = path.getElements();
      String snippet = Stream.concat(elements.subList(0, elements.size() - 1)
                                             .stream()
                                             .map(field -> dataFieldGenerationService.getEventsField(
                                                options, field).getJavaGetterName() + "()"),
         Stream.of(dataFieldGenerationService.getEventsField(options, path.getEnd()).getJavaSetterName()))
                             .collect(Collectors.joining("."));
      return snippet;
   }

   private String getGetterSnippet(IDataPath path, IJellyFishCommandOptions options) {
      String snippet = path.getElements()
                           .stream()
                           .map(field -> dataFieldGenerationService.getEventsField(
                              options, field).getJavaGetterName() + "()")
                           .collect(Collectors.joining("."));
      return snippet;
   }

   private List<IOCorrelationDto> getInputOutputCorrelations(ICorrelationDescription description, BaseServiceDto dto,
            IJellyFishCommandOptions options) {
      List<IOCorrelationDto> ioCorrelations = new ArrayList<>();

      for (ICorrelationExpression expression : description.getCorrelationExpressions()) {
         IDataPath left = expression.getLeftHandOperand();
         IDataPath right = expression.getRightHandOperand();
         IOCorrelationDto correlationDto = new IOCorrelationDto();
         correlationDto.setGetterSnippet(getGetterSnippet(left, options));
         correlationDto.setSetterSnippet(getSetterSnippet(right, options));
         correlationDto.setInputType(dataService.getEventClass(
            options, left.getStart().getType()).getTypeName());
         ioCorrelations.add(correlationDto);
      }

      return ioCorrelations;
   }
}
