package com.intuit.karate;

public class BranchCoverageInfo {
	public static boolean[][] branchFlags = new boolean[4][100];

	final public static int[] methodBranches = {31, 44, 18, 31};

	final public static String[] names = {"RequestHandler:handle", "HttpRequestBuilder:buildInternal", "ScenarioEngine:match", "MatchOperator:CoreOperator:execute"};
}
