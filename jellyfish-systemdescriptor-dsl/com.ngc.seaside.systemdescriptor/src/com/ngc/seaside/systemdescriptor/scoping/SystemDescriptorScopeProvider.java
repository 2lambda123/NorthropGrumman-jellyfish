package com.ngc.seaside.systemdescriptor.scoping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EReference;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.Scopes;
import org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider;

import com.google.common.base.Preconditions;
import com.ngc.seaside.systemdescriptor.systemDescriptor.BasePartDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.BaseRequireDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Data;
import com.ngc.seaside.systemdescriptor.systemDescriptor.FieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.FieldReference;
import com.ngc.seaside.systemdescriptor.systemDescriptor.LinkableExpression;
import com.ngc.seaside.systemdescriptor.systemDescriptor.Model;
import com.ngc.seaside.systemdescriptor.systemDescriptor.PropertyFieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.PropertyValueExpression;
import com.ngc.seaside.systemdescriptor.systemDescriptor.PropertyValueExpressionPathSegment;
import com.ngc.seaside.systemdescriptor.systemDescriptor.ReferencedDataModelFieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.ReferencedPropertyFieldDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.RefinedLinkDeclaration;
import com.ngc.seaside.systemdescriptor.systemDescriptor.SystemDescriptorPackage;

/**
 * The scope provider for the System Descriptor language.
 */
public class SystemDescriptorScopeProvider extends AbstractDeclarativeScopeProvider {

	/**
	 * Places all declared properties of the current model as well as any
	 * refined models in scope for property value expressions.
	 * 
	 * @return the scope for a property value expression
	 */
	public IScope scope_PropertyValueExpression_declaration(
			PropertyValueExpression context,
			EReference reference) {
		Collection<PropertyFieldDeclaration> properties = new ArrayList<>();

		Model model = (Model) context.eContainer() // PropertyValueAssignment
				.eContainer() // Properties
				.eContainer(); // Model

		do {
			if (model.getProperties() != null) {
				properties.addAll(model.getProperties().getDeclarations());
			}
			model = model.getRefinedModel();
		} while (model != null);

		return Scopes.scopeFor(properties);
	}

	/**
	 * Places all data fields of a data type in scope for property path segment
	 * where a value of a complex type is being set.
	 * 
	 * @return the scope of the segment
	 */
	public IScope scope_PropertyValueExpressionPathSegment_fieldDeclaration(
			PropertyValueExpressionPathSegment segment,
			EReference reference) {
		PropertyValueExpression exp = (PropertyValueExpression) segment.eContainer();
		Data data = getDataModelForProperty(exp.getDeclaration());

		for (int i = 0; i < exp.getPathSegments().indexOf(segment) && data != null; i++) {
			// Get the text value of the field. We have to do this because the
			// linking has not yet completed and proxy objects are set in the
			// data model.
			List<INode> nodes = NodeModelUtils.findNodesForFeature(
					exp.getPathSegments().get(i),
					SystemDescriptorPackage.Literals.PROPERTY_VALUE_EXPRESSION_PATH_SEGMENT__FIELD_DECLARATION);
			String fieldName = NodeModelUtils.getTokenText(nodes.get(0));

			// Get the field with that name. Then filter for fields that have a
			// complex data type (ie, not a primitive).
			ReferencedDataModelFieldDeclaration dataField = data.getFields().stream()
					.filter(f -> f.getName().equals(fieldName))
					.filter(f -> f instanceof ReferencedDataModelFieldDeclaration)
					.map(f -> (ReferencedDataModelFieldDeclaration) f)
					.findFirst()
					.orElse(null);

			// Note that the data model can be an enumeration at this point
			// (if the user created an invalid path, the validators will
			// catch it after scoping is finished).
			data = dataField.getDataModel() instanceof Data
					? (Data) dataField.getDataModel()
					: null;
		}

		return data != null
				? Scopes.scopeFor(data.getFields())
				: delegateGetScope(segment, reference);
	}

	public IScope scope_FieldReference_fieldDeclaration(FieldReference context, EReference reference) {
		IScope scope;
		if (context.eContainer() instanceof RefinedLinkDeclaration) {
			// This indicates the source or target of a link is directly
			// referencing an input or output. In the example below "a",
			// is the current context.
			// link a -> some.thing
			Model model = (Model) context
					.eContainer() // RefinedLinkDeclaration
					.eContainer() // Links
					.eContainer(); // Model
			scope = Scopes.scopeFor(getLinkableFieldsFrom(model));
		} else if (context.eContainer() instanceof LinkableExpression) {
			// This indicates the source or target of a link is an expression.
			// In the example below, "some", is the current context.
			// link a -> some.thing
			Model model = (Model) context
					.eContainer() // LinkableExpression
					.eContainer() // RefinedLinkDeclaration
					.eContainer() // Links
					.eContainer(); // Model
			scope = Scopes.scopeFor(getLinkableFieldsFrom(model));
		} else {
			scope = delegateGetScope(context, reference);
		}
		return scope;
	}

	/**
	 * Provides scope for a link expression of the form
	 * {@code link someInput to somePart.someMoreInput}.
	 * 
	 * @return scope for a link expression
	 */
	public IScope scope_LinkableExpression_tail(LinkableExpression context, EReference reference) {
		// Get the field reference thus far. If we are parsing the expression
		// link someInput to somePart.someMoreInput
		// than ref will equal the part "somePart".
		FieldReference ref = (FieldReference) context.getRef();
		// Get the field declaration the reference is pointing to.
		FieldDeclaration fieldDeclaration = ref.getFieldDeclaration();

		IScope scope;

		// We need to determine the type of the field declaration. Right now we
		// only support nested expressions for models. More requires and parts
		// can declare other models as fields. Input and output must declare
		// data as fields. Since we can't yet reference the contents of data,
		// we don't have to worry about referencing input or output.
		if (fieldDeclaration.eClass().equals(SystemDescriptorPackage.Literals.BASE_REQUIRE_DECLARATION)) {
			BaseRequireDeclaration casted = (BaseRequireDeclaration) fieldDeclaration;
			// Include all field declarations of the referenced model in the
			// scope.
			scope = Scopes.scopeFor(getLinkableFieldsFrom(casted.getType()));
		} else if (fieldDeclaration.eClass().equals(SystemDescriptorPackage.Literals.BASE_PART_DECLARATION)) {
			// Include all field declarations of the referenced model in the
			// scope.
			BasePartDeclaration casted = (BasePartDeclaration) fieldDeclaration;
			scope = Scopes.scopeFor(getLinkableFieldsFrom(casted.getType()));
		} else {
			// Otherwise, do the default behavior.
			scope = delegateGetScope(context, reference);
		}

		return scope;
	}

	/**
	 * Gets all field declarations that can be referenced that are contained by
	 * the given model.
	 *
	 * @param model
	 * @return all field declarations that can be referenced that are contained
	 *         by the given model
	 */
	private static Collection<FieldDeclaration> getLinkableFieldsFrom(Model model) {
		Collection<FieldDeclaration> fields = new ArrayList<>();

		do {
			if (model.getInput() != null) {
				fields.addAll(model.getInput().getDeclarations());
			}
			if (model.getOutput() != null) {
				fields.addAll(model.getOutput().getDeclarations());
			}
			if (model.getRequires() != null) {
				fields.addAll(model.getRequires().getDeclarations());
			}
			if (model.getParts() != null) {
				fields.addAll(model.getParts().getDeclarations());
			}
			model = model.getRefinedModel();
		} while (model != null);

		return fields;
	}

	private static Data getDataModelForProperty(PropertyFieldDeclaration declaration) {
		Preconditions.checkState(
				declaration instanceof ReferencedPropertyFieldDeclaration,
				"expected the declaration to be an instance of ReferencedPropertyFieldDeclaration!"
						+ "  Otherwise, why would the declaration need scoping help?");
		ReferencedPropertyFieldDeclaration referencedDeclaration = (ReferencedPropertyFieldDeclaration) declaration;

		Preconditions.checkState(
				referencedDeclaration.getDataModel() instanceof Data,
				"expected the declaration to have a data model of data instead of an enumeration!"
						+ "  Otherwise, you can't have a complex expression!");
		return (Data) referencedDeclaration.getDataModel();
	}
}
