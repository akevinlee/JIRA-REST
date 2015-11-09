import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

def statusName = props['statusName'];
def failMode = FailMode.valueOf(props['failMode']);
def issueIds = props['issueIds'].split(',') as List;
def statusCount = 0;
for (def issueId : issueIds.sort()) {
    if (helper.issueExists(issueId)) {
        def actualStatus = helper.issueStatus(issueId);
        if (actualStatus == statusName) {
            println "Found ${issueId} with the correct status: ${statusName}.";
            statusCount++;
        } else {
            if (failMode == FailMode.FAIL_FAST) {
                helper.exitFailure("Issue ${issueId} has an different status: ${actualStatus}.");
            } else {
                println "Issue ${issueId} has an different status: ${actualStatus}.";
            }
        }
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
        helper.exitFailure("No issues found to check.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!issueIds || (statusCount != totalNum)) {
        helper.exitFailure("Only found correct status on ${statusCount} out of ${totalNum} issues.");
    }
}
println "Found the correct status on ${statusCount} out of ${totalNum} issues.";

System.exit(0);