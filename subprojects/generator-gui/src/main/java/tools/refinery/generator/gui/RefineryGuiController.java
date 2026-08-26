package tools.refinery.generator.gui;

import tools.refinery.generator.ModelGenerator;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;

import com.github.weisj.jsvg.SVGDocument;

import java.util.ArrayList;
import java.util.List;

public class RefineryGuiController {
    private final RefineryGuiModel appModel;
    private final ModelGenerator generator;
    private final Model model;
    private final ProblemTrace trace;
    private final PropagationAdapter propagationAdapter;
    private final DesignSpaceExplorationAdapter dseAdapter;
    private final ModelQueryAdapter queryEngine;

    public RefineryGuiController(RefineryGuiModel appModel, ModelGenerator generator) {
        this.appModel = appModel;
        this.generator = generator;
        this.model = generator.getModel();
        this.trace = generator.getProblemTrace();

        var initial = this.model.commit();
        this.model.restore(initial);

        this.queryEngine = this.model.getAdapter(ModelQueryAdapter.class);
        this.propagationAdapter = this.model.getAdapter(PropagationAdapter.class);
        this.dseAdapter = this.model.getAdapter(DesignSpaceExplorationAdapter.class);

        refreshModel(null);
        refreshActivations();
    }

    public void onActivationSelected(Activation activation) {
        refreshModel(activation);
    }

    public void onActivationSent(Activation activation) {
        step(activation);
        refreshModel(null);
        refreshActivations();
    }

    private void step(Activation activation) {
        activation.fire();
        propagationAdapter.propagate();
        dseAdapter.checkAccept();
        model.commit();
        queryEngine.flushChanges();
    }

    private void refreshModel(Activation activation) {
        SVGDocument svg = Visualizer.renderModel(model, trace, activation);
        appModel.setSvg(svg);
    }

    private void refreshActivations() {
        List<Activation> activationList = new ArrayList<>();
        for (var transformation : dseAdapter.getTransformations()) {
            var activationCursor = transformation.getAllActivationsAsResultSet().getAll();
            while (activationCursor.move()) {
				var tuple = activationCursor.getKey();
				var names = new ArrayList<String>();
				for (int i = 0; i < tuple.getSize(); i++) {
					var id = tuple.get(i);
					names.add(trace.getNodeName(id));
				}
				activationList.add(new Activation(transformation, activationCursor.getKey(), names));
			}
		}
        appModel.setActivations(activationList);
    }
}

