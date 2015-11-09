import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray
import org.json.JSONObject;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

def failMode = FailMode.valueOf(props['failMode']);
def issueIds = props['issueIds'].split(',') as List;
def transName = props['transName'];
def updateCount = 0;
for (def issueId : issueIds.sort()) {
    if (helper.issueExists(issueId)) {

        HttpPost post = new HttpPost(helper.getServerURL() + "rest/api/latest/issue/" + issueId + "/transitions");

        JSONObject issue = new JSONObject();
        JSONObject transition = new JSONObject();
        JSONObject update = new JSONObject();
        JSONObject details = new JSONObject();

        // ger project key for the issue
        String projectKey = helper.getProjectKeyForIssue(issueId)

        // transition
        def transitionId = helper.getTransitionId(issueId, transName)
        if (transitionId == null) {
            if (failMode == FailMode.FAIL_FAST) {
                helper.exitFailure("Error: transition ${transName} not available for issue ${issueId}.");
            } else {
                println "Could not find transition ${transName} for issue ${issueId}, skipping...";
                continue;
            }
        }
        transition.put("id", transitionId)
        issue.put("transition", transition)

        // fix versions
        if (props['fixVersions']) {
            JSONObject versionsUpdate = new JSONObject();
            JSONArray versionsSet = new JSONArray()
            JSONArray versions = new JSONArray();
            props['fixVersions'].split(',').collect {
                if (props['checkData']) {
                    helper.versionExists(projectKey, it);
                }
                JSONObject version = new JSONObject();
                version.put("name", it);
                versions.put(version);
            }
            versionsUpdate.put("set", versions)
            versionsSet.put(versionsUpdate)
            update.put("fixVersions", versionsSet);
        }

        // comments
        if (props['commentBody']) {
            JSONObject commentsUpdate = new JSONObject();
            JSONArray commentsSet = new JSONArray()
            JSONObject comment = new JSONObject();
            comment.put("body", props['commentBody']);
            commentsUpdate.put("add", comment)
            commentsSet.put(commentsUpdate)
            update.put("comment", commentsSet);
        }

        issue.put("update", update);

        if (props['assignee']) {
            // TODO: check if assignee exists
            JSONObject assignee = new JSONObject();
            assignee.put("name", props['assignee'])
            details.put("assignee", assignee)
        }

        if (props['resolution']) {
            // TODO: check if resolution exists
            JSONObject resolution = new JSONObject();
            resolution.put("name", props['resolution'])
            details.put("resolution", resolution)
        }

        issue.put("fields", details);

        HttpResponse response = helper.executeHttpRequest(post, 204, issue);
        println "Successfully transitioned issue ${issueId} using ${transName}.";

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
        helper.exitFailure("No issues found to transition.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!issueIds || (updateCount != totalNum)) {
        helper.exitFailure("Only transitioned ${updateCount} out of ${totalNum} issues.");
    }
}
println "Transitioned ${updateCount} out of ${totalNum} issues.";

System.exit(0);

