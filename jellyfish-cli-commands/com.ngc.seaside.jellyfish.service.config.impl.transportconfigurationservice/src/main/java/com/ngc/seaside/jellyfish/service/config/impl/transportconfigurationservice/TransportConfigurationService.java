package com.ngc.seaside.jellyfish.service.config.impl.transportconfigurationservice;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.jellyfish.api.CommonParameters;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IParameter;
import com.ngc.seaside.jellyfish.service.config.api.ITransportConfigurationService;
import com.ngc.seaside.jellyfish.service.config.api.TransportConfigurationType;
import com.ngc.seaside.jellyfish.service.config.api.dto.MulticastConfiguration;
import com.ngc.seaside.jellyfish.service.config.api.dto.RestConfiguration;
import com.ngc.seaside.jellyfish.service.config.api.dto.zeromq.ZeroMqConfiguration;
import com.ngc.seaside.jellyfish.service.scenario.api.IMessagingFlow;
import com.ngc.seaside.systemdescriptor.model.api.FieldCardinality;
import com.ngc.seaside.systemdescriptor.model.api.data.DataTypes;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.IReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.link.IModelLink;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IProperty;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IPropertyDataValue;
import com.ngc.seaside.systemdescriptor.service.api.ISystemDescriptorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An implementation of {@code ITransportConfigurationService}.
 */
@Component(service = ITransportConfigurationService.class)
public class TransportConfigurationService implements ITransportConfigurationService {

   private static final Pattern[] PATTERNS = {Pattern.compile("([a-z\\d])([A-Z]+)"),
                                              Pattern.compile("([A-Z])([A-Z][a-z\\d])")};
   private static final String[] REPLACEMENTS = {"$1_$2", "$1_$2"};

   private ILogService logService;
   private ISystemDescriptorService sdService;

   @Override
   public String getTransportTopicName(IMessagingFlow flow, IDataReferenceField field) {
      String topic = field.getType().getName();

      for (int i = 0; i < PATTERNS.length; i++) {
         topic = PATTERNS[i].matcher(topic).replaceAll(REPLACEMENTS[i]);
      }

      return topic.toUpperCase();
   }

   @Override
   public Set<TransportConfigurationType> getConfigurationTypes(IJellyFishCommandOptions options,
                                                                IModel deploymentModel) {
      Set<TransportConfigurationType> types = new LinkedHashSet<>();
      IModel aggregatedDeploymentModel = sdService.getAggregatedView(getDeploymentModel(options));
      for (IModelLink<?> link : aggregatedDeploymentModel.getLinks()) {
         for (IProperty property : link.getProperties()) {
            if (property.getType() == DataTypes.DATA) {
               IData type = property.getReferencedDataType();
               if (MulticastConfigurationUtils.isMulticastConfiguration(type)) {
                  types.add(TransportConfigurationType.MULTICAST);
               } else if (RestConfigurationUtils.isRestConfiguration(type)) {
                  types.add(TransportConfigurationType.REST);
               } else if (ZeroMqConfigurationUtils.isZeroMqConfiguration(type)) {
                  types.add(TransportConfigurationType.ZERO_MQ);
               }
            }
         }
      }
      return types;
   }

   @Override
   public Collection<MulticastConfiguration> getMulticastConfiguration(IJellyFishCommandOptions options,
                                                                       IDataReferenceField field) {
      return getConfigurations(options,
                               field,
                               MulticastConfigurationUtils.MULTICAST_CONFIGURATION_QUALIFIED_NAME,
                               MulticastConfigurationUtils::getMulticastConfiguration);
   }

   @Override
   public Collection<RestConfiguration> getRestConfiguration(IJellyFishCommandOptions options,
                                                             IDataReferenceField field) {
      return getConfigurations(options,
                               field,
                               RestConfigurationUtils.REST_CONFIGURATION_QUALIFIED_NAME,
                               RestConfigurationUtils::getRestConfiguration);
   }

   @Override
   public Collection<ZeroMqConfiguration> getZeroMqConfiguration(IJellyFishCommandOptions options,
                                                                 IDataReferenceField field) {
      List<ZeroMqConfiguration> configurations = new ArrayList<>();
      configurations.addAll(getConfigurations(options,
                                              field,
                                              ZeroMqConfigurationUtils.ZERO_MQ_TCP_CONFIGURATION_QUALIFIED_NAME,
                                              ZeroMqConfigurationUtils::getZeroMqTcpConfiguration));
      configurations.addAll(getConfigurations(options,
                                              field,
                                              ZeroMqConfigurationUtils.ZERO_MQ_IPC_CONFIGURATION_QUALIFIED_NAME,
                                              ZeroMqConfigurationUtils::getZeroMqIpcConfiguration));
      configurations.addAll(getConfigurations(options,
                                              field,
                                              ZeroMqConfigurationUtils.ZERO_MQ_PGM_CONFIGURATION_QUALIFIED_NAME,
                                              ZeroMqConfigurationUtils::getZeroMqPgmConfiguration));
      configurations.addAll(getConfigurations(options,
                                              field,
                                              ZeroMqConfigurationUtils.ZERO_MQ_EPGM_CONFIGURATION_QUALIFIED_NAME,
                                              ZeroMqConfigurationUtils::getZeroMqEpgmConfiguration));
      configurations.addAll(getConfigurations(options,
                                              field,
                                              ZeroMqConfigurationUtils.ZERO_MQ_INPROC_CONFIGURATION_QUALIFIED_NAME,
                                              ZeroMqConfigurationUtils::getZeroMqInprocConfiguration));
      return configurations;
   }

   private static IModel getDeploymentModel(IJellyFishCommandOptions options) {
      IParameter<?> deploymentModelParameter = options.getParameters()
            .getParameter(CommonParameters.DEPLOYMENT_MODEL.getName());
      if (deploymentModelParameter == null) {
         throw new IllegalStateException(CommonParameters.DEPLOYMENT_MODEL.getName() + " parameter is not set");
      }
      String deploymentModel = deploymentModelParameter.getStringValue();
      return options.getSystemDescriptor()
            .findModel(deploymentModel)
            .orElseThrow(() -> new IllegalStateException("Cannot find deployment model " + deploymentModel));
   }

   /**
    * Returns all of the given model's links that contain the given field as either a target or source.
    */
   private static Collection<IModelLink<?>> findLinks(IModel model, IDataReferenceField field) {
      return model.getLinks()
            .stream()
            .filter(link -> Objects.equals(field, link.getSource()) || Objects.equals(field, link.getTarget()))
            .collect(Collectors.toList());
   }

   /**
    * Returns the collection of configurations for the given field.
    *
    * @param options             jellyfish options
    * @param field               field
    * @param configQualifiedName fully qualified name of configuration sd data type
    * @param function            function to convert {@link IPropertyDataValue} to the configuration type
    * @return the collection of configurations for the given field
    */
   private <T> Collection<T> getConfigurations(IJellyFishCommandOptions options,
                                               IDataReferenceField field, String configQualifiedName,
                                               Function<IPropertyDataValue, T> function) {
      IModel deploymentModel = sdService.getAggregatedView(getDeploymentModel(options));
      Collection<IModelLink<?>> links = findLinks(deploymentModel, field);
      Collection<T> configurations = new LinkedHashSet<>();
      for (IModelLink<?> link : links) {
         configurations.addAll(getConfigurations(link, configQualifiedName, function));
      }
      return configurations;
   }

   private static <T> Collection<T> getConfigurations(IModelLink<? extends IReferenceField> link, String qualifiedName,
                                                      Function<IPropertyDataValue, T> function) {
      Collection<IPropertyDataValue> propertyValues = link.getProperties()
            .stream()
            .filter(property -> DataTypes.DATA == property.getType())
            .filter(property -> qualifiedName.equals(
                  property.getReferencedDataType().getFullyQualifiedName()))
            .filter(
                  property -> FieldCardinality.SINGLE == property.getCardinality())
            .map(IProperty::getData)
            .collect(Collectors.toList());
      Collection<T> configurations = new ArrayList<>(propertyValues.size());
      for (IPropertyDataValue value : propertyValues) {
         if (!value.isSet()) {
            throw new IllegalStateException(String.format("Configuration is not completely set for link %s%s -> %s",
                                                          link.getName().orElse("") + " ", link.getSource().getName(),
                                                          link.getTarget().getName()));
         }
         configurations.add(function.apply(value));
      }
      return configurations;
   }

   static IDataField getField(IPropertyDataValue value, String fieldName) {
      return value.getFieldByName(fieldName)
            .orElseThrow(() -> new IllegalStateException("Missing " + fieldName + " field"));
   }

   @Activate
   public void activate() {
      logService.debug(getClass(), "activated");
   }

   @Deactivate
   public void deactivate() {
      logService.debug(getClass(), "deactivated");
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY,
         policy = ReferencePolicy.STATIC,
         unbind = "removeLogService")
   public void setLogService(ILogService ref) {
      this.logService = ref;
   }

   public void removeLogService(ILogService ref) {
      setLogService(null);
   }

   @Reference(cardinality = ReferenceCardinality.MANDATORY,
         policy = ReferencePolicy.STATIC,
         unbind = "removeSystemDescriptorService")
   public void setSystemDescriptorService(ISystemDescriptorService ref) {
      this.sdService = ref;
   }

   public void removeSystemDescriptorService(ISystemDescriptorService ref) {
      setSystemDescriptorService(null);
   }

}