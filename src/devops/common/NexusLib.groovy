package devops.common;

import groovy.json.JsonOutput


public class NexusLib implements Serializable {
    String nexusUrl
    //String nexusApiEndpoint


    def NexusLib(String nexusUrl) {
        this.nexusUrl = nexusUrl
        //this.nexusApiEndpoint = nexusUrl + '/service/rest/v1/search?sort=version'
    }


} //end class

