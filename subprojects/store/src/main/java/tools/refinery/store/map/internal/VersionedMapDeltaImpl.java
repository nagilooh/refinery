package tools.refinery.store.map.internal;

import java.util.*;

import tools.refinery.store.map.*;

public class VersionedMapDeltaImpl<K, V> implements VersionedMap<K, V> {
	protected final VersionedMapStoreDeltaImpl<K, V> store;

	final Map<K, V> current;

	final UncommittedDeltaStore<K, V> uncommittedStore;
	MapTransaction<K, V> previous;

	protected final V defaultValue;

	public VersionedMapDeltaImpl(VersionedMapStoreDeltaImpl<K, V> store, boolean summarizeChanges, V defaultValue) {
		this.store = store;
		this.defaultValue = defaultValue;

		current = new HashMap<>();
		if (summarizeChanges) {
			this.uncommittedStore = new UncommittedDeltaMapStore<>(this);
		} else {
			this.uncommittedStore = new UncommittedDeltaArrayStore<>();
		}
	}

	@Override
	public V getDefaultValue() {
		return defaultValue;
	}

	@Override
	public long commit() {
		MapDelta<K, V>[] deltas = uncommittedStore.extractAndDeleteDeltas();
		long[] versionContainer = new long[1];
		this.previous = this.store.appendTransaction(deltas, previous, versionContainer);
		return versionContainer[0];
	}

	@Override
	public void restore(long state) {
		// 1. restore uncommitted states
		MapDelta<K, V>[] uncommitted = this.uncommittedStore.extractAndDeleteDeltas();
		if (uncommitted != null) {
			backward(uncommitted);
		}

		// 2. get common ancestor
		final MapTransaction<K,V> parent;
		List<MapDelta<K, V>[]> forward = new ArrayList<>();
		if (this.previous == null) {
			parent = this.store.getPath(state, forward);
			this.forward(forward);
		} else {
			List<MapDelta<K, V>[]> backward = new ArrayList<>();
			parent = this.store.getPath(this.previous.version(), state, backward, forward);
			this.backward(backward);
			this.forward(forward);
		}
		this.previous = parent;
	}

	protected void forward(List<MapDelta<K, V>[]> changes) {
		for (int i = changes.size() - 1; i >= 0; i--) {
			forward(changes.get(i));
		}
	}

	protected void backward(List<MapDelta<K, V>[]> changes) {
		for (int i = 0; i < changes.size(); i++) {
			backward(changes.get(i));
		}
	}

	protected void forward(MapDelta<K, V>[] changes) {
		for (int i = 0; i < changes.length; i++) {
			final MapDelta<K, V> change = changes[i];
			K key = change.getKey();
			V newValue = change.getNewValue();

			if(newValue == defaultValue) {
				current.remove(key);
			} else {
				current.put(key,newValue);
			}
		}
	}

	protected void backward(MapDelta<K, V>[] changes) {
		for (int i = changes.length - 1; i >= 0; i--) {
			final MapDelta<K, V> change = changes[i];
			K key = change.getKey();
			V oldValue = change.oldValue();

			if(oldValue == defaultValue) {
				current.remove(key);
			} else {
				current.put(key,oldValue);
			}
		}
	}

	@Override
	public V get(K key) {
		return current.getOrDefault(key, defaultValue);
	}

	@Override
	public Cursor<K, V> getAll() {
		return new IteratorAsCursor<>(this, current);
	}

	@Override
	public V put(K key, V value) {
		final V oldValue;
		if (Objects.equals(value, defaultValue)) {
			final V res = current.remove(key);
			if (res == null) {
				// no changes: default > default
				oldValue = defaultValue;
			} else {
				oldValue = res;
			}
		} else {
			final var mapValue = current.put(key, value);
			if (mapValue == null) {
				oldValue = defaultValue;
			} else {
				oldValue = mapValue;
			}
		}
		if(!Objects.equals(oldValue,value)) {
			uncommittedStore.processChange(key, oldValue, value);
		}
		return oldValue;
	}

	@Override
	public void putAll(Cursor<K, V> cursor) {
		if (cursor.getDependingMaps().contains(this)) {
			List<K> keys = new ArrayList<>();
			List<V> values = new ArrayList<>();
			while (cursor.move()) {
				keys.add(cursor.getKey());
				values.add(cursor.getValue());
			}
			for (int i = 0; i < keys.size(); i++) {
				this.put(keys.get(i), values.get(i));
			}
		} else {
			while (cursor.move()) {
				this.put(cursor.getKey(), cursor.getValue());
			}
		}
	}

	@Override
	public long getSize() {
		return current.size();
	}

	@Override
	public DiffCursor<K, V> getDiffCursor(long state) {
		MapDelta<K, V>[] backward = this.uncommittedStore.extractDeltas();
		List<MapDelta<K, V>[]> backwardTransactions = new ArrayList<>();
		List<MapDelta<K, V>[]> forwardTransactions = new ArrayList<>();

		if (backward != null) {
			backwardTransactions.add(backward);
		}

		if (this.previous != null) {
			store.getPath(this.previous.version(), state, backwardTransactions, forwardTransactions);
		} else {
			store.getPath(state, forwardTransactions);
		}

		return new DeltaDiffCursor<>(backwardTransactions, forwardTransactions);
	}

	@Override
	public int contentHashCode(ContentHashCode mode) {
		return this.current.hashCode();
	}

	@Override
	public boolean contentEquals(AnyVersionedMap other) {
		if (other instanceof VersionedMapDeltaImpl<?, ?> versioned) {
			if (versioned == this) {
				return true;
			} else {
				return Objects.equals(this.defaultValue, versioned.defaultValue) && Objects.equals(this.current, versioned.current);
			}
		} else {
			throw new UnsupportedOperationException("Comparing different map implementations is ineffective.");
		}
	}

	@Override
	public void checkIntegrity() {
		this.uncommittedStore.checkIntegrity();

		for (var entry : this.current.entrySet()) {
			var value = entry.getValue();
			if (value == this.defaultValue) {
				throw new IllegalStateException("Default value stored in map!");
			} else if (value == null) {
				throw new IllegalStateException("null value stored in map!");
			}
		}
	}
}
