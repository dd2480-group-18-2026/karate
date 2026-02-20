# Report for assignment 3

This is a template for your report. You are free to modify it as needed.
It is not required to use markdown for your report either, but the report
has to be delivered in a standard, cross-platform format.

## 1) Project

Name: karatelabs/karate

URL: https://github.com/karatelabs/karate

Karate is a framework used for test automation. It combines API testing, mocking, performance testing and UI testing in one tool.

## 2) Onboarding experience

1. How easily can you build the project? Briefly describe if everything worked as documented or not:

   a. Did you have to install a lot of additional tools to build the software?
      
      The only additional tools I needed to build the software was an IDE, JDK, Maven and Git. 

   b. Were those tools well documented?
      
      Yes, The documentation clearly stated how the additional tools were to be installed, these are well documented tools and have clear and thorough documentation. Though they did not give a link to the official documentation for the tools.

   c. Were other components installed automatically by the build script?

      Yes, the components were installed automatically by Maven during the build process

   d. Did the build conclude automatically without errors?

      Building was succefull but there were scenario/assertion failures during Gatling simulations but these were intentional in the demo feature files.

   e. How well do examples and tests run on your system(s)?

      JUnit tests passed 6/6 but Gatling example simulations produced KO requests, but these were intentional after looking through the tests and coumentation. Gatlin also generated HTML performance reports

2. Do you plan to continue or choose another project?

   We plan on continuing with this project

## 3) Complexity

As we only have four members in the group, we only examined four functions in the code. We focused on functions in the karate-core project, mostly because it is the part that hosts the core logic of the app. We felt it was more important to have this part correctly implemented and covered.

We used [lizard](https://github.com/terryyin/lizard) as the tool to count both complexity and the number of lines of code (LOC).

### 1. RequestHandler -> handle

This function is located [here](karate-core/src/main/java/com/intuit/karate/http/RequestHandler.java#L58).

The `handle` method of `RequestHandler` is supposed to:
- normalize the request path
- ensure resource type is set on the request
- return early with static resource if that is requested
- handle multiple session scenarios (higher up tried first):
   1. session exists
      1. already loaded
      2. not loaded and then retreive from cache if possible
   2. no session exists
      1. use global session if allowed
      2. create session automatically if allowed
      3. create temporary session if authentication request
      4. redirect to authentication if none of the above are possible
- Actually handle the request (done outside of this method)

For the complexity, lizard gave us 22 with 68 LOC. By our own count, we found a complexity of 18.

This method has three `return` statements which were all included in the complexity calculations. We think this contributes to the difference in complexity score.

The method is not documented at all. 

### 2. ScenarioEngine -> match

This function is located [here](karate-core/src/main/java/com/intuit/karate/core/ScenarioEngine.java#L1769).

The `match` method of `ScenarioEngine` is supposed to evaluate if there is a match between the left-hand side (LHS) and right-hand side (RHS).
This is done by:
1. Resolving the LHS
2. Early return in header case  
3. Resolve the actual value  
4. Applying path to retreive the value from te expression if necessary
5. Evaluate the expected value
6. Perform match check by applying the specific type check

For the complexity, lizard gave us 21 with 46 LOC. By our own count, we found a complexity of 20.

There are two `return` statements in the method and both were included in the manual count. We think this the reason
for why our complexity score is one less than that of lizard.

There is a long comment about the previous state of the method and how that relates to how it is today. Otherwise there is no documentation.

This method could definately be made more readable and the complexity score could be significantly lowered through a refactor. However, as it is the brains behind the match operation in Karate, it is no surprise that it is not just a straight forward method. Luckily it is not too long so it is still quite easy to get an overview of how it works.

### 3. HttpRequestBuilder -> buildInternal

This function is located [here](karate-core/src/main/java/com/intuit/karate/http/HttpRequestBuilder.java#L164).

The `buildInternal` method of `HttpRequestBuilder` is supposed to build a ready to send request. To do that, it must:
- Handle fallback when URL or method parameter is not set
- Handle multipart request construction and special GET + multipart cases
- Convert multipart form fields into query parameters when needed
- Build and attach multipart bodies and boundaries
- Handle cookies and attach them as headers
- Generate content-type headers dependant on the type of request, including the charset

For the complexity, lizard gave us 26 with 67 LOC. By our own count, we found a complexity of 25. We got almost the same result, which can be explained by the way lizard counts complexity.

This function is not very complex, but it needs to deal with a lot of edge cases, hence the complexity.

For the complexity measurements, we included the exception in the count, which could be the reason why our count is slightly off from lizard's.

This function is neither commented nor documented.

### 4. MatchOperator -> execute

This function is located [here](karate-core/src/main/java/com/intuit/karate/MatchOperator.java#L111).

The `execute()` method performs a match operation between actual and expected values by:
- Handling missing actual values with macro-aware tolerance
- Resolving type mismatches with special handling for:
   - Contains-family coercion
   - XML-to-Map conversion
   - Macro-based type flexibility
- Evaluating macro expressions before structural comparison
- Dispatching to equality or contains-family comparison logic
- Returning structured pass/fail results with appropriate error messages

For the complexity, lizard gave us 22 with 44 LOC. By our own count, we found a complexity of 13. This discrepancy is probably due to lizard not counting the return points correctly. 

The function itself is pretty complex, even though most of the complexity of the operation is already extracted in other functions. Its complexity comes primarily from its role as the control center of the macro execution, where it needs to make sure that the data is valid and parse it a little bit to start execution.

For the complexity measurements, we included the exception in the count. However, this function also has a lot of failure states that are returned with normal `return`s.

This function is neither commented nor documented.

## 4) Refactoring

### 1. RequestHandler -> handle

- **Extract path adjustment**: Move the logic that adjusts `request.getPath()` into a `adjustPath(Request)` method.

- **Extract resource type initialization**: Move `request.setResourceType(...)` logic to `ensureResourceType(Request)`.

- **Extract static resource handling**: Create `tryHandleStatic(Request, ServerContext)` that returns a `Response` if the request matches static resource conditions.

- **Extract session resolution**: Move all session-loading, expiration-checking, and auto-creation logic into `resolveSessionIfRequired(Request, ServerContext)` and helper methods:
   - `resolveSession()`
      - `loadValidSession()`
      - `isAuthFlow()`
   - `redirectToSignIn`

This will significantly lower the complexity and make it more obvious what the intent of the `handle` method is. It can however obfuscate what actually is going on.

### 2. ScenarioEngine -> match

- **Extract LHS parsing**: Move all `name`/`path` adjustment logic into something like `parseLhs(...)`.

- **Break up and extract complex boolean conditions**:
   - `shouldEvaluateAsPath(...)`
   - `isSimpleReference(...)`
   - `isStructurePath(...)`
   - `shouldFallbackToFullExpression(...)`

- **Extract LHS evaluation**  
  Extract the logic that decides how to evaluate the LHS (JS vs JsonPath/XPath) into a dedicated method like `resolveActual(...)`.

- **Extract path application**: Move JSON/XPath branching into something like `applyPathIfNeeded(...)`.

  The structure of `match(...)` will be:
  1. Resolve LHS  
  2. Early return in header case  
  3. Resolve actual value  
  4. Apply path  
  5. Evaluate expected  
  6. Perform match

  Complexity will be lowered by the refactoring and by having the boolean statements in their own methods makes intent a lot clearer.


### 3. HttpRequestBuilder -> buildInternal

The responsabilities of the function outline a possible path for refactoring:
- **Extract all the fallbacks into helper methods**: All the code blocks that are below if statements checking whether or not a variable is null could be made to be separate methods to reduce unneeded complexity.

- **Extract cookie handling** : Cookie handling could be its own method, as it is functionnality that could be needed in other parts of the code later on. It could be called `addCookieToHeader` for example.

- **Extract both content-types blocks into their own methods**: The function currently has two blocks that can build content-type in different ways. These are moderately complex, so extracting them makes sense and would reduce complexity while improving readability.

- **Reorganize the function to have a single if else-if else statement for multipart and body**: Currently, the function checks whether multiPart and body are null twice. The checks are incompatible with each other, so they could be put in an if else-if branch to make it faster and more readable.

The new structure of `buildInternal` would be :
1. urlFallback()
2. methodFallback()
3. parts building stays as is
4. addCookiesToHeader()
5. ```Java
   if (multiPart != null && body == null) buildMultiPartContentType()
   else if (multiPart == null && body != null) buildSinglepartContentType()
   ```

### 4. MatchOperator(CoreOperator) -> execute

This method is the entry point for comparing two values. It should:
- Check if the actual value exists
- Handles type mismatches
- If the expected value is a special #... macro, it delegates to the macro logic
- Otherwise it performs either an equality match or a contains-style match
- returns pass or fail with the correct message

Some complexity is necessary beacuse it supports multiple match modes, macros and a few special type cases.
But most of the current complexity comes from doing all of it in one method, so it can be reduced by splitting the logic into small helpers.

This refactoring will reduce complexity and make execute() easier to read.
The only drawback is that the logic is split into more helper methods.

- **Extract Presence Check**: 

Move:
- Checks if the actual value is missing  
- Verifies that the expected value is not a macro  
- Fails with `"actual path does not exist"`  
Into:
```java
private boolean validateActualPresence(MatchOperation operation)
```

- **Extract Type Handling** 
Move:
- Wraps values for contains-family  
- Converts XML to Map in a special case  
- Allows macro strings to bypass mismatch  
- Otherwise fails  
Into: 
```java
private MatchOperation reconcileTypesIfNeeded(MatchOperation operation)
```

- **Extract Macro Handling**
Move:
- If expected is a string  
- If it starts with `#`  
- Then calls macro logic  
Into
```java
private Boolean tryMacroMatch(MatchOperation operation)
```
Return values:
- `null` → not a macro  
- `true` / `false` → macro handled it  

- **Extract Operator Logic**
Move:
- equals  
- contains-family  
Into:
```java
private boolean applyOperator(MatchOperation operation)
```
 
The new structure of execute() would be:
1. Check if the actual value exists (return early if it fails)
2. Fix type differences if needed (return early if it fails)
3. Try macro handling (return immidiately if it was handled)
4. Apply the operator (equals or conatins)

## 5) Coverage

### Tools

Document your experience in using a "new"/different coverage tool.

How well was the tool documented? Was it possible/easy/difficult to
integrate it with your build environment?

### Your own coverage tool

Show a patch (or link to a branch) that shows the instrumented code to
gather coverage measurements.

The patch is probably too long to be copied here, so please add
the git command that is used to obtain the patch instead:

git diff ...

What kinds of constructs does your tool support, and how accurate is
its output?

### Evaluation

1. How detailed is your coverage measurement?

2. What are the limitations of your own tool?

3. Are the results of your tool consistent with existing coverage tools?

## 6) Coverage improvement

Show the comments that describe the requirements for the coverage.

Report of old coverage: [link]

Report of new coverage: [link]

Test cases added:

git diff ...

Number of test cases added: two per team member (P) or at least four (P+).

### 1. RequestHandler -> handle

![Coverage of RequestHandler handle before new tests](/report_resources/RequestHandler_handle_before.png)

![Coverage of RequestHandler handle after new tests](/report_resources/RequestHandler_handle_after.png)

As can be seen in the two screenshots of the coverage reports for `RequestHandler`, the branch coverage for the `handle` method increased from 42 % to 64 percent after two new tests were added.

Added tests: `git diff master issue-18 karate-core/src/test/java/com/intuit/karate/http/RequestHandlerTest.java`

### 3. HttpRequestBuilder -> buildInternal

![Coverage of HttpRequestBuilder buildInternal before new tests](/report_resources/HttpRequestBuilder_buildInternal_before.png)

![Coverage of HttpRequestBuilder buildInternal after new tests](/report_resources/HttpRequestBuilder_buildInternal_after.png)

As can be seen in the two screenshots of the coverage reports for `HttpRequestBuilder`, the branch coverage for the `buildInternal` method increased from 70 % to 88 % after four new tests were added. The instruction coverage also jumped from 78 % to 98 %.

Added tests: `git diff master issue-18 karate-core/src/test/java/com/intuit/karate/http/HttpRequestBuilderTest.java`

### 4 ScenarioEngine -> stop

![Coverage of ScenarioEngine stop before new tests](/report_resources/ScenarioEngine_stop_before.png)

![Coverage of ScenarioEngine stop after new tests](/report_resources/ScenarioEngine_stop_after.png)

As can be seen in the screenshots the coverage reports for `ScenarioEngine`, the branch coverage of `stop` increased from 58% to 83% after the two new tests were added.

Added tests: `git diff master issue-18 karate-core/src/test/java/com/intuit/karate/core/ScenarioEngineStopTest.java`

## 7) Self-assessment: Way of working

For this lab we consider ourselves to be in the  In Place. We started with a meeting where we discussed how to approach the assignment and which tools to use. We also looked at the grading criteria together to make sure we had the same expectations. We did notice however that some responsibilities were unclear after that meeting. That was however quickly remedied over Discord. We are following our established way of working to a high degree, but it is still something we have to actively think about and constantly check that we follow. We have therefore not reached the Working well stage yet. To reach the next stage, we simply need to get more practice with the established methods to make them come more naturally.

## 8) Overall experience

What are your main take-aways from this project? What did you learn?

Is there something special you want to mention here?

## 9) P+ Contributions

**Felix:**
- I refactored `RequestHandler#handle`. The new complexity score for the method is 1. The maxinum complexity of the new methods is 5. This is a 72.2 % decrease.
- To see the changes run: `git diff master issue-44 karate-core/src/main/java/com/intuit/karate/http/RequestHandler.java`

**Eliott:**
- I refactored `HttpRequestBuilder -> buildInternal`. The new complexity score is 8, and the maximum complexity of the new methods is 9. This is a 64% decrease.
- To see the changes run: `git diff master issue-44 karate-core/src/main/java/com/intuit/karate/http/HttpRequestBuilder.java`
