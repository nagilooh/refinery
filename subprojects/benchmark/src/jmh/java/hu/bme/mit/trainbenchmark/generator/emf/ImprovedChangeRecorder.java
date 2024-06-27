package hu.bme.mit.trainbenchmark.generator.emf;

import java.util.Collections;
import java.util.Iterator;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.change.util.ChangeRecorder;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.InternalEList;

public class ImprovedChangeRecorder extends ChangeRecorder {
	
	public ImprovedChangeRecorder(Resource resource)
	  {
	    beginRecording(Collections.singleton(resource));
	  }
	
	@Override
	public void setTarget(Notifier target) {
		Iterator<? extends Notifier> contents = (target instanceof EObject)
				? isResolveProxies() ? ((EObject) target).eContents().iterator()
						: ((InternalEList<EObject>) ((EObject) target).eContents()).basicIterator()
				: target instanceof ResourceSet ? ((ResourceSet) target).getResources().iterator()
						: target instanceof Resource ? ((Resource) target).getContents().iterator() : null;

		if (contents != null) {
			while (contents.hasNext()) {
				Notifier notifier = contents.next();
				addAdapter(notifier);
			}
		}
	}
}
