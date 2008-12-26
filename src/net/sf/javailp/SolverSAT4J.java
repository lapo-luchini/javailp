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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	protected static boolean print = false;

	/**
	 * The {@code Hook} for the {@code SolverSAT4J}.
	 * 
	 * @author lukasiewycz
	 * 
	 */
	public interface Hook {

		/**
		 * This method is called once before the optimization and allows to
		 * change some internal settings.
		 * 
		 * @param solver
		 *            the sat4j solver
		 * @param varToIndex
		 *            the map of variables to sat4j specific variables
		 */
		public void call(PBSolverResolution solver,
				Map<Object, Integer> varToIndex);
	}

	protected final Set<Hook> hooks = new HashSet<Hook>();

	/**
	 * Adds a hook.
	 * 
	 * @param hook
	 *            the hook to be added
	 */
	public void addHook(Hook hook) {
		hooks.add(hook);
	}

	/**
	 * Removes a hook
	 * 
	 * @param hook
	 *            the hook to be removed
	 */
	public void removeHook(Hook hook) {
		hooks.remove(hook);
	}

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

			PBSolverResolution solver = SolverFactory
					.newPBResMixedConstraintsObjective();
			solver.newVar(problem.getVariablesCount());

			// boolean isMax = (problem.getOptType() == OptType.MAX);

			if (problem.getObjective() != null) {

				Linear objective = problem.getObjective();

				VecInt vars = new VecInt();
				IVec<BigInteger> coeffs = new Vec<BigInteger>();

				for (Term term : objective) {
					Object variable = term.getVariable();
					Number coeff = term.getCoefficient();
					int index = varToIndex.get(variable);

					BigInteger integer = toBigInt(coeff);

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
					for (Term term : linear) {
						Object variable = term.getVariable();
						Number coeff = term.getCoefficient();
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

			for (Hook hook : hooks) {
				hook.call(solver, varToIndex);
			}
			
			initWithParameters(solver);

			Map<Object, Number> r = new HashMap<Object, Number>();
			Linear objective = problem.getObjective();

			try {
				while (solver.isSatisfiable()) {
					r.clear();
					for (Object variable : problem.variables) {
						int index = varToIndex.get(variable);
						r.put(variable, solver.model(index) ? 1 : 0);
					}
					if (objective == null) {
						break;
					}

					Number value = objective.calculate(r);

					if (print) {
						System.out.println("Found new solution: " + value);
					}

					VecInt vars = new VecInt();
					IVec<BigInteger> coeffs = new Vec<BigInteger>();

					for (Term term : objective) {
						Object variable = term.getVariable();
						Number coeff = term.getCoefficient();
						int index = varToIndex.get(variable);

						BigInteger integer = toBigInt(coeff);

						vars.push(index);
						coeffs.push(integer);
					}

					long rhs = value.longValue();

					boolean isMax = problem.getOptType() == OptType.MAX;
					if (isMax) {
						rhs++;
					} else {
						rhs--;
					}
					solver.addPseudoBoolean(vars, coeffs, isMax, toBigInt(rhs));
				}
			} catch (ContradictionException ex) {
			}

			if (r.isEmpty()) {
				return null;
			} else {
				Result result;

				if (objective == null) {
					result = new Result();
				} else {
					result = new Result(objective);
				}

				for (Object variable : problem.variables) {
					Number b = r.get(variable);
					result.put(variable, b);
				}

				return result;
			}

		} catch (ContradictionException ex) {
			System.err.println("ContradictionException.");
		} catch (TimeoutException ex) {
			System.err.println("TimeoutException.");
		}
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
				print = false;
				// do nothing
			} else if (value > 0) {
				print = true;
				solver.printStat(System.out, " ");
			}
		}

	}

	protected void check(Object variable, Problem problem) {
		VarType type = problem.getVarType(variable);
		if (type != VarType.BOOL) {
			throw new IllegalArgumentException(
					"Variable "
							+ variable
							+ " is not a binary variable. SAT4J can only solve 0-1 ILPs.");
		}
	}

	protected BigInteger toBigInt(Number number) {
		Long lvalue = number.longValue();
		Double dvalue = number.doubleValue();

		if (dvalue != Math.round(dvalue)) {
			throw new IllegalArgumentException(
					"SAT4J can only solve 0-1 ILPs (all coefficients have to be integer values). Found coefficient: "
							+ dvalue);
		}

		BigInteger big = BigInteger.valueOf(lvalue);
		return big;
	}

}
