package com.ngc.seaside.systemdescriptor.validation;

import com.google.inject.Inject;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model;
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage;

import org.eclipse.xtext.resource.impl.ResourceDescriptionsProvider;
import org.eclipse.xtext.validation.Check;

public class RefinedModelValidator extends AbstractUnregisteredSystemDescriptorValidator {
    @Inject
    private ResourceDescriptionsProvider resourceDescriptionsProvider;

    @Check
    public void checkDoesNotParseModelThatRefinesItself(Model model) {
    }

    @Check
    public void checkDoesNotParseModelsThatCircularlyRefineEachOther(Model model) {
    }

    @Check
    public void checkDoesNotParseModelThatRedeclaresInputs() {
    }

    @Check
    public void checkDoesNotParseModelThatRedeclaresOutputs() {
    }

    @Check
    public void checkDoesNotParseModelThatRedeclaresRequires() {
    }

    @Check
    public void checkDoesNotParseModelThatRedeclaresScenarios() {
    }
}
