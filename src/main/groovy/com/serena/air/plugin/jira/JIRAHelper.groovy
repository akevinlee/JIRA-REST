package com.serena.air.plugin.jira;

import com.urbancode.air.AirPluginTool;
import com.serena.air.plugin.jira.addcomments.FailMode;

import org.apache.http.client.HttpClient
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.HttpException;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;

public class JIRAHelper {

    private AirPluginTool pluginTool;
    private String username;
    private String password;
    private String serverURL;
    private String authToken;
    private def props;
    private def failMode;

    /**
     * Constructs an JIRA Helper and creates an authentication header for REST calls
     * @params pluginTool The AirPluginTool containing all step properties
     */
    public JIRAHelper(AirPluginTool pluginTool) {
        this.pluginTool = pluginTool;
        this.props = this.pluginTool.getStepProperties();
        if ((props['username'] == null) || (props['serverUrl'] == null))
            exitFailure("A username, password and server URL have not been provided.");
        this.username = props['username'];
        this.password = props['password'];
        if (props['serverUrl'].endsWith("/")) {
            this.serverURL = props['serverUrl'];
        } else {
            this.serverURL = props['serverUrl'] + "/";
        }
        String creds = this.username + ':' + this.password;
        this.authToken = "Basic " + creds.bytes.encodeBase64().toString();
        if (props['failMode'])
            this.failMode = FailMode.valueOf(props['failMode']);
        createSession();
    }

    //
    // public methods
    //

    public getServerURL() { return this.serverURL; }

    /**
     * Check if a project exists
     * @param projectKey The project by key to check for
     * @return True if project exists else false
     */
    public boolean projectExists(String projectKey) {
        if (props['debug']) println ">>> Checking if project ${projectKey} exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/" + projectKey);
            executeHttpRequest(get, 200, null);
            if (props['debug']) println ">>> Found project.";
            return true;
        } catch (HttpResponseException ex) {
            exitFailure("The project with key ${projectKey} does not exist, or is not visible to the user.");
            return false;
        }
    }

    /**
     * Check if an issue exists
     * @param issueKey The issue by key to check for
     * @return True if issue exists else false
     */
    public boolean issueExists(String issueKey) {
        if (props['debug']) println ">>> Checking if issue ${issueKey} exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issue/" + issueKey);
            executeHttpRequest(get, 200, null);
            if (props['debug']) println ">>> Found issue.";
            return true;
        } catch (HttpResponseException ex) {
            println "The issue with key ${issueKey} does not exist, or is not visible to the user.";
            return false;
        }
    }

    /**
     * Get the status of an issue
     * @param issueKey The issue to check the status for
     * @return String containing status name or null if status does not exist
     */
    public String issueStatus(String issueKey) {
        if (props['debug']) println ">>> Checking status of issue ${issueKey}.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issue/" + issueKey + "?fields=status");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            String status = jsonResponse.getJSONObject("fields").getJSONObject("status").getString("name");
            if (props['debug']) println ">>> Found status ${status}";
            return status;
        } catch (HttpResponseException ex) {
            println "The issue with key ${issueKey} does not exist, or is not visible to the user.";
            return null;
        }
    }

    /**
     * Check if an issue type exists
     * @param issueTypeName The name of the issue type
     * @return True if the issue type exists else false
     */
    public boolean issueTypeExists(String issueTypeName) {
        if (props['debug']) println ">>> Checking if issue type ${issueTypeName} exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issuetype/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == issueTypeName) {
                    if (props['debug']) println ">>> Found issue type.";
                    return true;
                };
            }
            println "The issuetype with name ${issueTypeName} does not exist, or is not visible to the user.";
            return false;
        } catch (HttpResponseException ex) {
            println "The issuetype with name ${issueTypeName} does not exist, or is not visible to the user.";
            return false;
        }
    }

    /**
     * Check if a priority exists
     * @param priorityName The name of the priority
     * @return True if priority exists else false
     */
    public boolean priorityExists(String priorityName) {
        if (props['debug']) println ">>> Checking if priority ${priorityName} exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/priority/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == priorityName) {
                    if (props['debug']) println ">>> Found priority.";
                    return true;
                };
            }
            println "The priority with name ${priorityName} does not exist, or is not visible to the user.";
            return false;
        } catch (HttpResponseException ex) {
            println "The priority with name ${priorityName} does not exist, or is not visible to the user.";
            return false;
        }
    }

    /**
     * Check if a component exists
     * @param projectKey The project to check the component for
     * @param componentName The name of the component
     * @return True if component exists else false
     */
    public boolean componentExists(String projectKey, String componentName) {
        if (props['debug']) println ">>> Checking if component ${componentName} exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/${projectKey}/components/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == componentName) {
                    if (props['debug']) println ">>> Found component.";
                    return true;
                };
            }
            println "The component with name ${componentName} does not exist, or is not visible to the user.";
            return false;
        } catch (HttpResponseException ex) {
            println "The component with name ${componentName} does not exist, or is not visible to the user.";
            return false;
        }
    }

    /**
     * Check if a version exists
     * @param projectKey The project to check the version for
     * @param versionName The name of the version
     * @return True if version exists else false
     */
    public boolean versionExists(String projectKey, String versionName) {
        if (props['debug']) println ">>> Checking if version ${versionName} exists.";
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/project/${projectKey}/versions/");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONArray jsonResponse = new JSONArray(new JSONTokener(json));
            for (int i = 0; i < jsonResponse.length(); i++) {
                if (jsonResponse.getJSONObject(i).getString("name") == versionName) {
                    if (props['debug']) println ">>> Found version.";
                    return true;
                };
            }
            println "The version with name ${versionName} does not exist, or is not visible to the user.";
            return false;
        } catch (HttpResponseException ex) {
            println "The versions with name ${versionName} does not exist, or is not visible to the user.";
            return false;
        }
    }

    /**
     * Get the id for a named transition
     * @param issueId The issue id (by key) to get the transition for
     * @param transitionName The name of the transition
     * @return A String with the transition id or null if the transition does not exist
     */
    public String getTransitionId(String issueId, String transitionName) {
        if (props['debug']) println ">>> Getting transition id for transition ${transitionName}.";
        String transitionId = null;
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issue/${issueId}/transitions");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            JSONArray transitions = jsonResponse.getJSONArray("transitions");
            for (int i = 0; i < transitions.length(); i++) {
                if (transitions.getJSONObject(i).getString("name") == transitionName) {
                    transitionId = transitions.getJSONObject(i).getString("id");
                    if (props['debug']) {
                        println ">>> Found transition id '${transitionId}' for transition: ${transitionName}";
                    }
                    break;
                };
            }
            if (transitionId == null)
                println "A transition with name ${transitionName} does not exist, or is not currently available to the user.";
        } catch (HttpResponseException ex) {
            println "A transition with name ${transitionName} does not exist, or is not currently available to the user.";
        }
        return transitionId;
    }

    /**
     * Get the project key for an issue - can't assume its the first part of the format
     * @param issueId The issue id (by key) to get the project for
     * @return A String with the project key or null if the issue does not exist
     */
    public String getProjectKeyForIssue(String issueId) {
        if (props['debug']) println ">>> Getting project key issue ${issueId}.";
        String projectKey = null;
        try {
            HttpGet get = new HttpGet(this.serverURL + "rest/api/latest/issue/${issueId}?fields=project");
            HttpResponse response = executeHttpRequest(get, 200, null);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            String json = reader.readLine();
            JSONObject jsonResponse = new JSONObject(new JSONTokener(json));
            projectKey = jsonResponse.getJSONObject("fields").getJSONObject("project").getString("key");
            if (props['debug']) {
                println ">>> Found project key id '${projectKey}' for issue: ${issueId}";
            }
            if (projectKey == null)
                println "A project for issue ${issueId} does not exist, or is not currently available to the user.";
        } catch (HttpResponseException ex) {
            println "A project for issue ${issueId} does not exist, or is not currently available to the user.";
        }
        return projectKey;
    }

    /**
     * Executes the given HTTP request and checks for a correct response status
     * @param request The HttpRequest to execute
     * @param expectedStatus The response status that indicates a successful request
     * @param body The JSONObject containing the request body
     * @return A JSONObject containing the response to the HTTP request executed
     */
    private HttpResponse executeHttpRequest(Object request, int expectedStatus, JSONObject body) {
        // Make sure the required parameters are there
        if ((request == null) || (expectedStatus == null)) exitFailure("An error occurred executing the request.");

        if (props['debug']) {
            println ">>> Sending request: ${request}"
            if (body != null) println "\n>>> Body contents: ${body}";
        }

        HttpClient client = new DefaultHttpClient();
        request.setHeader("Authorization", this.authToken);
        if (body) {
            StringEntity input = new StringEntity(body.toString());
            input.setContentType("application/json");
            request.setEntity(input);
        }

        HttpResponse response;
        try {
            response = client.execute(request);
        } catch (HttpException e) {
            exitFailure("There was an error executing the request.");
        }

        if (!(response.getStatusLine().getStatusCode() == expectedStatus))
            httpFailure(response);

        if (props['debug']) {
            println ">>> Received the response: " + response.getStatusLine();
        }
        return response;
    }

    /**
     * Write an error message to console and exit on a fail status.
     * @param message The error message to write to the console.
     */
    private void exitFailure(String message) {
        println "${message}";
        System.exit(1);
    }

    /**
     * Write a HTTP error message to console and exit on a fail status.
     * @param message The error message to write to the console.
     */
    private void httpFailure(HttpResponse response) {
        println ">>> Request failed : " + response.getStatusLine();
        String responseString = new BasicResponseHandler().handleResponse(response);
        println "${responseString}";
        System.exit(1);
    }

    //
    // private methods
    //

    /**
     * Pre authenticate and creates a session for the JIRA user
     */
    private void createSession() {
        if (props['debug']) {
            println ">>> Creating sesssion for user ${this.username} to JIRA instance: ${this.serverURL}"
        }
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(this.serverURL + "rest/auth/latest/session")
        get.addHeader("Authorization", this.authToken);
        get.addHeader("accept", "application/json");

        HttpResponse response;
        try {
            response = client.execute(get);
        } catch (HttpException e) {
            exitFailure("There was an error executing the request.");
        }

        if (!(response.getStatusLine().getStatusCode() != "200"))
            httpFailure(response);
    }

}