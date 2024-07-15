package hu.bme.mit.trainbenchmark.generator.git;

import hu.bme.mit.trainbenchmark.generator.CSVSerializer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JGitSerializer extends CSVSerializer {

	Git git;

	protected Map<Long, RevCommit> versions;
	protected List<String> branches;
	long commits = 0;

	public JGitSerializer() {
		super();
		versions = new HashMap<>();
		branches = new ArrayList<>();
		MODEL_PATH = "models/jgit/";
	}


	@Override
	public String syntax() {
		return "jgit";
	}

	@Override
	public void initModel() throws IOException {
		JGitProcess.cleanGit();
		try {
			git = Git.init().setDirectory(new File(MODEL_PATH)).call();
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
		super.initModel();
	}

	@Override
	public long commit() {
		try {
			persist();
			git.add().addFilepattern(".").call();
			var commit = git.commit().setMessage("trainbenchmark").call();
			versions.put(commits, commit);
			return commits++;
		} catch (IOException | GitAPIException e) {
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
			git.reset().setMode(ResetCommand.ResetType.HARD).call();
		} catch (GitAPIException e) {
			throw new RuntimeException(e);
		}
		var branchName = String.valueOf(version);
		if (branches.contains(branchName)) {
			try {
				git.checkout().setName(branchName).call();
			} catch (GitAPIException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			try {
				git.checkout().setStartPoint(versions.get(version)).setCreateBranch(true).setName(branchName).call();
				branches.add(branchName);
			} catch (GitAPIException e) {
				throw new RuntimeException(e);
			}
		}
		for(String type : types) {
			initType(writers, type, true);
		}

	}



}
