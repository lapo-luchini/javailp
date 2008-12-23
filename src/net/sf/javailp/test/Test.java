package net.sf.javailp.test;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryLpSolve;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SolverFactory factory = new SolverFactoryLpSolve();
		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 100);

		/**
		 * Constructing a Problem: 
		 * Maximize: 143x+60y 
		 * Subject to: 
		 * 120x+210y <= 15000 
		 * 110x+30y <= 4000 
		 * x+y <= 75
		 * 
		 * With x,y being integers
		 * 
		 */
		Problem problem = new Problem();

		Linear linear = new Linear();
		linear.add(143, "x");
		linear.add(60, "y");

		problem.setObjective(linear, OptType.MAX);

		linear = new Linear();
		linear.add(120, "x");
		linear.add(210, "y");

		problem.add(linear, "<=", 15000);

		linear = new Linear();
		linear.add(110, "x");
		linear.add(30, "y");

		problem.add(linear, "<=", 4000);

		linear = new Linear();
		linear.add(1, "x");
		linear.add(1, "y");

		problem.add(linear, "<=", 75);

		problem.setVarType("x", Integer.class);
		problem.setVarType("y", Integer.class);

		Solver solver = factory.get();
		Result result = solver.solve(problem);

		System.out.println(result);

		/**
		 * Extend the problem with x <= 16 and solve it again
		 */
		problem.setVarUpperBound("x", 16);

		solver = factory.get();
		result = solver.solve(problem);

		System.out.println(result);

	}

}
