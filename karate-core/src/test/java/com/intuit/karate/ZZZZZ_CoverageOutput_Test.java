package com.intuit.karate;

import static com.intuit.karate.BranchCoverageInfo.branchFlags;
import static com.intuit.karate.BranchCoverageInfo.methodBranches;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;

class ZZZZZ_CoverageOutput_Test {
	@Test
	void testThatPasses() {
		assertTrue(true);
    }
	
	@AfterAll 
	static void coverageInfo() {
		int covered = 0;
		for (int j = 0; j < methodBranches[0]; j++) {
			if (branchFlags[0][j] == true) {
				covered++;
				System.out.println("Branch " + j + " reached");
			}
			else {
				System.out.println("Branch " + j + " not accessed");
			}
		}
		System.out.println("Coverage = " + 100.0*((double)covered / (double)methodBranches[0]) + " %");
	}
}