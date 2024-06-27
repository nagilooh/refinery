package hu.bme.mit.trainbenchmark.generator.emf;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil.Copier;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import hu.bme.mit.trainbenchmark.railway.RailwayContainer;

public class EMFSerializerByResourceCopyState {
	protected XMIResourceImpl resource;
	protected RailwayContainer container;
	Map<String,EObject> intrinsicMap;
	public EMFSerializerByResourceCopyState(XMIResourceImpl resource, RailwayContainer container,
			Map<String, EObject> intrinsicMap) {
		super();
		this.resource = resource;
		this.container = container;
		this.intrinsicMap = intrinsicMap;
	}
	
	public EMFSerializerByResourceCopyState copy(ResourceSet resourceSet, String uri) {
		Copier copyer = new Copier();
		
		RailwayContainer newcontainer = (RailwayContainer) copyer.copy(container);
		
		Map<String,EObject> newIntristicMap = new HashMap<>();
		for (Entry<String, EObject> entry : intrinsicMap.entrySet()) {
			newIntristicMap.put(entry.getKey(), copyer.get(entry.getValue()));
		}
		
		final String modelPath = uri+"."+ EmfConstants.MODEL_EXTENSION;
		final URI resourceURI = URI.createFileURI(modelPath);
		
		XMIResourceImpl resource = (XMIResourceImpl) resourceSet.createResource(resourceURI);
		Map<String,EObject> intrinsicMap = new HashMap<>();
		
		resource.getContents().add(newcontainer);
		resource.setIntrinsicIDToEObjectMap(intrinsicMap);
		
		return new EMFSerializerByResourceCopyState(resource, newcontainer, intrinsicMap);
		
	}
}
