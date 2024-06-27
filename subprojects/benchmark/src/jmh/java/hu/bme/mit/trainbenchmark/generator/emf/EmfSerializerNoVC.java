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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import hu.bme.mit.trainbenchmark.railway.RailwayContainer;
import hu.bme.mit.trainbenchmark.railway.RailwayElement;
import hu.bme.mit.trainbenchmark.railway.RailwayFactory;
import hu.bme.mit.trainbenchmark.railway.RailwayPackage;
import hu.bme.mit.trainbenchmark.railway.Region;
import hu.bme.mit.trainbenchmark.railway.Route;

public class EmfSerializerNoVC extends ModelSerializer {

	public EmfSerializerNoVC() {

	}

	@Override
	public String syntax() {
		return "EMF";
	}

	protected Resource resource;
	protected RailwayFactory factory;
	protected RailwayContainer container;

	@Override
	public void initModel() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(EmfConstants.MODEL_EXTENSION, new XMIResourceFactoryImpl());
		final String modelPath = "example."	+ EmfConstants.MODEL_EXTENSION;
		final URI resourceURI = URI.createFileURI(modelPath);
		final ResourceSet resourceSet = new ResourceSetImpl();
		resource = resourceSet.createResource(resourceURI);
		resource.getContents().clear();
		factory = RailwayFactory.eINSTANCE;
		container = factory.createRailwayContainer();
		resource.getContents().add(container);
	}

	@Override
	public Object createVertex(final int id, final String type, final Map<String, ? extends Object> attributes,
			final Map<String, Object> outgoingEdges, final Map<String, Object> incomingEdges) throws IOException {
		final EClass clazz = (EClass) RailwayPackage.eINSTANCE.getEClassifier(type);
		final RailwayElement railwayElement = (RailwayElement) RailwayFactory.eINSTANCE.create(clazz);
		railwayElement.setId(id);
		for (final Entry<String, ? extends Object> attribute : attributes.entrySet()) {
			setEmfAttribute(clazz, railwayElement, attribute.getKey(), attribute.getValue());
		}

		switch (type) {
		case ModelConstants.REGION:
			container.getRegions().add((Region) railwayElement);
			break;
		case ModelConstants.ROUTE:
			container.getRoutes().add((Route) railwayElement);
			break;
		default:
			break;
		}

		for (final Entry<String, Object> outgoingEdge : outgoingEdges.entrySet()) {
			createEdge(outgoingEdge.getKey(), railwayElement, outgoingEdge.getValue());
		}

		for (final Entry<String, Object> incomingEdge : incomingEdges.entrySet()) {
			createEdge(incomingEdge.getKey(), incomingEdge.getValue(), railwayElement);
		}

		return railwayElement;
	}

	@Override
	public void createEdge(final String label, final Object from, final Object to) throws IOException {
		final EObject objectFrom = (EObject) from;
		final EStructuralFeature edgeType = objectFrom.eClass().getEStructuralFeature(label);

		if (edgeType.isMany()) {
			@SuppressWarnings("unchecked")
			final List<Object> l = (List<Object>) objectFrom.eGet(edgeType);
			l.add(to);
		} else {
			objectFrom.eSet(edgeType, to);
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
		EObject source = (EObject) from;
		EStructuralFeature feature = source.eClass().getEStructuralFeature(label);
		if(feature.isMany()) {
			@SuppressWarnings("unchecked")
			List<EObject> list = (List<EObject>) source.eGet(feature);
			list.remove(to);
		} else {
			source.eSet(feature, null);
		}
	}

	@Override
	public void setAttribute(String label, Object object, Object value) throws IOException {
		EObject source = (EObject) object;
		EStructuralFeature feature = source.eClass().getEStructuralFeature(label);
		final Object target;
		if(SIGNAL.equals(label)) {
			final int ordinal = ((hu.bme.mit.trainbenchmark.constants.Signal) value).ordinal();
			target = hu.bme.mit.trainbenchmark.railway.Signal.get(ordinal);
		} else if(CURRENTPOSITION.equals(label) || POSITION.equals(label)) {
			final int ordinal = ((hu.bme.mit.trainbenchmark.constants.Position) value).ordinal();
			target = hu.bme.mit.trainbenchmark.railway.Position.get(ordinal);
		} else {

			target = value;
		}
		source.eSet(feature, target);
	}

	@Override
	public long commit() {
		return 0;
	}
	@Override
	public void restore(long version) {
	}
}
