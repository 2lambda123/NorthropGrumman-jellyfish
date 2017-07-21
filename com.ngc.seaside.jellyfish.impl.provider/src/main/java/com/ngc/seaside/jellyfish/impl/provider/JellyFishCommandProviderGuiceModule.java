package com.ngc.seaside.jellyfish.impl.provider;

import com.google.inject.AbstractModule;

import com.ngc.seaside.jellyfish.api.IJellyFishCommandProvider;

/**
 * Guice wrapper around the {@link JellyFishCommandProvider} implementation.
 */
public class JellyFishCommandProviderGuiceModule extends AbstractModule {

   @Override
   protected void configure() {
      bind(IJellyFishCommandProvider.class).to(JellyFishCommandProviderGuiceWrapper.class);
   }

}
