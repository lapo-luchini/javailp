package net.sf.javailp.test;

import junit.framework.Assert;
import net.sf.javailp.Linear;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryCPLEX;
import net.sf.javailp.SolverFactoryGLPK;
import net.sf.javailp.SolverFactoryLpSolve;
import net.sf.javailp.SolverFactoryMosek;
import net.sf.javailp.SolverFactorySAT4J;

import org.junit.Test;

public class UnsatTest {

	@Test
	public void testSAT4J() {
		testUnsat(new SolverFactorySAT4J());
	}

	@Test
	public void testCPLEX() {
		testUnsat(new SolverFactoryCPLEX());
	}

	@Test
	public void testLpSolve() {
		testUnsat(new SolverFactoryLpSolve());
	}

	@Test
	public void testMosek() {
		testUnsat(new SolverFactoryMosek());
	}
	
	@Test
	public void testGLPK() {
		testUnsat(new SolverFactoryGLPK());
	}

	protected void testUnsat(SolverFactory factory) {

		Problem problem = getProblem();
		Solver solver = factory.get();
		solver.setParameter(Solver.VERBOSE, 0);

		Result result = solver.solve(problem);

		Assert.assertEquals(result, null);
	}

	protected Problem getProblem() {

		Problem problem = new Problem();

		Linear linear = new Linear();

		for (int i = 0; i < 10; i++) {
			linear.add(1, i);
			problem.setVarType(i, Boolean.class);
		}

		problem.add(linear, "=", 5);
		problem.add(linear, "=", 6);

		problem.setObjective(linear);

		return problem;
	}

}
