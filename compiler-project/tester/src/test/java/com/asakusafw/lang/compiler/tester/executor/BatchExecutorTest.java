/**
 * Copyright 2011-2016 Asakusa Framework Team.
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
package com.asakusafw.lang.compiler.tester.executor;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.asakusafw.lang.compiler.api.basic.BasicBatchReference;
import com.asakusafw.lang.compiler.api.basic.BasicJobflowReference;
import com.asakusafw.lang.compiler.api.basic.TaskContainerMap;
import com.asakusafw.lang.compiler.api.reference.CommandTaskReference;
import com.asakusafw.lang.compiler.api.reference.CommandToken;
import com.asakusafw.lang.compiler.api.reference.JobflowReference;
import com.asakusafw.lang.compiler.api.reference.TaskReference;
import com.asakusafw.lang.compiler.common.Location;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.info.BatchInfo;
import com.asakusafw.lang.compiler.model.info.JobflowInfo;
import com.asakusafw.lang.compiler.tester.BatchArtifact;
import com.asakusafw.lang.compiler.tester.ExternalPortMap;
import com.asakusafw.lang.compiler.tester.JobflowArtifact;
import com.asakusafw.lang.compiler.tester.TesterContext;
import com.asakusafw.lang.compiler.tester.executor.BatchExecutor.Action;
import com.asakusafw.lang.compiler.tester.executor.BatchExecutor.Context;

/**
 * Test for {@link BatchExecutor}.
 */
public class BatchExecutorTest {

    /**
     * temporary folder for testing.
     */
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    /**
     * simple case.
     * @throws Exception if failed
     */
    @Test
    public void simple() throws Exception {
        DummyTaskExecutor tracker = new DummyTaskExecutor();
        BatchExecutor executor = new BatchExecutor(new JobflowExecutor(Collections.singletonList(tracker)));

        TaskReference t0 = task("t0");
        JobflowReference j0 = jobflow(t0);
        BatchArtifact artifact = batch(j0);

        executor.execute(context(), artifact);

        List<TaskReference> results = tracker.getTasks();
        assertThat(results, containsInAnyOrder(t0));
    }

    /**
     * tasks w/ dependencies.
     * @throws Exception if failed
     */
    @Test
    public void dependencies() throws Exception {
        DummyTaskExecutor tracker = new DummyTaskExecutor();
        BatchExecutor executor = new BatchExecutor(new JobflowExecutor(Collections.singletonList(tracker)));

        TaskReference t0 = task("t0");
        TaskReference t1 = task("t1");
        TaskReference t2 = task("t2");
        TaskReference t3 = task("t3");
        JobflowReference j0 = jobflow(t0);
        JobflowReference j1 = jobflow(t1, j0);
        JobflowReference j2 = jobflow(t2, j0);
        JobflowReference j3 = jobflow(t3, j1, j2);
        BatchArtifact artifact = batch(j0, j1, j2, j3);

        executor.execute(context(), artifact);

        List<TaskReference> results = tracker.getTasks();
        assertThat(results, hasSize(4));
        assertThat(results, containsInAnyOrder(t0, t1, t2, t3));
        checkOrder(results, t0, t1);
        checkOrder(results, t0, t2);
        checkOrder(results, t1, t3);
        checkOrder(results, t2, t3);
    }

    /**
     * w/ hook actions.
     * @throws Exception if failed
     */
    @Test
    public void hooks() throws Exception {
        final DummyTaskExecutor tracker = new DummyTaskExecutor();
        final AtomicBoolean sawBefore = new AtomicBoolean();
        final AtomicBoolean sawAfter = new AtomicBoolean();
        BatchExecutor executor = new BatchExecutor(new JobflowExecutor(Collections.singletonList(tracker)));

        TaskReference t0 = task("t0");
        JobflowReference j0 = jobflow(t0);
        BatchArtifact artifact = batch(j0);

        executor.withBefore(new Action() {
            @Override
            public void perform(Context context, BatchArtifact a) {
                assertThat(tracker.getTasks(), is(empty()));
                assertThat(sawBefore.get(), is(false));
                sawBefore.set(true);
            }
        }).withAfter(new Action() {
            @Override
            public void perform(Context context, BatchArtifact a) {
                assertThat(tracker.getTasks(), is(not(empty())));
                assertThat(sawAfter.get(), is(false));
                sawAfter.set(true);
            }
        }).execute(context(), artifact);

        assertThat(sawBefore.get(), is(true));
        assertThat(sawAfter.get(), is(true));
    }

    private void checkOrder(List<TaskReference> tasks, TaskReference pred, TaskReference succ) {
        int predIndex = tasks.indexOf(pred);
        int succIndex = tasks.indexOf(succ);
        assertThat(predIndex, is(greaterThanOrEqualTo(0)));
        assertThat(succIndex, is(greaterThanOrEqualTo(0)));
        assertThat(predIndex, is(lessThan(succIndex)));
    }

    private BatchArtifact batch(JobflowReference... jobflows) {
        BasicBatchReference reference = new BasicBatchReference(
                new BatchInfo.Basic("BID", new ClassDescription("BID")),
                Arrays.asList(jobflows));
        List<JobflowArtifact> elements = new ArrayList<>();
        for (JobflowReference jobflow : jobflows) {
            elements.add(new JobflowArtifact(reference, jobflow, new ExternalPortMap()));
        }
        BatchArtifact result = new BatchArtifact(reference, elements);
        return result;
    }

    private JobflowReference jobflow(TaskReference task, JobflowReference... blockers) {
        String id = ((CommandTaskReference) task).getCommand().toPath();
        TaskContainerMap tasks = new TaskContainerMap();
        tasks.getMainTaskContainer().add(task);
        return new BasicJobflowReference(
                new JobflowInfo.Basic(id, new ClassDescription(id)),
                tasks,
                Arrays.asList(blockers));
    }

    private TaskReference task(String id) {
        return new CommandTaskReference(
                "testing",
                "testing",
                Location.of(id),
                Collections.<CommandToken>emptyList(),
                Collections.<String>emptySet(),
                Collections.<TaskReference>emptyList());
    }


    private TesterContext context() {
        try {
            return context(folder.newFolder());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private TesterContext context(File home) {
        return new TesterContext(getClass().getClassLoader(), Collections.<String, String>emptyMap());
    }
}
