package devops.common;

import jenkins.model.*
import groovy.json.JsonOutput
import com.cloudbees.plugins.credentials.domains.*
import org.jenkinsci.plugins.plaincredentials.StringCredentials
import java.util.logging.Level; 
import java.util.logging.Logger;

// LOGGER.info("START...");

public class ActiveChoiseGitHub implements Serializable {

    def LOGGER

    def ActiveChoiseGitHub(boolean enableLogger=false) {
        this.enableLogger = enableLogger
        if (this.enableLogger) {
            this.LOGGER = Logger.getLogger("devops.common.ActiveChoiseGitHub")
            this.LOGGER.info("START...")
        }
    }

    def getHtmlAndCss() {
        if (this.enableLogger) {
             this.LOGGER.info("Return Css...")
        }
        return '''
<head>
    <style>
        .noselect {
            -webkit-touch-callout: none;
            -webkit-user-select: none;
            -khtml-user-select: none;
            -moz-user-select: none;
            -ms-user-select: none;
            user-select: none;
        }
        table.jenkins-parameter-table {
            border: 0;
            border-spacing: 10px;
            border-collapse: separate;
        }
        table.jenkins-parameter-table td{ 
            text-align: center; 
            vertical-align: middle;
            //margin: 10px;
            padding: 5px;
        }
        .jenkins-parameter-container {
            float: left;
            //width: 200px;
            // margin: 20px auto 10px;
            font-size: 14px;
            font-family: sans-serif;
            //overflow: auto;
        }
        .jenkins-parameter-select {
         width: 200px;
         }

        .jenkins-parameter-list {
            float: left;
            width: 100%;

            border: 1px solid lightgray;
            box-sizing: border-box;
            padding: 10px 12px;
        }

        .jenkins-parameter-search {
            padding: 2px 0;
        }

        .jenkins-parameter-input {
            margin: 10px 0;
            max-height: 200px;
            //overflow-y: auto;
        }
    </style>
</head>
<div class="jenkins-parameter-container">
        <input type="hidden" class="jenkins-parameter-result" name="value">
        <table class="jenkins-parameter-table" id="jenkins-parameter-table"><tbody></tbody></table>
</div>
'''
    }

    def gitHubGetBranches(String credentialsId, String account, String repo, boolean showResponce = false) {
        def String api_endpoint = "https://api.github.com/repos/${account}/${repo}/branches";
        if (this.enableLogger) {
            this.LOGGER.info("Getting branches for ${repo}");
        }
        def GH_TOKEN = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
            com.cloudbees.plugins.credentials.common.IdCredentials.class, Jenkins.instance, null, null
        ).find{it.id == credentialsId}.properties.secret
        def http = new URL(api_endpoint).openConnection() as HttpURLConnection
        http.setRequestMethod('GET')
        http.setRequestProperty('Authorization', "token " + GH_TOKEN)
        http.setRequestProperty("Accept", 'application/vnd.github.v3+json')
        int responseCode = http.getResponseCode();
        http.connect()
        if (http.responseCode in [200, 201]) {
            response = http.inputStream.getText('UTF-8')
            return(response)
        } else {
            response = http.errorStream.getText('UTF-8')
        }
        if (showResponce) {
            println("response: ${response}")
        }
    }

    def filterRepos(String reposList, String startsWithFilter) {
        def parser = new groovy.json.JsonSlurper()
        def parsedJSON = parser.parseText(reposList)
        def filtered = parsedJSON.findAll{it.name.startsWith(startsWithFilter)}
        return filtered
    }

    def limitArr(targetArr, limit) {
        if (targetArr.size() <= limit) {
            return targetArr
        } else {
            return targetArr.subList(0, limit)
        }
    }

    def getJavaScryptAndCss(Map rolesRepos) {
        def Map RolesWithBranches = [:]
        if (this.enableLogger) { this.LOGGER.info("Getting getJavaScryptAndCss...") }
        rolesRepos.each { key, val ->
            if (this.enableLogger) { this.LOGGER.info("Process...${key}") }
            RolesWithBranches[key] = filterRepos(gitHubGetBranches("jenkins-api-key", "Picket-Homes", val), 'rel').name;
        }
        def jsonRolesWithBranches = JsonOutput.toJson(RolesWithBranches);
        return """
<script src="https://cdnjs.cloudflare.com/ajax/libs/lodash.js/3.5.0/lodash.js"
    integrity="sha256-FnlET6HjAjzJk+xp/VPmlH5dKcYClzy2/yl08DuN4eA=" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/selectize.js/0.12.6/js/standalone/selectize.min.js" integrity="sha256-+C0A5Ilqmu4QcSPxrlGpaZxJ04VjsRjKu+G82kl5UJk=" crossorigin="anonymous"></script>

<script>
   
    var rolesBranches = {};
    function generateSelect(name, branches) {
        var sel = `<select class="jenkins-parameter-select" id="select-state-` + name + `" name="` + name + `">`;
        sel  = sel + `<option value="">Select a branch...</option>`;
        branches.each(function(branch) {
            sel =  sel + '<option value="' + branch +'">' + branch +'</option>';
        }); 
        sel =  sel + `</select>`;
        var lab = '<label for="select-state-' + name + '">' + name + '</label>';
        return '<td>' + lab +'</td><td>' + sel + '</td>';
    }

    /*function generateTableRow() {
        Q('#jenkins-parameter-table > tbody:last-child').append();
    }*/
   
    Q('.jenkins-parameter-container')
        .on('change', 'select', function () {
            console.log('on change select');
            const serviceName = Q(this).attr('name');
            const inputContainer =  Q(this).closest('td').find('.selectize-input').css('color', 'green');
            if(Q(this).val() !== ''){
                inputContainer.css('color', 'green');
                rolesBranches[serviceName] = Q(this).val();
            }else{
                inputContainer.css('color', '');
                delete rolesBranches[serviceName];
            }
            Q(this).closest('.jenkins-parameter-container').find('.jenkins-parameter-result').val(JSON.stringify(rolesBranches) || '[]');
        });
    var items = ${jsonRolesWithBranches};
    console.log(items);
    var i = 0;
    var tableRow = '';
    Object.keys(items).forEach(function(key) {
        i++;
        const val = items[key];
        tableRow = tableRow +  generateSelect(key, val);    
        if ( i % 3 == 0 ) {
            Q('#jenkins-parameter-table > tbody:last-child').append("<tr>" + tableRow + "</tr>");
            //console.log(tableRow);
            tableRow = "";
        } 
        //Q('#jenkins-parameter-table > tbody:last-child').append(generateSelect(key, val));
    });
    if (tableRow != '') {
        Q('#jenkins-parameter-table > tbody:last-child').append("<tr>" + tableRow + "</tr>");
    }

    Q('.jenkins-parameter-select').selectize({
        allowEmptyOption: true,
        sortField: 'val'
        //sortField: 'text'
    });
</script>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/selectize.js/0.12.6/css/selectize.bootstrap3.min.css" integrity="sha256-ze/OEYGcFbPRmvCnrSeKbRTtjG4vGLHXgOqsyLFTRjg=" crossorigin="anonymous" />
"""
    }
}