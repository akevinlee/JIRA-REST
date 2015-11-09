import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

def failMode = FailMode.valueOf(props['failMode']);
def issueIds = props['issueIds'].split(',') as List;
def checkCount = 0;
for (def issueId : issueIds.sort()) {
    if (helper.issueExists(issueId)) {
        println "Found issue ${issueId}.";
        checkCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: issue ${issueId} not found.");
        } else {
            println "Could not find issue ${issueId}.";
        }
    }
}
def totalNum = issueIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!issueIds) {
        helper.exitFailure("No issues found.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!issueIds || (checkCount != totalNum)) {
        helper.exitFailure("Only found ${checkCount} out of ${totalNum} issues.");
    }
}
println "Found ${checkCount} out of ${totalNum} issues.";

System.exit(0);