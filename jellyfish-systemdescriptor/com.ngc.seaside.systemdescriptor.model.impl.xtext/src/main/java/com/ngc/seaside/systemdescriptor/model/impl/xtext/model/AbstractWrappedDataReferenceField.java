package com.ngc.seaside.systemdescriptor.model.impl.xtext.model;

import com.ngc.seaside.systemdescriptor.model.api.metadata.IMetadata;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.AbstractWrappedXtext;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.metadata.WrappedMetadata;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.store.IWrapperResolver;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Data;
import com.ngc.seaside.systemdescriptor.systemDescriptor.FieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model;

/**
 * Base class for types that adapts input or output field declarations to {@link IDataReferenceField}s.
 *
 * This class is not threadsafe.
 *
 * @param <T> the type of XText field this class is wrapping
 * @param <I> the type of the class implementing this class
 */
public abstract class AbstractWrappedDataReferenceField<T extends FieldDeclaration, I extends AbstractWrappedDataReferenceField<T, I>> extends AbstractWrappedXtext<T>
      implements IDataReferenceField {

   public AbstractWrappedDataReferenceField(IWrapperResolver resolver, T wrapped) {
      super(resolver, wrapped);
   }

   @Override
   public IMetadata getMetadata() {
      return WrappedMetadata.fromXtextJson(wrapped.getMetadata());
   }

   @SuppressWarnings("unchecked")
   @Override
   public I setMetadata(IMetadata metadata) {
      wrapped.setMetadata(WrappedMetadata.toXtextJsonObject(metadata.getJson()));
      return (I) this;
   }

   @Override
   public String getName() {
      return wrapped.getName();
   }

   @Override
   public IModel getParent() {
      return resolver.getWrapperFor((Model) wrapped.eContainer().eContainer());
   }

   /**
    * Finds the XText {@code Data} object with the given name and package.
    *
    * @param name        the name of the of type
    * @param packageName the name of the package that contains the type
    * @return the XText type
    * @throws IllegalStateException if the XText type could not be found
    */
   Data findXtextData(String name, String packageName) {
      // Declared package protected for ease of testing.
      return doFindXtextData(resolver, name, packageName);
   }

   /**
    * Finds the XText {@code Data} object with the given name and package.
    *
    * @param resolver    the resolver that can location XText data objects
    * @param name        the name of the of type
    * @param packageName the name of the package that contains the type
    * @return the XText type
    * @throws IllegalStateException if the XText type could not be found
    */
   static Data doFindXtextData(IWrapperResolver resolver, String name, String packageName) {
      // Declared package protected for ease of testing.
      return resolver.findXTextData(name, packageName).orElseThrow(() -> new IllegalStateException(String.format(
            "Could not find XText type for data type '%s' in package '%s'!"
            + "  Make sure the IData object is added to"
            + " a package within the ISystemDescriptor before adding a reference to it!",
            name,
            packageName)));
   }
}