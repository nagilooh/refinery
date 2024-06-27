/**
 */
package hu.bme.mit.trainbenchmark.railway;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Track Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link TrackElement#getMonitoredBy <em>Monitored By</em>}</li>
 *   <li>{@link TrackElement#getConnectsTo <em>Connects To</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getTrackElement()
 * @model abstract="true"
 * @generated
 */
public interface TrackElement extends RailwayElement {
	/**
	 * Returns the value of the '<em><b>Monitored By</b></em>' reference list.
	 * The list contents are of type {@link Sensor}.
	 * It is bidirectional and its opposite is '{@link Sensor#getMonitors <em>Monitors</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Monitored By</em>' reference list.
	 * @see RailwayPackage#getTrackElement_MonitoredBy()
	 * @see Sensor#getMonitors
	 * @model opposite="monitors"
	 * @generated
	 */
	EList<Sensor> getMonitoredBy();

	/**
	 * Returns the value of the '<em><b>Connects To</b></em>' reference list.
	 * The list contents are of type {@link TrackElement}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Connects To</em>' reference list.
	 * @see RailwayPackage#getTrackElement_ConnectsTo()
	 * @model
	 * @generated
	 */
	EList<TrackElement> getConnectsTo();

} // TrackElement
