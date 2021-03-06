/**
 * Copyright 2011-2017 Asakusa Framework Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.asakusafw.lang.compiler.testdriver.adapter;

import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.runtime.stage.StageConstants;
import com.asakusafw.testdriver.compiler.CompilerConstants;

final class Util {

    private static final String WILDCARD_SUFFIX = "-*";

    private Util() {
        return;
    }

    public static String createInputPath(String name) {
        return Location.of(CompilerConstants.getRuntimeWorkingDirectory())
                .append(StageConstants.EXPR_EXECUTION_ID)
                .append("input") //$NON-NLS-1$
                .append(normalize(name) + WILDCARD_SUFFIX)
                .toPath();
    }

    public static String createOutputPath(String name) {
        return Location.of(CompilerConstants.getRuntimeWorkingDirectory())
                .append(StageConstants.EXPR_EXECUTION_ID)
                .append("output") //$NON-NLS-1$
                .append(normalize(name) + WILDCARD_SUFFIX)
                .toPath();
    }

    public static String createStagePath() {
        return Location.of(CompilerConstants.getRuntimeWorkingDirectory())
                .append(StageConstants.EXPR_EXECUTION_ID)
                .append("stage") //$NON-NLS-1$
                .toPath();
    }

    private static String normalize(String name) {
        // Hadoop MultipleInputs/Outputs only can accept alphameric characters
        StringBuilder buf = new StringBuilder();
        for (char c : name.toCharArray()) {
            // we use '0' as escape symbol
            if ('1' <= c && c <= '9' || 'A' <= c && c <= 'Z' || 'a' <= c && c <= 'z') {
                buf.append(c);
            } else if (c <= 0xff) {
                buf.append('0');
                buf.append(String.format("%02x", (int) c)); //$NON-NLS-1$
            } else {
                buf.append("0u"); //$NON-NLS-1$
                buf.append(String.format("%04x", (int) c)); //$NON-NLS-1$
            }
        }
        return buf.toString();
    }
}
