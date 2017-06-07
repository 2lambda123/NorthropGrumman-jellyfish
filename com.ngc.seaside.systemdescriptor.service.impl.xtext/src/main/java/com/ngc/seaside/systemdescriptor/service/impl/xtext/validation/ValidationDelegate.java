package com.ngc.seaside.systemdescriptor.service.impl.xtext.validation;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.ngc.blocs.service.log.api.ILogService;
import com.ngc.seaside.systemdescriptor.extension.IValidatorExtension;
import com.ngc.seaside.systemdescriptor.model.api.IPackage;
import com.ngc.seaside.systemdescriptor.model.api.ISystemDescriptor;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.data.IDataField;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.IModelReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.link.IModelLink;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenario;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenarioStep;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.IUnwrappable;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.WrappedSystemDescriptor;
import com.ngc.seaside.systemdescriptor.model.impl.xtext.exception.UnrecognizedXtextTypeException;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Data;
import com.ngc.seaside.systemdescriptor.systemDescriptor.DataFieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.InputDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.LinkDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model;
import com.ngc.seaside.systemdescriptor.systemDescriptor.OutputDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Package;
import com.ngc.seaside.systemdescriptor.systemDescriptor.PartDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.RequireDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Scenario;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Step;
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage;
import com.ngc.seaside.systemdescriptor.validation.SystemDescriptorValidator;
import com.ngc.seaside.systemdescriptor.validation.api.ISystemDescriptorValidator;
import com.ngc.seaside.systemdescriptor.validation.api.IValidationContext;

import org.eclipse.emf.ecore.EObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ValidationDelegate implements IValidatorExtension {

   private final Collection<ISystemDescriptorValidator> validators = Collections.synchronizedList(new ArrayList<>());
   private final SystemDescriptorValidator validator;
   private final ILogService logService;

   @Inject
   public ValidationDelegate(SystemDescriptorValidator validator,
                             ILogService logService) {
      this.validator = Preconditions.checkNotNull(validator, "validator may not be null!");
      this.logService = Preconditions.checkNotNull(logService, "logService may not be null!");
   }

   @Override
   public void validate(EObject source, ValidationHelper helper) {
      // Walk the source object up the containment hierarchy to find the Package object.  Build a system descriptor
      // for the entire package.  Then instruct the validator to validate associated wrapper of the source object.
      ISystemDescriptor descriptor = new WrappedSystemDescriptor(findPackage(source));
      doValidate(source, helper, descriptor);
   }

   public void addValidator(ISystemDescriptorValidator validator) {
      Preconditions.checkNotNull(validator, "validator may not be null!");
      logService.debug(getClass(), "Adding validator %s.", validator);

      // Synchronize to ensure the add and size calls are safe.
      synchronized (validators) {
         validators.add(validator);
         // If this is the first validator added, register our self.
         if (validators.size() == 1) {
            registerSelf();
         }
      }
   }

   public boolean removeValidator(ISystemDescriptorValidator validator) {
      Preconditions.checkNotNull(validator, "validator may not be null!");
      boolean result;
      // Synchronize to ensure the remove and isEmpty calls are safe.
      synchronized (validators) {
         result = validators.remove(validator);
         if (result && validators.isEmpty()) {
            logService.debug(getClass(), "Removed validator %s.", validator);
            // If there are no validators, unregister our self with the DSL.
            unregisterSelf();
         }
      }
      return result;
   }

   protected void doValidate(EObject source, ValidationHelper helper, ISystemDescriptor descriptor) {
      switch (source.eClass().getClassifierID()) {
         case SystemDescriptorPackage.PACKAGE:
            String name = ((Package) source).getName();
            IValidationContext<IPackage> ctx1 = newContext(
                  descriptor.getPackages()
                        .getByName(name)
                        .get(),
                  helper);
            doValidation(ctx1);
            break;
         case SystemDescriptorPackage.DATA:
            String packageName = ((Package) source.eContainer()).getName();
            IValidationContext<IData> ctx2 = newContext(
                  descriptor.findData(packageName, ((Data) source).getName()).get(),
                  helper);
            doValidation(ctx2);
            break;
         case SystemDescriptorPackage.DATA_FIELD_DECLARATION:
            String fieldName = ((DataFieldDeclaration) source).getName();
            String dataName = ((Data) source.eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer()).getName();
            IValidationContext<IDataField> ctx3 = newContext(
                  descriptor.findData(packageName, dataName).get()
                        .getFields()
                        .getByName(fieldName)
                        .get(),
                  helper);
            doValidation(ctx3);
            break;
         case SystemDescriptorPackage.MODEL:
            packageName = ((Package) source.eContainer()).getName();
            IValidationContext<IModel> ctx4 = newContext(
                  descriptor.findModel(packageName, ((Model) source).getName()).get(),
                  helper);
            doValidation(ctx4);
            break;
         case SystemDescriptorPackage.INPUT_DECLARATION:
            fieldName = ((InputDeclaration) source).getName();
            String modelName = ((Model) source.eContainer().eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer().eContainer()).getName();
            IValidationContext<IDataReferenceField> ctx5 = newContext(
                  descriptor.findModel(packageName, modelName).get()
                        .getInputs()
                        .getByName(fieldName)
                        .get(),
                  helper);
            doValidation(ctx5);
            break;
         case SystemDescriptorPackage.OUTPUT_DECLARATION:
            fieldName = ((OutputDeclaration) source).getName();
            modelName = ((Model) source.eContainer().eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer().eContainer()).getName();
            IValidationContext<IDataReferenceField> ctx6 = newContext(
                  descriptor.findModel(packageName, modelName).get()
                        .getOutputs()
                        .getByName(fieldName)
                        .get(),
                  helper);
            doValidation(ctx6);
            break;
         case SystemDescriptorPackage.PART_DECLARATION:
            fieldName = ((PartDeclaration) source).getName();
            modelName = ((Model) source.eContainer().eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer().eContainer()).getName();
            IValidationContext<IModelReferenceField> ctx7 = newContext(
                  descriptor.findModel(packageName, modelName).get()
                        .getParts()
                        .getByName(fieldName)
                        .get(),
                  helper);
            doValidation(ctx7);
            break;
         case SystemDescriptorPackage.REQUIRE_DECLARATION:
            fieldName = ((RequireDeclaration) source).getName();
            modelName = ((Model) source.eContainer().eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer().eContainer()).getName();
            IValidationContext<IModelReferenceField> ctx8 = newContext(
                  descriptor.findModel(packageName, modelName).get()
                        .getRequiredModels()
                        .getByName(fieldName)
                        .get(),
                  helper);
            doValidation(ctx8);
            break;
         case SystemDescriptorPackage.LINK_DECLARATION:
            modelName = ((Model) source.eContainer().eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer().eContainer()).getName();
            IValidationContext<IModelLink<?>> ctx9 = newContext(
                  findLink(descriptor.findModel(packageName, modelName).get(), (LinkDeclaration) source),
                  helper);
            doValidation(ctx9);
            break;
         case SystemDescriptorPackage.SCENARIO:
            String scenarioName = ((Scenario) source).getName();
            modelName = ((Model) source.eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer()).getName();
            IValidationContext<IScenario> ctx10 = newContext(
                  descriptor.findModel(packageName, modelName).get()
                        .getScenarios()
                        .getByName(scenarioName)
                        .get(),
                  helper);
            doValidation(ctx10);
            break;
         case SystemDescriptorPackage.GIVEN_STEP:
         case SystemDescriptorPackage.WHEN_STEP:
         case SystemDescriptorPackage.THEN_STEP:
            scenarioName = ((Scenario) source.eContainer().eContainer()).getName();
            modelName = ((Model) source.eContainer().eContainer().eContainer()).getName();
            packageName = ((Package) source.eContainer().eContainer().eContainer().eContainer()).getName();
            IValidationContext<IScenarioStep> ctx11 = newContext(
                  findStep(descriptor.findModel(packageName, modelName).get(), scenarioName, (Step) source),
                  helper);
            doValidation(ctx11);
            break;
         default:
            // Do nothing, this is not a type we want to validate.
      }
   }

   protected <T> IValidationContext<T> newContext(T object, ValidationHelper helper) {
      return new ProxyingValidationContext<>(object, helper);
   }

   protected void doValidation(IValidationContext<?> context) {
      synchronized (validators) {
         for (ISystemDescriptorValidator validator : validators) {
            safelyInvokeValidator(validator, context);
         }
      }
   }

   private void safelyInvokeValidator(ISystemDescriptorValidator validator, IValidationContext<?> context) {
      // Do not allow a misbehaving validator stop the entire parsing process.
      try {
         validator.validate(context);
      } catch (Throwable t) {
         logService.error(getClass(),
                          t,
                          "Validator %s threw an exception, consuming exception so parsing may continue.",
                          validator.getClass());
      }
   }

   private void registerSelf() {
      // Note this method and unregisterSelf get called while by guarded by the validators list.  This means this
      // and unregisterSelf can't be called concurrently.
      logService.trace(getClass(), "Registering self as a DSL validation extension.");
      validator.addValidatorExtension(this);
   }

   private void unregisterSelf() {
      // See registerSelf comments above.
      logService.trace(getClass(), "Unregistering self as a DSL validation extension.");
      validator.removeValidatorExtension(this);
   }

   private static Package findPackage(EObject source) {
      if (source.eClass().equals(SystemDescriptorPackage.Literals.PACKAGE)) {
         return (Package) source;
      }
      EObject parent = source.eContainer();
      if (parent == null) {
         throw new IllegalStateException(String.format(
               "unable to find a root container object of type %s while walking the containment hierarchy of %s!",
               Package.class.getName(),
               source));
      }
      return findPackage(parent);
   }

   private static IModelLink<?> findLink(IModel model, LinkDeclaration xtext) {
      return (IModelLink<?>) model.getLinks()
            .stream()
            .map(l -> (IUnwrappable<?>) l)
            .filter(l -> l.unwrap().equals(xtext))
            .findAny()
            .get();
   }

   @SuppressWarnings("unchecked")
   private static IScenarioStep findStep(IModel model, String scenarioName, Step xtext) {
      IScenario scenario = model.getScenarios().getByName(scenarioName).get();
      Collection<IScenarioStep> steps;
      switch (xtext.eClass().getClassifierID()) {
         case SystemDescriptorPackage.GIVEN_STEP:
            steps = scenario.getGivens();
            break;
         case SystemDescriptorPackage.WHEN_STEP:
            steps = scenario.getWhens();
            break;
         case SystemDescriptorPackage.THEN_STEP:
            steps = scenario.getThens();
            break;
         default:
            throw new UnrecognizedXtextTypeException(xtext);
      }
      return (IScenarioStep) steps.stream()
            .map(s -> (IUnwrappable<Step>) s)
            .filter(s -> s.unwrap().equals(xtext))
            .findAny()
            .get();
   }
}
