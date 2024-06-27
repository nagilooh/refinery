package hu.bme.mit.trainbenchmark.generator.emf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.change.ChangeDescription;
import org.eclipse.emf.ecore.change.util.ChangeRecorder;

public class EMFSerializerByTransactions extends EmfSerializerNoVC{
	final boolean assumeAllChangesCommited = true;

	ChangeRecorder recorder;
	Map<Long, EMFSerializerTransaction> states = new HashMap<>();

	EMFSerializerTransaction lastCommit = null;

	long lastState = -1;

	final boolean linearHistory;

	public EMFSerializerByTransactions(boolean linearHistory) {
		this.linearHistory = linearHistory;
	}

	@Override
	public void initModel() {
		super.initModel();
		this.recorder = new ImprovedChangeRecorder(resource);//new ChangeRecorder(resource);
		this.recorder.beginRecording(List.of(resource));
	}

	@Override
	public long commit() {
		// 1. stop recording the transactions
		ChangeDescription description = recorder.endRecording();
		// 2. store transaction
		long nextID = states.size();
		EMFSerializerTransaction transaction = new EMFSerializerTransaction(lastCommit, description, nextID);
		states.put(nextID, transaction);
		lastCommit = transaction;
		// 3. restart recording into a new transaction
		this.recorder.beginRecording(List.of(resource));
		// 4. return state identifier
		return nextID;
	}

	@Override
	public void restore(long version) {
		// 1. Go back to commited state
		if(!assumeAllChangesCommited) {
			ChangeDescription uncommited = recorder.endRecording();
			uncommited.apply();
		}
		// 2. discover common ancestor
		List<EMFSerializerTransaction> backwardStack = new ArrayList<>();
		List<EMFSerializerTransaction> forwardStack = new ArrayList<>();
		EMFSerializerTransaction targetState = states.get(version);
		commonAncestor(version, backwardStack, forwardStack, targetState);
		// 3. go back to the common ancestor
		for(int i = 0; i < backwardStack.size(); i++) {
			backwardStack.get(i).getChanges().applyAndReverse();
		}
		// 4. go down to the selected state, backward
		for(int i = forwardStack.size()-1; i >= 0; i--) {
			forwardStack.get(i).getChanges().applyAndReverse();
		}
		// 5. reset lastCommit
		this.lastCommit = states.get(version);
		// 6. restart recording
		recorder.beginRecording(List.of(resource));
		// 7. If history linear, backward stack can be freed
		if(linearHistory) {
			for (EMFSerializerTransaction emfSerializerTransaction : backwardStack) {
				states.remove(emfSerializerTransaction.getVersion());
			}
		}
	}

	private void commonAncestor(long version,
			List<EMFSerializerTransaction> backwardStack,
			List<EMFSerializerTransaction> forwardStack,
			EMFSerializerTransaction targetState) {
		// can be null
		EMFSerializerTransaction fromBranch = lastCommit;
		// cannot be null
		EMFSerializerTransaction toBranch = targetState;

		while(fromBranch != toBranch) {
			if(fromBranch != null && fromBranch.getVersion()>toBranch.getVersion()) {
				backwardStack.add(fromBranch);
				fromBranch = fromBranch.getPrevious();
			} else if(toBranch != null && toBranch.getVersion()>fromBranch.getVersion()) {
				forwardStack.add(toBranch);
				toBranch = toBranch.getPrevious();
			}
		}
	}
}
