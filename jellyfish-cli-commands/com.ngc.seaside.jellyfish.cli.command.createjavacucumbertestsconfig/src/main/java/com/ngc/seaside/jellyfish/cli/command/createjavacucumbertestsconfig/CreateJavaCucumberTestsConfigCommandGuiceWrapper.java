package com.ngc.seaside.jellyfish.cli.command.createjavacucumbertestsconfig;

import com.google.inject.Inject;
import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IUsage;
import com.ngc.seaside.jellyfish.cli.command.createjavaservicegeneratedconfig.plugin.IConfigurationPlugin;
import com.ngc.seaside.jellyfish.service.buildmgmt.api.IBuildManagementService;
import com.ngc.seaside.jellyfish.service.name.api.IPackageNamingService;
import com.ngc.seaside.jellyfish.service.name.api.IProjectNamingService;
import com.ngc.seaside.jellyfish.service.template.api.ITemplateService;

import java.util.Set;

public class CreateJavaCucumberTestsConfigCommandGuiceWrapper implements IJellyFishCommand {

   private final CreateJavaCucumberTestsConfigCommand delegate = new CreateJavaCucumberTestsConfigCommand();

   @Inject
   public CreateJavaCucumberTestsConfigCommandGuiceWrapper(ILogService logService,
                                                           ITemplateService templateService,
                                                           IProjectNamingService projectNamingService,
                                                           IPackageNamingService packageNamingService,
                                                           IBuildManagementService buildManagementService,
                                                           Set<IConfigurationPlugin> plugins) {
      delegate.setLogService(logService);
      delegate.setTemplateService(templateService);
      delegate.setProjectNamingService(projectNamingService);
      delegate.setPackageNamingService(packageNamingService);
      delegate.setBuildManagementService(buildManagementService);
      plugins.forEach(delegate::addConfigurationPlugin);
      delegate.activate();
   }

   @Override
   public String getName() {
      return delegate.getName();
   }

   @Override
   public IUsage getUsage() {
      return delegate.getUsage();
   }

   @Override
   public void run(IJellyFishCommandOptions commandOptions) {
      delegate.run(commandOptions);
   }

}
