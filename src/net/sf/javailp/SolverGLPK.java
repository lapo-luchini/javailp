package net.sf.javailp;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.gnu.glpk.GlpkSolver;

/**
 * The {@code SolverCPLEX} is the {@code Solver} GLPK.
 * 
 * @author lukasiewycz
 * 
 */
public class SolverGLPK extends AbstractSolver {

	/**
	 * The {@code Hook} for the {@code SolverGLPK}.
	 * 
	 * @author lukasiewycz
	 * 
	 */
	public interface Hook {

		/**
		 * This method is called once before the optimization and allows to
		 * change some internal settings.
		 * 
		 * @param glpk
		 *            the glpk solver
		 * @param varToIndex
		 *            the map of variables to glpk specific variables
		 */
		public void call(GlpkSolver glpk, Map<Object, Integer> varToIndex);
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

		Map<Integer, Object> indexToVar = new HashMap<Integer, Object>();
		Map<Object, Integer> varToIndex = new HashMap<Object, Integer>();

		int i = 1;
		for (Object variable : problem.getVariables()) {
			indexToVar.put(i, variable);
			varToIndex.put(variable, i);
			i++;
		}

		int ncon = problem.getConstraintsCount();
		int nvar = problem.getVariablesCount();

		GlpkSolver solver = new GlpkSolver();

		solver.setObjDir(GlpkSolver.LPX_MAX);
		solver.setClss(GlpkSolver.LPX_MIP);

		{
			solver.addCols(nvar);

			for (Object variable : problem.getVariables()) {
				int index = varToIndex.get(variable);

				VarType varType = problem.getVarType(variable);
				Number lowerBound = problem.getVarLowerBound(variable);
				Number upperBound = problem.getVarUpperBound(variable);

				int type = GlpkSolver.LPX_CV;

				switch (varType) {
				case BOOL:
				case INT:
					type = GlpkSolver.LPX_IV;
					break;
				default: // REAL
					type = GlpkSolver.LPX_CV;
				}

				Double lb = null;
				Double ub = null;

				if (varType == VarType.BOOL) {
					lb = 0.0;
					ub = 1.0;
					if (lowerBound != null && lowerBound.doubleValue() > 0) {
						lb = 1.0;
					}
					if (upperBound != null && upperBound.doubleValue() < 1) {
						ub = 0.0;
					}
				} else {
					if (lowerBound != null) {
						lb = lowerBound.doubleValue();
					}
					if (upperBound != null) {
						ub = upperBound.doubleValue();
					}
				}

				int bounds = GlpkSolver.LPX_FR;
				if (lb != null && ub != null) {
					bounds = GlpkSolver.LPX_DB;
				} else if (lb != null) {
					bounds = GlpkSolver.LPX_LO;
				} else if (ub != null) {
					bounds = GlpkSolver.LPX_UP;
				}

				if (lb == null) {
					lb = 0.0;
				}
				if (ub == null) {
					ub = 0.0;
				}

				solver.setColName(index, variable.toString());
				solver.setColKind(index, type);
				solver.setColBnds(index, bounds, lb, ub);
			}

		}

		if (problem.getObjective() != null) {
			Linear objective = problem.getObjective();

			for (Term term : objective) {
				Object variable = term.getVariable();
				int index = varToIndex.get(variable);
				double coeff = term.getCoefficient().doubleValue();

				solver.setObjCoef(index, coeff);
			}

			if (problem.getOptType() == OptType.MAX) {
				solver.setObjDir(GlpkSolver.LPX_MAX);
			} else {
				solver.setObjDir(GlpkSolver.LPX_MIN);
			}

		}

		{
			solver.addRows(ncon);

			int k = 1;
			for (Constraint constraint : problem.getConstraints()) {
				Linear linear = constraint.getLhs();
				double rhs = constraint.getRhs().doubleValue();

				int size = linear.size();

				int[] vars = new int[size + 1];
				double[] coeffs = new double[size + 1];

				int j = 1;
				for(Term term: linear){
					Object variable = term.getVariable();
					int index = varToIndex.get(variable);
					double coeff = term.getCoefficient().doubleValue();

					vars[j] = index;
					coeffs[j] = coeff;
					j++;
				}

				solver.setMatRow(k, size, vars, coeffs);

				final int comp;
				switch (constraint.getOperator()) {
				case LE:
					comp = GlpkSolver.LPX_UP;
					break;
				case GE:
					comp = GlpkSolver.LPX_LO;
					break;
				default: // EQ
					comp = GlpkSolver.LPX_FX;
				}

				solver.setRowBnds(k, comp, rhs, rhs);

				k++;
			}
		}
		Object timeout = parameters.get(Solver.TIMEOUT);
		Object verbose = parameters.get(Solver.VERBOSE);

		if (verbose != null && verbose instanceof Number) {
			Number number = (Number) verbose;
			int value = number.intValue();
			if (value == 0) {
				solver.enablePrints(false);
			} else if (value > 0) {
				solver.enablePrints(true);
			}
		}
		if (timeout != null) {
			System.err.println("Cannot set TIMEOUT parameter for Glpk.");
		}

		for (Hook hook : hooks) {
			hook.call(solver, varToIndex);
		}

		solver.simplex();
		solver.integer();

		Result result;
		if (problem.getObjective() != null) {
			double obj = solver.mipObjVal();
			result = new Result(obj);
		} else {
			result = new Result();
		}

		// System.out.println(solver.getStatus() == GlpkSolver.LPX_OPT);

		for (Object variable : problem.getVariables()) {
			int index = varToIndex.get(variable);

			double value = solver.mipColVal(index);

			if (problem.getVarType(variable).isInt()) {
				int v = (int) Math.round(value);
				result.put(variable, v);
			} else {
				result.put(variable, value);
			}
		}

		return result;
	}

}
