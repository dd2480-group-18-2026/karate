package com.intuit.karate;

import static com.intuit.karate.BranchCoverageInfo.branchFlags;
import static com.intuit.karate.BranchCoverageInfo.methodBranches;
import static com.intuit.karate.BranchCoverageInfo.names;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;

class ZZZZZ_CoverageOutput_Test {
	@Test
	void testThatPasses() {
		assertTrue(true);
    }
	
	@AfterAll 
	/*
	Print all recorded coverage info 
	 */
	static void coverageInfo() {
		for (int i = 0; i < 100; i++) {
			System.out.println("");
		}
		int totalCovered = 0;
		double[] methodCoveragePercentage = new double[4];
		for (int i = 0; i < 4; i++) {
			int covered = 0;
			System.out.println("Method: " + names[i]);
			for (int j = 0; j < methodBranches[i]; j++) {
				if (branchFlags[i][j] == true) {
					covered++;
					System.out.println("Branch " + j + " reached");
				}
				else {
					System.out.println("Branch " + j + " not accessed");
				}
			}
			methodCoveragePercentage[i] = 100.0*((double)covered / (double)methodBranches[i]);
			totalCovered += covered;
		}
		int totalBranches = methodBranches[0] + methodBranches[1] + methodBranches[2] + methodBranches[3];
		
		for (int i = 0; i < 4; i++) {
			System.out.println("Coverage for method " + names[i] + " = " + methodCoveragePercentage[i] + " %");
		}
		System.out.println("Total coverage = " + 100.0*((double)totalCovered / (double)totalBranches) + " %");
	}
}