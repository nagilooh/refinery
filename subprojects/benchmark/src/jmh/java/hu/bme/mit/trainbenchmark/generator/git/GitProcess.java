package hu.bme.mit.trainbenchmark.generator.git;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class GitProcess {

	private static final String WORKING_DIRECTORY = "models/git/";

	public static void cleanGit() throws IOException {
		var output1 = runShell("echo \"$PWD\"", ".");
//		System.out.println(output1);
		var output2 = runShell("rm -rf " + WORKING_DIRECTORY, ".");
//		System.out.println(output2);
	}

	public static void initGit() throws IOException {
		runShell("mkdir -p " + WORKING_DIRECTORY, ".");
		var output = runShell("git init");
//		System.out.println(output);
	}

	public static String commitGit() throws IOException {
		var output1 = runShell("git add .");
//		System.out.println(output1);
		var output2 = runShell("git commit -m \"trainbenchmark\"");
//		System.out.println(output2);
		var split = output2.split("]")[0].split(" ");
		return split[split.length - 1];
	}

	public static String runShell(final String shellCommand, String workingDirectory) throws IOException {
		final Executor executor = DefaultExecutor.builder().get();
		executor.setWorkingDirectory(new File(workingDirectory));
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		PumpStreamHandler psh = new PumpStreamHandler(stdout);
		executor.setStreamHandler(psh);
//		System.out.println(shellCommand);
		final CommandLine commandLine = new CommandLine("bash");
		commandLine.addArgument("-c");
		commandLine.addArgument(shellCommand, false);
//		System.out.println(commandLine);
		executor.execute(commandLine);
		return stdout.toString(Charset.defaultCharset());
	}
	public static String runShell(final String shellCommand) throws IOException {
		return runShell(shellCommand, WORKING_DIRECTORY);
	}

}
