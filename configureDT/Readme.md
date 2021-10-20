# Configure decision tables properties in Decision Center

This tool allows to set properties on all the Decision tables in a given decision service. Changes will be applied on all branches that can be modified. 

## Compiling

This solution is build using the dependencies from [this project](https://github.com/ODMDev/odm-libs-in-maven/blob/master/README.md) and an installation of ODM.

1. Clone the project from github  
1. Configure the properties in pom.xml to match your version and configuration
1. Run maven clean deploy


## Installing

- Download the [release](https://github.com/ODMDev/decision-center-api-samples/releases/tag/2.0.0)
- Add the jar configure-dt.jar in teamserver/lib folder

## Usage

- run 
java -cp "<odm_home>/teamserver/lib/*:<odm_home>/teamserver/lib/eclipse_plugins/*:" com.ibm.odm.tools.ConfigureDT

  java com.ibm.odm.tools.ConfigureDT -username username -password password -url url -dataSource dataSource -decisionService <dsName> [-overlapCheck true|false] [-gapCheck true|false] [-manualOrdering true|false] [-autoResize true|false]   
	Update all decision tables properties according to option selected in the decision service selected  
	If an option is not selected the property is not modified. At least one option must be selected  

### Arguments

-username        : an administrator account   
-password        : the user password  
-url             : decision center URL   
-dataSource      : the JNDI name of the datasource usually jdbc/ilogDataSource  
-decisionService : the decision service name  
-overlapCheck    : set false to disable overlap check on all DT and true to enable it. If this option is not defined the property is not modified  
-gapCheck        : set false to disable gap check on all DT and true to enable it. If this option is not defined the property is not modified  
-manualOrdering  : set true to use manual ordering on all DT and false for Automatic. If this option is not defined the property is not modified  
-autoResize      : set false to disable auto resize on all DT and true to enable it. If this option is not defined the property is not modified  
