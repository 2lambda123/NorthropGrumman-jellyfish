/**
 * UNCLASSIFIED
 * Northrop Grumman Proprietary
 * ____________________________
 *
 * Copyright (C) 2019, Northrop Grumman Systems Corporation
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
package com.ngc.seaside.jellyfish.service.sequence.impl.sequenceservice.cases;

import com.google.common.collect.Iterables;

import com.ngc.seaside.jellyfish.service.scenario.api.MessagingParadigm;
import com.ngc.seaside.jellyfish.service.sequence.api.ISequence;
import com.ngc.seaside.jellyfish.service.sequence.api.ISequenceFlow;
import com.ngc.seaside.jellyfish.service.sequence.impl.sequenceservice.ItTestState;
import com.ngc.seaside.systemdescriptor.model.api.model.IDataReferenceField;
import com.ngc.seaside.systemdescriptor.model.api.model.IModel;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * This test verifies the sequence of a simple pub/sub component.  This component only has a single scenario and two
 * inputs and one output.  The inputs must be correlated.  The resulting sequence contains one flow with no impl.
 */
public class CreateSequenceForSinglePubSubComponentWithCorrelatedInputsTestCase {

   private final ItTestState state;

   /**
    * Creates a new test.
    */
   public CreateSequenceForSinglePubSubComponentWithCorrelatedInputsTestCase(ItTestState state) {
      this.state = state;
   }

   /**
    * Runs the test.
    */
   public void execute() {
      IModel model = state.getSystemDescriptor()
            .findModel("com.ngc.seaside.threateval.DefendedAreaTrackPriorityService")
            .get();
      IDataReferenceField systemTrack = model.getInputs()
            .getByName("systemTrack")
            .get();
      IDataReferenceField impactAssessment = model.getInputs()
            .getByName("impactAssessment")
            .get();
      IDataReferenceField trackPriority = model.getOutputs()
            .getByName("trackPriority")
            .get();

      List<ISequence> sequences = state.getService().getSequences(state.getOptions(), model);
      assertEquals("did not generate the correct number of sequences!",
                   1,
                   sequences.size());

      ISequence sequence = sequences.get(0);
      assertEquals("sequence IDs should be sequential!",
                   1,
                   sequence.getId());
      assertEquals("model not correct!",
                   model,
                   sequence.getModel());
      assertEquals("wrong input for sequence!",
                   systemTrack,
                   Iterables.get(sequence.getInputs(), 0));
      assertEquals("wrong input for sequence!",
                   impactAssessment,
                   Iterables.get(sequence.getInputs(), 1));
      assertEquals("wrong number of inputs",
                   2,
                   sequence.getInputs().size());
      assertEquals("wrong output for sequence!",
                   trackPriority,
                   Iterables.get(sequence.getOutputs(), 0));
      assertEquals("wrong number of outputs!",
                   1,
                   sequence.getOutputs().size());

      assertEquals("wrong number of flows!",
                   1,
                   sequence.getFlows().size());
      ISequenceFlow flow = Iterables.get(sequence.getFlows(), 0);
      assertFalse("flow should not have implementation!",
                  flow.getImplementation().isPresent());
      assertEquals("flow not correct!",
                   MessagingParadigm.PUBLISH_SUBSCRIBE,
                   flow.getMessagingFlow().getMessagingParadigm());
      assertTrue("inputs for flow not correct!",
                 flow.getInputs().contains(systemTrack));
      assertTrue("inputs for flow not correct!",
                 flow.getInputs().contains(impactAssessment));
      assertEquals("inputs for flow not correct!",
                   flow.getInputs().size(),
                   2);
      assertEquals("inputs for flow not correct!",
                   Iterables.get(flow.getOutputs(), 0),
                   trackPriority);
   }
}