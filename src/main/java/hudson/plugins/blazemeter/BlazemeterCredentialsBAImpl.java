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

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import javax.annotation.CheckForNull;
import javax.validation.constraints.NotNull;
import hudson.Extension;
import hudson.Extension;
import hudson.Util;
import hudson.plugins.blazemeter.api.Api;
import hudson.plugins.blazemeter.api.ApiImpl;
import hudson.plugins.blazemeter.utils.JobUtility;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import net.sf.json.JSONException;
import okhttp3.Credentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

@SuppressWarnings("unused") // read resolved by extension plugins
public class BlazemeterCredentialsBAImpl extends BaseStandardCredentials implements BlazemeterCredentials, StandardUsernamePasswordCredentials {

    public static BlazemeterCredentialsBAImpl EMPTY = new BlazemeterCredentialsBAImpl(CredentialsScope.GLOBAL, "", "", "", "");
    /**
     * The username.
     */
    @NotNull
    private final String username;

    /**
     * The password.
     */
    @NotNull
    private final Secret password;

    /**
     * Constructor.
     *
     * @param scope       the credentials scope
     * @param id          the ID or {@code null} to generate a new one.
     * @param description the description.
     * @param username    the username.
     * @param password    the password.
     */
    @DataBoundConstructor
    @SuppressWarnings("unused") // by stapler
    public BlazemeterCredentialsBAImpl(@CheckForNull CredentialsScope scope,
        @CheckForNull String id, @CheckForNull String description,
        @CheckForNull String username, @CheckForNull String password) {
        super(scope, id, description);
        this.username = Util.fixNull(username);
        this.password = Secret.fromString(password);
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public Secret getPassword() {
        return password;
    }

    /**
     * {@inheritDoc}
     */
    @NotNull
    public String getUsername() {
        return username;
    }

    /**
     * {@inheritDoc}
     */
    @Extension(ordinal = 1)
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDisplayName() {
            return Messages.BlazemeterCredential_DisplayName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getIconClassName() {
            return "icon-credentials-userpass";
        }

        public FormValidation doTestConnection(@QueryParameter("username") final String username, @QueryParameter("password") final String password)
            throws MessagingException, IOException, JSONException, ServletException {
            String plainPass = null;
            Secret decrPassword = Secret.fromString(password);
            try {
                plainPass = decrPassword.getPlainText();
            } catch (NullPointerException npe) {
                return FormValidation.error("Failed to decrypt password to plain text");
            }
            String serverUrl = BlazeMeterPerformanceBuilderDescriptor.getDescriptor().getBlazeMeterURL();
            String cred = "";
            cred = Credentials.basic(username, plainPass);
            Api api = new ApiImpl(cred, serverUrl, false);
            return JobUtility.validateCredentials(api);
        }

    }
}
