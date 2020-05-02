package com.ssy.srouter.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.ssy.srouter.annotation.Route;
import com.ssy.srouter.enums.RouteType;
import com.ssy.srouter.model.RouteMeta;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.ssy.srouter.utils.Consts.ACTIVITY;
import static com.ssy.srouter.utils.Consts.ANNOTATION_TYPE_ROUTE;
import static com.ssy.srouter.utils.Consts.IPROVIDER;
import static com.ssy.srouter.utils.Consts.IPROVIDER_GROUP;
import static com.ssy.srouter.utils.Consts.IROUTE_GROUP;
import static com.ssy.srouter.utils.Consts.ITROUTE_ROOT;
import static com.ssy.srouter.utils.Consts.METHOD_LOAD_INTO;
import static com.ssy.srouter.utils.Consts.NAME_OF_GROUP;
import static com.ssy.srouter.utils.Consts.NAME_OF_PROVIDER;
import static com.ssy.srouter.utils.Consts.NAME_OF_ROOT;
import static com.ssy.srouter.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.ssy.srouter.utils.Consts.SEPARATOR;
import static com.ssy.srouter.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

@AutoService(Processor.class)
@SupportedAnnotationTypes({ANNOTATION_TYPE_ROUTE})
public class RouteProcessor extends BaseProcessor {


    private Map<String, Set<RouteMeta>> groupMap = new HashMap<>();
    private Map<String, String> rootMap = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        if (CollectionUtils.isNotEmpty(set)) {
            try {
                logger.info(">>> RouteProcessor process. <<<");
                Set<? extends Element> routeElement = roundEnvironment.getElementsAnnotatedWith(Route.class);
                this.parseRoutes(routeElement);
            } catch (Exception e) {

            }
            return true;
        }

        return false;
    }

    private void parseRoutes(Set<? extends Element> routeElements) throws IOException {

        TypeMirror type_activity = elementUtils.getTypeElement(ACTIVITY).asType();
        TypeMirror type_provider = elementUtils.getTypeElement(IPROVIDER).asType();
        TypeElement type_IRouteGroup = elementUtils.getTypeElement(IROUTE_GROUP);
        TypeElement type_IProviderGroup = elementUtils.getTypeElement(IPROVIDER_GROUP);

        ClassName routeMetaCn = ClassName.get(RouteMeta.class);
        ClassName routeTypeCn = ClassName.get(RouteType.class);

        ParameterizedTypeName inputMapTypeOfGroup = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ClassName.get(RouteMeta.class)
        );

        ParameterSpec groupParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "atlas").build();
        ParameterSpec porviderParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "providers").build();
        for (Element element : routeElements) {
            TypeMirror mirror = element.asType();
            Route route = element.getAnnotation(Route.class);
            RouteMeta routeMeta = null;

            if (types.isSubtype(mirror, type_activity)) {
                logger.info(">>> Found activity route: " + mirror.toString() + " <<<");
                Map<String, Integer> paramsType = new HashMap<>();
                routeMeta = new RouteMeta(route, element, RouteType.ACTIVITY, paramsType);
            } else if (types.isSubtype(mirror, type_provider)) {
                logger.info(">>> Found provider route: " + mirror.toString() + " <<<");
                routeMeta = new RouteMeta(route, element, RouteType.PROVIDER, null);
            } else {
                throw new RuntimeException("SRouter::Compiler >>> Found unsupported class type, type = [" + types.toString() + "].");

            }
            categories(routeMeta);

        }


        for (Map.Entry<String, Set<RouteMeta>> entry : groupMap.entrySet()) {
            String groupName = entry.getKey();
            MethodSpec.Builder loadIntoMethodOfGroupBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(groupParamSpec);

            MethodSpec.Builder loadIntoMethodOfProviderBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(porviderParamSpec);

            Set<RouteMeta> groupData = entry.getValue();

            for (RouteMeta routeMeta1 : groupData) {
                ClassName className = ClassName.get((TypeElement) routeMeta1.getRawType());

                switch (routeMeta1.getType()) {
                    case PROVIDER:
                        List<? extends TypeMirror> interfaces = ((TypeElement) routeMeta1.getRawType()).getInterfaces();
                        for (TypeMirror typeMirror : interfaces) {
                            if (types.isSameType(typeMirror, type_provider)) {
                                loadIntoMethodOfProviderBuilder.addStatement(
                                        "providers.put($S, $T.build($T." + routeMeta1.getType() + ", $T.class, $S, $S, null, " + routeMeta1.getPriority() + ", " + routeMeta1.getExtra() + "))",
                                        routeMeta1.getRawType().toString(),    // So stupid, will duplicate only save class name.
                                        routeMetaCn,
                                        routeTypeCn,
                                        className,
                                        routeMeta1.getPath(),
                                        routeMeta1.getGroup());

                            } else if (types.isSubtype(typeMirror, type_provider)) {

                                loadIntoMethodOfProviderBuilder.addStatement(
                                        "providers.put($S, $T.build($T." + routeMeta1.getType() + ", $T.class, $S, $S, null, " + routeMeta1.getPriority() + ", " + routeMeta1.getExtra() + "))",
                                        typeMirror.toString(),    // So stupid, will duplicate only save class name.
                                        routeMetaCn,
                                        routeTypeCn,
                                        className,
                                        routeMeta1.getPath(),
                                        routeMeta1.getGroup());
                                logger.info(">>> 1111111222 <<<" + className);
                            }
                        }
                        break;
                    default:
                        break;
                }
                StringBuilder mapBodyBuilder = new StringBuilder();
                String mapBody = mapBodyBuilder.toString();
                loadIntoMethodOfGroupBuilder.addStatement(
                        "atlas.put($S, $T.build($T." + routeMeta1.getType() + ", $T.class, $S, $S, " +
                                (StringUtils.isEmpty(mapBody) ? null : ("new java.util.HashMap<String, Integer>(){{" + mapBodyBuilder.toString() + "}}")) + ", "
                                + routeMeta1.getPriority() + ", " + routeMeta1.getExtra() + "))",
                        routeMeta1.getPath(),
                        routeMetaCn,
                        routeTypeCn,
                        className,
                        routeMeta1.getPath().toLowerCase(),
                        routeMeta1.getGroup().toLowerCase());

            }

            // Write provider into disk
            String providerMapFileName = NAME_OF_PROVIDER + groupName;
            logger.info(">>>  providerMapFileName: " + providerMapFileName + "<<<");
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(providerMapFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(type_IProviderGroup))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfProviderBuilder.build())
                            .build()
            ).build().writeTo(mFiler);
            logger.info(">>> Generated prvider: " + groupName + "<<<");
            // Generate groups
            String groupFileName = NAME_OF_GROUP + groupName;
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(groupFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(type_IRouteGroup))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfGroupBuilder.build())
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Generated group: " + groupName + "<<<");
            rootMap.put(groupName, groupFileName);


            // Make map body for paramsType

        }
        //生成 root 文件
         String rootFileName11 = NAME_OF_ROOT + SEPARATOR + moduleName;
        logger.info(">>> parseRoutes rootFileName11. <<<"+rootFileName11);
        ParameterizedTypeName inputMapTypeOfRoot = ParameterizedTypeName.get(
                ClassName.get(Map.class),
                ClassName.get(String.class),
                ParameterizedTypeName.get(
                        ClassName.get(Class.class),
                        WildcardTypeName.subtypeOf(ClassName.get(type_IRouteGroup))
                )
        );

        ParameterSpec rootParamSpec = ParameterSpec.builder(inputMapTypeOfRoot, "routes").build();


        MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(rootParamSpec);

        if (MapUtils.isNotEmpty(rootMap)) {
            for (Map.Entry<String, String> entry1 : rootMap.entrySet()) {
                loadIntoMethodOfRootBuilder.addStatement("routes.put($S,$T.class)", entry1.getKey(), ClassName.get(PACKAGE_OF_GENERATE_FILE, entry1.getValue()));
            }
        }

        String rootFileName = NAME_OF_ROOT + SEPARATOR + moduleName;
        JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                TypeSpec.classBuilder(rootFileName)
                        .addJavadoc(WARNING_TIPS)
                        .addSuperinterface(ClassName.get(elementUtils.getTypeElement(ITROUTE_ROOT)))
                        .addModifiers(PUBLIC)
                        .addMethod(loadIntoMethodOfRootBuilder.build())
                        .build()
        ).build().writeTo(mFiler);

        logger.info(">>> Generated root, name is " + rootFileName + " <<<");

    }

    private void categories(RouteMeta routeMeta) {
        if (routeVerify(routeMeta)) {
            logger.info(">>> Start categories, group = " + routeMeta.getGroup() + ", path = " + routeMeta.getPath() + " <<<");

            Set<RouteMeta> routeMetas = groupMap.get(routeMeta.getGroup());
            if (CollectionUtils.isEmpty(routeMetas)) {
                Set<RouteMeta> routeMetaSet = new TreeSet<>(new Comparator<RouteMeta>() {
                    @Override
                    public int compare(RouteMeta r1, RouteMeta r2) {
                        try {
                            return r1.getPath().compareTo(r2.getPath());
                        } catch (NullPointerException npe) {
                            logger.error(npe.getMessage());
                            return 0;
                        }
                    }
                });
                routeMetaSet.add(routeMeta);
                groupMap.put(routeMeta.getGroup(), routeMetaSet);
            } else {
                routeMetas.add(routeMeta);
            }
        } else {
            logger.warning(">>> Route meta verify error, group is " + routeMeta.getGroup() + " <<<");
        }


    }

    private boolean routeVerify(RouteMeta meta) {
        String path = meta.getPath();

        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {   // The path must be start with '/' and not empty!
            return false;
        }
        if (StringUtils.isEmpty(meta.getGroup())) { // Use default group(the first word in path)
            try {
                String defaultGroup = path.substring(1, path.indexOf("/", 1));
                if (StringUtils.isEmpty(defaultGroup)) {
                    return false;
                }

                meta.setGroup(defaultGroup);
                return true;
            } catch (Exception e) {
                logger.error("Failed to extract default group! " + e.getMessage());
                return false;
            }
        }

        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Route.class.getCanonicalName());
        return types;
    }
}
