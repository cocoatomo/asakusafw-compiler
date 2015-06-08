/**
 * Copyright 2011-2015 Asakusa Framework Team.
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
package com.asakusafw.bridge.hadoop;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.util.ReflectionUtils;

import com.asakusafw.runtime.compatibility.JobCompatibility;
import com.asakusafw.runtime.io.util.DataBuffer;

/**
 * Testing utilities for {@link InputFormat}.
 */
public class InputFormatTester {

    private final InputFormat<?, ?> format;

    private final Configuration conf;

    /**
     * Creates a new instance.
     * @param conf the current configuration
     * @param format the target format object
     */
    public InputFormatTester(Configuration conf, InputFormat<?, ?> format) {
        this.conf = conf;
        this.format = format;
    }

    /**
     * Creates a new instance.
     * @param conf the current configuration
     * @param format the target format class
     */
    public InputFormatTester(Configuration conf, Class<?> format) {
        this.conf = conf;
        this.format = (InputFormat<?, ?>) ReflectionUtils.newInstance(format, conf);
    }

    /**
     * Collects input contents.
     * @param <T> the data type
     * @param collector the target collector
     * @throws IOException if failed
     * @throws InterruptedException if interrupted
     */
    @SuppressWarnings("unchecked")
    public <T> void collect(Collector<T> collector) throws IOException, InterruptedException {
        TaskAttemptID id = JobCompatibility.newTaskAttemptId(JobCompatibility.newTaskId(JobCompatibility.newJobId()));
        TaskAttemptContext context = JobCompatibility.newTaskAttemptContext(conf, id);
        List<InputSplit> splits = format.getSplits(context);
        for (InputSplit split : splits) {
            InputSplit restored = restore(split);
            try (RecordReader<?, ?> reader = format.createRecordReader(restored, context)) {
                reader.initialize(restored, context);
                while (reader.nextKeyValue()) {
                    assertThat(reader.getCurrentKey(), is(notNullValue()));
                    assertThat(reader.getProgress(), is(lessThanOrEqualTo(1.0f)));
                    collector.handle((T) reader.getCurrentValue());
                }
            }
        }
    }

    private InputSplit restore(InputSplit split) throws IOException {
        if (split instanceof Writable) {
            DataBuffer buffer = new DataBuffer();
            ((Writable) split).write(buffer);
            ((Writable) split).readFields(buffer);
        }
        return split;
    }

    /**
     * Collects
     * @param <T> the target data type
     */
    public interface Collector<T> {

        /**
         * Handles an object.
         * @param object the target
         */
        void handle(T object);
    }
}