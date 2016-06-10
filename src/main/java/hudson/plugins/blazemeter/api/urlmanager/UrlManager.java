package hudson.plugins.blazemeter.api.urlmanager;

import hudson.plugins.blazemeter.utils.JobUtility;

/**
 * Created by dzmitrykashlach on 10/11/14.
 */
public interface UrlManager {

    String CLIENT_IDENTIFICATION = "&_clientId=CI_JENKINS&_clientVersion="
            + JobUtility.getVersion();

    String LATEST="/api/latest";
    String TESTS="/tests";
    String MASTERS="/masters";
    String WEB="/web";
    String getServerUrl();

    void setServerUrl(String serverUrl);

    String masterStatus(String appKey, String userKey, String testId);

    String tests(String appKey, String userKey);

    String activeTests(String appKey, String userKey);

    String masterId(String appKey,String userKey, String masterId);

    String testStart(String appKey, String userKey, String testId);

    String collectionStart(String appKey, String userKey, String collectionId);

    String testStop(String appKey, String userKey, String testId);

    String testTerminate(String appKey, String userKey, String testId);

    String testReport(String appKey, String userKey, String reportId);

    String getUser(String appKey, String userKey);

    String getCIStatus(String appKey, String userKey, String sessionId);

    String getTestConfig(String appKey, String userKey, String testId);

    String postJsonConfig(String appKey, String userKey, String testId);

    String createTest(String appKey, String userKey);

    String retrieveJUNITXML(String appKey, String userKey, String sessionId);

    String retrieveJTLZIP(String appKey, String userKey, String sessionId);

    String generatePublicToken(String appKey, String userKey, String sessionId);

    String listOfSessionIds(String appKey, String userKey, String masterId);

    String version(String appKey);

    String properties(String appKey, String userKey, String sessionId);
}
