/**
 * Copyright 2016 BlazeMeter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hudson.plugins.blazemeter;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.model.Result;
import hudson.plugins.blazemeter.api.Api;
import hudson.plugins.blazemeter.api.ApiImpl;
import hudson.plugins.blazemeter.entities.CIStatus;
import hudson.plugins.blazemeter.utils.Constants;
import hudson.plugins.blazemeter.utils.JobUtility;
import hudson.util.FormValidation;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import javax.mail.MessagingException;
import okhttp3.Credentials;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.log.StdErrLog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

@Ignore("Junit tests are hanging.JobUtility.java refactoring is needed")
public class TestJobUtility {
    private static StdErrLog stdErrLog = Mockito.mock(StdErrLog.class);

    @BeforeClass
    public static void setUp() throws IOException {
        MockedAPI.startAPI();
        MockedAPI.userProfile();
        MockedAPI.stopMaster();
        MockedAPI.getMasterStatus();
        MockedAPI.getCIStatus();
        MockedAPI.getReportUrl();
        MockedAPI.getTests();
        MockedAPI.notes();
        MockedAPI.getTestReport();
        MockedAPI.jtl();
        MockedAPI.getListOfSessionIds();
        MockedAPI.junit();
        MockedAPI.jtl_zip();
        MockedAPI.properties();

    }

    @AfterClass
    public static void tearDown() {
        MockedAPI.stopAPI();
    }

    @Test
    public void getUserEmail_positive() throws IOException, JSONException {
        ApiImpl api = new ApiImpl(TestConstants.MOCK_VALID_CR,TestConstants.mockedApiUrl,false);
        String email = JobUtility.getUserEmail(api);
        Assert.assertEquals(email, "dzmitry.kashlach@blazemeter.com");
    }

    @Test
    public void getUserEmail_negative() throws IOException, JSONException {
        ApiImpl api = new ApiImpl(TestConstants.MOCK_INVALID_CR,TestConstants.mockedApiUrl,false);
        String email = JobUtility.getUserEmail(api);
        Assert.assertEquals(email, "");
    }

    @Test
    public void getUserEmail_exception() throws IOException, JSONException {
        ApiImpl api = new ApiImpl(TestConstants.MOCK_EXCEPTION_CR,TestConstants.mockedApiUrl,false);
        String email = JobUtility.getUserEmail(api);
        Assert.assertEquals(email, "");
    }

    @Test
    public void validateUserKey_positive() throws IOException, JSONException {
        String cred = Credentials.basic(TestConstants.MOCK_VALID_USER, TestConstants.MOCK_VALID_PASSWORD);
        Api api = new ApiImpl(cred, TestConstants.mockedApiUrl, false);
        FormValidation validation = JobUtility.validateCredentials(api);
        Assert.assertEquals(validation.kind, FormValidation.Kind.OK);
        Assert.assertEquals(validation.getMessage(), Constants.CRED_ARE_VALID + "dzmitry.kashlach@blazemeter.com");
    }

    @Test
    public void validateUserKey_negative() throws IOException, JSONException {
        String cred = Credentials.basic(TestConstants.MOCK_INVALID_USER, TestConstants.MOCK_INVALID_PASSWORD);
        Api api = new ApiImpl(cred, TestConstants.mockedApiUrl, false);
        FormValidation validation = JobUtility.validateCredentials(api);
        Assert.assertEquals(validation.kind, FormValidation.Kind.ERROR);
    }

    @Test
    public void validateUserKey_exception() throws IOException, JSONException {
        String cred = Credentials.basic(TestConstants.MOCK_EXCEPTION_USER,
            TestConstants.MOCK_EXCEPTION_PASSWORD);
        Api api = new ApiImpl(cred, TestConstants.mockedApiUrl, false);
        FormValidation validation = JobUtility.validateCredentials(api);
        Assert.assertEquals(validation.kind, FormValidation.Kind.ERROR);
        Assert.assertEquals(validation.getMessage(),
            "Credentials are not valid: unexpected exception = A JSONObject text must begin with '{' at character 1");
    }

    @Test
    public void validateUserKey_empty() throws IOException, JSONException {
        String cred = Credentials.basic(TestConstants.MOCK_EMPTY_USER,
            TestConstants.MOCK_EMPTY_PASSWORD);
        Api api = new ApiImpl(cred, TestConstants.mockedApiUrl, false);
        FormValidation validation = JobUtility.validateCredentials(api);
        Assert.assertEquals(validation.kind, FormValidation.Kind.ERROR);
        Assert.assertEquals(validation.getMessage(), Constants.CRED_EMPTY);
    }


    @Test
    public void getVersion() throws IOException,JSONException {
        String version= JobUtility.version();
        Assert.assertTrue(version.matches("^(\\d{1,}\\.+\\d{1,2}\\S*)$"));
    }


    @Test
    public void stopMaster(){
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        try{

        boolean terminate = JobUtility.stopMaster(api, TestConstants.TEST_MASTER_25);
        Assert.assertEquals(terminate, true);
        terminate = JobUtility.stopMaster(api, TestConstants.TEST_MASTER_70);
        Assert.assertEquals(terminate, true);
        terminate = JobUtility.stopMaster(api, TestConstants.TEST_MASTER_100);
        Assert.assertEquals(terminate, false);
        terminate = JobUtility.stopMaster(api, TestConstants.TEST_MASTER_140);
        Assert.assertEquals(terminate, false);
        }catch (Exception e){
            Assert.fail();
        }

    }


    @Test
    public void getCIStatus_success(){
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        CIStatus ciStatus= JobUtility.validateCIStatus(api, TestConstants.TEST_MASTER_SUCCESS, stdErrLog, stdErrLog);
        Assert.assertEquals(CIStatus.success,ciStatus);
    }



    @Test
    public void getCIStatus_failure(){
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        CIStatus ciStatus= JobUtility.validateCIStatus(api, TestConstants.TEST_MASTER_FAILURE, stdErrLog, stdErrLog);
        Assert.assertEquals(CIStatus.failures,ciStatus);
    }



    @Test
    public void getCIStatus_error_61700(){
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        CIStatus ciStatus= JobUtility.validateCIStatus(api, TestConstants.TEST_MASTER_ERROR_61700, stdErrLog, stdErrLog);
        Assert.assertEquals(CIStatus.errors,ciStatus);
    }



    @Test
    public void getCIStatus_error_0(){
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        CIStatus ciStatus= JobUtility.validateCIStatus(api, TestConstants.TEST_MASTER_ERROR_0, stdErrLog, stdErrLog);
        Assert.assertEquals(CIStatus.failures,ciStatus);
    }

    @Test
    public void getCIStatus_error_70404() {
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        CIStatus ciStatus = JobUtility.validateCIStatus(api, TestConstants.TEST_MASTER_ERROR_70404, stdErrLog, stdErrLog);
        Assert.assertEquals(CIStatus.failures, ciStatus);
    }

    @Test
    public void getReportUrl_pos(){

        String expectedReportUrl=TestConstants.mockedApiUrl+"/app/?public-token=ohImO6c8xstG4qBFqgRnsMSAluCBambtrqsTvAEYEXItmrCfgO#masters/testMasterId/summary";
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        String actReportUrl= null;
        try {
            actReportUrl = JobUtility.getReportUrl(api, TestConstants.TEST_MASTER_ID, stdErrLog);
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(expectedReportUrl,actReportUrl);
    }



    @Test
    public void getReportUrl_neg(){
        String expectedReportUrl=TestConstants.mockedApiUrl+"/app/#masters/testMasterId/summary";
        Api api = new ApiImpl(TestConstants.MOCK_INVALID_CR, TestConstants.mockedApiUrl,false);
        String actReportUrl= null;
        try {
            actReportUrl = JobUtility.getReportUrl(api, TestConstants.TEST_MASTER_ID, stdErrLog);
        } catch (Exception e) {
            Assert.fail();
        }
        Assert.assertEquals(expectedReportUrl,actReportUrl);
    }

/*

    @Test
    public void collection_true(){
        ApiImpl api=new ApiImpl(TestConstants.MOCK_VALID_CR,TestConstants.mockedApiUrl,false);

        try {
            Assert.assertTrue(JobUtility.collection(TestConstants.TEST_5039530_ID,TestConstants.TEST_WORKSPACE_ID,api));
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (MessagingException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Test
    public void collection_false(){
        try {
            ApiImpl api=new ApiImpl(TestConstants.MOCK_VALID_CR,TestConstants.mockedApiUrl,false);
            Assert.assertFalse(JobUtility.collection(TestConstants.TEST_5075679_ID,TestConstants.TEST_WORKSPACE_ID,api));
        } catch (JSONException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }catch (MessagingException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

*/


    @Test
    public void getSessionId() throws JSONException, IOException {
        File getSessionId_v3=new File(TestConstants.RESOURCES+"/getSessionId_v3.json");
        String getSessionId_v3_str= FileUtils.readFileToString(getSessionId_v3);
        JSONObject getSession_json=new JSONObject(getSessionId_v3_str);
        String session= JobUtility.getSessionId(getSession_json, stdErrLog);
        Assert.assertEquals(session,"r-v3-55a6136b314bd");
    }



    @Test
    public void errorsFailed_true_0() throws JSONException, IOException {
        File error_0=new File(TestConstants.RESOURCES+ "/ciStatus_error_0.json");
        String error_0_str= FileUtils.readFileToString(error_0);
        JSONArray error_0_json=new JSONArray(error_0_str);
        Assert.assertTrue(JobUtility.errorsFailed(error_0_json));
    }

    @Test
    public void errorsFailed_true_70404() throws JSONException, IOException {
        File error=new File(TestConstants.RESOURCES+ "/ciStatus_error_70404.json");
        String error_str=FileUtils.readFileToString(error);
        JSONArray error_json=new JSONArray(error_str);
        Assert.assertTrue(JobUtility.errorsFailed(error_json));
    }

    @Test
    public void errorsFailed_false_61700() throws JSONException, IOException {
        File error=new File(TestConstants.RESOURCES+ "/ciStatus_error_61700.json");
        String error_str=FileUtils.readFileToString(error);
        JSONArray error_json=new JSONArray(error_str);
        Assert.assertFalse(JobUtility.errorsFailed(error_json));
    }

    @Test
    public void prepareProps() throws JSONException {
        String prps = "v=r,v=i";
        JSONArray arr = JobUtility.prepareSessionProperties(prps, new EnvVars(), stdErrLog);
        Assert.assertTrue(arr.length()==2);
    }



    @Test
    public void notes(){
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        boolean notes=JobUtility.notes(api,TestConstants.TEST_MASTER_100_notes,"bbbbbbbbbbbbbbbbbbbbb",stdErrLog);
        Assert.assertTrue(notes);
    }

    @Test
    public void agReport() {
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        String ar = JobUtility.requestReport(api, TestConstants.TEST_MASTER_ID, stdErrLog, stdErrLog);
        Assert.assertTrue(ar.length() == 105);
    }

    @Test
    public void jtlUrls() {
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        HashMap<String, String> sessions = JobUtility.jtlUrls(api, TestConstants.TEST_MASTER_ID, stdErrLog, stdErrLog);
        Assert.assertTrue(sessions.size() == 1);
        Assert.assertEquals(sessions.get(TestConstants.MOCKED_SESSION), TestConstants.JTL_URL);
    }


    @Test
    public void retrieveJUNITXMLreport() {
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        FilePath fp = new FilePath(new File(System.getProperty("user.dir") + "/junit"));
        try {
            fp.mkdirs();
            JobUtility.retrieveJUNITXMLreport(api, TestConstants.TEST_MASTER_ID, fp, stdErrLog, stdErrLog);
            Assert.assertTrue(fp.exists());
            Assert.assertTrue(fp.list().size()==1);
            Assert.assertFalse(StringUtils.isBlank(fp.list().get(0).readToString()));
            fp.deleteRecursive();
            Assert.assertFalse(fp.exists());
        } catch (IOException e) {
            Assert.fail();
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }


    @Test
    public void downloadJtlReport() {
        String dataUrl = TestConstants.mockedApiUrl + "/users/1689/tests/5283127/reports/r-v3-585114ca535ed/jtls_and_more.zip";
        FilePath fp = new FilePath(new File(System.getProperty("user.dir") + "/jtl"));
        try {
            fp.mkdirs();
            JobUtility.downloadJtlReport(TestConstants.MOCKED_SESSION, dataUrl, fp, stdErrLog, stdErrLog);
            Assert.assertTrue(fp.list().size() == 1);
            Assert.assertTrue(fp.list().get(0).getName().equals("jtl"));
            fp.deleteRecursive();
        } catch (IOException ioe) {
            Assert.fail();
        } catch (InterruptedException ie) {
            Assert.fail();
        }

    }

    @Test
    public void properties() {
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        String prps = "v=r,v=i";
        JSONArray arr=null;
        try {
            arr = JobUtility.prepareSessionProperties(prps, new EnvVars(), stdErrLog);
            boolean submit=JobUtility.properties(api,arr,TestConstants.TEST_MASTER_ID,stdErrLog);
            Assert.assertTrue(submit);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void postProcess_success(){
        FilePath fp = new FilePath(new File(System.getProperty("user.dir") + "/jtl"));
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        try {
            Result r=JobUtility.postProcess(fp,"1",api,TestConstants.TEST_MASTER_SUCCESS,new EnvVars(),false,"",false,"",stdErrLog,stdErrLog);
            Assert.assertEquals(Result.SUCCESS,r);
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void postProcess_failure() {
        FilePath fp = new FilePath(new File(System.getProperty("user.dir") + "/jtl"));
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        try {
            Result r = JobUtility.postProcess(fp, "1", api, TestConstants.TEST_MASTER_FAILURE, new EnvVars(), false, "", false, "", stdErrLog, stdErrLog);
            Assert.assertEquals(Result.FAILURE, r);
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

    @Test
    public void postProcess_failure_junit() {
        FilePath fp = new FilePath(new File(System.getProperty("user.dir") + "/jtl"));
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        try {
            Result r = JobUtility.postProcess(fp, "1", api, TestConstants.TEST_MASTER_FAILURE, new EnvVars(), true, "junit", false, "", stdErrLog, stdErrLog);
            Assert.assertEquals(Result.FAILURE, r);
            Assert.assertTrue(fp.list().size() == 1);
            Assert.assertTrue(fp.list().get(0).getName().equals("1"));
            fp.deleteRecursive();
            Assert.assertFalse(fp.exists());
        } catch (InterruptedException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void postProcess_success_jtl() {
        FilePath fp = new FilePath(new File(System.getProperty("user.dir") + "/jtl"));
        Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
        try {
            Result r = JobUtility.postProcess(fp, "1", api, TestConstants.TEST_MASTER_SUCCESS, new EnvVars(), false, "", true, "jtl", stdErrLog, stdErrLog);
            Assert.assertEquals(Result.SUCCESS, r);
            Assert.assertTrue(fp.list().size() == 1);
            Assert.assertTrue(fp.list().get(0).getName().equals("1"));
            Assert.assertTrue(fp.list().get(0).list().get(0).list().size() == 1);
            fp.deleteRecursive();
            Assert.assertFalse(fp.exists());
        } catch (InterruptedException e) {
            Assert.fail();
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void waitForFinish() {
        Date start = Calendar.getInstance().getTime();
        try {
            Api api = new ApiImpl(TestConstants.MOCK_VALID_CR, TestConstants.mockedApiUrl,/*TODO*/false);
            JobUtility.waitForFinish(api, "1", stdErrLog, TestConstants.TEST_MASTER_WAIT_FOR_FINISH);
            long after = Calendar.getInstance().getTime().getTime();
            long diffInSec = (after - start.getTime()) / 1000;
            Assert.assertTrue(diffInSec > 25);
            Assert.assertTrue(diffInSec < 35);
        } catch (InterruptedException e) {
            Assert.fail();
        }
    }

}
