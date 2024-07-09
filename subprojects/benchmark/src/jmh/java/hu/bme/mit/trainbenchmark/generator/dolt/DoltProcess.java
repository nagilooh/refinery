package hu.bme.mit.trainbenchmark.generator.dolt;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.output.ByteArrayOutputStream;

public class DoltProcess {

	// It is generally considered good practice to use Apache Commmons Exec.
	// However, it does not work with MySQL start/stop properly, with frequent
	// hangs. Therefore, we recommend using the simple ProcessBuilder instead.

	private static final String SCRIPT_DIRECTORY = "subprojects/benchmark/src/jmh/scripts/";
	private static final String WORKING_DIRECTORY = "models/dolt/";

	public static void cleanDolt() throws IOException {
		runScript(SCRIPT_DIRECTORY + "clean-dolt.sh");
	}

	public static void cleanSql() throws IOException {
		runScript(SCRIPT_DIRECTORY + "clean-mysql.sh");
	}

	public static void initDolt() throws IOException {
		runScript(SCRIPT_DIRECTORY + "init-dolt.sh");
	}

	public static String commitDolt() throws IOException {
		var output = runScript((SCRIPT_DIRECTORY + "commit-dolt.sh"));
		return output.split(" ")[1];
	}

	public static void restoreDolt(long version, String hash) {
		try {
			runShell("dolt checkout " + version);
		} catch (IOException e) {
			try {
				runShell("dolt checkout -b " + version + " " + hash);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

	public static String runShell(final String shellCommand) throws IOException {
		final Executor executor = DefaultExecutor.builder().get();
		executor.setWorkingDirectory(new File(WORKING_DIRECTORY));
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

	public static String runScript(final String scriptFile) throws IOException {
		final DefaultExecutor executor = DefaultExecutor.builder().get();
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		PumpStreamHandler psh = new PumpStreamHandler(stdout);
		executor.setStreamHandler(psh);
		final CommandLine commandLine = new CommandLine("bash");
		commandLine.addArgument(scriptFile, false);
		executor.execute(commandLine);
		return stdout.toString(Charset.defaultCharset());
	}

}
