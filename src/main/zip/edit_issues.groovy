import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;
import com.serena.air.plugin.jira.addcomments.FailMode;

import org.apache.http.client.methods.HttpPut;
import org.json.JSONArray;
import org.json.JSONObject;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

def failMode = FailMode.valueOf(props['failMode']);
def issueIds = props['issueIds'].split(',') as List;
def updateCount = 0;
for (def issueId : issueIds.sort()) {
    if (helper.issueExists(issueId)) {

        HttpPut put = new HttpPut(helper.getServerURL() + "rest/api/latest/issue/" + issueId);

        JSONObject issue = new JSONObject();
        JSONObject update = new JSONObject();
        JSONObject details = new JSONObject();

        // ger project key for the issue
        String projectKey = helper.getProjectKeyForIssue(issueId)

        // issueType
        if (props['issueTypeName']) {
            if (props['checkData']) {
                helper.issueTypeExists(props['issueTypeName']);
            }
            JSONObject issueType = new JSONObject();
            issueType.put("name", props['issueTypeName'])
            details.put("issuetype", issueType);
        }
        // priority
        if (props['priorityName']) {
            if (props['checkData']) {
                helper.priorityExists(props['priorityName']);
            }
            JSONObject priority = new JSONObject();
            priority.put("name", props['priorityName']);
            details.put("priority", priority);
        }

        // components
        if (props['components']) {
            JSONObject componentsUpdate = new JSONObject();
            JSONArray componentsSet = new JSONArray()
            JSONArray components = new JSONArray();
            props['components'].split(',').collect {
                if (props['checkData']) {
                    helper.componentExists(projectKey, it);
                }
                JSONObject component = new JSONObject();
                component.put("name", it);
                components.put(component);
            }
            componentsUpdate.put("set", components)
            componentsSet.put(componentsUpdate)
            update.put("components", componentsSet);
        }
        // versions
        if (props['versions']) {
            JSONObject versionsUpdate = new JSONObject();
            JSONArray versionsSet = new JSONArray()
            JSONArray versions = new JSONArray();
            props['versions'].split(',').collect {
                if (props['checkData']) {
                    helper.versionExists(projectKey, it);
                }
                JSONObject version = new JSONObject();
                version.put("name", it);
                versions.put(version);
            }
            versionsUpdate.put("set", versions)
            versionsSet.put(versionsUpdate)
            update.put("versions", versionsSet);
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

        if (props['summary'])
            details.put("summary", props['summary']);
        if (props['description'])
            details.put("description", props['description']);
        if (props['assignee']) {
            // TODO: check if assignee exists
            JSONObject assignee = new JSONObject();
            assignee.put("name", props['assignee'])
            details.put("assignee", assignee)
        }

        // additional fields
        if (props['additionalFields']) {
            props['additionalFields'].split('\n').collect {
                def (fldName, fldVal) = it.tokenize('=');
                details.put(fldName, fldVal);
            }
        }

        issue.put("fields", details);

        helper.executeHttpRequest(put, 204, issue);
        println "Successfully edited issue ${issueId}.";

        updateCount++;
    } else {
        if (failMode == FailMode.FAIL_FAST) {
            helper.exitFailure("Error: issue ${issueId} not found.")
        } else {
            println "Could not find issue ${issueId}.";
        }
    }
}
def totalNum = issueIds.size();
if (failMode == FailMode.FAIL_ON_NO_UPDATES) {
    if (!issueIds) {
        helper.exitFailure("No issues found to edit.");
    }
}
if (failMode == FailMode.FAIL_ON_ANY_FAILURE) {
    if (!issueIds || (updateCount != totalNum)) {
        helper.exitFailure("Only edited ${updateCount} out of ${totalNum} issues.");
    }
}
println "Edited ${updateCount} out of ${totalNum} issues.";

System.exit(0);
