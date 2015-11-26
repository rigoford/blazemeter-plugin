package hudson.plugins.blazemeter.utils;

import hudson.FilePath;
import hudson.model.AbstractBuild;
import hudson.model.Result;
import hudson.plugins.blazemeter.*;
import hudson.plugins.blazemeter.api.APIFactory;
import hudson.plugins.blazemeter.api.ApiVersion;
import hudson.plugins.blazemeter.api.BlazemeterApi;
import hudson.plugins.blazemeter.entities.TestStatus;
import hudson.plugins.blazemeter.testresult.TestResult;
import hudson.util.FormValidation;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.util.log.AbstractLogger;
import org.eclipse.jetty.util.log.StdErrLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by dzmitrykashlach on 18/11/14.
 */
public class BzmServiceManager {
    private static StdErrLog logger = new StdErrLog(Constants.BZM_JEN);
    private final static int BUFFER_SIZE = 2048;
    private final static String ZIP_EXTENSION = ".zip";
    private BzmServiceManager() {
    }

    public static String autoDetectApiVersion(String apiKey,String blazeMeterUrl) {
        BlazemeterApi api = null;
        String detectedApiVersion = null;
            api = APIFactory.getAPI(apiKey, ApiVersion.v3, blazeMeterUrl);
            boolean isV3 = false;
            try {
                isV3 = api.getUser().getJSONObject("features").getBoolean("v3");
                if (isV3) {
                    detectedApiVersion="v3";
                } else {
                    detectedApiVersion="v2";
                }
            } catch (JSONException je) {
                detectedApiVersion="v3";
            } catch (NullPointerException npe) {
                detectedApiVersion="v3";
            }finally {
                return detectedApiVersion;
            }
    }


    public static JSONObject updateTestDuration(BlazemeterApi api,
                                          String testId,
                                          String testDuration,
                                          StdErrLog bzmBuildLog){
        JSONObject result;
        JSONObject updateResult=null;
        try {
            JSONObject jo = api.getTestConfig(testId);
            result = jo.getJSONObject(JsonConstants.RESULT);
            JSONObject configuration = result.getJSONObject(JsonConstants.CONFIGURATION);
            JSONObject plugins = configuration.getJSONObject(JsonConstants.PLUGINS);
            String type = configuration.getString(JsonConstants.TYPE);
            JSONObject options = plugins.getJSONObject(type);
            JSONObject override = options.getJSONObject(JsonConstants.OVERRIDE);
            override.put(JsonConstants.DURATION, testDuration);
            override.put("threads", JSONObject.NULL);
            configuration.put("serversCount", JSONObject.NULL);
            updateResult = api.putTestInfo(testId, result);
        } catch (JSONException je) {
            bzmBuildLog.warn("Received JSONException while updating test duration: ", je);
        } catch (Exception e) {
            bzmBuildLog.warn("Received JSONException while updating test duration: ", e);
        }
        return updateResult;
    }



    public static void waitForFinish(BlazemeterApi api, String apiVersion, String testId, AbstractLogger bzmBuildLog,
                                     String session) throws InterruptedException {
        Date start = null;
        long lastPrint = 0;
        while (true) {
            Thread.sleep(15000);
            TestStatus testStatus = api.getTestStatus(apiVersion.equals("v2") ? testId : session);

            if (!testStatus.equals(TestStatus.Running)) {
                bzmBuildLog.info("TestStatus for session " + (apiVersion.equals("v2") ? testId : session)
                        + testStatus);
                bzmBuildLog.info("BlazeMeter TestStatus for session" +
                        (apiVersion.equals("v2") ? testId : session)
                        + " is not 'Running': finishing build.... ");
                bzmBuildLog.info("Timestamp: " + Calendar.getInstance().getTime());
                break;
            }

            if (start == null)
                start = Calendar.getInstance().getTime();
            long now = Calendar.getInstance().getTime().getTime();
            long diffInSec = (now - start.getTime()) / 1000;
            if (now - lastPrint > 10000) { //print every 10 sec.
                bzmBuildLog.info("BlazeMeter test# " + testId + ", session # " + session + " running from " + start + " - for " + diffInSec + " seconds");
                lastPrint = now;
            }

            if (Thread.interrupted()) {
                bzmBuildLog.info("Test was interrupted: throwing Interrupted Exception");
                throw new InterruptedException();
            }
        }
    }

    public static String getReportUrl(BlazemeterApi api, String masterId,
                                      StdErrLog jenBuildLog, StdErrLog bzmBuildLog) {
        JSONObject jo=null;
        String publicToken="";
        String reportUrl=null;
        try {
            jo = api.generatePublicToken(masterId);
            if(jo.get(JsonConstants.ERROR).equals(JSONObject.NULL)){
                JSONObject result=jo.getJSONObject(JsonConstants.RESULT);
                publicToken=result.getString("publicToken");
                reportUrl=api.getBlazeMeterURL()+"/app/?public-token="+publicToken+"#masters/"+masterId+"/summary";
            }else{
                jenBuildLog.warn("Problems with generating public-token for report URL: "+jo.get(JsonConstants.ERROR).toString());
                bzmBuildLog.warn("Problems with generating public-token for report URL: "+jo.get(JsonConstants.ERROR).toString());
                reportUrl=api.getBlazeMeterURL()+"/app/#masters/"+masterId+"/summary";
            }

        } catch (Exception e){
          jenBuildLog.warn("Problems with generating public-token for report URL");
          bzmBuildLog.warn("Problems with generating public-token for report URL",e);
        }finally {
                return reportUrl;
        }
    }

    public static void uploadFile(String testId, BlazemeterApi bmAPI, File file, StdErrLog bzmBuildLog) {
        String fileName = file.getName();
        org.json.JSONObject json = bmAPI.uploadBinaryFile(testId, file);
        try {
            if (!json.get(JsonConstants.RESPONSE_CODE).equals(200)) {
                bzmBuildLog.info("Could not upload file " + fileName + " " + json.get(JsonConstants.ERROR).toString());
            }
        } catch (JSONException e) {
            bzmBuildLog.info("Could not upload file " + fileName + " " + e.getMessage());
        }
    }

    public static String getSessionId(JSONObject json,
                                ApiVersion apiVersion,StdErrLog bzmBuildLog,StdErrLog jenBuildLog) throws JSONException {
        String session = "";
        try {
            if (apiVersion.equals(ApiVersion.v2.name()) && !json.get(JsonConstants.RESPONSE_CODE).equals(200)) {
                if (json.get(JsonConstants.RESPONSE_CODE).equals(500) && json.get(JsonConstants.ERROR).toString()
                        .startsWith("Test already running")) {
                    bzmBuildLog.warn("Test already running, please stop it first");
                    return session;
                }
            }
            // get sessionId add to interface
            if (apiVersion.equals(ApiVersion.v2)) {
                session = json.get("session_id").toString();
            } else {
                JSONObject startJO = (JSONObject) json.get(JsonConstants.RESULT);
                session = ((JSONArray) startJO.get("sessionsId")).get(0).toString();
            }
        } catch (Exception e) {
            jenBuildLog.info("Failed to get session_id: " + e.getMessage());
            bzmBuildLog.info("Failed to get session_id. ", e);
        }
        return session;
    }

    public static void publishReport(BlazemeterApi api, String masterId,
                                     AbstractBuild<?, ?> build,
                                     String bzmBuildLogPath,
                                     StdErrLog jenBuildLog,
                                     StdErrLog bzmBuildLog){

        String reportUrl= getReportUrl(api, masterId, jenBuildLog,bzmBuildLog);
        jenBuildLog.info("Blazemeter test report will be available at " + reportUrl);
        jenBuildLog.info("Blazemeter test log will be available at " + bzmBuildLogPath);

        PerformanceBuildAction a = new PerformanceBuildAction(build);
        a.setReportUrl(reportUrl);
        build.addAction(a);

    }

    public static void saveReport(String filename,
                                  String report,
                                  FilePath filePath,
                                  StdErrLog jenBuildLog) {
        File reportFile = new File(filePath.getParent()
                + "/" + filePath.getName() + "/" + filename);
        try {
            if (!reportFile.exists()) {
                FileUtils.forceMkdir(reportFile.getParentFile());
                reportFile.createNewFile();
            }
            BufferedWriter out = new BufferedWriter(new FileWriter(reportFile));
            out.write(report);
            out.close();

        } catch (FileNotFoundException fnfe) {
            jenBuildLog.info("ERROR: Failed to save XML report to workspace " + fnfe.getMessage());
        } catch (IOException e) {
            jenBuildLog.info("ERROR: Failed to save XML report to workspace " + e.getMessage());
        }
    }

    public static Result validateCIStatus(BlazemeterApi api, String session, StdErrLog jenBuildLog){
        Result result;
        JSONObject jo;
        boolean success=true;
        JSONArray failures=new JSONArray();
        JSONArray errors=new JSONArray();
        try {
            jo=api.getCIStatus(session);
            jenBuildLog.info("Test status object = " + jo.toString());
            success=!jo.getString(JsonConstants.STATUS).equals(JsonConstants.FAILURE);
            failures=jo.getJSONArray(JsonConstants.FAILURES);
            errors=jo.getJSONArray(JsonConstants.ERRORS);
        } catch (JSONException je) {
            jenBuildLog.warn("No tresholds on server: setting SUCCESS for build ");
            success=true;
        } catch (Exception e) {
            jenBuildLog.warn("No tresholds on server: setting SUCCESS for build ");
            success=true;
        }
        jenBuildLog.info("Validating test status: " + (success ? "PASSED" : "FAILED") + "\n");

        result = success?Result.SUCCESS:Result.FAILURE;
        if(result.equals(Result.FAILURE)){
            jenBuildLog.info("Having failure while test status validation....");
            jenBuildLog.info("Failures: "+failures.toString());
            return result;
        }else{
            jenBuildLog.info("Setting SUCCESS for build after test status validation...");
            jenBuildLog.info("Errors: "+errors.toString());
        }
        return result;
    }

    public static String selectUserKeyOnId(BlazeMeterPerformanceBuilderDescriptor descriptor,
                                           String id){
        String userKey=null;
        List<BlazemeterCredential> credentialList=descriptor.getCredentials("Global");
        if(credentialList.size()==1){
            userKey=credentialList.get(0).getApiKey();
        }else{
            for(BlazemeterCredential c:credentialList){
                if(c.getId().equals(id)){
                    userKey=c.getApiKey();
                    break;
                }
            }
        }
        return userKey;
    }

    public static String selectUserKeyId(BlazeMeterPerformanceBuilderDescriptor descriptor,
                                           String userKey){
        String userKeyId=null;
        List<BlazemeterCredential> credentialList=descriptor.getCredentials("Global");
        if(credentialList.size()==1){
            userKeyId=credentialList.get(0).getId();
        }else{
            for(BlazemeterCredential c:credentialList){
                if(c.getApiKey().equals(userKey)){
                    userKeyId=c.getId();
                    break;
                }
            }
        }
        return userKeyId;
    }

    public static void downloadJtlReport(BlazemeterApi api, String sessionId, FilePath filePath,
                                         String buildNumber,
                                         StdErrLog jenBuildLog,
                                         StdErrLog bzmBuildLog) {

        JSONObject jo=api.retrieveJtlZip(sessionId);
        String dataUrl=null;
        URL url=null;
        try {
            JSONArray data=jo.getJSONObject(JsonConstants.RESULT).getJSONArray(JsonConstants.DATA);
            for(int i=0;i<data.length();i++){
                String title=data.getJSONObject(i).getString("title");
                if(title.equals("Zip")){
                    dataUrl=data.getJSONObject(i).getString(JsonConstants.DATA_URL);
                    break;
                }
            }
            File jtlZip=new File(filePath.getParent()
                    + "/" + buildNumber + "/" +sessionId+"-"+ Constants.BM_ARTEFACTS);

            if(!dataUrl.contains("amazonaws")){
                url=new URL(dataUrl+"?api_key="+api.getApiKey());
            }else{
                url=new URL(dataUrl);
            }

            FileUtils.copyURLToFile(url, jtlZip);
            jenBuildLog.info("Downloading JTLZIP from " + url);
            String jtlZipCanonicalPath=jtlZip.getCanonicalPath();
            jenBuildLog.info("Saving ZIP to " + jtlZipCanonicalPath);
            unzip(jtlZip.getAbsolutePath(), jtlZipCanonicalPath.substring(0,jtlZipCanonicalPath.length()-4), jenBuildLog);
            FilePath sample_jtl=new FilePath(filePath,"sample.jtl");
            FilePath bm_kpis_jtl=new FilePath(filePath,Constants.BM_KPIS);
            if(sample_jtl.exists()){
                sample_jtl.renameTo(bm_kpis_jtl);
            }
        } catch (JSONException e) {
            bzmBuildLog.warn("Unable to get  JTLZIP from "+url, e);
            jenBuildLog.warn("Unable to get  JTLZIP from "+url+" "+e.getMessage());
        } catch (MalformedURLException e) {
            bzmBuildLog.warn("Unable to get  JTLZIP from "+url, e);
            jenBuildLog.warn("Unable to get  JTLZIP from "+url+" "+e.getMessage());
        } catch (IOException e) {
            bzmBuildLog.warn("Unable to get JTLZIP from "+url, e);
            jenBuildLog.warn("Unable to get JTLZIP from "+url+" "+e.getMessage());
        } catch (InterruptedException e) {
            bzmBuildLog.warn("Unable to get JTLZIP from "+url, e);
            jenBuildLog.warn("Unable to get JTLZIP from "+url+" "+e.getMessage());
        }
    }

    public static void downloadJtlReports(BlazemeterApi api, String masterId, FilePath filePath,
                                          String buildNumber, StdErrLog jenBuildLog,
                                          StdErrLog bzmBuildLog) {
        List<String> sessionsIds = api.getListOfSessionIds(masterId);
        for (String s : sessionsIds) {
            downloadJtlReport(api, s, filePath,buildNumber, jenBuildLog, bzmBuildLog);
        }
    }



    public static void unzip(String srcZipFileName,
                             String destDirectoryName, StdErrLog jenBuildLog) {
        try {
            BufferedInputStream bufIS = null;
            // create the destination directory structure (if needed)
            File destDirectory = new File(destDirectoryName);
            destDirectory.mkdirs();

            // open archive for reading
            File file = new File(srcZipFileName);
            ZipFile zipFile = new ZipFile(file, ZipFile.OPEN_READ);

            //for every zip archive entry do
            Enumeration<? extends ZipEntry> zipFileEntries = zipFile.entries();
            while (zipFileEntries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
                if(entry.getName().substring((entry.getName().length()-4)).equals(".jtl")&!entry.isDirectory()){
                    jenBuildLog.info("\tExtracting jtl report: " + entry);

                    //create destination file
                    File destFile = new File(destDirectory, entry.getName());

                    //create parent directories if needed
                    File parentDestFile = destFile.getParentFile();
                    parentDestFile.mkdirs();

                        bufIS = new BufferedInputStream(
                                zipFile.getInputStream(entry));
                        int currentByte;

                        // buffer for writing file
                        byte data[] = new byte[BUFFER_SIZE];

                        // write the current file to disk
                        FileOutputStream fOS = new FileOutputStream(destFile);
                        BufferedOutputStream bufOS = new BufferedOutputStream(fOS, BUFFER_SIZE);

                        while ((currentByte = bufIS.read(data, 0, BUFFER_SIZE)) != -1) {
                            bufOS.write(data, 0, currentByte);
                        }

                        // close BufferedOutputStream
                        bufOS.flush();
                        bufOS.close();

                }

            }
            bufIS.close();
        } catch (Exception e) {
            jenBuildLog.warn("Failed to unzip report: check that it is downloaded");
        }
    }

    public static void retrieveJUNITXMLreport(BlazemeterApi api, String masterId, FilePath workspace, String buildNumber, StdErrLog jenBuildLog){
        String junitReport="";
        jenBuildLog.info("Requesting JUNIT report from server, masterId="+masterId);
        try{
            junitReport = api.retrieveJUNITXML(masterId);
        } catch (Exception e) {
            jenBuildLog.warn("Problems with receiving JUNIT report from server, masterId=" + masterId + " " + e.getMessage());
        }
        String junitReportName = masterId + "-" + Constants.BM_TRESHOLDS;
        FilePath junitReportFilePath = new FilePath(workspace,buildNumber);
        jenBuildLog.warn("build number="+buildNumber);
        jenBuildLog.warn("masterId=" + masterId);
        String junitReportPath = workspace.getParent()
                + "/" + workspace.getName() + "/" +buildNumber+"/"+ masterId + "-" + Constants.BM_TRESHOLDS;
        jenBuildLog.info("Received Junit report from server.... masterId=" + masterId);
        jenBuildLog.info("Saving it to " + junitReportPath);
        saveReport(junitReportName, junitReport, junitReportFilePath, jenBuildLog);
    }

    public static Result postProcess(PerformanceBuilder builder,String masterId,String buildNumber) throws InterruptedException {
        Thread.sleep(10000); // Wait for the report to generate.
        //get tresholds from server and check if test is success
        BlazemeterApi api=builder.getApi();
        StdErrLog jenBuildLog=builder.getJenBuildLog();
        Result result = Result.SUCCESS;
        ApiVersion apiVersion=ApiVersion.valueOf(builder.getApiVersion());
        FilePath workspace=builder.getBuild().getWorkspace();
        if(apiVersion.equals(ApiVersion.v3)&builder.isGetJunit()){
            retrieveJUNITXMLreport(api, masterId, workspace, buildNumber, jenBuildLog);
            } else {
            jenBuildLog.info("JUNIT report won't be requested: apiVersion is v2 or check-box is unchecked.");
        }
        Thread.sleep(30000);
        if(apiVersion.equals(ApiVersion.v3)&builder.isGetJtl()){
            AbstractBuild build=builder.getBuild();
            FilePath jtlPath = new FilePath(build.getWorkspace(), build.getId());
            BzmServiceManager.downloadJtlReports(api, masterId, jtlPath, buildNumber, jenBuildLog, jenBuildLog);
        } else {
            jenBuildLog.info("JTL report won't be requested: apiVersion is v2 or check-box is unchecked.");
        }
        result = BzmServiceManager.validateCIStatus(api, masterId, jenBuildLog);



        //get testGetArchive information
        JSONObject testReport=null;
        try{
            testReport = api.testReport(masterId);
        }catch (Exception e){
            jenBuildLog.info("Failed to get test report from server.");
        }


        if (testReport == null || testReport.equals("null")) {
            jenBuildLog.warn("Requesting aggregate is not available. " +
                    "Build won't be validated against local tresholds");
            return result;
        }
        TestResult testResult = null;
        Result localTresholdsResult=null;
        try {
            testResult = new TestResult(testReport);
            jenBuildLog.info(testResult.toString());
        } catch (IOException ioe) {
            jenBuildLog.info("Failed to get test result. Try to check server for it");
            jenBuildLog.info("ERROR: Failed to generate TestResult: " + ioe);
        } catch (JSONException je) {
            jenBuildLog.info("Failed to get test result. Try to check server for it");
            jenBuildLog.info("ERROR: Failed to generate TestResult: " + je);
        }finally{
            return localTresholdsResult!=null?localTresholdsResult:result;
        }

    }


    public static boolean stopTestSession(BlazemeterApi api, String masterId, StdErrLog jenBuildLog) {
        boolean terminate = false;
        try {
                int statusCode = api.getTestMasterStatusCode(masterId);
                if (statusCode < 100 & statusCode != 0) {
                    api.terminateTest(masterId);
                    terminate = true;
                }
                if (statusCode >= 100 | statusCode == -1 | statusCode == 0) {
                    api.stopTest(masterId);
                    terminate = false;
                }
        } catch (Exception e) {
            jenBuildLog.warn("Error while trying to stop test with testId=" + masterId + ", " + e.getMessage());
        } finally {
            return terminate;
        }
    }

    public static String getVersion() {
        Properties props = new Properties();
        try {
            props.load(BzmServiceManager.class.getResourceAsStream("version.properties"));
        } catch (IOException ex) {
            props.setProperty(Constants.VERSION, "N/A");
        }
        return props.getProperty(Constants.VERSION);
    }

    public static FormValidation validateUserKey(String userKey, String blazeMeterUrl) {
        String encryptedKey=userKey.substring(0,4)+"..."+userKey.substring(17);
        try {
            logger.info("Validating user-key started: user-key=" + encryptedKey);
            BlazemeterApi bzm = APIFactory.getAPI(userKey, ApiVersion.v3, blazeMeterUrl);
            logger.info("Getting user details from server: serverUrl=" + blazeMeterUrl);
            JSONObject u = bzm.getUser();
            net.sf.json.JSONObject user = null;
            if (u!= null) {
                user = net.sf.json.JSONObject.fromObject(u.toString());
                if (user.has("error") && !user.get("error").equals(null)) {
                    logger.warn("UserKey is not valid: error=" + user.get("error").toString());
                    logger.warn("User profile: "+user.toString());
                    return FormValidation.errorWithMarkup("UserKey is not valid: error=" + user.get("error").toString());
                } else {
                    logger.warn("UserKey is valid: user e-mail=" + user.getString("mail"));
                    return FormValidation.ok("User Key Valid. Email - " + user.getString("mail"));
                }
            }
        } catch (Exception e) {
            logger.warn("UserKey is not valid: unexpected exception=" + e.getMessage().toString());
            logger.warn(e);
            return FormValidation.errorWithMarkup("UserKey is not valid: unexpected exception=" + e.getMessage().toString());
        }
        logger.warn("UserKey is not valid: userKey="+encryptedKey+" blazemeterUrl="+blazeMeterUrl+". Please, check manually.");
        return FormValidation.error("UserKey is not valid: userKey="+encryptedKey+" blazemeterUrl="+blazeMeterUrl+". Please, check manually.");
    }

    public static String getUserEmail(String userKey,String blazemeterUrl){
        BlazemeterApi bzm = APIFactory.getAPI(userKey, ApiVersion.v3, blazemeterUrl);
        try {
            net.sf.json.JSONObject user= net.sf.json.JSONObject.fromObject(bzm.getUser().toString());
            if (user.has("mail")) {
                return user.getString("mail");
            } else {
                return "";
            }
        }catch (Exception e){
            return "";
        }
    }
}
