/**
 */
package hu.bme.mit.trainbenchmark.railway;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Semaphore</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link Semaphore#getSignal <em>Signal</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getSemaphore()
 * @model
 * @generated
 */
public interface Semaphore extends RailwayElement {
	/**
	 * Returns the value of the '<em><b>Signal</b></em>' attribute.
	 * The literals are from the enumeration {@link Signal}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Signal</em>' attribute.
	 * @see Signal
	 * @see #setSignal(Signal)
	 * @see RailwayPackage#getSemaphore_Signal()
	 * @model unique="false"
	 * @generated
	 */
	Signal getSignal();

	/**
	 * Sets the value of the '{@link Semaphore#getSignal <em>Signal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Signal</em>' attribute.
	 * @see Signal
	 * @see #getSignal()
	 * @generated
	 */
	void setSignal(Signal value);

} // Semaphore
