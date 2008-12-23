/**
 * Java ILP is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Opt4J is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Opt4J. If not, see http://www.gnu.org/licenses/.
 */
package net.sf.javailp;

import java.util.ArrayList;
import java.util.List;

/**
 * The class {@code Linear} is a linear expression consisting of variables and
 * their coefficients.
 * 
 * @author lukasiewycz
 * 
 */
public class Linear {

	protected final List<Number> coefficients = new ArrayList<Number>();
	protected final List<Object> variables = new ArrayList<Object>();

	/**
	 * Constructs an empty linear expression.
	 */
	public Linear() {
		super();
	}

	/**
	 * Constructs a linear expression with the predefined variables and their
	 * coefficients.
	 * 
	 * @param coefficients
	 *            the coefficients
	 * @param variables
	 *            the variables
	 */
	public Linear(List<Number> coefficients, List<Object> variables) {
		this();
		if (coefficients.size() != variables.size()) {
			throw new IllegalArgumentException("The size of the varibales and coefficients must be equal.");
		}
		this.coefficients.addAll(coefficients);
		this.variables.addAll(variables);
	}

	/**
	 * Returns the coefficients.
	 * 
	 * @return the coefficients
	 */
	public List<Number> getCoefficients() {
		return coefficients;
	}

	/**
	 * Returns the variables.
	 * 
	 * @return the variables
	 */
	public List<Object> getVariables() {
		return variables;
	}

	/**
	 * Adds an element to the linear expression.
	 * 
	 * @param coefficient
	 *            the coefficient
	 * @param variable
	 *            the variable
	 */
	public void add(Number coefficient, Object variable) {
		coefficients.add(coefficient);
		variables.add(variable);
	}

	/**
	 * Returns the size (number of variables) of the linear expression.
	 * 
	 * @return the size
	 */
	public int size() {
		return coefficients.size();
	}

	/**
	 * Removes all elements.
	 */
	public void clear() {
		coefficients.clear();
		variables.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < size(); i++) {
			Number coeff = coefficients.get(i);
			Object variable = variables.get(i);

			s += coeff + "*" + variable;
			if (i < size() - 1) {
				s += " + ";
			}
		}
		return s;
	}

}
