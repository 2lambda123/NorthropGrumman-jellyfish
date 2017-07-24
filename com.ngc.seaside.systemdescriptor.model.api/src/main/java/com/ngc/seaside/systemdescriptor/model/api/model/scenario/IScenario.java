package com.ngc.seaside.systemdescriptor.model.api.model.scenario;

import com.ngc.seaside.systemdescriptor.model.api.INamedChild;
import com.ngc.seaside.systemdescriptor.model.api.metadata.IMetadata;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;

import java.util.Collection;

/**
 * Represents a scenario that describes the behavior of an {@link IModel}.  Operations that change the state of this
 * object may throw {@code UnsupportedOperationException}s if the object is immutable.
 */
public interface IScenario extends INamedChild<IModel> {

   /**
    * Gets the metadata associated with this scenario.
    *
    * @return the metadata associated with this scenario
    */
   IMetadata getMetadata();

   /**
    * Sets the metadata associated with this scenario.
    *
    * @param metadata the metadata associated with this scenario
    * @return this scenario
    */
   IScenario setMetadata(IMetadata metadata);

   /**
    * Gets the "given" steps of this scenario which are effectively pre conditions for the scenario.  Keywords for these
    * types of steps are usually verbs that are in the past tense.  The steps are listed in the order they were
    * declared. The returned list may not be modifiable if this object is immutable.
    *
    * @return the "given" steps of this scenario
    */
   Collection<IScenarioStep> getGivens();

   /**
    * Gets the "when" steps of this scenario which are effectively triggering conditions for the scenario.  Keywords for
    * these types of steps are usually verbs that are in the current tense.  The steps are listed in the order they were
    * declared.  The returned list may not be modifiable if this object is immutable.
    *
    * @return the "when" steps of this scenario
    */
   Collection<IScenarioStep> getWhens();

   /**
    * Gets the "then" steps of this scenario which are effectively post conditions for the scenario.  Keywords for these
    * types of steps are usually verbs that are in the future tense.  The steps are listed in the order they were
    * declared.  The returned list may not be modifiable if this object is immutable.
    *
    * @return the "then" steps of this scenario
    */
   Collection<IScenarioStep> getThens();
}
