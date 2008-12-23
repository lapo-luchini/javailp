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

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.DoubleParam;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The {@code SolverCPLEX} is the {@code Solver} CPLEX.
 * 
 * @author lukasiewycz
 * 
 */
public class SolverCPLEX implements Solver {

	protected final Map<Object, Object> parameters = new HashMap<Object, Object>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.javailp.Solver#getParameters()
	 */
	public Map<Object, Object> getParameters() {
		return parameters;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.javailp.Solver#setParameter(java.lang.Object,
	 * java.lang.Object)
	 */
	public void setParameter(Object parameter, Object value) {
		parameters.put(parameter, value);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.javailp.Solver#solve(net.sf.javailp.Problem)
	 */
	public Result solve(Problem problem) {
		Map<IloNumVar, Object> numToVar = new HashMap<IloNumVar, Object>();
		Map<Object, IloNumVar> varToNum = new HashMap<Object, IloNumVar>();

		try {
			IloCplex cplex = new IloCplex();

			initWithParameters(cplex);

			for (Object variable : problem.getVariables()) {
				VarType varType = problem.getVarType(variable);
				Number lowerBound = problem.getVarLowerBound(variable);
				Number upperBound = problem.getVarUpperBound(variable);

				double lb = (lowerBound != null ? lowerBound.doubleValue() : Double.NEGATIVE_INFINITY);
				double ub = (upperBound != null ? upperBound.doubleValue() : Double.POSITIVE_INFINITY);

				final IloNumVarType type;
				switch (varType) {
				case BOOL:
					type = IloNumVarType.Bool;
					break;
				case INT:
					type = IloNumVarType.Int;
					break;
				default: // REAL
					type = IloNumVarType.Float;
					break;
				}

				IloNumVar num = cplex.numVar(lb, ub, type);

				numToVar.put(num, variable);
				varToNum.put(variable, num);
			}

			for (Constraint constraint : problem.getConstraints()) {
				IloLinearNumExpr lin = cplex.linearNumExpr();
				Linear linear = constraint.getLhs();
				convert(linear, lin, varToNum);

				double rhs = constraint.getRhs().doubleValue();

				switch (constraint.getOperator()) {
				case LE:
					cplex.addLe(lin, rhs);
					break;
				case GE:
					cplex.addGe(lin, rhs);
					break;
				default: // EQ
					cplex.addEq(lin, rhs);
				}
			}

			if (problem.getObjective() != null) {
				IloLinearNumExpr lin = cplex.linearNumExpr();
				Linear objective = problem.getObjective();
				convert(objective, lin, varToNum);

				if (problem.getOptType() == OptType.MIN) {
					cplex.addMinimize(lin);
				} else {
					cplex.addMaximize(lin);
				}
			}

			cplex.solve();

			final Result result;
			if (problem.getObjective() != null) {
				double obj = cplex.getObjValue();
				result = new Result(obj);
			} else {
				result = new Result();
			}

			for (Entry<Object, IloNumVar> entry : varToNum.entrySet()) {
				Object variable = entry.getKey();
				IloNumVar num = entry.getValue();
				VarType varType = problem.getVarType(variable);

				double value = cplex.getValue(num);
				if (varType.isInt()) {
					int v = (int) Math.round(value);
					result.put(variable, v);
				} else {
					result.put(variable, value);
				}
			}

			cplex.end();

			return result;

		} catch (IloException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected void initWithParameters(IloCplex cplex) throws IloException {
		Object timeout = parameters.get(Solver.TIMEOUT);
		Object verbose = parameters.get(Solver.VERBOSE);

		if (timeout != null && timeout instanceof Number) {
			Number number = (Number) timeout;
			double value = number.doubleValue();
			cplex.setParam(DoubleParam.TiLim, value);
		}
		if (verbose != null && verbose instanceof Number) {
			Number number = (Number) verbose;
			int value = number.intValue();

			if (value == 0) {
				cplex.setOut(null);
			}
		}

	}

	protected void convert(Linear linear, IloLinearNumExpr lin, Map<Object, IloNumVar> varToNum) throws IloException {
		int size = linear.size();
		for (int i = 0; i < size; i++) {
			Number coeff = linear.getCoefficients().get(i);
			Object variable = linear.getVariables().get(i);

			IloNumVar num = varToNum.get(variable);

			lin.addTerm(coeff.doubleValue(), num);
		}

	}

}
