# Report for assignment 3

This is a template for your report. You are free to modify it as needed.
It is not required to use markdown for your report either, but the report
has to be delivered in a standard, cross-platform format.

## Project

Name: karatelabs/karate

URL: https://github.com/karatelabs/karate

Karate is a framework used for test automation. It combines API testing, mocking, performance testing and UI testing in one tool.

## Onboarding experience

1. How easily can you build the project? Briefly describe if everything worked as documented or not:
(a) Did you have to install a lot of additional tools to build the software?
The only additional tools I needed to build the software was an IDE, JDK, Maven and Git. 
(b) Were those tools well documented?
Yes, The documentation clearly stated how the additional tools were to be installed, these are well documented tools and have clear and thorough documentation. Though they did not give a link to the official documentation for the tools.
(c) Were other components installed automatically by the build script?
Yes, the components were installed automatically by Maven during the build process
(d) Did the build conclude automatically without errors?
Building was succefull but there were scenario/assertion failures during Gatling simulations but these were intentional in the demo feature files.
(e) How well do examples and tests run on your system(s)?
JUnit tests passed 6/6 but Gatling example simulations produced KO requests, but these were intentional after looking through the tests and coumentation. Gatlin also generated HTML performance reports
2. Do you plan to continue or choose another project?
we plan on continuing with this project


## Complexity

1. What are your results for five complex functions?
   * Did all methods (tools vs. manual count) get the same result?
   * Are the results clear?
2. Are the functions just complex, or also long?
3. What is the purpose of the functions?
4. Are exceptions taken into account in the given measurements?
5. Is the documentation clear w.r.t. all the possible outcomes?

## Refactoring

Plan for refactoring complex code:

Estimated impact of refactoring (lower CC, but other drawbacks?).

Carried out refactoring (optional, P+):

git diff ...

### RequestHandler#handle

- **Extract path adjustment**: Move the logic that adjusts `request.getPath()` into a `adjustPath(Request)` method.

- **Extract resource type initialization**: Move `request.setResourceType(...)` logic to `ensureResourceType(Request)`.

- **Extract static resource handling**: Create `tryHandleStatic(Request, ServerContext)` that returns a `Response` if the request matches static resource conditions.

- **Extract session resolution**: Move all session-loading, expiration-checking, and auto-creation logic into `resolveSessionIfRequired(Request, ServerContext)` and helper methods:
   - `resolveSession()`
      - `loadValidSession()`
      - `isAuthFlow()`
   - `redirectToSignIn`

This will significantly lower the complexity and make it more obvious what the intent of the `handle` method is. It can however obfuscate what actually is going on.

### ScenarioEngine#match

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

## Coverage

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

## Coverage improvement

Show the comments that describe the requirements for the coverage.

Report of old coverage: [link]

Report of new coverage: [link]

Test cases added:

git diff ...

Number of test cases added: two per team member (P) or at least four (P+).

### RequestHandler#handle

#### Requirements
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

#### Coverage Improvement

![Coverage of RequestHandler handle before new tests](/report_resources/RequestHandler_handle_after.png)

![Coverage of RequestHandler handle before new tests]("./report_resources/RequestHandler_handle_after.png")

As can be seen in the two screenshots of the coverage reports for `RequestHandler`, the branch coverage for the `handle` method
increased from 42 % to 64 percent after two new tests were added.

## Self-assessment: Way of working

Current state according to the Essence standard: ...

Was the self-assessment unanimous? Any doubts about certain items?

How have you improved so far?

Where is potential for improvement?

## Overall experience

What are your main take-aways from this project? What did you learn?

Is there something special you want to mention here?