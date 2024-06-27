/**
 */
package hu.bme.mit.trainbenchmark.railway;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Region</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link Region#getSensors <em>Sensors</em>}</li>
 *   <li>{@link Region#getElements <em>Elements</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getRegion()
 * @model
 * @generated
 */
public interface Region extends RailwayElement {
	/**
	 * Returns the value of the '<em><b>Sensors</b></em>' containment reference list.
	 * The list contents are of type {@link Sensor}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Sensors</em>' containment reference list.
	 * @see RailwayPackage#getRegion_Sensors()
	 * @model containment="true"
	 * @generated
	 */
	EList<Sensor> getSensors();

	/**
	 * Returns the value of the '<em><b>Elements</b></em>' containment reference list.
	 * The list contents are of type {@link TrackElement}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Elements</em>' containment reference list.
	 * @see RailwayPackage#getRegion_Elements()
	 * @model containment="true"
	 * @generated
	 */
	EList<TrackElement> getElements();

} // Region
