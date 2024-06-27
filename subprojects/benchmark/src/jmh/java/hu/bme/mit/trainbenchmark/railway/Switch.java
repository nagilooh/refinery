/**
 */
package hu.bme.mit.trainbenchmark.railway;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Switch</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link Switch#getCurrentPosition <em>Current Position</em>}</li>
 *   <li>{@link Switch#getPositions <em>Positions</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getSwitch()
 * @model
 * @generated
 */
public interface Switch extends TrackElement {
	/**
	 * Returns the value of the '<em><b>Current Position</b></em>' attribute.
	 * The literals are from the enumeration {@link Position}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Current Position</em>' attribute.
	 * @see Position
	 * @see #setCurrentPosition(Position)
	 * @see RailwayPackage#getSwitch_CurrentPosition()
	 * @model unique="false"
	 * @generated
	 */
	Position getCurrentPosition();

	/**
	 * Sets the value of the '{@link Switch#getCurrentPosition <em>Current Position</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Current Position</em>' attribute.
	 * @see Position
	 * @see #getCurrentPosition()
	 * @generated
	 */
	void setCurrentPosition(Position value);

	/**
	 * Returns the value of the '<em><b>Positions</b></em>' reference list.
	 * The list contents are of type {@link SwitchPosition}.
	 * It is bidirectional and its opposite is '{@link SwitchPosition#getTarget <em>Target</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Positions</em>' reference list.
	 * @see RailwayPackage#getSwitch_Positions()
	 * @see SwitchPosition#getTarget
	 * @model opposite="target"
	 * @generated
	 */
	EList<SwitchPosition> getPositions();

} // Switch
