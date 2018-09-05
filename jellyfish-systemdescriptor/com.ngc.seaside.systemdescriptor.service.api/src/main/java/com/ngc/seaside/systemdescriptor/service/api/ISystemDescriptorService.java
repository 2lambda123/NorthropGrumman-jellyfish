/**
 * UNCLASSIFIED
 * Northrop Grumman Proprietary
 * ____________________________
 *
 * Copyright (C) 2018, Northrop Grumman Systems Corporation
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of
 * Northrop Grumman Systems Corporation. The intellectual and technical concepts
 * contained herein are proprietary to Northrop Grumman Systems Corporation and
 * may be covered by U.S. and Foreign Patents or patents in process, and are
 * protected by trade secret or copyright law. Dissemination of this information
 * or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from Northrop Grumman.
 */
package com.ngc.seaside.systemdescriptor.service.api;

import com.ngc.seaside.systemdescriptor.model.api.ISystemDescriptor;
import com.ngc.seaside.systemdescriptor.model.api.data.IData;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;
import com.ngc.seaside.systemdescriptor.model.api.model.scenario.IScenarioStep;
import com.ngc.seaside.systemdescriptor.scenario.api.IScenarioStepHandler;
import com.ngc.seaside.systemdescriptor.validation.api.ISystemDescriptorValidator;

import java.nio.file.Path;
import java.util.Collection;

/**
 * The top level service used to interface with a system descriptor.  This is the entry point for most system descriptor
 * related operations.
 */
public interface ISystemDescriptorService {

   /**
    * Parses a system descriptor project where all the {@code .sd} files are organized under {@code src/main/sd}. Always
    * check the {@link IParsingResult#isSuccessful() result} of parsing to before inspecting the {@code
    * ISystemDescriptor}.  If the parsing was not successful, the descriptor may be in a inconsistent state.
    *
    * @param projectDirectory a path to the root directory of the system descriptor project
    * @return the result of parsing
    * @throws ParsingException if some exception occurs during parsing
    */
   IParsingResult parseProject(Path projectDirectory);

   /**
    * Parses a system descriptor project where all the {@code .sd} files are located in the artifact specified by the
    * given identifier.
    *
    * @param artifactIdentifier project artifact identifier
    * @return the result of parsing
    * @throws ParsingException              if some exception occurs during parsing
    * @throws UnsupportedOperationException the implementation does not support parsing artifact projects
    */
   IParsingResult parseProject(String artifactIdentifier);

   /**
    * Parses the given {@code .sd} files.  Always check the {@link IParsingResult#isSuccessful() result} of parsing to
    * before inspecting the {@code ISystemDescriptor}.  If the parsing was not successful, the descriptor may be in a
    * inconsistent state.
    *
    * @param paths the paths that point to {@code .sd} files
    * @return the result of parsing
    * @throws ParsingException if some exception occurs during parsing
    */
   @Deprecated
   IParsingResult parseFiles(Collection<Path> paths);

   /**
    * Returns an immutable copy of the given {@code ISystemDescriptor}
    *
    * @param descriptor the descriptor to create a copy of
    * @return an immutable copy
    */
   ISystemDescriptor immutableCopy(ISystemDescriptor descriptor);

   /**
    * Gets a view of the given {@code IData} instance that takes into account the extension hierarchy of the data.
    * Invoking {@link IData#getFields()} on the view will return all fields declared in the object as well as all fields
    * declared in the data type the object extends.  If the object being extended also extends another object, those
    * fields will also be included.  Metadata functions in a similar manner.  Invoking {@link IData#getMetadata()}
    * returns all metadata declared directly by the type as well as metadata declared in extending types.  If duplicate
    * metadata values are declared, the value closest to the original data object in terms of the extension hierarchy is
    * used.  This makes it possible to <i>overwrite</i> metadata when extending data types.
    * <p/>
    * Any changes made to the view write-thought to the original data object.  Likewise, changes in the original object
    * will be manifested by the view.
    *
    * @param data the data to decorate
    * @return a view that aggregates the state of the object and the state of its extension hierarchy
    */
   IData getAggregatedView(IData data);

   /**
    * Gets a view of the given {@code IModel} instance that takes into account the refinement hierarchy of the model.
    * Invoking {@link IModel#getInputs()}, {@link IModel#getOutputs()}, {@link IModel#getParts()}, {@link
    * IModel#getRequiredModels()}, {@link IModel#getScenarios()}, or {@link IModel#getLinks()} on the view will return
    * all values declared in the object as well as all values declared in the model the model refines.  {@link
    * IModel#getLinkByName(String)} will also attempt to receive the link with the given name using all links in the
    * model as well as models that are refined.  If the model being refined also refines another model, those values
    * will also be included.  Metadata functions in a similar manner.  Invoking {@link IModel#getMetadata()} returns all
    * metadata declared directly by the model as well as metadata declared in refined model.  If duplicate metadata
    * values are declared, the value closest to the original model in terms of the refinement hierarchy is used.  This
    * makes it possible to <i>overwrite</i> metadata when refining models.
    * <p/>
    * Any changes made to the view write-thought to the original model.  Likewise, changes in teh original model will be
    * manifested by the view.
    *
    * @param model the model to decorate
    * @return a view that aggregates the state of the model and the state of its refinement hierarchy
    */
   IModel getAggregatedView(IModel model);

   /**
    * Gets an unmodifiable, threadsafe collection of all {@link IScenarioStepHandler} that have been registered.
    *
    * @return an unmodifiable, threadsafe collection of all {@link IScenarioStepHandler} that have been registered
    */
   Collection<IScenarioStepHandler> getScenarioStepHandlers();

   /**
    * Registers a handler that is used to process keywords referenced in scenario steps.
    *
    * @param handler the handler to register
    * @see IScenarioStepHandler
    * @see IScenarioStep#getKeyword()
    */
   void addScenarioStepHandler(IScenarioStepHandler handler);

   /**
    * Unregisters a step handler that has been registered via {@link #addScenarioStepHandler(IScenarioStepHandler)}.
    *
    * @param handler the handler to unregister
    * @return true if the handler was removed, false if the handler was never added
    */
   boolean removeScenarioStepHandler(IScenarioStepHandler handler);

   /**
    * Registers a validator that is used when parsing and validating system descriptor.
    *
    * @param validator the validator to register
    * @see ISystemDescriptorValidator
    */
   void addValidator(ISystemDescriptorValidator validator);

   /**
    * Unregisters a validator that has been registered via {@link #addValidator(ISystemDescriptorValidator)}.
    *
    * @param validator the validator to remove
    * @return true if the validator was removed, false if the validator was never added
    * @see ISystemDescriptorValidator
    */
   boolean removeValidator(ISystemDescriptorValidator validator);
}
