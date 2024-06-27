/**
 */
package hu.bme.mit.trainbenchmark.railway;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Switch Position</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link SwitchPosition#getPosition <em>Position</em>}</li>
 *   <li>{@link SwitchPosition#getRoute <em>Route</em>}</li>
 *   <li>{@link SwitchPosition#getTarget <em>Target</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getSwitchPosition()
 * @model
 * @generated
 */
public interface SwitchPosition extends RailwayElement {
	/**
	 * Returns the value of the '<em><b>Position</b></em>' attribute.
	 * The literals are from the enumeration {@link Position}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Position</em>' attribute.
	 * @see Position
	 * @see #setPosition(Position)
	 * @see RailwayPackage#getSwitchPosition_Position()
	 * @model unique="false"
	 * @generated
	 */
	Position getPosition();

	/**
	 * Sets the value of the '{@link SwitchPosition#getPosition <em>Position</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Position</em>' attribute.
	 * @see Position
	 * @see #getPosition()
	 * @generated
	 */
	void setPosition(Position value);

	/**
	 * Returns the value of the '<em><b>Route</b></em>' container reference.
	 * It is bidirectional and its opposite is '{@link Route#getFollows <em>Follows</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Route</em>' container reference.
	 * @see #setRoute(Route)
	 * @see RailwayPackage#getSwitchPosition_Route()
	 * @see Route#getFollows
	 * @model opposite="follows" transient="false"
	 * @generated
	 */
	Route getRoute();

	/**
	 * Sets the value of the '{@link SwitchPosition#getRoute <em>Route</em>}' container reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Route</em>' container reference.
	 * @see #getRoute()
	 * @generated
	 */
	void setRoute(Route value);

	/**
	 * Returns the value of the '<em><b>Target</b></em>' reference.
	 * It is bidirectional and its opposite is '{@link Switch#getPositions <em>Positions</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target</em>' reference.
	 * @see #setTarget(Switch)
	 * @see RailwayPackage#getSwitchPosition_Target()
	 * @see Switch#getPositions
	 * @model opposite="positions"
	 * @generated
	 */
	Switch getTarget();

	/**
	 * Sets the value of the '{@link SwitchPosition#getTarget <em>Target</em>}' reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target</em>' reference.
	 * @see #getTarget()
	 * @generated
	 */
	void setTarget(Switch value);

} // SwitchPosition
