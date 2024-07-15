package hu.bme.mit.trainbenchmark.generator;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public abstract class CSVSerializer extends ModelSerializer {

	protected String MODEL_PATH = "models/csv/";

	protected String[] types;
	protected Map<String, BufferedWriter> writers;

	public CSVSerializer() {
		super();
		writers = new HashMap<>();
	}

	public void write(String type, final String... s) {
		try {
			var writer = writers.get(type);
			if(writer == null) {
				System.out.println(type);
			}
			writer.write(String.join(",", s));
			writer.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void initType(Map<String, BufferedWriter> writers, String name) {
		initType(writers, name, false);
	}

	protected void initType(Map<String, BufferedWriter> writers, String name, boolean append) {
		try {
			writers.put(name, new BufferedWriter(new FileWriter(MODEL_PATH + name + ".csv", append)));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String syntax() {
		return "csv";
	}

	@Override
	public void initModel() throws IOException {
		new File(MODEL_PATH).mkdirs();

		types = new String[] {
				ModelConstants.TRACKELEMENT,
				ModelConstants.REGION,
				ModelConstants.ROUTE,
				ModelConstants.SEGMENT,
				ModelConstants.SENSOR,
				ModelConstants.SEMAPHORE,
				ModelConstants.SWITCH,
				ModelConstants.SWITCHPOSITION,
				ModelConstants.CONNECTS_TO,
				ModelConstants.ELEMENTS,
				ModelConstants.EXIT,
				ModelConstants.ENTRY,
				ModelConstants.FOLLOWS,
				ModelConstants.REQUIRES,
				ModelConstants.MONITORED_BY,
				ModelConstants.SEMAPHORES,
				ModelConstants.SENSORS,
				ModelConstants.TARGET,
				ModelConstants.SIGNAL,
				ModelConstants.ACTIVE,
				ModelConstants.CURRENTPOSITION,
				ModelConstants.LENGTH,
				ModelConstants.POSITION
		};
		for(String type : types) {
			initType(writers, type);
		}
		persist();
	}

	@Override
	public Object createVertex(int id, String type, Map<String, ? extends Object> attributes,
			Map<String, Object> outgoingEdges, Map<String, Object> incomingEdges) throws IOException {
		Integer object = id;
		write(type, String.valueOf(id));
		for(Entry<String, ?> entry: attributes.entrySet()) {
			write(entry.getKey(), String.valueOf(id), String.valueOf(entry.getValue()));
		}
		for(Entry<String, ?> entry: outgoingEdges.entrySet()) {
			write(entry.getKey(), String.valueOf(id), String.valueOf(entry.getValue()));
		}
		for(Entry<String, ?> entry: incomingEdges.entrySet()) {
			write(entry.getKey(), String.valueOf(entry.getValue()), String.valueOf(id));
		}
		return object;
	}

	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		write(label, String.valueOf(from), String.valueOf(to));
	}

	@Override
	public void removeEdge(String label, Object from, Object to) {
		try {
			persist();
			writers.get(label).close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			String content = Files.readString(Paths.get(MODEL_PATH + label + ".csv"), Charset.defaultCharset());
			initType(writers, label);
			for (String line : content.split("\n")) {
				if (!line.strip().equals(String.join(",", String.valueOf(from), String.valueOf(to)))) {
					write(label, line.strip());
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setAttribute(String label, Object object, Object value) {
		write(label, String.valueOf(object), String.valueOf(value));
	}

	@Override
	public long commit() {
		// TODO Auto-generated method stub
		try {
			persist();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return 0;
	}

	public void persist() throws IOException {
		for(String type : types) {
			writers.get(type).flush();
		}

	}

	@Override
	public void restore(long version) {
		// TODO Auto-generated method stub

	}

	@Override
	public void endTransaction() {
		for(String type : types) {
			try {
				writers.get(type).close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
