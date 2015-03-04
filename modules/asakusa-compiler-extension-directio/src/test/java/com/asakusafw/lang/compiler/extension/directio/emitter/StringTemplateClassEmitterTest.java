package com.asakusafw.lang.compiler.extension.directio.emitter;

import static com.asakusafw.lang.compiler.model.description.Descriptions.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.net.URLClassLoader;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.asakusafw.runtime.stage.directio.StringTemplate;

/**
 * Test for {@link StringTemplateClassEmitter}.
 */
public class StringTemplateClassEmitterTest {

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
                OutputPattern.compile(dataModel, "output/{intValue}/[100..999].txt"),
                Collections.<FilePattern>emptyList(),
                classOf(MockDataFormat.class));

        ClassDescription targetClass = new ClassDescription("com.example.Template");
        StringTemplateClassEmitter.emit(targetClass, operation, javac);


        Pattern pattern = Pattern.compile("output/(\\d+)/[1-9]\\d{2}\\.txt$");
        try (URLClassLoader loader = javac.load()) {
            Class<?> aClass = targetClass.resolve(loader);
            assertThat(aClass, is(typeCompatibleWith(StringTemplate.class)));
            StringTemplate template = aClass.asSubclass(StringTemplate.class).newInstance();

            MockData data = new MockData();
            data.getIntValueOption().modify(12345);
            template.set(data);

            Matcher m0 = pattern.matcher(template.apply());
            assertThat(m0.matches(), is(true));
            assertThat(m0.group(1), is("12345"));

            data.getIntValueOption().modify(123456);
            template.set(data);

            Matcher m1 = pattern.matcher(template.apply());
            assertThat(m1.matches(), is(true));
            assertThat(m1.group(1), is("123456"));
        }
    }
}
