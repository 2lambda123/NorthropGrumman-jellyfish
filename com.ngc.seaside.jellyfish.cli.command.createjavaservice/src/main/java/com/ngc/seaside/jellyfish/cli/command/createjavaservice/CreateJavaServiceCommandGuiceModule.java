package com.ngc.seaside.jellyfish.cli.command.createjavaservice;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import com.ngc.seaside.command.api.ICommand;
import com.ngc.seaside.jellyfish.api.IJellyFishCommand;
import com.ngc.seaside.jellyfish.api.JellyFishCommandConfiguration;
import com.ngc.seaside.jellyfish.cli.command.createjavaservice.dao.ITemplateDaoFactory;
import com.ngc.seaside.jellyfish.cli.command.createjavaservice.dao.TemplateDaoFactory;

@JellyFishCommandConfiguration(autoTemplateProcessing = false)
public class CreateJavaServiceCommandGuiceModule extends AbstractModule {

   @Override
   protected void configure() {
      Multibinder.newSetBinder(binder(), IJellyFishCommand.class)
            .addBinding().to(CreateJavaServiceCommandGuiceWrapper.class);
      Multibinder.newSetBinder(binder(), ICommand.class)
            .addBinding().to(CreateJavaServiceCommandGuiceWrapper.class);
      bind(ITemplateDaoFactory.class).to(TemplateDaoFactory.class);
   }
}
