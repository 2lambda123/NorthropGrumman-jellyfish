/**
 * 
 */
package com.ngc.seaside.systemdescriptor.validation.util;

import com.ngc.seaside.systemdescriptor.systemDescriptor.FieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model;
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage;

/**
 * @author ceacide
 *
 * Utility methods to help with validation 
 */
public class ValidatorUtil {
	

	/**
	 * 
	 * Grabs the container that contains the field declaration for this requirement 
	 * 
	 * @param declaration that we want its Model container for 
	 * @return Model that contains the refined part
	 */
	static public Model getModel(FieldDeclaration declaration){
		if (declaration.eContainer().eContainer().eClass().equals(SystemDescriptorPackage.Literals.MODEL)) {
			return (Model)declaration.eContainer().eContainer();
		}
		return null;
	}
	
	/**
	 * 
	 * Looks for the part declaration thats being refined in the Model hierarchy
	 * 
	 * @param model used as a starting point for the Model hierarchy 
	 * @param fieldDeclaration the Name of the variable we are looking for 
	 * @return boolean as to whether we have found the variable in the Model
	 *    hierarchy 
	 */
	static private boolean findPartDeclarationName(Model model, String fieldDeclaration) {
		boolean found = false;
		Model parentModel = model.getRefinedModel();
		
		while (parentModel != null) {
			// Part Declaration
			if (parentModel.getParts() != null && 
					parentModel.getParts().getDeclarations() != null)
			{
				for(FieldDeclaration fieldDec : parentModel.getParts().getDeclarations()) {
					if (fieldDec.getName().equals(fieldDeclaration)) {
						found = true;
						break;
					}
				}
			}
			parentModel = parentModel.getRefinedModel();
		}
		
		return found;
	}
	
	/**
	 * 
	 * Looks for the require declaration thats being refined in the Model hierarchy
	 * 
	 * @param model used as a starting point for the Model hierarchy 
	 * @param fieldDeclaration the Name of the variable we are looking for 
	 * @return boolean as to whether we have found the variable in the Model
	 *    hierarchy 
	 */
	static private boolean findRequireDeclarationName(Model model, String fieldDeclaration) {
		boolean found = false;
		Model parentModel = model.getRefinedModel();
		
		while (parentModel != null) {
			if (parentModel.getRequires() != null && 
					parentModel.getRequires().getDeclarations() != null)
			{
				for(FieldDeclaration fieldDec : parentModel.getRequires().getDeclarations()) {
					if (fieldDec.getName().equals(fieldDeclaration)) {
						found = true;
						break;
					}
				}
			}
			parentModel = parentModel.getRefinedModel();
		}
		
		return found;
	}
	
	/**
	 * Validates that the Model thats not refining another Model can't then do a refine on
	 *  a fieldDeclaration 
	 *  
	 * @param fieldDeclaration thats being validated 
	 */
	static public String checkForNonRefinedModelUsingRefinedfields(FieldDeclaration fieldDeclaration) {
		//Bring us up to the part model
		String msg = "";
		Model fieldDeclarationModel = getModel(fieldDeclaration);
		if (fieldDeclarationModel !=null) {
			if (((Model)fieldDeclaration.eContainer().eContainer()).getRefinedModel() == null) {
				msg = String.format(
						"Cannot refine field Declaration  '%s' if this model '%s' is not refining another model",
						fieldDeclaration.getName(), fieldDeclarationModel.getName());
			}
		}
		return msg;
	}
	
	/**
	 * Validates that the Model or somewhere in the hierarchy that this requirement declaration 
	 * has already been defined 
	 *  
	 * @param fieldDeclaration thats being validated 
	 */
	static public String checkForRefinementOfAFieldThatsNotInModelBeingRefined(FieldDeclaration fieldDeclaration) {
		String msg = "";
		Model fieldDeclarationModel = ValidatorUtil.getModel(fieldDeclaration);
		if (fieldDeclarationModel != null){
			if (fieldDeclaration.eClass().equals(SystemDescriptorPackage.Literals.REFINED_PART_DECLARATION)) {
				if (!ValidatorUtil.findPartDeclarationName(fieldDeclarationModel, fieldDeclaration.getName())){
					msg = String.format(
							"The field Declaration '%s' cannot be refined without first being defined in Model"
							+ " '%s' thats being refined or its hierarchy",
							fieldDeclaration.getName(), fieldDeclarationModel.getRefinedModel().getName());	
				}
			}
			else if (fieldDeclaration.eClass().equals(SystemDescriptorPackage.Literals.REFINED_REQUIRE_DECLARATION)) {
				if (!ValidatorUtil.findRequireDeclarationName(fieldDeclarationModel, fieldDeclaration.getName())){
					msg = String.format(
							"The field Declaration '%s' cannot be refined without first being defined in Model"
							+ " '%s' thats being refined or its hierarchy",
							fieldDeclaration.getName(), fieldDeclarationModel.getRefinedModel().getName());	
				}
				
			}
			
		}
		return msg;
	}

}
