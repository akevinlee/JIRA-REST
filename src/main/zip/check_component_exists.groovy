import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);
final def projectKey = props['projectKey'];

def failMode = FailMode.valueOf(props['failMode']);
def componentIds = props['componentIds'].split(',') as List;
def checkCount = 0;
for (def componentId : componentIds.sort()) {
    if (helper.componentExists(projectKey, componentId)) {
        println "Found component ${componentId}.";
        checkCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: component ${componentId} for project ${projectKey} not found.");
        } else {
            println "Could not find component ${componentId} for project ${projectKey}.";
        }
    }
}
def totalNum = componentIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!componentIds) {
        helper.exitFailure("No components found.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!componentIds || (checkCount != totalNum)) {
        helper.exitFailure("Only found ${checkCount} out of ${totalNum} components.");
    }
}
println "Found ${checkCount} out of ${totalNum} components.";

System.exit(0);