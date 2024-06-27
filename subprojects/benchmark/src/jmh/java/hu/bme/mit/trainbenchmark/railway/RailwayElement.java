/**
 */
package hu.bme.mit.trainbenchmark.railway;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Element</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link RailwayElement#getId <em>Id</em>}</li>
 * </ul>
 *
 * @see RailwayPackage#getRailwayElement()
 * @model abstract="true"
 * @generated
 */
public interface RailwayElement extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(int)
	 * @see RailwayPackage#getRailwayElement_Id()
	 * @model unique="false" id="true"
	 * @generated
	 */
	int getId();

	/**
	 * Sets the value of the '{@link RailwayElement#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(int value);

} // RailwayElement
