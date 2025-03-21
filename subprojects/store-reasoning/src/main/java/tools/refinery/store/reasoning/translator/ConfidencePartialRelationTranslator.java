package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.interpretation.ConfidenceRelationInterpretationFactory;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.representation.Symbol;

public final class ConfidencePartialRelationTranslator extends PartialSymbolTranslator<TruthValueConfidence, Boolean> {
	private final ConfidencePartialRelation confidencePartialRelation;
	private final RoundingMode roundingMode;

	public ConfidencePartialRelationTranslator(ConfidencePartialRelation confidencePartialRelation,
											   RoundingMode roundingMode) {
		super(confidencePartialRelation);
		this.confidencePartialRelation = confidencePartialRelation;
		this.roundingMode = roundingMode;
	}

	public ConfidencePartialRelation getConfidencePartialRelation() {
		return confidencePartialRelation;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		interpretationFactory =
				new ConfidenceRelationInterpretationFactory((Symbol<TruthValueConfidence>) storageSymbol, roundingMode);
		super.doConfigure(storeBuilder);
	}
}
