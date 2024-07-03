/*******************************************************************************
 * Copyright (c) 2010-2015, Benedek Izso, Gabor Szarnyas, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Benedek Izso - initial API and implementation
 *   Gabor Szarnyas - initial API and implementation
 *******************************************************************************/

package hu.bme.mit.trainbenchmark.generator.emf;

import static hu.bme.mit.trainbenchmark.constants.ModelConstants.CURRENTPOSITION;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.POSITION;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SIGNAL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import hu.bme.mit.trainbenchmark.railway.RailwayContainer;
import hu.bme.mit.trainbenchmark.railway.RailwayElement;
import hu.bme.mit.trainbenchmark.railway.RailwayFactory;
import hu.bme.mit.trainbenchmark.railway.RailwayPackage;
import hu.bme.mit.trainbenchmark.railway.Region;
import hu.bme.mit.trainbenchmark.railway.Route;

public class EmfSerializerByResourceCopy extends ModelSerializer {

	public EmfSerializerByResourceCopy() {

	}

	@Override
	public String syntax() {
		return "EMF";
	}

	protected RailwayFactory factory;


	List<EMFSerializerByResourceCopyState> states = new ArrayList<>();
	EMFSerializerByResourceCopyState actual;
	protected ResourceSet resourceSet;
//	protected XMIResource resource;
//	Map<String,EObject> intrinsicMap;

	@Override
	public void initModel() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(EmfConstants.MODEL_EXTENSION, new XMIResourceFactoryImpl());
		final String modelPath = "actual."	+ EmfConstants.MODEL_EXTENSION;
		final URI resourceURI = URI.createFileURI(modelPath);
		resourceSet = new ResourceSetImpl();

		factory = RailwayFactory.eINSTANCE;

		XMIResourceImpl resource = (XMIResourceImpl) resourceSet.createResource(resourceURI);
		Map<String,EObject> intrinsicMap = new HashMap<>();
		RailwayContainer container = factory.createRailwayContainer();
		resource.setIntrinsicIDToEObjectMap(intrinsicMap);

//		xmiResource.getEObjectToIDMap();
		resource.getContents().clear();
		resource.getContents().add(container);

		actual = new EMFSerializerByResourceCopyState(resource,container,intrinsicMap);
	}

	@Override
	public Object createVertex(final int id, final String type, final Map<String, ? extends Object> attributes,
			final Map<String, Object> outgoingEdges, final Map<String, Object> incomingEdges) throws IOException {
		final EClass clazz = (EClass) RailwayPackage.eINSTANCE.getEClassifier(type);
		final RailwayElement railwayElement = (RailwayElement) RailwayFactory.eINSTANCE.create(clazz);
		railwayElement.setId(id);
		//resource.setID(railwayElement, id+"");
		String intrinsicID = ""+id;
		actual.intrinsicMap.put(intrinsicID, railwayElement);

		for (final Entry<String, ? extends Object> attribute : attributes.entrySet()) {
			setEmfAttribute(clazz, railwayElement, attribute.getKey(), attribute.getValue());
		}

		switch (type) {
		case ModelConstants.REGION:
			actual.container.getRegions().add((Region) railwayElement);
			break;
		case ModelConstants.ROUTE:
			actual.container.getRoutes().add((Route) railwayElement);
			break;
		default:
			break;
		}

		for (final Entry<String, Object> outgoingEdge : outgoingEdges.entrySet()) {
			createEdge(outgoingEdge.getKey(), intrinsicID, outgoingEdge.getValue());
		}

		for (final Entry<String, Object> incomingEdge : incomingEdges.entrySet()) {
			createEdge(incomingEdge.getKey(), incomingEdge.getValue(), intrinsicID);
		}

		//System.out.println(resource.getEObject(""+id));
		//resource.save(null);
		return intrinsicID;
	}

	@Override
	public void createEdge(final String label, final Object from, final Object to) throws IOException {
		final EObject objectFrom = actual.resource.getEObject((String) from);
		final EStructuralFeature edgeType = objectFrom.eClass().getEStructuralFeature(label);
		final EObject objectTo = actual.resource.getEObject((String) to);

		if (edgeType.isMany()) {
			@SuppressWarnings("unchecked")
			final List<Object> l = (List<Object>) objectFrom.eGet(edgeType);
			l.add(objectTo);
		} else {
			objectFrom.eSet(edgeType, objectTo);
		}
	}

	protected void setEmfAttribute(final EClass clazz, final RailwayElement node, final String key, Object value) {
		// change the enum value from the
		// hu.bme.mit.trainbenchmark.constants.Signal enum to the
		// hu.bme.mit.trainbenchmark.railway.Signal enum
		if (SIGNAL.equals(key)) {
			final int ordinal = ((hu.bme.mit.trainbenchmark.constants.Signal) value).ordinal();
			value = hu.bme.mit.trainbenchmark.railway.Signal.get(ordinal);
		} else if (CURRENTPOSITION.equals(key) || POSITION.equals(key)) {
			final int ordinal = ((hu.bme.mit.trainbenchmark.constants.Position) value).ordinal();
			value = hu.bme.mit.trainbenchmark.railway.Position.get(ordinal);
		}

		final EStructuralFeature feature = clazz.getEStructuralFeature(key);
		node.eSet(feature, value);
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		EObject source = (EObject) actual.resource.getEObject((String) from);
		EStructuralFeature feature = source.eClass().getEStructuralFeature(label);
		if(feature.isMany()) {
			@SuppressWarnings("unchecked")
			List<EObject> list = (List<EObject>) source.eGet(feature);
			EObject target = (EObject) actual.resource.getEObject((String) to);
			list.remove(target);
		} else {
			source.eSet(feature, null);
		}
	}

	@Override
	public void setAttribute(String label, Object object, Object value) throws IOException {
		EObject source = (EObject) actual.resource.getEObject((String) object);
		EStructuralFeature feature = source.eClass().getEStructuralFeature(label);
		source.eSet(feature, value);
	}

	@Override
	public long commit() {
		int version = states.size();
		states.add(this.actual.copy(resourceSet, "state"+version));
		return version;
	}

	public void print() {
		TreeIterator<EObject> i = this.actual.resource.getAllContents();
		Map<EReference,Integer> upperLimits = new HashMap<>();
		while(i.hasNext()) {
			EObject o = i.next();
			for (EReference reference : o.eClass().getEAllReferences()) {
				if(reference.isMany()) {
					List<?> list = (List<?>) o.eGet(reference);
					Integer upper = upperLimits.get(reference);
					if(upper == null || upper < list.size()) {
						upperLimits.put(reference, list.size());
					}
				}
			}
		}

		for (Entry<EReference, Integer> entry : upperLimits.entrySet()) {
			System.out.println(entry.getKey().getName() + " <= " + entry.getValue());
		}
	}
	@Override
	public void restore(long version) {
		this.actual = this.states.get((int) version);
	}

	@Override
	public void endTransaction() {

	}
}
