import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

def failMode = FailMode.valueOf(props['failMode']);
def issueIds = props['issueIds'].split(',') as List;
def updateCount = 0;
for (def issueId : issueIds.sort()) {
    if (helper.issueExists(issueId)) {
        HttpPost post = new HttpPost(helper.getServerURL() + "rest/api/latest/issue/${issueId}/comment?expand=renderedBody");
        JSONObject comment = new JSONObject();
        comment.put("body", props['commentBody']);
        helper.executeHttpRequest(post, 201, comment);
        println "Successfully added comment to issue ${issueId}.";
        updateCount++;
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
        helper.exitFailure("No issues found to update.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!issueIds || (updateCount != totalNum)) {
        helper.exitFailure("Only added comments to ${updateCount} out of ${totalNum} issues.");
    }
}
println "Added comments to ${updateCount} out of ${totalNum} issues.";

System.exit(0)
