import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

def failMode = FailMode.valueOf(props['failMode']);
def projectIds = props['projectIds'].split(',') as List;
def checkCount = 0;
for (def projectId : projectIds.sort()) {
    if (helper.projectExists(projectId)) {
        println "Found project ${projectId}.";
        checkCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: project ${projectId} not found.");
        } else {
            println "Could not find project ${projectId}.";
        }
    }
}
def totalNum = projectIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!projectIds) {
        helper.exitFailure("No projects found.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!projectIds || (checkCount != totalNum)) {
        helper.exitFailure("Only found ${checkCount} out of ${totalNum} projects.");
    }
}
println "Found ${checkCount} out of ${totalNum} projects.";

System.exit(0);