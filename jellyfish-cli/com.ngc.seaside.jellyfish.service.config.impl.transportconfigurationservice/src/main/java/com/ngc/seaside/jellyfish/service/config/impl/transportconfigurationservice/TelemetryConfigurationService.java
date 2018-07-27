package com.ngc.seaside.jellyfish.service.config.impl.transportconfigurationservice;

import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.service.config.api.ITelemetryConfigurationService;
import com.ngc.seaside.jellyfish.service.config.api.dto.RestConfiguration;
import com.ngc.seaside.jellyfish.service.config.api.dto.telemetry.RestTelemetryConfiguration;
import com.ngc.seaside.jellyfish.service.config.api.dto.telemetry.TelemetryConfiguration;
import com.ngc.seaside.jellyfish.service.config.impl.transportconfigurationservice.utils.RestConfigurationUtils;
import com.ngc.seaside.systemdescriptor.model.api.model.IModelReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.properties.IPropertyDataValue;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Pattern;

public class TelemetryConfigurationService extends ModelPropertyConfigurationService<TelemetryConfiguration>
         implements ITelemetryConfigurationService {

   private static final Pattern[] PATTERNS = { Pattern.compile("([a-z\\d])([A-Z]+)"),
      Pattern.compile("([A-Z])([A-Z][a-z\\d])") };
   private static final String[] REPLACEMENTS = { "$1_$2", "$1_$2" };

   public static final String REST_TELEMETRY_CONFIGURATION_QUALIFIED_NAME =
            "com.ngc.seaside.systemdescriptor.telemetry.RestTelemetry";

   public static final String CONFIG_FIELD_NAME = "config";

   @Override
   public Optional<String> getTransportTopicName(IJellyFishCommandOptions options, IModelReferenceField part) {
      Collection<TelemetryConfiguration> configs = getConfigurations(options, part.getType());
      if (configs.isEmpty()) {
         return Optional.empty();
      }
      String topic = part.getName();
      for (int i = 0; i < PATTERNS.length; i++) {
         topic = PATTERNS[i].matcher(topic).replaceAll(REPLACEMENTS[i]);
      }
      return Optional.of(topic.toUpperCase() + "_TELEMETRY");
   }

   @Override
   protected boolean isConfigurationProperty(String qualifiedName) {
      return REST_TELEMETRY_CONFIGURATION_QUALIFIED_NAME.equals(qualifiedName);
   }

   @Override
   protected TelemetryConfiguration convert(IPropertyDataValue value) {
      String type = value.getReferencedDataType().getFullyQualifiedName();
      switch (type) {
         case REST_TELEMETRY_CONFIGURATION_QUALIFIED_NAME:
            return convertRest(value);
         default:
            throw new IllegalArgumentException("Unable to convert " + type + " property to telemetry");
      }
   }

   private RestTelemetryConfiguration convertRest(IPropertyDataValue value) {
      RestTelemetryConfiguration configuration = new RestTelemetryConfiguration();
      IPropertyDataValue restProperty = value.getData(getField(value, CONFIG_FIELD_NAME));
      RestConfiguration restConfig = RestConfigurationUtils.getRestConfiguration(restProperty);
      configuration.setConfig(restConfig);
      return configuration;
   }

}