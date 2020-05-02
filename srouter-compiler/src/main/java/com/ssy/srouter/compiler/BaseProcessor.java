package com.ssy.srouter.compiler;

import com.ssy.srouter.utils.Logger;

import org.apache.commons.collections4.MapUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public abstract class BaseProcessor extends AbstractProcessor {
    Elements elementUtils;
    Types types;
    Logger logger;
    public static final String KEY_MODULE_NAME = "SROUTER_MODULE_NAME";
    protected Filer mFiler;
    String moduleName = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {

        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        elementUtils = processingEnvironment.getElementUtils();
        types = processingEnvironment.getTypeUtils();
        logger = new Logger(processingEnv.getMessager());
        logger.info(">>> BaseProcessor init. <<<");

        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(KEY_MODULE_NAME);
        }
    }




    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        return new HashSet<String>() {{
            this.add(KEY_MODULE_NAME);
        }};
    }
}
