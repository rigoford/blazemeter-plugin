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

package hudson.plugins.blazemeter.api;

import hudson.plugins.blazemeter.utils.Constants;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import org.apache.commons.lang.StringUtils;

public class CutCredentialsFormatter extends SimpleFormatter {

    @Override
    public synchronized String format(LogRecord record) {
        String logEntry = super.format(record);
        int authorization = logEntry.lastIndexOf(Api.AUTHORIZATION) + 1;
        if (authorization > 0) {
            String keyToReplace = logEntry.substring(authorization + 13, logEntry.length() - 10);
            return StringUtils.replace(logEntry, keyToReplace, Constants.SPACE_THREE_DOTS);
        }
        int apiKey = logEntry.lastIndexOf(Api.X_API_KEY) + 1;
        if (apiKey > 0) {
            String keyToReplace = logEntry.substring(apiKey + 13, apiKey + 28);
            return StringUtils.replace(logEntry, keyToReplace, Constants.SPACE_THREE_DOTS);
        }
        return logEntry;
    }
}
