package com.ngc.seaside.jellyfish.cli.command.report.console;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

import com.ngc.seaside.jellyfish.api.ICommand;

public class ConsoleAnalysisReportCommandGuiceModule extends AbstractModule {

   @Override
   protected void configure() {
      Multibinder.newSetBinder(binder(), ICommand.class)
            .addBinding()
            .to(ConsoleAnalysisReportCommandGuiceWrapper.class);
   }
}
