import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);

HttpPost post = new HttpPost(helper.getServerURL() + "rest/api/latest/issue");

if (props['checkData']) {
    helper.projectExists(props['projectKey']);
    helper.issueTypeExists(props['issueTypeName']);
    helper.priorityExists(props['priorityName']);
}

JSONObject issue = new JSONObject();
JSONObject details = new JSONObject();

// project
JSONObject project = new JSONObject();
project.put("key", props['projectKey'])
details.put("project", project);
// issueType
JSONObject issueType = new JSONObject();
issueType.put("name", props['issueTypeName'])
details.put("issuetype", issueType);
// priority
JSONObject priority = new JSONObject();
priority.put("name", props['priorityName']);
details.put("priority", priority);

// components
if (props['components']) {
    JSONArray components = new JSONArray();
    props['components'].split(',').collect {
        if (props['checkData']) {
            helper.componentExists(props['projectKey'], it);
        }
        JSONObject component = new JSONObject();
        component.put("name", it);
        components.put(component);
    }
    details.put("components", components);
}

// versions
if (props['versions']) {
    JSONArray versions = new JSONArray();
    props['versions'].split(',').collect {
        if (props['checkData']) {
            helper.versionExists(props['projectKey'], it);
        }
        JSONObject version = new JSONObject();
        version.put("name", it);
        versions.put(version);
    }
    details.put("versions", versions);
}

details.put("summary", props['summary']);
details.put("description", props['description']);
// TODO: check if assignee exists
if (props['assignee']) {
    JSONObject assignee = new JSONObject();
    assignee.put("name", props['assignee']);
    details.put("assignee", assignee);
}

// additional fields
if (props['additionalFields']) {
    props['additionalFields'].split('\n').collect {
        def (fldName, fldVal) = it.tokenize('=');
        details.put(fldName, fldVal);
    }
}

issue.put("fields", details);

HttpResponse response = helper.executeHttpRequest(post, 201, issue);
BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
String json = reader.readLine();
JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
String issueId = jsonResponse.getString("key");
String issueUrl = jsonResponse.getString("self");
println "Successfully created issue ${issueId}.";
println "See ${issueUrl} for more information.";

apTool.setOutputProperty("issueId", issueId);
apTool.setOutputProperty("issueUrl", issueUrl);
apTool.setOutputProperties();

System.exit(0);
