package tools.refinery.store.dse;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.refinery.store.dse.logging.LoggingAdapter;
import tools.refinery.store.dse.logging.loggers.FileFormat;
import tools.refinery.store.dse.logging.loggers.VisualLogger;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.tests.DummyRandomObjective;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.query.term.int_.IntTerms;
import tools.refinery.store.query.viatra.ViatraModelQueryAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;

import java.util.List;

import static tools.refinery.store.dse.modification.actions.ModificationActionLiterals.create;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.add;
import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.literal.Literals.not;

public class FileSystemExampleTest {
	private static final Symbol<Boolean> folder = Symbol.of("Folder", 1);
	private static final Symbol<Boolean> file = Symbol.of("File", 1);
	private static final Symbol<Boolean> contains = Symbol.of("contains", 2);

	private static final AnySymbolView folderView = new KeyOnlyView<>(folder);
//	private static final AnySymbolView fileView = new KeyOnlyView<>(file);
	private static final AnySymbolView containsView = new KeyOnlyView<>(contains);

	@Test
	@Disabled("This test is only for debugging purposes")
	void fileSystemExampleTest() {
		var createFolderRule = Rule.of("CreateFolder", (builder, rootFolder) -> builder
				.clause(
						folderView.call(rootFolder)
				)
				.action((newFolder) -> List.of(
						create(newFolder),
						add(folder, newFolder),
						add(contains, rootFolder, newFolder)
				)));

		var createFileRule = Rule.of("CreateFile", (builder, rootFolder) -> builder
				.clause(
						folderView.call(rootFolder)
				)
				.action((newFile) -> List.of(
						create(newFile),
						add(file, newFile),
						add(contains, rootFolder, newFile)
				)));

		var emptyFolder = Query.of("emptyFolder", (builder, empty) -> builder
				.clause(
						folderView.call(empty),
						not(containsView.call(empty, Variable.of()))
				));

		var moreThan3Contained = Query.of("moreThan3Contained", (builder, tooMuchContained) -> builder
				.clause(Integer.class, (numberOfContained) -> List.of(
						numberOfContained.assign(containsView.count(tooMuchContained, Variable.of())),
						check(IntTerms.less(IntTerms.constant(3), numberOfContained)),
						folderView.call(tooMuchContained)
				)));

		var moreThan2Deep = Query.of("moreThan3Deep", (builder, tooDeep) -> builder
				.clause((folder0, folder1, folder2) -> List.of(
						containsView.call(folder0, folder1),
						containsView.call(folder1, folder2),
						containsView.call(folder2, tooDeep),
						folder0.notEquivalent(folder1),
						folder0.notEquivalent(folder2),
						folder1.notEquivalent(folder2)
				)));

		var store = ModelStore.builder()
				.symbols(folder, file, contains)
				.with(ViatraModelQueryAdapter.builder())
				.with(LoggingAdapter.builder()
						.withLoggers(new VisualLogger()
								.withOutputPath("test_output")
								.withFormat(FileFormat.DOT)
								.withFormat(FileFormat.SVG)
								.withDotExecutable("C:/Program Files/Graphviz/bin/dot.exe")
								.setSaveStates()
								.setSaveDesignSpace()))
				.with(StateCoderAdapter.builder())
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder()
						.transformations(createFolderRule, createFileRule)
						.objectives(new DummyRandomObjective())
						.accept(Criteria.whenNoMatch(emptyFolder))
						.exclude(Criteria.or(Criteria.whenHasMatch(moreThan2Deep),
								Criteria.whenHasMatch(moreThan3Contained)))
				)
				.build();

		var initialModel = store.createEmptyModel();
		var queryAdapter = initialModel.getAdapter(ModelQueryAdapter.class);
		var modificationAdapter = initialModel.getAdapter(ModificationAdapter.class);

		var folderInterpretation = initialModel.getInterpretation(folder);
		folderInterpretation.put(modificationAdapter.createObject(), true);
		var initialVersion = initialModel.commit();
		queryAdapter.flushChanges();

		var bestFirst = new BestFirstStoreManager(store, 50);
		bestFirst.startExploration(initialVersion);

		var solutionStore = bestFirst.getSolutionStore();
		System.out.println("Number of solutions: " + solutionStore.getSolutions().size());
	}
}
