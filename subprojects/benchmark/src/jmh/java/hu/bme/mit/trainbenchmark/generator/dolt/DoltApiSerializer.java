package hu.bme.mit.trainbenchmark.generator.dolt;

import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import static hu.bme.mit.trainbenchmark.constants.ModelConstants.*;

public class DoltApiSerializer extends ModelSerializer {

	protected String SQL_FORMAT_DIR = "subprojects/benchmark/";
	protected String SQL_METAMODEL_DIR = SQL_FORMAT_DIR + "src/jmh/resources/dolt/metamodel/";
	protected Map<Long, String> versions;
	protected List<String> branches;
	long commits = 0;

	String user = "root";
	String port = "3306";
	String db   = "trainbenchmark";
	String password = "";
	Connection conn;
	Statement st;

	public DoltApiSerializer() {
		versions  = new HashMap<>();
		branches = new ArrayList<>();

		String url = "jdbc:mysql://127.0.0.1:" + port + "/" + db;
		try {
			conn = DriverManager.getConnection(url, user, password);
			st = conn.createStatement();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public String syntax() {
		return "SQL";
	}

	public void write(final String s) throws IOException {

	}

	String[] initQueries = {
			"CREATE TABLE IF NOT EXISTS `Route` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  `active` int,\n" +
					"  `entry` int,\n" +
					"  `exit` int,\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `Region` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `Segment` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  `length` int NOT NULL DEFAULT 1,\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `Sensor` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  `region` int, -- inverse of the `sensors` edge\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `Semaphore` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  `segment` int, -- inverse of the `semaphores` edge\n" +
					"  `signal` int NOT NULL,\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `Switch` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  `currentPosition` int NOT NULL,\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `SwitchPosition` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  `route` int, -- inverse of the `follows` edge\n" +
					"  `target` int,\n" +
					"  `position` int NOT NULL,\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `TrackElement` (\n" +
					"  `id` int NOT NULL AUTO_INCREMENT,\n" +
					"  `region` int, -- inverse of the `elements` edge\n" +
					"  PRIMARY KEY  (`id`)\n" +
					") DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `connectsTo` (\n" +
					"  `TrackElement1_id` int NOT NULL,\n" +
					"  `TrackElement2_id` int NOT NULL,\n" +
					"  PRIMARY KEY  (`TrackElement1_id`, `TrackElement2_id`)\n" +
					") DEFAULT CHARSET=utf8 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `monitoredBy` (\n" +
					"  `TrackElement_id` int NOT NULL,\n" +
					"  `Sensor_id` int NOT NULL,\n" +
					"  PRIMARY KEY  (`TrackElement_id`, `Sensor_id`)\n" +
					") DEFAULT CHARSET=utf8 ENGINE=MEMORY;",

			"CREATE TABLE IF NOT EXISTS `requires` (\n" +
					"  `Route_id` int NOT NULL,\n" +
					"  `Sensor_id` int NOT NULL,\n" +
					"  PRIMARY KEY  (`Route_id`, `Sensor_id`)\n" +
					") DEFAULT CHARSET=utf8 ENGINE=MEMORY;",
	};

	@Override
	public void initModel() throws IOException {

		try {
			st.execute("drop database if exists trainbenchmark;");
			st.execute("call dolt_purge_dropped_databases();");
			st.execute("create database trainbenchmark;");
			st.execute("use trainbenchmark;");
			for (var query : initQueries) {
				st.execute(query);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public long commit() {

		try {
			if (st.execute("call dolt_add('-A');")) {
				ResultSet rs = st.getResultSet();
			}
			if (st.execute("call dolt_commit('-m', 'trainbenchmark');")) {
				ResultSet rs = st.getResultSet();
				if (rs.next()) {
					var hash = rs.getString(1);
					versions.put(commits, hash);
					return commits++;
				}
				else {
					throw new RuntimeException("Commit failed");
				}
			}
			else {
				throw new RuntimeException("Commit failed");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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

		try {

			if (SUPERTYPES.containsKey(type)) {
				final String ancestorType = SUPERTYPES.get(type);
				st.execute(String.format("INSERT INTO %s (`%s`) VALUES (%s);", ancestorType, ID, id));
				st.execute(String.format("INSERT INTO %s (%s) VALUES (%s);", type, columns, values));
			} else {
				final String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s);", type, columns, values);
				st.execute(insertQuery);
			}
		}
		catch (SQLException e) {
			throw new RuntimeException(e);
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

		try {
			st.execute(insertQuery);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
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

		try {
			st.execute(deleteQuery);
		} catch (SQLException e) {
			System.out.println("deleteQuery:" + deleteQuery);
			throw new RuntimeException(e);
		}
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

		try {
			st.execute(updateQuery);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void restore(long version) {
		var branchName = String.valueOf(version);
		try {
			if (branches.contains(branchName)) {
				if (st.execute("call dolt_checkout('" + branchName + "');")) {
//					ResultSet rs = st.getResultSet();
//					if (rs.next()) {
//						System.out.println(rs.getString(1));
//					}
				}
			}
			else {
				if (st.execute("call dolt_checkout('-b', '" + branchName + "' ,'" + versions.get(version) + "');")) {
//					ResultSet rs = st.getResultSet();
//					if (rs.next()) {
//						System.out.println(rs.getString(1));
//					}
					branches.add(branchName);
				}

			}

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void endTransaction() {
//		try {
//			writer.close();
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

//		final String footerFilePath = SQL_METAMODEL_DIR + "railway-footer.sql";
//		final File footerFile = new File(footerFilePath);
//
//		// this overwrites the destination file if it exists
//		try {
//			FileUtils.copyFile(footerFile, sqlRawFile);
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

//		try {
//			persist();
//		} catch (IOException | InterruptedException e) {
//			throw new RuntimeException(e);
//		}
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
