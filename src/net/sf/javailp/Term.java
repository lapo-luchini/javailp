package net.sf.javailp;

public class Term {

	protected final Object variable;
	protected final Number coefficient;

	public Term(Object variable, Number coefficient) {
		super();
		this.variable = variable;
		this.coefficient = coefficient;
	}

	public Object getVariable() {
		return variable;
	}

	public Number getCoefficient() {
		return coefficient;
	}

}
