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
final def componentName = props['componentName'];

HttpPost post = new HttpPost(helper.getServerURL() + "rest/api/latest/component");

JSONObject component = new JSONObject();
component.put("name", componentName);
component.put("description", props['description']);
component.put("project", projectKey);

helper.executeHttpRequest(post, 201, component);

// get component key by getting project components
HttpGet get = new HttpGet(helper.getServerURL() + "rest/api/latest/project/${projectKey}/components");
HttpResponse response = helper.executeHttpRequest(get, 200, null);
BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
String json = reader.readLine();
JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
String componentId = null;
String componentUrl = null;
for (int i = 0; i < jsonResponse.length(); i++) {
    if (jsonResponse.getJSONObject(i).getString("name") == componentName) {
        componentId = jsonResponse.getJSONObject(i).getString("id");
        componentUrl = jsonResponse.getJSONObject(i).getString("self");
    };
}
println "Successfully created component ${componentName}.";
println "See ${componentUrl} for more information.";

apTool.setOutputProperty("componentId", componentId);
apTool.setOutputProperty("componentUrl", componentUrl);
apTool.setOutputProperties();

System.exit(0);
