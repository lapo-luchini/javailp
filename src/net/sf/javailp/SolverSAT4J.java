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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.sat4j.core.Vec;
import org.sat4j.core.VecInt;
import org.sat4j.pb.ObjectiveFunction;
import org.sat4j.pb.SolverFactory;
import org.sat4j.pb.core.PBSolverResolution;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVec;
import org.sat4j.specs.TimeoutException;

/**
 * The {@code SolverCPLEX} is the {@code Solver} SAT4J.
 * 
 * @author lukasiewycz
 * 
 */
public class SolverSAT4J extends AbstractSolver {

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.javailp.Solver#solve(net.sf.javailp.Problem)
	 */
	public Result solve(Problem problem) {

		try {

			Map<Integer, Object> indexToVar = new HashMap<Integer, Object>();
			Map<Object, Integer> varToIndex = new HashMap<Object, Integer>();

			int i = 1;
			for (Object variable : problem.getVariables()) {
				check(variable, problem);
				indexToVar.put(i, variable);
				varToIndex.put(variable, i);
				i++;
			}

			PBSolverResolution solver = SolverFactory.newPBResMixedConstraintsObjective();
			solver.newVar(problem.getVariablesCount());

			if (problem.getObjective() != null) {
				boolean isMax = (problem.getOptType() == OptType.MAX);
				Linear objective = problem.getObjective();

				VecInt vars = new VecInt();
				IVec<BigInteger> coeffs = new Vec<BigInteger>();

				for (int j = 0; j < objective.size(); j++) {
					Object variable = objective.getVariables().get(j);
					Number coeff = objective.getCoefficients().get(j);
					int index = varToIndex.get(variable);

					BigInteger integer = toBigInt(coeff);
					if (isMax) {
						integer = integer.negate();
					}

					vars.push(index);
					coeffs.push(integer);
				}

				ObjectiveFunction function = new ObjectiveFunction(vars, coeffs);
				solver.setObjectiveFunction(function);
			}

			{
				for (Constraint constraint : problem.getConstraints()) {
					Linear linear = constraint.getLhs();
					Operator operator = constraint.getOperator();
					BigInteger rhs = toBigInt(constraint.getRhs());

					VecInt vars = new VecInt();
					IVec<BigInteger> coeffs = new Vec<BigInteger>();
					for (int j = 0; j < linear.size(); j++) {
						Object variable = linear.getVariables().get(j);
						Number coeff = linear.getCoefficients().get(j);
						int index = varToIndex.get(variable);
						vars.push(index);
						coeffs.push(toBigInt(coeff));
					}

					if (operator == Operator.LE || operator == Operator.EQ) {
						solver.addPseudoBoolean(vars, coeffs, false, rhs);
					}
					if (operator == Operator.GE || operator == Operator.EQ) {
						solver.addPseudoBoolean(vars, coeffs, true, rhs);
					}
				}
			}

			{
				for (Object variable : problem.getVariables()) {
					int index = varToIndex.get(variable);

					Number lowerBound = problem.getVarLowerBound(variable);
					Number upperBound = problem.getVarUpperBound(variable);

					if (lowerBound != null && lowerBound.doubleValue() > 0) {
						VecInt vars = new VecInt();
						vars.push(index);
						solver.addAtLeast(vars, 1);
					}
					if (upperBound != null && upperBound.doubleValue() < 1) {
						VecInt vars = new VecInt();
						vars.push(index);
						solver.addAtMost(vars, 0);
					}
				}
			}

			if (solver.isSatisfiable()) {

				Map<Object, Boolean> r = new HashMap<Object, Boolean>();
				for (Object variable : problem.variables) {
					int index = varToIndex.get(variable);
					r.put(variable, solver.model(index));
				}

				Result result;
				if (problem.getObjective() != null) {
					Linear objective = problem.getObjective();
					double sum = 0;

					for (int j = 0; j < objective.size(); j++) {
						Object variable = objective.getVariables().get(j);
						Number coeff = objective.getCoefficients().get(j);

						if (r.get(variable)) {
							sum += coeff.doubleValue();
						}
					}

					result = new Result(sum);
				} else {
					result = new Result();
				}

				for (Object variable : problem.variables) {
					boolean b = r.get(variable);
					result.put(variable, b ? 1 : 0);

				}

				return result;

			} else {
				return null;
			}

		} catch (ContradictionException e) {
			System.err.println("ContradictionException.");
		} catch (TimeoutException e) {
			System.err.println("TimeoutException.");
		}
		// TODO Auto-generated method stub
		return null;
	}

	protected void initWithParameters(PBSolverResolution solver) {
		Object timeout = parameters.get(Solver.TIMEOUT);
		Object verbose = parameters.get(Solver.VERBOSE);

		if (timeout != null && timeout instanceof Number) {
			Number number = (Number) timeout;
			int value = number.intValue();
			solver.setTimeout(value);
		}
		if (verbose != null && verbose instanceof Number) {

			Number number = (Number) verbose;
			int value = number.intValue();
			if (value == 0) {
				// do nothing
			} else if (value > 0) {
				solver.printStat(System.out, "");
			}
		}

	}

	protected void check(Object variable, Problem problem) {
		VarType type = problem.getVarType(variable);
		if (type != VarType.BOOL) {
			throw new IllegalArgumentException("Variable " + variable + " is not a binary variable. SAT4J can only solve 0-1 ILPs.");
		}
	}

	protected BigInteger toBigInt(Number number) {
		Long lvalue = number.longValue();
		Double dvalue = number.doubleValue();

		if (dvalue != Math.round(dvalue)) {
			throw new IllegalArgumentException(
					"SAT4J can only solve 0-1 ILPs (all coefficients have to be integer values). Found coefficient: " + dvalue);
		}

		BigInteger big = BigInteger.valueOf(lvalue);
		return big;
	}

}
