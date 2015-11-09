import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);
final def projectKey = props['projectKey'];

def failMode = FailMode.valueOf(props['failMode']);
def versionIds = props['versionIds'].split(',') as List;
def checkCount = 0;
for (def versionId : versionIds.sort()) {
    if (helper.versionExists(projectKey, versionId)) {
        println "Found version ${versionId}.";
        checkCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: version ${versionId} for project ${projectKey} not found.");
        } else {
            println "Could not find version ${versionId} for project ${projectKey}.";
        }
    }
}
def totalNum = versionIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!versionIds) {
        helper.exitFailure("No versions found.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!versionIds || (checkCount != totalNum)) {
        helper.exitFailure("Only found ${checkCount} out of ${totalNum} versions.");
    }
}
println "Found ${checkCount} out of ${totalNum} versions.";

System.exit(0);