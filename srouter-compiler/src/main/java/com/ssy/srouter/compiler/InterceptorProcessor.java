package com.ssy.srouter.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.ssy.srouter.annotation.Interceptor;
import com.ssy.srouter.utils.Consts;

import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.ssy.srouter.utils.Consts.IINTERCEPTION_GROUP;
import static com.ssy.srouter.utils.Consts.IINTERCEPTOR;
import static com.ssy.srouter.utils.Consts.METHOD_LOAD_INTO;
import static com.ssy.srouter.utils.Consts.NAME_OF_INTERCEPTOR;
import static com.ssy.srouter.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.ssy.srouter.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;


@AutoService(Processor.class)
public class InterceptorProcessor extends BaseProcessor {
    private Map<Integer, Element> interceptors = new TreeMap<>();
    private TypeMirror iInterceptor = null;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        iInterceptor = elementUtils.getTypeElement(Consts.IINTERCEPTOR).asType();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (CollectionUtils.isNotEmpty(set)) {
            try {
                logger.info(">>> InterceptorProcessor process. <<<");
                Set<? extends Element> routeElement = roundEnvironment.getElementsAnnotatedWith(Interceptor.class);
                this.parseInterceptors(routeElement);
            } catch (Exception e) {

            }
            return true;
        }

        return false;
    }

    private void parseInterceptors(Set<? extends Element> routeElement) {

        TypeMirror type_iiterception_group = elementUtils.getTypeElement(IINTERCEPTION_GROUP).asType();
        for (Element element : routeElement) {
            TypeMirror typeMirror = element.asType();
            Interceptor interceptor = element.getAnnotation(Interceptor.class);
            Element lastInterceptor = interceptors.get(interceptor.priority());
            if (null != lastInterceptor) {
                throw new IllegalArgumentException(
                        String.format(Locale.getDefault(), "More than one interceptors use same priority [%d], They are [%s] and [%s].",
                                interceptor.priority(),
                                lastInterceptor.getSimpleName(),
                                element.getSimpleName())
                );
            }
            logger.info(">>> Generated interceptor: " + interceptor.toString() + "<<<");

            interceptors.put(interceptor.priority(), element);

        }
        ParameterizedTypeName loadIntoType = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(Integer.class),
                ParameterizedTypeName.get(
                ClassName.get(Class.class),
                WildcardTypeName.subtypeOf(ClassName.get(iInterceptor)))

        );
        ParameterSpec.Builder loadIntoParameterBuilder = ParameterSpec.builder(loadIntoType, "interceptors");
        MethodSpec.Builder loadIntoMethodOfGroupBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(loadIntoParameterBuilder.build());



        if (null != interceptors && interceptors.size() > 0) {

            for (Map.Entry<Integer, Element> entry : interceptors.entrySet()) {
                loadIntoMethodOfGroupBuilder.addStatement("interceptors.put("+ entry.getKey()+ ",$T.class)", entry.getValue());
            }
        }

        // Generate groups
        String groupFileName = NAME_OF_INTERCEPTOR + moduleName;
        try {
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(groupFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(type_iiterception_group))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfGroupBuilder.build())
                            .build()
            ).build().writeTo(mFiler);
            logger.info(">>> Generated interceptor, name is " + groupFileName + " <<<");

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Interceptor.class.getCanonicalName());
        return types;
    }

    private boolean verify(Element element) {
        Interceptor interceptor = element.getAnnotation(Interceptor.class);
        // It must be implement the interface IInterceptor and marked with annotation Interceptor.
        return null != interceptor && ((TypeElement) element).getInterfaces().contains(iInterceptor);
    }
}
