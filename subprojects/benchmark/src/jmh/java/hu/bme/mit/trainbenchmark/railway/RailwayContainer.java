/**
 */
package hu.bme.mit.trainbenchmark.railway;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Container</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link RailwayContainer#getRoutes <em>Routes</em>}</li>
 *   <li>{@link RailwayContainer#getRegions <em>Regions</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getRailwayContainer()
 * @model
 * @generated
 */
public interface RailwayContainer extends EObject {
	/**
	 * Returns the value of the '<em><b>Routes</b></em>' containment reference list.
	 * The list contents are of type {@link Route}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Routes</em>' containment reference list.
	 * @see RailwayPackage#getRailwayContainer_Routes()
	 * @model containment="true"
	 * @generated
	 */
	EList<Route> getRoutes();

	/**
	 * Returns the value of the '<em><b>Regions</b></em>' containment reference list.
	 * The list contents are of type {@link Region}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Regions</em>' containment reference list.
	 * @see RailwayPackage#getRailwayContainer_Regions()
	 * @model containment="true"
	 * @generated
	 */
	EList<Region> getRegions();

} // RailwayContainer
