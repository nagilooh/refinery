package hu.bme.mit.trainbenchmark.generator.git;

import hu.bme.mit.trainbenchmark.generator.CSVSerializer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GitSerializer extends CSVSerializer {

	protected Map<Long, String> versions;
	long commits = 0;

	public GitSerializer() {
		super();
		versions  = new HashMap<>();
		MODEL_PATH = "models/git/";
	}


	@Override
	public String syntax() {
		return "git";
	}

	@Override
	public void initModel() throws IOException {
		GitProcess.cleanGit();
		GitProcess.initGit();
		super.initModel();
	}

	@Override
	public long commit() {
		try {
			persist();
			var commit = GitProcess.commitGit();
			versions.put(commits, commit);
			return commits++;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	@Override
	public void restore(long version) {
		try {
			for(String type : types) {
				writers.get(type).close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			GitProcess.runShell("git checkout " + version);
		} catch (IOException e) {
			try {
				GitProcess.runShell("git checkout -b " + version + " " + versions.get(version));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		try {
			GitProcess.runShell("git restore .");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		for(String type : types) {
			initType(writers, type, true);
		}

	}



}