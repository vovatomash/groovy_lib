import devops.common.utils


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
