package hu.bme.mit.trainbenchmark.generator.runner;

import java.io.IOException;

public class RunnerRunner {
	public static void main(String[] args) throws IOException {
		int[] sizes = new int[] {
				2,
				5,
				10,
//				50,
//				100,
//				150,
//				200,
//				250,
//				300
/*, 20,40/*15, 32, 75, 125, 250, 500,*/ /* 1000, 2000,*/ /*4000,  8000, 16000 */};
		int[] widths = new int[] {1};
		Tool[] tools = new Tool[] {
				Tool.RefineryR,
//				Tool.DoltNoP,
//				Tool.Dolt,
				Tool.Git,
				//Tool.RefineryR, Tool.RefineryF
				/*, Tool.Dummy, Tool.EMFnoVC/*,  Tool.Refinery, /*Tool.EMFCopyVC,
				 Tool.EMFDelta, Tool.Dummy  Tool.EMFDelta*/ };
		int[] commitsAfterActionsDuringBuilding = new int[] {100};
		final int repeat = 1;
		final int[] errorsInOneEditAction = new int[] {/*10, 20, 30, 40, 50, 60, 70, 80, 90,*/ 5};
		final int numberOfEditActions = 10;
		final int restoreAfterCommits = 10;
		final boolean linearHistory = true;

		BenchmarkRunner runner = new BenchmarkRunner();

		int cases = sizes.length*widths.length*tools.length*commitsAfterActionsDuringBuilding.length*repeat*errorsInOneEditAction.length;
		System.out.println("number of cases: " + cases);
		//System.in.read();

		runner.printModelBuildHeader();

		for (int size : sizes) {
			for (int width : widths) {
				for(int commitAfterActionsDuringBuilding : commitsAfterActionsDuringBuilding) {
					for (Tool tool : tools) {
						for (int errorInOneEditAction : errorsInOneEditAction) {
							BenchmarkConfig config = new ModelBuildConfig(
									tool, size, width, repeat,
									commitAfterActionsDuringBuilding,
									errorInOneEditAction,
									numberOfEditActions,
									restoreAfterCommits,
									linearHistory);
							runner.run(config);
						}
					}
				}
			}

		}
	}
}
