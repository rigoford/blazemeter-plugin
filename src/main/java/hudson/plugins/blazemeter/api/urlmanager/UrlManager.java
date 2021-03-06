/**
 Copyright 2017 BlazeMeter Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package hudson.plugins.blazemeter.api.urlmanager;

import hudson.plugins.blazemeter.utils.JobUtility;


public interface UrlManager {

    String CLIENT_IDENTIFICATION = "&_clientId=CI_JENKINS&_clientVersion="
            + JobUtility.version();

    String V4 ="/api/v4";

    String MASTERS="/masters";

    String SESSIONS="/sessions";

    String CI_STATUS="/ci-status";

    String getServerUrl();

    String masterStatus(String appKey, String testId);

    String tests(String appKey, int workspaceId);

    String masterId(String appKey, String masterId);

    String testStart(String appKey, String testId);

    String collectionStart(String appKey, String collectionId);

    String testStop(String appKey, String testId);

    String testTerminate(String appKey, String testId);

    String testReport(String appKey, String reportId);

    String getUser(String appKey);

    String getCIStatus(String appKey, String sessionId);

    String retrieveJUNITXML(String appKey, String sessionId);

    String retrieveJTLZIP(String appKey, String sessionId);

    String generatePublicToken(String appKey, String sessionId);

    String listOfSessionIds(String appKey, String masterId);

    String properties(String appKey, String sessionId);

    String workspaces(String appKey, int accountId);

    String accounts(String appKey);

    String multiTests(String appKey, int workspaceId);

    String testId(String appKey, String testId);

    String collectionId(String appKey, String testId);

    String workspaceId(String appKey, String projectId);
}

