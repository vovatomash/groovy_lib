package devops.common;

import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.StringCredentials
import org.jenkinsci.plugins.workflow.cps.CpsThread

public class GitHubNotify implements Serializable {

    def steps
    String credentialsId
    String account
    String repo
    String sha
    String targetUrl

    def GitHubNotify(steps, String credentialsId, String account, String repo, String sha, String targetUrl) {
        this.steps = steps
        this.credentialsId = credentialsId
        this.account = account
        this.repo = repo
        this.sha = sha
        this.targetUrl = targetUrl
    }

    def getSecretById(String secretId) {
        // set Credentials domain name (null means is it global)
        def domainName = null

        def credentialsStore = Jenkins.instance.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0]?.getStore()
        domain = new Domain(domainName, null, Collections.<DomainSpecification>emptyList())
        def c = credentialsStore?.getCredentials(domain).findResult { it.id == secretId ? it : null }
        if ( c ) {
          return c.secret.getPlainText()
        }
        return null
    }

    def sendPostRequest(String apiEndpoint, Map requestData, boolean showResponce = false) {
        def response = null
        def http = new URL(apiEndpoint).openConnection() as HttpURLConnection
        http.setRequestMethod('POST')
        http.setDoOutput(true)
        http.setRequestProperty("PRIVATE-TOKEN", this.getSecretById(this.credentialsId))
        // http.setRequestProperty("Accept", 'application/json')
        http.setRequestProperty("Content-Type", "application/json")
        def json = new groovy.json.JsonBuilder()
        json requestData
        http.outputStream.write(json.toString().getBytes("UTF-8"))
        http.connect()
        if (http.responseCode in [200, 201]) {
            response = http.inputStream.getText('UTF-8')
            if (showResponce) {
                this.steps.println("response: ${response}")
            }
            return [true, response]
        } else {
            response = http.errorStream.getText('UTF-8')
            if (showResponce) {
                this.steps.println("response: ${response}")
            }
            return [false, response]
        }
        return [false, null]
    }

    def sendGetRequest(String apiEndpoint) {
        def response = null
        def http = new URL(apiEndpoint).openConnection() as HttpURLConnection
        http.setRequestMethod('GET')
        http.setDoOutput(true)
        http.setRequestProperty("PRIVATE-TOKEN", this.steps.pipeline_utils.getSecretById(this.credentialsId))
        // http.setRequestProperty("Accept", 'application/json')
        http.setRequestProperty("Content-Type", "application/json")
        http.connect()
        if (http.responseCode in [200, 201]) {
            response = http.inputStream.getText('UTF-8')
            return [true, response]
        } else {
            response = http.errorStream.getText('UTF-8')
            return [false, response]
        }
        return [false, null]

    }

    def sendStatus(String state, String context, String description = '') {
        if (this.sha == null) {
            throw new Exception("GitHubNotify.sha is empty, please set commit sha explicitly")
        }
        def String api_endpoint = "https://api.github.com/repos/${this.account}/${this.repo}/statuses/${this.sha}"
        def Map data = [
            state: "${state}".toString(),
            target_url: "${this.targetUrl}".toString(),
            description: "${description}".toString(),
            context: "${context}".toString()
        ]

        this.sendPostRequest(api_endpoint, data, true)
    }

    def setSha(String sha) {
        this.sha = sha
    }

    def setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl
    }

    def sendStepStatus(String stepName, String description, Closure c) {
        try {
            this.sendStatus("pending", stepName, description)
            def r = c()
            this.sendStatus("success", stepName, description)
            return r
        } catch (hudson.AbortException er) {
            println(er)
            this.sendStatus("failure", stepName, description)
        } catch (Exception er1) {
            println(er1)
            this.sendStatus("error", stepName, description)
            throw er1
        }
    }
}
