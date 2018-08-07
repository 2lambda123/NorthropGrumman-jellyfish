package com.ngc.seaside.jellyfish.cli.command.createjavasystemproject;

import com.google.inject.Inject;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandOptions;
import com.ngc.seaside.jellyfish.api.IJellyFishCommandProvider;
import com.ngc.seaside.jellyfish.api.IUsage;

public class CreateJavaSystemProjectCommandGuiceWrapper implements IJellyFishCommand {

   private final CreateJavaSystemProjectCommand delegate = new CreateJavaSystemProjectCommand();

   @Inject
   public CreateJavaSystemProjectCommandGuiceWrapper(ILogService logService,
                                                      IJellyFishCommandProvider jellyFishCommandProvider) {
      delegate.setLogService(logService);
      delegate.setJellyFishCommandProvider(jellyFishCommandProvider);
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
