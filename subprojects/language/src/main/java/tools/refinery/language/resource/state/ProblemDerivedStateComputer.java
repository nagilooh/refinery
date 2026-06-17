/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.resource.state;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.Constants;
import org.eclipse.xtext.resource.DerivedStateAwareResource;
import org.eclipse.xtext.resource.IDerivedStateComputer;
import org.eclipse.xtext.resource.XtextResource;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.utils.ProblemUtil;

import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Singleton
public class ProblemDerivedStateComputer implements IDerivedStateComputer {
	public static final String NEW_NODE = "new";
	public static final String COMPUTED_NAME = "computed";
	public static final String PRECONDITION_NAME = "precondition";

	@Inject
	@Named(Constants.LANGUAGE_NAME)
	private String languageName;

	@Inject
	private Provider<NodeNameCollector> nodeNameCollectorProvider;

	@Inject
	private DerivedVariableComputer derivedVariableComputer;

	@Override
	public void installDerivedState(DerivedStateAwareResource resource, boolean preLinkingPhase) {
		var problem = getProblem(resource);
		if (problem != null) {
			var adapter = getOrInstallAdapter(resource);
			installDerivedProblemState(problem, adapter, preLinkingPhase);
		}
	}

	protected Problem getProblem(Resource resource) {
		List<EObject> contents = resource.getContents();
		if (contents.isEmpty()) {
			return null;
		}
		EObject object = contents.getFirst();
		if (object instanceof Problem problem) {
			return problem;
		}
		return null;
	}

	protected void installDerivedProblemState(Problem problem, Adapter adapter, boolean preLinkingPhase) {
		installDerivedDeclarationState(problem, adapter);
		if (preLinkingPhase) {
			return;
		}
		installDerivedNodes(problem);
		derivedVariableComputer.installDerivedVariables(problem);
	}

	protected void installDerivedDeclarationState(Problem problem, Adapter adapter) {
		for (var statement : problem.getStatements()) {
			switch (statement) {
			case ClassDeclaration classDeclaration -> {
				installOrRemoveNewNode(adapter, classDeclaration);
				for (var referenceDeclaration : classDeclaration.getFeatureDeclarations()) {
					installOrRemoveInvalidMultiplicityPredicate(adapter, classDeclaration, referenceDeclaration);
				}
			}
			case PredicateDefinition predicateDefinition ->
					installOrRemoveComputedValuePredicate(adapter, predicateDefinition);
			case FunctionDefinition functionDefinition -> {
				installOrRemoveComputedValueFunction(adapter, functionDefinition);
				installOrRemoveDomainPredicate(adapter, functionDefinition);
			}
			case RuleDefinition ruleDefinition -> installOrRemovePreconditionPredicate(adapter, ruleDefinition);
			default -> {
				// Nothing to install.
			}
			}
		}
	}

	protected void installOrRemoveNewNode(Adapter adapter, ClassDeclaration declaration) {
		if (declaration.isAbstract()) {
			var newNode = declaration.getNewNode();
			if (newNode != null) {
				declaration.setNewNode(null);
				adapter.removeNewNode(declaration);
			}
		} else {
			if (declaration.getNewNode() == null) {
				var newNode = adapter.createNewNodeIfAbsent(declaration, _ -> createNode(NEW_NODE));
				declaration.setNewNode(newNode);
			}
		}
	}

	protected void installOrRemoveInvalidMultiplicityPredicate(
			Adapter adapter, ClassDeclaration containingClassDeclaration, ReferenceDeclaration declaration) {
		if (ProblemUtil.hasMultiplicityConstraint(declaration)) {
			if (declaration.getInvalidMultiplicity() == null) {
				var invalidMultiplicity = adapter.createInvalidMultiplicityPredicateIfAbsent(declaration, _ -> {
					var predicate = ProblemFactory.eINSTANCE.createPredicateDefinition();
					predicate.setKind(PredicateKind.ERROR);
					predicate.setName("invalidMultiplicity");
					var parameter = ProblemFactory.eINSTANCE.createParameter();
					parameter.setParameterType(containingClassDeclaration);
					parameter.setName("node");
					predicate.getParameters().add(parameter);
					return predicate;
				});
				declaration.setInvalidMultiplicity(invalidMultiplicity);
			}
		} else {
			var invalidMultiplicity = declaration.getInvalidMultiplicity();
			if (invalidMultiplicity != null) {
				declaration.setInvalidMultiplicity(null);
				adapter.removeInvalidMultiplicityPredicate(declaration);
			}
		}
	}

	protected void installOrRemoveComputedValuePredicate(Adapter adapter, PredicateDefinition predicateDefinition) {
		if (ProblemUtil.hasComputedValue(predicateDefinition)) {
			var computedValue = adapter.createComputedValuePredicateIfAbsent(predicateDefinition, _ -> {
				var predicate = ProblemFactory.eINSTANCE.createPredicateDefinition();
				predicate.setKind(PredicateKind.SHADOW);
				predicate.setName(COMPUTED_NAME);
				return predicate;
			});
			copyParameters(computedValue, predicateDefinition);
			predicateDefinition.setComputedValue(computedValue);
		} else {
			var computedValue = predicateDefinition.getComputedValue();
			if (computedValue != null) {
				predicateDefinition.setComputedValue(null);
				adapter.removeComputedValuePredicate(predicateDefinition);
			}
		}
	}

	private static void copyParameters(ParametricDefinition target, ParametricDefinition source) {
		var targetParameters = target.getParameters();
		targetParameters.clear();
		targetParameters.addAll(EcoreUtil.copyAll(source.getParameters()));
	}

	protected void installOrRemoveComputedValueFunction(Adapter adapter, FunctionDefinition functionDefinition) {
		if (ProblemUtil.hasComputedValue(functionDefinition)) {
			var computedValue = adapter.createComputedValueFunctionIfAbsent(functionDefinition, _ -> {
				var function = ProblemFactory.eINSTANCE.createFunctionDefinition();
				function.setShadow(true);
				function.setName(COMPUTED_NAME);
				return function;
			});
			computedValue.setFunctionType(functionDefinition.getFunctionType());
			copyParameters(computedValue, functionDefinition);
			functionDefinition.setComputedValue(computedValue);
		} else {
			var computedValue = functionDefinition.getComputedValue();
			if (computedValue != null) {
				functionDefinition.setComputedValue(null);
				adapter.removeComputedValueFunction(functionDefinition);
			}
		}
	}

	protected void installOrRemoveDomainPredicate(Adapter adapter, FunctionDefinition functionDefinition) {
		if (ProblemUtil.hasDomainPredicate(functionDefinition)) {
			var domainPredicate = adapter.createDomainPredicateIfAbsent(functionDefinition, _ -> {
				var function = ProblemFactory.eINSTANCE.createPredicateDefinition();
				function.setName("defined");
				return function;
			});
			copyParameters(domainPredicate, functionDefinition);
			domainPredicate.setKind(functionDefinition.isShadow() ? PredicateKind.SHADOW : PredicateKind.DEFAULT);
			functionDefinition.setDomainPredicate(domainPredicate);
			installOrRemoveComputedValuePredicate(adapter, domainPredicate);
		} else {
			var domainPredicate = functionDefinition.getDomainPredicate();
			if (domainPredicate != null) {
				functionDefinition.setComputedValue(null);
				adapter.removeDomainPredicate(functionDefinition);
			}
		}
	}

	protected void installOrRemovePreconditionPredicate(Adapter adapter, RuleDefinition ruleDefinition) {
		if (ProblemUtil.hasPreconditionPredicate(ruleDefinition)) {
			var preconditionPredicate = adapter.createPreconditionPredicateIfAbsent(ruleDefinition, _ -> {
				var predicate = ProblemFactory.eINSTANCE.createPredicateDefinition();
				predicate.setName(PRECONDITION_NAME);
				return predicate;
			});
			var targetParameters = preconditionPredicate.getParameters();
			targetParameters.clear();
			for (var parameter : ruleDefinition.getParameters()) {
				var newParameter = ProblemFactory.eINSTANCE.createParameter();
				newParameter.setParameterType(parameter.getParameterType());
				newParameter.setKind(parameter.getKind());
				newParameter.setName(parameter.getName());
				newParameter.setAnnotations(ProblemFactory.eINSTANCE.createAnnotationContainer());
				targetParameters.add(newParameter);
			}
			preconditionPredicate.setKind(
					ruleDefinition.getKind() == RuleKind.TRANSITION ? PredicateKind.DEFAULT : PredicateKind.SHADOW);
			ruleDefinition.setPreconditionPredicate(preconditionPredicate);
			installOrRemoveComputedValuePredicate(adapter, preconditionPredicate);
		} else {
			var preconditionPredicate = ruleDefinition.getPreconditionPredicate();
			if (preconditionPredicate != null) {
				ruleDefinition.setPreconditionPredicate(null);
				adapter.removePreconditionPredicate(ruleDefinition);
			}
		}
	}

	protected void installDerivedNodes(Problem problem) {
		var collector = nodeNameCollectorProvider.get();
		collector.collectNodeNames(problem);
		Set<String> nodeNames = collector.getNodeNames();
		List<Node> graphNodes = problem.getNodes();
		for (String nodeName : nodeNames) {
			var graphNode = createNode(nodeName);
			graphNodes.add(graphNode);
		}
	}

	protected Node createNode(String name) {
		var node = ProblemFactory.eINSTANCE.createNode();
		node.setName(name);
		return node;
	}

	@Override
	public void discardDerivedState(DerivedStateAwareResource resource) {
		var problem = getProblem(resource);
		if (problem != null) {
			var adapter = getOrInstallAdapter(resource);
			discardDerivedProblemState(problem, adapter);
		}
	}

	protected void discardDerivedProblemState(Problem problem, Adapter adapter) {
		var abstractClassDeclarations = new HashSet<ClassDeclaration>();
		var referenceDeclarationsWithMultiplicity = new HashSet<ReferenceDeclaration>();
		var predicateDefinitionsWithComputedValue = new HashSet<PredicateDefinition>();
		var functionDefinitionsWithComputedValue = new HashSet<FunctionDefinition>();
		var functionDefinitionsWithDomainPredicate = new HashSet<FunctionDefinition>();
		var ruleDefinitionsWithDomainPredicate = new HashSet<RuleDefinition>();
		problem.getNodes().clear();
		for (var statement : problem.getStatements()) {
			switch (statement) {
			case ClassDeclaration classDeclaration -> {
				classDeclaration.setNewNode(null);
				if (classDeclaration.isAbstract()) {
					abstractClassDeclarations.add(classDeclaration);
				}
				for (var referenceDeclaration : classDeclaration.getFeatureDeclarations()) {
					if (ProblemUtil.hasMultiplicityConstraint(referenceDeclaration)) {
						referenceDeclarationsWithMultiplicity.add(referenceDeclaration);
					}
				}
			}
			case PredicateDefinition predicateDefinition when ProblemUtil.hasComputedValue(predicateDefinition) ->
					predicateDefinitionsWithComputedValue.add(predicateDefinition);
			case FunctionDefinition functionDefinition -> {
				if (ProblemUtil.hasComputedValue(functionDefinition)) {
					functionDefinitionsWithComputedValue.add(functionDefinition);
				}
				if (ProblemUtil.hasDomainPredicate(functionDefinition)) {
					functionDefinitionsWithDomainPredicate.add(functionDefinition);
					var domainPredicate = functionDefinition.getDomainPredicate();
					if (domainPredicate != null && ProblemUtil.hasComputedValue(domainPredicate)) {
						predicateDefinitionsWithComputedValue.add(domainPredicate);
					}
				}
			}
			case RuleDefinition ruleDefinition -> {
				if (ProblemUtil.hasPreconditionPredicate(ruleDefinition)) {
					ruleDefinitionsWithDomainPredicate.add(ruleDefinition);
					var preconditionPredicate = ruleDefinition.getPreconditionPredicate();
					if (preconditionPredicate != null && ProblemUtil.hasComputedValue(preconditionPredicate)) {
						predicateDefinitionsWithComputedValue.add(preconditionPredicate);
					}
				}
			}
			default -> {
				// Nothing to discard.
			}
			}
		}
		adapter.retainAll(abstractClassDeclarations, referenceDeclarationsWithMultiplicity,
				predicateDefinitionsWithComputedValue, functionDefinitionsWithComputedValue,
				functionDefinitionsWithDomainPredicate, ruleDefinitionsWithDomainPredicate);
		derivedVariableComputer.discardDerivedVariables(problem);
	}

	protected Adapter getOrInstallAdapter(Resource resource) {
		if (!(resource instanceof XtextResource)) {
			return new Adapter();
		}
		String resourceLanguageName = ((XtextResource) resource).getLanguageName();
		if (!languageName.equals(resourceLanguageName)) {
			return new Adapter();
		}
		var adapter = (Adapter) EcoreUtil.getAdapter(resource.eAdapters(), Adapter.class);
		if (adapter == null) {
			adapter = new Adapter();
			resource.eAdapters().add(adapter);
		}
		return adapter;
	}

	protected static class Adapter extends AdapterImpl {
		private final Map<ClassDeclaration, Node> newNodes = new HashMap<>();
		private final Map<ReferenceDeclaration, PredicateDefinition> invalidMultiplicityPredicates = new HashMap<>();
		private final Map<PredicateDefinition, PredicateDefinition> computedValuePredicates = new HashMap<>();
		private final Map<FunctionDefinition, FunctionDefinition> computedFunctionValues = new HashMap<>();
		private final Map<FunctionDefinition, PredicateDefinition> domainPredicates = new HashMap<>();
		private final Map<RuleDefinition, PredicateDefinition> preconditionPredicates = new HashMap<>();

		public Node createNewNodeIfAbsent(ClassDeclaration classDeclaration,
		                                  Function<ClassDeclaration, Node> createNode) {
			return newNodes.computeIfAbsent(classDeclaration, createNode);
		}

		public void removeNewNode(ClassDeclaration classDeclaration) {
			newNodes.remove(classDeclaration);
		}

		public PredicateDefinition createInvalidMultiplicityPredicateIfAbsent(
				ReferenceDeclaration referenceDeclaration,
				Function<ReferenceDeclaration, PredicateDefinition> createPredicate) {
			return invalidMultiplicityPredicates.computeIfAbsent(referenceDeclaration, createPredicate);
		}

		public void removeInvalidMultiplicityPredicate(ReferenceDeclaration referenceDeclaration) {
			invalidMultiplicityPredicates.remove(referenceDeclaration);
		}

		public PredicateDefinition createComputedValuePredicateIfAbsent(
				PredicateDefinition predicateDefinition, UnaryOperator<PredicateDefinition> createPredicate) {
			return computedValuePredicates.computeIfAbsent(predicateDefinition, createPredicate);
		}

		public void removeComputedValuePredicate(PredicateDefinition predicateDefinition) {
			computedValuePredicates.remove(predicateDefinition);
		}

		public FunctionDefinition createComputedValueFunctionIfAbsent(
				FunctionDefinition functionDefinition, UnaryOperator<FunctionDefinition> createFunction) {
			return computedFunctionValues.computeIfAbsent(functionDefinition, createFunction);
		}

		public void removeComputedValueFunction(FunctionDefinition functionDefinition) {
			computedFunctionValues.remove(functionDefinition);
		}

		public PredicateDefinition createDomainPredicateIfAbsent(
				FunctionDefinition functionDefinition,
				Function<FunctionDefinition, PredicateDefinition> createPredicate) {
			return domainPredicates.computeIfAbsent(functionDefinition, createPredicate);
		}

		public void removeDomainPredicate(FunctionDefinition functionDefinition) {
			var predicate = domainPredicates.remove(functionDefinition);
			if (predicate != null && predicate.getComputedValue() != null) {
				removeComputedValuePredicate(predicate);
			}
		}

		public PredicateDefinition createPreconditionPredicateIfAbsent(
				RuleDefinition ruleDefinition,
				Function<RuleDefinition, PredicateDefinition> createPredicate) {
			return preconditionPredicates.computeIfAbsent(ruleDefinition, createPredicate);
		}

		public void removePreconditionPredicate(RuleDefinition ruleDefinition) {
			var predicate = preconditionPredicates.remove(ruleDefinition);
			if (predicate != null && predicate.getComputedValue() != null) {
				removeComputedValuePredicate(predicate);
			}
		}

		public void retainAll(Collection<ClassDeclaration> abstractClassDeclarations,
		                      Collection<ReferenceDeclaration> referenceDeclarationsWithMultiplicity,
		                      Collection<PredicateDefinition> predicateDefinitionsWithComputedValue,
		                      Collection<FunctionDefinition> functionDefinitionsWithComputedValue,
		                      Collection<FunctionDefinition> functionDefinitionsWithDomainPredicate,
		                      Collection<RuleDefinition> ruleDefinitionsWithDomainPredicate) {
			newNodes.keySet().retainAll(abstractClassDeclarations);
			invalidMultiplicityPredicates.keySet().retainAll(referenceDeclarationsWithMultiplicity);
			computedValuePredicates.keySet().retainAll(predicateDefinitionsWithComputedValue);
			computedFunctionValues.keySet().retainAll(functionDefinitionsWithComputedValue);
			domainPredicates.keySet().retainAll(functionDefinitionsWithDomainPredicate);
			preconditionPredicates.keySet().retainAll(ruleDefinitionsWithDomainPredicate);
		}

		@Override
		public boolean isAdapterForType(Object type) {
			return Adapter.class == type;
		}
	}
}
