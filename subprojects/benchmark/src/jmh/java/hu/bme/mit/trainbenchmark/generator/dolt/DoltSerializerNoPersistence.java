package hu.bme.mit.trainbenchmark.generator.dolt;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DoltSerializerNoPersistence extends DoltSerializer {

//	protected StringBuilder stringBuilder;


	public void write(String s) throws IOException {
		s = s.replace("`", "");
		System.out.println("\n" + "dolt sql -q \"" + s + "\"");
		System.out.println(DoltProcess.runShell("dolt sql -q \"" + s + "\""));
	}

	@Override
	public void initModel() throws IOException {
		DoltProcess.cleanDolt();
		DoltProcess.cleanSql();
		DoltProcess.initDolt();

//		stringBuilder = new StringBuilder();

		// header file (DDL operations)
		final String headerFilePath = "C:/git/refinery/subprojects/benchmark/src/jmh/resources/dolt/metamodel/railway-header.sql";
		DoltProcess.runShell(String.format("dolt sql < %s", headerFilePath));
		commit();

//		stringBuilder.append(header);
	}

	@Override
	public long commit() {
		try {
//			DoltProcess.runShell("dolt sql -q \"" + stringBuilder.toString() + "\"");
//			var commit = DoltProcess.commitDolt();
			DoltProcess.runShell("dolt sql -q \"call dolt_commit('-am', 'Modifications on a branch');\"");
			var commit = "test";
			versions.put(commits, commit);
			return commits++;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void endTransaction() {}


}
