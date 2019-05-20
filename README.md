# log4jadder
Simple Project used to add logging to a project

Add change to java source file to add logging to methods:
Changes are as below
 - Add logging to all public methods
 - Add debug logging to all methods at entry
 - Log all input parameters
 - Add debug logging to all methods at exit
 - Log exit and any return parameter
 - Encapsulate the log with ifDebugEnabled check
 - Add a logger if not exists

The basic goal of the project is to speed up the tasks of logging without AOP.
This is just a simple way to speed up a very boring part of coding.

TODO:
 - Add code to standardize all logging instances
 - Add code to enable annotation based logging
 
