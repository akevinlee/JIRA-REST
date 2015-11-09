import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

def issueId = props['issueId'];
def failMode = FailMode.valueOf(props['failMode']);

if (helper.issueExists(issueId)) {
    def statusName = helper.issueStatus(issueId);
    def projectKey = helper.getProjectKeyForIssue(issueId);
    println "Issue ${issueId} has status ${statusName}.";
    apTool.setOutputProperty("issueStatus", statusName);
    apTool.setOutputProperty("issueProject", projectKey);
    apTool.setOutputProperties();
} else {
    if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
        helper.exitFailure("Error: issue ${issueId} not found.");
    } else {
        println "Could not find issue ${issueId}.";
    }
}

System.exit(0);