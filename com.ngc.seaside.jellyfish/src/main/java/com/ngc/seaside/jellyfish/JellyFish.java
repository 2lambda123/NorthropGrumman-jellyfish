package com.ngc.seaside.jellyfish;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.ngc.seaside.jellyfish.impl.provider.JellyFishCommandProviderModule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

/**
 * @author bperkins
 */
public class JellyFish {

   /**
    * Run the JellyFish application
    */
   public static void main(String[] args) {
      Injector injector = Guice.createInjector(getModules());
      JellyFishCommandProviderModule provider = injector.getInstance(JellyFishCommandProviderModule.class);
      provider.run(args);
      
   }

   /**
    * Get a collection of Guice modules from the classpath. The service loader will look for a
    * property file called com.google.inject.Module located in the META-INF/services directory
    * of the jar. The file just needs to list all Guice Module classes with the fully qualified name.
    *
    * @return A collection of modules or an empty collection.
    */
   private static Collection<Module> getModules() {
      Collection<Module> modules = new ArrayList<>();
      modules.add(new JellyFishServiceModule());
      for (Module dynamicModule : ServiceLoader.load(Module.class)) {
         //TODO log this
         System.out.println(String.format("%s", dynamicModule.getClass()));
         modules.add(dynamicModule);
      }
      return modules;
   }
}
