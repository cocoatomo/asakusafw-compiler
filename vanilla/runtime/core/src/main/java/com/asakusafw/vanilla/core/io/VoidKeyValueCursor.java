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
package com.asakusafw.vanilla.core.io;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A {@link KeyValueCursor} which provides nothing.
 * @since 0.4.0
 */
public class VoidKeyValueCursor implements KeyValueCursor {

    @Override
    public boolean next() throws IOException, InterruptedException {
        return false;
    }

    @Override
    public ByteBuffer getKey() throws IOException, InterruptedException {
        throw new IllegalStateException();
    }

    @Override
    public ByteBuffer getValue() throws IOException, InterruptedException {
        throw new IllegalStateException();
    }

    @Override
    public void close() throws IOException, InterruptedException {
        return;
    }
}
