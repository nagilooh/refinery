package hu.bme.mit.trainbenchmark.generator.emf;

import org.eclipse.emf.ecore.change.ChangeDescription;

public class EMFSerializerTransaction {
	private EMFSerializerTransaction previous;
	private ChangeDescription changes;
	private long version;
	public EMFSerializerTransaction(EMFSerializerTransaction previous, ChangeDescription changes,
			long version) {
		if(changes == null) {
			throw new IllegalArgumentException("Changes are empty!");
		}
		this.previous = previous;
		this.changes = changes;
		this.version = version;
	}
	public EMFSerializerTransaction getPrevious() {
		return previous;
	}
	public ChangeDescription getChanges() {
		return changes;
	}
	public long getVersion() {
		return version;
	}
}
