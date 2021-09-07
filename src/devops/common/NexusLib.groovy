package devops.common;

import groovy.json.JsonOutput
// import java.util.logging.Level; 
// import java.util.logging.Logger;


public class NexusLib implements Serializable {
    String nexusUrl
    String nexusApiEndpoint
    boolean showResponce


    def NexusLib(String nexusUrl, boolean showResponce = false) {
        this.nexusUrl = nexusUrl
        this.nexusApiEndpoint = nexusUrl + '/service/rest/v1/search?sort=version'
        this.showResponce = showResponce
    }


    def getNexusActifacts(String repository, String name, String group, String version, String continuationToken, String direction='desc') {
        def response = null

        def nullTrustManager = [
            checkClientTrusted: { chain, authType ->  },
            checkServerTrusted: { chain, authType ->  },
            getAcceptedIssuers: { null }
        ]

        def nullHostnameVerifier = [
            verify: { hostname, session -> 
                true 
            }
        ]

        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL")
        sc.init(null, [nullTrustManager as  javax.net.ssl.X509TrustManager] as  javax.net.ssl.X509TrustManager[], null)
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory())
        javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(nullHostnameVerifier as javax.net.ssl.HostnameVerifier)

        def String api_endpoint = this.nexusApiEndpoint
        if ( direction && direction !='' ) {
            api_endpoint += "&direction=${direction}"
        }
        if ( repository && repository !='' ) {
            api_endpoint += "&repository=${repository}"
        }
        if ( group && group !='' ) {
            api_endpoint += "&group=${group}"
        }
        if ( name && name !='' ) {
            api_endpoint += "&name=${name}"
        }
        if ( version && version !='' ) {
            api_endpoint += "&version=${version}"
        }
        if ( continuationToken && continuationToken !='' ) {
            api_endpoint += "&continuationToken=${continuationToken}"
        }

        def http = new URL(api_endpoint).openConnection() as HttpURLConnection
        http.setRequestMethod('GET')
        http.setRequestProperty("Accept", 'application/json')
        int responseCode = http.getResponseCode();
        http.connect()
        if (http.responseCode in [200, 201]) {
            response = http.inputStream.getText('UTF-8')
            return(response)
        } else {
            response = http.errorStream.getText('UTF-8')
        }
        if (this.showResponce) {
            println("response: ${response}")
        }
    }


    def retriveNexusPaginetedData(String repository, String name, String group, String version) {
        def nexusFilteredResult = [:]
        def continuationToken = ''
        def tmp_result = ''
        def parser = new groovy.json.JsonSlurper()
        for(int i in 1..50) {
            def parsedJSON = parser.parseText(this.getNexusActifacts(repository, name, group, version, continuationToken))
            continuationToken = parsedJSON['continuationToken']
            nexusFilteredResult << parsedJSON
            if (continuationToken == null || continuationToken == '') {
                break
            }
        }
        return nexusFilteredResult
    }


    def getLatestVersion(String repository, String name, String group) {
        def parser = new groovy.json.JsonSlurper()
        def parsedJSON = parser.parseText(this.getNexusActifacts(repository, name, group))
        return parsedJSON.items[0]?.version
    }


    def getNexusVersions(parsedJSON) {
        //def filtered = parsedJSON.items.findAll{it.assets.contentType == "application/java-archive"}
        def filtered = [:] 
        parsedJSON.items.each{ item ->
            if ( !filtered.containsKey(item.name) ) {
                filtered[item.name] = [:]
            }
            if ( !filtered[item.name].containsKey(item.version) ) {
                filtered[item.name][item.version] = [:]
            }
            item.assets.each{ asset ->
                if (asset.contentType == "application/java-archive") {
                    filtered[item.name][item.version] = [
                        "path": asset.path,
                        "url": asset.downloadUrl,
                        "version": item.version
                    ]
                }
            } 
        }    
        return filtered
    }
} //end class

