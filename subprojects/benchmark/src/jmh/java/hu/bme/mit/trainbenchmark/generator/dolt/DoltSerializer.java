package hu.bme.mit.trainbenchmark.generator.dolt;

import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import org.apache.commons.io.FileUtils;
import tools.refinery.store.map.Version;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import static hu.bme.mit.trainbenchmark.constants.ModelConstants.*;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.ROUTE;

public class DoltSerializer extends ModelSerializer {

	protected String sqlRawPath = "models/railway-raw.sql";
	protected BufferedWriter writer;
	protected String SQL_FORMAT_DIR = "subprojects/benchmark/";
	protected String SQL_METAMODEL_DIR = SQL_FORMAT_DIR + "src/jmh/resources/dolt/metamodel/";
	protected String SQL_SCRIPT_DIR = SQL_FORMAT_DIR + "src/jmh/scripts/";
	protected File sqlRawFile;
	protected Map<Long, String> versions;
	long commits = 0;

	public DoltSerializer() {
		versions  = new HashMap<>();
	}


	@Override
	public String syntax() {
		return "SQL";
	}

	public void write(final String s) throws IOException {
		writer.write(s + "\n");
	}

	@Override
	public void initModel() throws IOException {
		DoltProcess.cleanDolt();
//		DoltProcess.cleanSql();
		DoltProcess.initDolt();

		// header file (DDL operations)
		final String headerFilePath = SQL_METAMODEL_DIR + "railway-header.sql";
		final File headerFile = new File(headerFilePath);

		// destination file
		sqlRawFile = new File(sqlRawPath);

		// this overwrites the destination file if it exists
		FileUtils.copyFile(headerFile, sqlRawFile);

		try {
			persist();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		writer = new BufferedWriter(new FileWriter(sqlRawFile, false));
	}

	@Override
	public long commit() {
		try {
			writer.close();
			persist();
			var commit = DoltProcess.commitDolt();
//			System.out.println("commit" + commits + ": " + commit);
			versions.put(commits, commit);
			return commits++;

		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public void persist() throws IOException, InterruptedException {
//		log("Loading the raw model");
		DoltProcess.runShell(String.format("dolt sql < %s", sqlRawPath.replace("models", "..")));
		writer = new BufferedWriter(new FileWriter(sqlRawFile, false));
	}

	protected void log(final String message) {
		System.out.println(message);
	}

	@Override
	public Object createVertex(final int id, final String type, final Map<String, ? extends Object> attributes, final Map<String, Object> outgoingEdges,
			final Map<String, Object> incomingEdges) throws IOException {
		final StringBuilder columns = new StringBuilder();
		final StringBuilder values = new StringBuilder();

		columns.append("`" + ID + "`");
		values.append(id);

		structuralFeaturesToSQL(attributes, columns, values);
		structuralFeaturesToSQL(outgoingEdges, columns, values);
		structuralFeaturesToSQL(incomingEdges, columns, values);

		if (SUPERTYPES.containsKey(type)) {
			final String ancestorType = SUPERTYPES.get(type);
			write(String.format("INSERT INTO %s (`%s`) VALUES (%s);", ancestorType, ID, id));
			write(String.format("INSERT INTO %s (%s) VALUES (%s);", type, columns, values));
		} else {
			final String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s);", type, columns, values);
			write(insertQuery);
		}
//		System.out.println(id + " : " + type + ", " + attributes + ", " + outgoingEdges + ", " + incomingEdges);
		return id;
	}

	@Override
	public void createEdge(final String label, final Object from, final Object to) throws IOException {
		if (from == null || to == null) {
			return;
		}

		String insertQuery;
		switch (label) {
		// n:m edges
		case MONITORED_BY:
		case CONNECTS_TO:
		case REQUIRES:
			insertQuery = String.format("INSERT INTO %s VALUES (%s, %s);", label, from, to);
			break;
		// n:1 edges
		case FOLLOWS:
			insertQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SWITCHPOSITION, "route", from, ID, to);
			break;
		case SENSORS:
			insertQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SENSOR, "region", from, ID, to);
			break;
		case ELEMENTS:
			insertQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", TRACKELEMENT, "region", from, ID, to);
			break;
		case SEMAPHORES:
			insertQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SEMAPHORE, "segment", from, ID, to);
			break;
		default:
			throw new UnsupportedOperationException("Label '" + label + "' not supported.");
		}

		write(insertQuery);
//		System.out.println(label + " : " + from + " -> " + to);
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		if (from == null || to == null) {
			return;
		}

		String deleteQuery;
		switch (label) {
		// n:m edges
		case MONITORED_BY:
			deleteQuery = String.format("DELETE FROM %s WHERE TrackElement_id = %s AND Sensor_id = %s;", label, from, to);
			break;
		case CONNECTS_TO:
			deleteQuery = String.format("DELETE FROM %s WHERE TrackElement1_id = %s AND TrackElement2_id = %s;", label, from, to);
			break;
		case REQUIRES:
			deleteQuery = String.format("DELETE FROM %s WHERE Route_id = %s AND Sensor_id = %s;", label, from, to);
			break;
		// n:1 edges
		case FOLLOWS:
			deleteQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SWITCHPOSITION, "route", "NULL", ID, to);
			break;
		case SENSORS:
			deleteQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SENSOR, "region", "NULL", ID, to);
			break;
		case ELEMENTS:
			deleteQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", TRACKELEMENT, "region", "NULL", ID, to);
			break;
		case SEMAPHORES:
			deleteQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SEMAPHORE, "segment", "NULL", ID, to);
			break;
		//
		case ENTRY:
			deleteQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", ROUTE, "entry", "NULL", ID, to);
			break;
		default:
			throw new UnsupportedOperationException("Label '" + label + "' not supported.");
		}

		write(deleteQuery);
	}

	@Override
	public void setAttribute(String label, Object object, Object value) throws IOException {
		if (object == null) {
			return;
		}

		String updateQuery;
		switch (label) {
		case ACTIVE:
			updateQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", ROUTE, ACTIVE, valueToString(value), ID, object);
			break;
		case LENGTH:
			updateQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SEGMENT, LENGTH, valueToString(value), ID, object);
			break;
		case SIGNAL:
			updateQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SEMAPHORE, SIGNAL, valueToString(value), ID, object);
			break;
		case CURRENTPOSITION:
			updateQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SWITCH, CURRENTPOSITION, valueToString(value), ID, object);
			break;
		case POSITION:
			updateQuery = String.format("UPDATE %s SET `%s` = %s WHERE `%s` = %s;", SWITCHPOSITION, POSITION, valueToString(value), ID, object);
			break;
		default:
			throw new UnsupportedOperationException("Label '" + label + "' not supported.");
		}

		write(updateQuery);
	}

	@Override
	public void restore(long version) {
		DoltProcess.restoreDolt(version, versions.get(version));
	}

	@Override
	public void endTransaction() {
		try {
			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

//		final String footerFilePath = SQL_METAMODEL_DIR + "railway-footer.sql";
//		final File footerFile = new File(footerFilePath);
//
//		// this overwrites the destination file if it exists
//		try {
//			FileUtils.copyFile(footerFile, sqlRawFile);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		try {
			persist();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	protected void structuralFeaturesToSQL(final Map<String, ? extends Object> attributes, final StringBuilder columns, final StringBuilder values) {
		for (final Entry<String, ? extends Object> entry : attributes.entrySet()) {
			final String key = entry.getKey();
			final Object value = entry.getValue();

			columns.append(", `" + key + "`");
			values.append(", ");

			final String stringValue = (value == null ? "NULL" : valueToString(value));
			values.append(stringValue);
		}
	}

	private String valueToString(final Object value) {
		String stringValue;
		if (value instanceof Boolean) {
			stringValue = (Boolean) value ? "1" : "0";
		} else if (value instanceof String) {
			// escape string
			stringValue = "`" + value + "`";
		} else if (value instanceof Enum) {
			// change enum to ordinal value
			final Enum<?> enumeration = (Enum<?>) value;
			stringValue = Integer.toString(enumeration.ordinal());
		} else {
			stringValue = value.toString();
		}
		return stringValue;
	}

}
