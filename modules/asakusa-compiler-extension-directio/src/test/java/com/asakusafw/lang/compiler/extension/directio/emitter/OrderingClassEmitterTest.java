package com.asakusafw.lang.compiler.extension.directio.emitter;

import static com.asakusafw.lang.compiler.model.description.Descriptions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import com.asakusafw.lang.compiler.api.reference.DataModelReference;
import com.asakusafw.lang.compiler.api.testing.MockDataModelLoader;
import com.asakusafw.lang.compiler.extension.directio.OutputPattern;
import com.asakusafw.lang.compiler.extension.directio.mock.MockData;
import com.asakusafw.lang.compiler.extension.directio.mock.MockDataFormat;
import com.asakusafw.lang.compiler.extension.directio.mock.WritableInputFormat;
import com.asakusafw.lang.compiler.javac.testing.JavaCompiler;
import com.asakusafw.lang.compiler.mapreduce.SourceInfo;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.runtime.directio.FilePattern;
import com.asakusafw.runtime.stage.directio.DirectOutputOrder;

/**
 * Test for {@link OrderingClassEmitter}.
 */
public class OrderingClassEmitterTest {

    /**
     * Java compiler for testing.
     */
    @Rule
    public final JavaCompiler javac = new JavaCompiler();

    /**
     * simple case.
     * @throws Exception if failed
     */
    @SuppressWarnings("deprecation")
    @Test
    public void simple() throws Exception {
        DataModelReference dataModel = MockDataModelLoader.load(MockData.class);
        OutputStageInfo.Operation operation = new OutputStageInfo.Operation(
                "testing",
                dataModel,
                Collections.singletonList(new SourceInfo(
                        "testing/input/*.txt",
                        classOf(MockData.class),
                        classOf(WritableInputFormat.class),
                        Collections.<String, String>emptyMap())),
                "testing/stage",
                OutputPattern.compile(dataModel, "testing/output/*.txt", Arrays.asList("+intValue", "-stringValue")),
                Collections.<FilePattern>emptyList(),
                classOf(MockDataFormat.class));

        ClassDescription targetClass = new ClassDescription("com.example.Ordering");
        OrderingClassEmitter.emit(targetClass, operation, javac);

        try (URLClassLoader loader = javac.load()) {
            Class<?> aClass = targetClass.resolve(loader);
            assertThat(aClass, is(typeCompatibleWith(DirectOutputOrder.class)));
            DirectOutputOrder o0 = aClass.asSubclass(DirectOutputOrder.class).newInstance();
            DirectOutputOrder o1 = aClass.asSubclass(DirectOutputOrder.class).newInstance();
            DirectOutputOrder o2 = aClass.asSubclass(DirectOutputOrder.class).newInstance();
            DirectOutputOrder o3 = aClass.asSubclass(DirectOutputOrder.class).newInstance();
            DirectOutputOrder o4 = aClass.asSubclass(DirectOutputOrder.class).newInstance();

            MockData data = new MockData();
            data.getIntValueOption().modify(100);
            data.getStringValueOption().modify("Hello, world!");
            o0.set(data);
            o1.set(data); // equivalent

            data.getIntValueOption().modify(99);
            o2.set(data); // intValue is smaller

            data.getIntValueOption().modify(101);
            o3.set(data); // intValue is larger

            data.getIntValueOption().modify(100);
            data.getStringValueOption().modify("Hello, world");
            o4.set(data); // stringValue is smaller

            assertThat(o0.compareTo(o1), is(0));
            assertThat(o0.compareTo(o2), is(+1));
            assertThat(o0.compareTo(o3), is(-1));
            assertThat(o0.compareTo(o4), is(-1));
        }
    }
}
