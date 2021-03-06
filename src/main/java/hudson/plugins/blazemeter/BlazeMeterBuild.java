/**
 Copyright 2016 BlazeMeter Inc.

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

package hudson.plugins.blazemeter;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.ProxyConfiguration;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.plugins.blazemeter.api.Api;
import hudson.plugins.blazemeter.api.ApiImpl;
import hudson.plugins.blazemeter.api.HttpLogger;
import hudson.plugins.blazemeter.entities.TestStatus;
import hudson.plugins.blazemeter.utils.Constants;
import static hudson.plugins.blazemeter.utils.Constants.ENCRYPT_CHARS_NUM;
import hudson.plugins.blazemeter.utils.JobUtility;
import hudson.plugins.blazemeter.utils.JsonConsts;
import hudson.plugins.blazemeter.utils.LogEntries;
import hudson.plugins.blazemeter.utils.Utils;
import hudson.remoting.Callable;
import java.io.File;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.HashMap;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.log.StdErrLog;
import org.jenkinsci.remoting.RoleChecker;
import org.json.JSONArray;
import org.json.JSONException;


public class BlazeMeterBuild implements Callable<Result, Exception> {

    private boolean credLegacy=false;

    private String credential = null;

    private String workspaceId = null;

    private String serverUrl = "";

    private String testId = "";

    private String notes = "";

    private String sessionProperties = "";

    private String jtlPath = "";

    private String junitPath = "";

    private boolean getJtl = false;

    private boolean getJunit = false;

    private String buildId = null;

    private String jobName = null;

    private FilePath ws = null;

    private EnvVars ev = null;

    private TaskListener listener=null;

    @Override
    public Result call() throws Exception {

        Result result=Result.SUCCESS;
        StringBuilder lentry=new StringBuilder();
        File ld = new File(this.ws.getRemote()+
                File.separator + this.buildId);
        File httpLog_f = new File(ld, Constants.HTTP_LOG);
        File bzmLog_f = new File(ld, Constants.BZM_LOG);
        FileUtils.touch(httpLog_f);
        FileUtils.touch(bzmLog_f);

        PrintStream bzmLog_str = new PrintStream(bzmLog_f);
        StdErrLog bzmLog = new StdErrLog(Constants.BZM_JEN);
        bzmLog.setStdErrStream(bzmLog_str);
        bzmLog.setDebugEnabled(true);

        PrintStream console_logger=this.listener.getLogger();
        StdErrLog consLog=new StdErrLog(Constants.BZM_JEN);
        consLog.setStdErrStream(console_logger);
        consLog.setDebugEnabled(true);

        HttpLoggingInterceptor.Logger httpLogger = new HttpLogger(httpLog_f.getAbsolutePath());
        HttpLoggingInterceptor httpLog = new HttpLoggingInterceptor(httpLogger);

        File mf = null;
        Api api = new ApiImpl(this.credential, this.serverUrl, httpLog, bzmLog,this.credLegacy);
        if (this.credLegacy) {
            lentry.append("==================================================================================================================================================");
            consLog.debug(lentry.toString());
            lentry.setLength(0);
            lentry.append("YOU'RE CURRENTLY USING LEGACY KEY WHICH IS DEPRECATED.");
            consLog.debug(lentry.toString());
            lentry.setLength(0);
            lentry.append("PLEASE, FOLLOW THE LINK FROM BELOW AND MIGRATE TO NEW API KEY.");
            consLog.debug(lentry.toString());
            lentry.setLength(0);
            lentry.append("https://guide.blazemeter.com/hc/en-us/articles/115002213289-BlazeMeter-API-keys");
            consLog.debug(lentry.toString());
            lentry.setLength(0);
            lentry.append("==================================================================================================================================================");
            consLog.debug(lentry.toString());
            lentry.setLength(0);
        }

        String userEmail = JobUtility.getUserEmail(api);
        if (userEmail.isEmpty()) {
            lentry.append("Please, check that credentials are valid.");
            bzmLog.info(lentry.toString());
            consLog.info(lentry.toString());
            lentry.setLength(0);
            lentry.append("Please, check that settings(credentials & serverUrl) are valid.");
            bzmLog.info(lentry.toString());
            consLog.info(lentry.toString());
            lentry.setLength(0);
            try {
                bzmLog.info(lentry.toString());
                lentry.setLength(0);
                ((HttpLogger) httpLogger).close();
            } catch (Exception e) {
                lentry.append(e);
                bzmLog.info(lentry.toString());
            }

            ProxyConfiguration proxy = ProxyConfiguration.load();
            if (proxy != null) {
                lentry.append("ProxyHost = " + proxy.name);
                bzmLog.info(lentry.toString());
                consLog.info(lentry.toString());
                lentry.setLength(0);

                lentry.append("ProxyPort = " + proxy.port);
                bzmLog.info(lentry.toString());
                consLog.info(lentry.toString());
                lentry.setLength(0);

                lentry.append("ProxyUser = " + proxy.getUserName());
                bzmLog.info(lentry.toString());
                consLog.info(lentry.toString());
                lentry.setLength(0);

                String proxyPass = proxy.getPassword();

                lentry.append("ProxyPass = " + (StringUtils.isBlank(proxyPass) ? "" : proxyPass.substring(0, ENCRYPT_CHARS_NUM)) + "...");
                bzmLog.info(lentry.toString());
                consLog.info(lentry.toString());
                lentry.setLength(0);
            }
            return Result.FAILURE;
        }

        lentry.append("BlazeMeter plugin version = " + JobUtility.version());
        bzmLog.info(lentry.toString());
        consLog.info(lentry.toString());
        lentry.setLength(0);

        lentry.append("User's e-mail = " + userEmail);
        bzmLog.info(lentry.toString());
        consLog.info(lentry.toString());
        lentry.setLength(0);

        String testId_num = Utils.getTestId(this.testId);

        HashMap<String,String> startTestResp=new HashMap<String, String>();
        String masterId = "";

        lentry.append("### About to start BlazeMeter test # " + testId_num);
        bzmLog.info(lentry.toString());
        consLog.info(lentry.toString());
        lentry.setLength(0);

        lentry.append("Timestamp: " + Calendar.getInstance().getTime());
        bzmLog.info(lentry.toString());
        consLog.info(lentry.toString());
        lentry.setLength(0);

        try {
            startTestResp = api.startMaster(testId_num);
            if (startTestResp.size()==0) {
                lentry.append("Server returned status = 500 while trying to start test.");
                consLog.warn(lentry.toString());
                lentry.setLength(0);
                return Result.FAILURE;
            }
            if(startTestResp.containsKey(JsonConsts.ERROR)){
                throw new NumberFormatException(startTestResp.get(JsonConsts.ERROR));
            }
            masterId=startTestResp.get(JsonConsts.ID);
            mf = new File(ld,masterId);
            FileUtils.touch(mf);
            Integer.parseInt(masterId);
        } catch (JSONException e) {
            lentry.append("Unable to start test: check userKey, testId, server url.");
            consLog.warn(lentry.toString()+e.getMessage());
            bzmLog.warn(lentry.toString(), e);
            lentry.setLength(0);
            ((HttpLogger) httpLogger).close();
            FileUtils.forceDelete(mf);
            return Result.FAILURE;
        }catch (NumberFormatException e) {
            lentry.append("Error while starting BlazeMeter Test: "+masterId+" "+e.getMessage());
            consLog.warn(lentry.toString());
            bzmLog.warn(lentry.toString());
            lentry.setLength(0);
            ((HttpLogger) httpLogger).close();
            FileUtils.forceDelete(mf);
            throw new Exception("Error while starting BlazeMeter Test: "+masterId+" "+e.getMessage());
        }
        catch (Exception e) {
            lentry.append("Unable to start test: check userKey, testId, server url.");
            consLog.warn(lentry.toString()+e.getMessage());
            bzmLog.warn(lentry.toString(), e);
            lentry.setLength(0);
            ((HttpLogger) httpLogger).close();
            FileUtils.forceDelete(mf);
            return Result.FAILURE;
        }


        lentry.append("Test ID = " + startTestResp.get(JsonConsts.TEST_ID));
        bzmLog.info(lentry.toString());
        consLog.info(lentry.toString());
        lentry.setLength(0);

        lentry.append("Test name = " + startTestResp.get(JsonConsts.NAME));
        bzmLog.info(lentry.toString());
        consLog.info(lentry.toString());
        lentry.setLength(0);


        ev.put(this.jobName+"-"+this.buildId+"-"+Constants.MASTER_ID,masterId);
        String reportUrl= JobUtility.getReportUrl(api, masterId, bzmLog);
        lentry.append("BlazeMeter test report will be available at " + reportUrl);
        consLog.info(lentry.toString());
        bzmLog.info(lentry.toString());
        lentry.setLength(0);

        consLog.info("For more detailed logs, please, refer to " + bzmLog_f.getCanonicalPath());
        consLog.info("Communication with BZM server is logged at " + httpLog_f.getCanonicalPath());

        EnvVars.masterEnvVars.put(this.jobName+"-"+this.buildId,reportUrl);
        JobUtility.notes(api, masterId, this.notes, bzmLog);
        try {
            if (!StringUtils.isBlank(this.sessionProperties)) {
                JSONArray props = JobUtility.prepareSessionProperties(this.sessionProperties, this.ev, bzmLog);
                JobUtility.properties(api, props, masterId, bzmLog);
            }
            JobUtility.waitForFinish(api, testId_num, bzmLog, masterId);
            lentry.append("BlazeMeter test# " + testId_num + " ended at " + Calendar.getInstance().getTime());
            consLog.info(lentry.toString());
            bzmLog.info(lentry.toString());
            lentry.setLength(0);
            result = JobUtility.postProcess(this.ws,
                    buildId,
                    api,
                    masterId,
                    this.ev,
                    this.getJunit,
                    this.junitPath,
                    this.getJtl,
                    this.jtlPath,
                    bzmLog,
                consLog);
            Thread.sleep(15000);//let master pull logs to browser
            return result;
        } catch (InterruptedException e) {
            lentry.append(LogEntries.JOB_WAS_STOPPED_BY_USER);
            consLog.warn(lentry.toString());
            bzmLog.warn(lentry.toString());
            lentry.setLength(0);
            result = Result.ABORTED;
            ((HttpLogger) httpLogger).close();
            FileUtils.forceDelete(mf);
            return result;
        } catch (Exception e) {
            lentry.append("Job was stopped due to unknown reason");
            consLog.warn(lentry.toString());
            bzmLog.warn(lentry.toString());
            lentry.setLength(0);
            result = Result.NOT_BUILT;
            return result;
        } finally {
            lentry.append("BlazeMeter test set result = "+result.toString());
            consLog.info(lentry.toString());
            bzmLog.info(lentry.toString());
            lentry.setLength(0);

            EnvVars.masterEnvVars.remove(this.jobName+"-"+this.buildId);
            TestStatus testStatus = api.getTestStatus(masterId);

            if (testStatus.equals(TestStatus.Running)) {
                lentry.append("Shutting down test");
                consLog.info(lentry.toString());
                bzmLog.info(lentry.toString());
                lentry.setLength(0);

                lentry.append(masterId+" is still running after finishing job post-process");
                consLog.info(lentry.toString());
                bzmLog.info(lentry.toString());
                lentry.setLength(0);

                lentry.append(masterId+" will be aborted");
                consLog.info(lentry.toString());
                bzmLog.info(lentry.toString());
                lentry.setLength(0);


                JobUtility.stopMaster(api, masterId);
                return Result.ABORTED;
            } else if (testStatus.equals(TestStatus.NotFound)) {
                lentry.append("Test not found error");
                consLog.info(lentry.toString());
                bzmLog.info(lentry.toString());
                lentry.setLength(0);
                return Result.FAILURE;
            } else if (testStatus.equals(TestStatus.Error)) {
                lentry.append("Test is not running on server. Check http-log & bzm-log for detailed errors");
                consLog.info(lentry.toString());
                bzmLog.info(lentry.toString());
                lentry.setLength(0);
                return Result.FAILURE;
            }
             console_logger.close();
            ((HttpLogger) httpLogger).close();
            FileUtils.forceDelete(mf);

        }
    }

    @Override
    public void checkRoles(RoleChecker roleChecker) throws SecurityException {

    }

    public void setEv(EnvVars ev) {
        this.ev = ev;
    }


    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getTestId() {
        return testId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }


    public void setNotes(String notes) {
        this.notes = notes;
    }


    public void setSessionProperties(String sessionProperties) {
        this.sessionProperties = sessionProperties;
    }


    public void setJtlPath(String jtlPath) {
        this.jtlPath = jtlPath;
    }


    public void setJunitPath(String junitPath) {
        this.junitPath = junitPath;
    }


    public void setGetJtl(boolean getJtl) {
        this.getJtl = getJtl;
    }


    public void setGetJunit(boolean getJunit) {
        this.getJunit = getJunit;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setWs(FilePath ws) {
        this.ws = ws;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public void setListener(TaskListener listener) {
        this.listener = listener;
    }

    public void setCredLegacy(final boolean credLegacy) {
        this.credLegacy = credLegacy;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }
}
