import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.JIRAHelper;

import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener;

final def apTool = new AirPluginTool(args[0], args[1])
final def props = apTool.getStepProperties();
final def helper = new JIRAHelper(apTool);
final def projectKey = props['projectKey'];
final def versionName = props['versionName'];

HttpPost post = new HttpPost(helper.getServerURL() + "rest/api/latest/version");

JSONObject version = new JSONObject();
version.put("name", versionName);
version.put("description", props['description']);
version.put("project", projectKey);

helper.executeHttpRequest(post, 201, version);

// get version key by getting project versions
HttpGet get = new HttpGet(helper.getServerURL() + "rest/api/latest/project/${projectKey}/versions");
HttpResponse response = helper.executeHttpRequest(get, 200, null);
BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
String json = reader.readLine();
JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
String versionId = null;
String versionUrl = null;
for (int i = 0; i < jsonResponse.length(); i++) {
    if (jsonResponse.getJSONObject(i).getString("name") == versionName) {
        versionId = jsonResponse.getJSONObject(i).getString("id");
        versionUrl = jsonResponse.getJSONObject(i).getString("self");
    };
}
println "Successfully created version ${versionName}.";
println "See ${versionUrl} for more information.";

apTool.setOutputProperty("versionId", versionId);
apTool.setOutputProperty("versionUrl", versionUrl);
apTool.setOutputProperties();

System.exit(0);
