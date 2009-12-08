/**
 * Java ILP is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Java ILP is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Java ILP. If not, see http://www.gnu.org/licenses/.
 */
package net.sf.javailp;

import java.util.HashMap;

/**
 * The class {@code ResultImpl} is a {@code Map} based implementation of the
 * {@link Result}.
 * 
 * @author lukasiewycz
 * 
 */
public class ResultImpl extends HashMap<Object, Number> implements Result {

	protected Number objectiveValue = null;
	protected Linear objectiveFunction = null;

	/**
	 * Constructs a {@code ResultImpl} for a {@code Problem} without objective
	 * function.
	 */
	public ResultImpl() {
	}

	/**
	 * Constructs a {@code ResultImpl} for a {@code Problem} with objective
	 * function and the optimal value.
	 */
	public ResultImpl(Number objectiveValue) {
		this.objectiveValue = objectiveValue;
	}

	/**
	 * Constructs a {@code ResultImpl} for a {@code Problem} with an objective
	 * function.
	 */
	public ResultImpl(Linear objectiveFunction) {
		this.objectiveFunction = objectiveFunction;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.javailp.Result#getObjective()
	 */
	public Number getObjective() {
		if (objectiveValue != null) {
			return objectiveValue;
		} else if (objectiveFunction != null) {
			objectiveValue = objectiveFunction.evaluate(this);
			return objectiveValue;
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.javailp.Result#getBoolean(java.lang.Object)
	 */
	public boolean getBoolean(Object key) {
		Number number = get(key);
		double v = number.doubleValue();
		if (v == 0) {
			return false;
		} else {
			return true;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.javailp.Result#containsVar(java.lang.Object)
	 */
	public Boolean containsVar(Object var) {
		return containsKey(var);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.AbstractMap#toString()
	 */
	@Override
	public String toString() {
		return "Objective: " + getObjective() + " " + super.toString();
	}

	private static final long serialVersionUID = 1L;

}