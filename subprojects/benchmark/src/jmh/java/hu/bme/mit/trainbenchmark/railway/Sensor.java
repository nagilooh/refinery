/**
 */
package hu.bme.mit.trainbenchmark.railway;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Sensor</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link Sensor#getMonitors <em>Monitors</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getSensor()
 * @model
 * @generated
 */
public interface Sensor extends RailwayElement {
	/**
	 * Returns the value of the '<em><b>Monitors</b></em>' reference list.
	 * The list contents are of type {@link TrackElement}.
	 * It is bidirectional and its opposite is '{@link TrackElement#getMonitoredBy <em>Monitored By</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Monitors</em>' reference list.
	 * @see RailwayPackage#getSensor_Monitors()
	 * @see TrackElement#getMonitoredBy
	 * @model opposite="monitoredBy"
	 * @generated
	 */
	EList<TrackElement> getMonitors();

} // Sensor
