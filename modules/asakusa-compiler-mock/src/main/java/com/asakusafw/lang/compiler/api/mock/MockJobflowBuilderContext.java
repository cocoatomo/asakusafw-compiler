package com.asakusafw.lang.compiler.api.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.asakusafw.lang.compiler.api.CompilerOptions;
import com.asakusafw.lang.compiler.api.DataModelLoader;
import com.asakusafw.lang.compiler.api.basic.AbstractJobflowBuilderContext;
import com.asakusafw.lang.compiler.api.reference.ExternalInputReference;
import com.asakusafw.lang.compiler.api.reference.ExternalOutputReference;
import com.asakusafw.lang.compiler.model.Location;
import com.asakusafw.lang.compiler.model.description.ClassDescription;
import com.asakusafw.lang.compiler.model.graph.ExternalInput;

/**
 * Mock implementation of {@link com.asakusafw.lang.compiler.api.JobflowBuilder.Context}.
 */
public class MockJobflowBuilderContext extends AbstractJobflowBuilderContext {

    /**
     * Returns the base path of {@link #addExternalInput(String, ClassDescription) external inputs}.
     * The actual path will be follow its {@link ExternalInput#getName() name} after this prefix,
     * and it is relative from {@link CompilerOptions#getRuntimeWorkingDirectory()}.
     */
    public static final String EXTERNAL_INPUT_BASE = "extenal/input/"; //$NON-NLS-1$

    private final CompilerOptions options;

    private final ClassLoader classLoader;

    private final DataModelLoader dataModelLoader;

    private final File outputDirectory;

    private final Map<Class<?>, Object> extensions = new HashMap<>();

    /**
     * Creates a new instance w/ using {@link MockDataModelLoader}.
     * @param options the compiler options
     * @param classLoader the target application loader
     * @param outputDirectory the build output directory
     * @see #addExtension(Class, Object)
     */
    public MockJobflowBuilderContext(
            CompilerOptions options,
            ClassLoader classLoader,
            File outputDirectory) {
        this(options, classLoader, new MockDataModelLoader(classLoader), outputDirectory);
    }

    /**
     * Creates a new instance.
     * @param options the compiler options
     * @param classLoader the target application loader
     * @param dataModelLoader the data model loader
     * @param outputDirectory the build output directory
     * @see #addExtension(Class, Object)
     */
    public MockJobflowBuilderContext(
            CompilerOptions options,
            ClassLoader classLoader,
            DataModelLoader dataModelLoader,
            File outputDirectory) {
        this.options = options;
        this.classLoader = classLoader;
        this.dataModelLoader = dataModelLoader;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public CompilerOptions getOptions() {
        return options;
    }

    @Override
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public DataModelLoader getDataModelLoader() {
        return dataModelLoader;
    }

    /**
     * Returns the base output directory in this context.
     * @return the base output directory
     * @see #addClassFile(ClassDescription)
     * @see #addResourceFile(Location)
     */
    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public OutputStream addResourceFile(Location location) throws IOException {
        File file = new File(outputDirectory, location.toPath(File.separatorChar)).getAbsoluteFile();
        if (file.exists()) {
            throw new IOException(MessageFormat.format(
                    "generating file already exists: {0}",
                    file));
        }
        File parent = file.getParentFile();
        if (parent.mkdirs() == false && parent.isDirectory() == false) {
            throw new IOException(MessageFormat.format(
                    "failed to prepare a parent directory: {0}",
                    file));
        }
        return new FileOutputStream(file);
    }

    @Override
    protected ExternalInputReference createExternalInput(String name, ClassDescription descriptionClass) {
        return new ExternalInputReference(
                name,
                descriptionClass,
                Collections.singleton(path(EXTERNAL_INPUT_BASE + name)));
    }

    @Override
    protected ExternalOutputReference createExternalOutput(
            String name,
            ClassDescription descriptionClass,
            Collection<String> internalOutputPaths) {
        return new ExternalOutputReference(name, descriptionClass, internalOutputPaths);
    }

    private String path(String relative) {
        return String.format("%s/%s", getOptions().getRuntimeWorkingDirectory(), relative); //$NON-NLS-1$
    }

    /**
     * Adds an {@link #getExtension(Class) extension service}.
     * @param extensionClass the extension type
     * @param service the extension service
     * @param <T> the extension type
     * @return this
     */
    public <T> MockJobflowBuilderContext addExtension(Class<T> extensionClass, T service) {
        extensions.put(extensionClass, service);
        return this;
    }

    @Override
    public <T> T getExtension(Class<T> extension) {
        Object service = extensions.get(extension);
        if (service == null) {
            return null;
        }
        return extension.cast(service);
    }
}
