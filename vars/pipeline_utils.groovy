
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.StringCredentials

def getGitBranchSha(String branch) {
    return sh(script: "git rev-parse origin/${branch}^{commit}", returnStdout: true).trim().split('\n').collect{it as String}[0]
}

def gitHubNotifyStatus(String credentialsId, String account, String repo, String sha, String target_url, String state, String context, String description, boolean showResponce = false){
    def Map data = [
        state: "${state}".toString(),
        target_url: "${target_url}".toString(),
        description: "${description}".toString(),
        context: "${context}".toString()
    ]
    def String api_endpoint = "https://api.github.com/repos/${account}/${repo}/statuses/${sha}"
    withCredentials([string(credentialsId: credentialsId.toString(), variable: 'GH_TOKEN')]) {
        def http = new URL(api_endpoint).openConnection() as HttpURLConnection
        http.setRequestMethod('POST')
        http.setDoOutput(true)
        http.setRequestProperty('Authorization', "token ${GH_TOKEN}".toString())
        http.setRequestProperty("Accept", 'application/json')
        http.setRequestProperty("Content-Type", 'application/json')
        def json = new groovy.json.JsonBuilder()
        json data
        http.outputStream.write(json.toString().getBytes("UTF-8"))
        http.connect()
        if (http.responseCode in [200, 201]) {
            response = http.inputStream.getText('UTF-8')
        } else {
            response = http.errorStream.getText('UTF-8')
        }
        if (showResponce) {
            println "response: ${response}"
        }
    }
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


def isStartedByUser(jobInstance) {
    // Check if the build was triggered by some jenkins user
    def usercause = jobInstance.getCause(hudson.model.Cause.UserIdCause.class)
    if (usercause != null) {
        return usercause.getUserName()
    }
    return
}


def isStartedByWebHook(jobInstance) {
    // Check if the build was triggered by SCM change
    def scmCause = jobInstance.getCause(hudson.triggers.SCMTrigger.SCMTriggerCause)
    if (scmCause != null) {
        return scmCause.getShortDescription()
    }
    if (env.GIT_AUTHOR && env.GIT_AUTHOR != '' ) {
        return env.GIT_AUTHOR
    }
    return
}


def isStartedByJob(jobInstance) {
    //Check if the build was triggered by some jenkins project(job)
    def upstreamcause = jobInstance.getCause(hudson.model.Cause.UpstreamCause.class)
    if (upstreamcause != null) {
        def job = Jenkins.getInstance().getItemByFullName(upstreamcause.getUpstreamProject(), hudson.model.Job.class)
        if (job != null) {
            def upstream = job.getBuildByNumber(upstreamcause.getUpstreamBuild())
            if (upstream != null) {
                return upstream.getFullDisplayName()
            }
        }
    }
    return
}


def findCause(upStreamBuild) {
    def result

    result = isStartedByWebHook(upStreamBuild)
    if ( result ) {
        return result
    }

    result = isStartedByUser(upStreamBuild)
    if ( result ) {
        return result
    }

    result = isStartedByJob(upStreamBuild)
    if ( result ) {
        return result
    }
    return;
}


def notifySlack(String channel , String buildStatus = 'STARTED', String message='', List attachments=[]) {
    // Build status of null means success.
    buildStatus = buildStatus ?: 'SUCCESS'
    def color
    def startedBy = ''
    def emoji = ''
    if (buildStatus == 'STARTED') {
        color = '#D4DADF'
        startedBy = findCause(currentBuild.rawBuild)
        emoji = ":fast_forward: "
    } else if (buildStatus == 'SUCCESS') {
        color = '#BDFFC3'
        emoji = ":check_yes:"
    } else if (buildStatus == 'UNSTABLE') {
        color = '#FFFE89'
        emoji = ":check_no::fix_it_bold: "
    } else {
        color = '#FF9FA1'
        emoji = ":check_no::fix_it_bold: "
    }

    def msg = "${emoji}${buildStatus}: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n${env.BUILD_URL}"
    if (startedBy != '') {
        msg += "\nStarted by: ${startedBy}"
    }
    if (message != '') {
        msg += "\nMessage: ${message}"
    }

    slackSend(color: color, message: msg, channel: channel, username: "Jenkins", attachments: attachments)
}
