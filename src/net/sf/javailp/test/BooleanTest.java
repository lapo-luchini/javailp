package net.sf.javailp.test;

import java.util.Random;

import junit.framework.Assert;
import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
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

public class BooleanTest {

	@Test
	public void testCPLEXMin() {
		testMin(new SolverFactoryCPLEX());
	}

	@Test
	public void testCPLEXMax() {
		testMax(new SolverFactoryCPLEX());
	}

	public void testGLPKMin() {
		testMin(new SolverFactoryGLPK());
	}

	public void testGLPKMax() {
		testMax(new SolverFactoryGLPK());
	}

	@Test
	public void testLpSolveMin() {
		testMin(new SolverFactoryLpSolve());
	}

	@Test
	public void testLpSolveMax() {
		testMax(new SolverFactoryLpSolve());
	}

	@Test
	public void testMosekMin() {
		testMin(new SolverFactoryMosek());
	}

	@Test
	public void testMosekMax() {
		testMax(new SolverFactoryMosek());
	}

	@Test
	public void testSAT4JMin() {
		testMin(new SolverFactorySAT4J());
	}

	@Test
	public void testSAT4JMax() {
		testMax(new SolverFactorySAT4J());
	}

	protected void testMin(SolverFactory factory) {

		Problem problem = getProblem(8, 0);
		problem.setOptimizationType(OptType.MIN);
		Solver solver = factory.get();
		solver.setParameter(Solver.VERBOSE, 0);

		Result result = solver.solve(problem);

		Assert.assertEquals(result.getObjective().intValue(), 219);
	}

	protected void testMax(SolverFactory factory) {

		Problem problem = getProblem(8, 0);
		problem.setOptimizationType(OptType.MAX);
		Solver solver = factory.get();
		solver.setParameter(Solver.VERBOSE, 0);

		Result result = solver.solve(problem);

		Assert.assertEquals(result.getObjective().intValue(), 537);
	}

	protected Problem getProblem(int size, int seed) {
		Problem problem = new Problem();

		Random random = new Random(seed);

		Linear objective = new Linear();
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				int var = size * i + j;
				problem.setVarType(var, Boolean.class);
				objective.add(random.nextInt(100), var);
			}
		}
		problem.setObjective(objective);

		for (int i = 0; i < size; i++) {
			Linear l1 = new Linear();
			Linear l2 = new Linear();
			for (int j = 0; j < size; j++) {
				l1.add(1, i * size + j);
				l2.add(1, j * size + i);
			}

			problem.add(l1, "=", 1);
			problem.add(l2, "=", 1);
		}

		for (int k = -size + 1; k < size; k++) {
			// diagonal 1
			Linear linear = new Linear();
			for (int j = 0; j < size; j++) {
				int i = k + j;
				if (0 <= i && i < size) {
					linear.add(1, i * size + j);
				}
			}
			problem.add(linear, "<=", 1);
		}

		for (int k = 0; k < 2 * size - 1; k++) {
			// diagonal 2
			Linear linear = new Linear();
			for (int j = 0; j < size; j++) {
				int i = k - j;
				if (0 <= i && i < size) {
					linear.add(1, i * size + j);
				}
			}
			problem.add(linear, "<=", 1);
		}
		return problem;
	}

}
