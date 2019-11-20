package cc.eamon.open.mapping.mapper.support;

import cc.eamon.open.mapping.mapper.structure.item.MapperField;
import cc.eamon.open.mapping.mapper.structure.item.MapperType;
import cc.eamon.open.mapping.mapper.support.strategy.*;
import cc.eamon.open.mapping.mapper.util.ClassUtils;
import cc.eamon.open.mapping.mapper.util.MapperUtils;
import cc.eamon.open.mapping.mapper.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.squareup.javapoet.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Author: eamon
 * Email: eamon@eamon.cc
 * Time: 2019-09-14 18:21:02
 */
public class MapperBuilder {

    private static Logger logger = LoggerFactory.getLogger(MapperBuilder.class);

    public static TypeSpec build(MapperType type) {
        // mapper strategies
        BasicMapperStrategy basicMapperStrategy = (BasicMapperStrategy) type.getStrategies().get(MapperEnum.MAPPER.getName());

        // define new type
        TypeSpec.Builder typeSpec = TypeSpec.classBuilder(basicMapperStrategy.getBuildTypeName()).addModifiers(Modifier.PUBLIC);

        // type strategies
        ExtendsStrategy extendsStrategy = (ExtendsStrategy) type.getStrategies().get(MapperEnum.EXTENDS.getName());

        if (extendsStrategy.open()) {
            typeSpec.superclass(ClassName.get(extendsStrategy.getPackageName(), extendsStrategy.getSuperMapperName()));
        }

        // define import items
        ClassName self = ClassName.get(type.getPackageName(), type.getSimpleName());

        // init: build map
        logger.info("Mapping build init buildMap:" + type.getQualifiedName());
        String buildMapStaticMethod = "buildMap";
        MethodSpec.Builder buildMapStaticMethodSpec = MethodSpec.methodBuilder(buildMapStaticMethod)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(self, "obj")
                .returns(ClassUtils.getParameterizedMap());

        // build resultMap
        if (extendsStrategy.open()) {
            buildMapStaticMethodSpec.addStatement("Map<String, String> map = $T.buildMap(obj)", ClassName.get(extendsStrategy.getPackageName(), extendsStrategy.getSuperMapperName()));
        } else {
            buildMapStaticMethodSpec.addStatement("Map<String, String> map = new $T<>()", ClassUtils.getLinkedHashMap());
        }
        buildMapStaticMethodSpec.addStatement("if (obj == null) return map");


        // init: build entity
        logger.info("Mapping build init buildEntity:" + type.getQualifiedName());
        String buildEntityMethod = "buildEntity";
        MethodSpec.Builder buildEntityMethodSpec = MethodSpec.methodBuilder(buildEntityMethod)
                .addModifiers(Modifier.PUBLIC)
                .returns(self);

        // build obj
        buildEntityMethodSpec.addStatement("$T obj = new $T()", self, self);
        if (extendsStrategy.open()) {
            buildEntityMethodSpec.addStatement("$T.copyEntity(super.buildEntity(), obj)", ClassName.get(extendsStrategy.getPackageName(), extendsStrategy.getSuperMapperName()));
        }


        // init: parse entity
        logger.info("Mapping build init parseEntity:" + type.getQualifiedName());
        String parseEntityStaticMethod = "parseEntity";
        MethodSpec.Builder parseEntityStaticMethodSpec = MethodSpec.methodBuilder(parseEntityStaticMethod)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(ClassUtils.getParameterizedMap(), "map")
                .returns(self);

        // build obj
        parseEntityStaticMethodSpec.addStatement("$T obj = new $T()", self, self);
        if (extendsStrategy.open()) {
            parseEntityStaticMethodSpec.addStatement("$T.copyEntity($T.parseEntity(map), obj)", ClassName.get(extendsStrategy.getPackageName(), extendsStrategy.getSuperMapperName()), ClassName.get(extendsStrategy.getPackageName(), extendsStrategy.getSuperMapperName()));
        }
        parseEntityStaticMethodSpec.addStatement("if (map == null) return obj");

        // init: copy entity
        logger.info("Mapping build init copyEntity:" + type.getQualifiedName());
        String copyEntityStaticMethod = "copyEntity";
        MethodSpec.Builder copyEntityStaticMethodSpec = MethodSpec.methodBuilder(copyEntityStaticMethod)
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .addParameter(self, "from")
                .addParameter(self, "to")
                .returns(TypeName.VOID);

        copyEntityStaticMethodSpec.addStatement("if (from == null || to == null) return");

        for (MapperField field : type.getMapperFieldList()) {

            IgnoreStrategy ignoreStrategy = (IgnoreStrategy) field.getStrategies().get(MapperEnum.IGNORE.getName());
            RenameStrategy renameStrategy = (RenameStrategy) field.getStrategies().get(MapperEnum.RENAME.getName());
            ModifyStrategy modifyStrategy = (ModifyStrategy) field.getStrategies().get(MapperEnum.MODIFY.getName());

            if (ignoreStrategy.ignore()) {
                continue;
            }

            FieldSpec.Builder fieldSpec = FieldSpec.builder(
                    TypeName.get(modifyStrategy.getModifyType()),
                    renameStrategy.getName(),
                    Modifier.PUBLIC);
            typeSpec.addField(fieldSpec.build());

            buildMapStaticMethodSpec.addStatement("map.put(\"" + renameStrategy.getName() + "\", $T.toJSONString(" + modifyStrategy.getModifyName("obj") + "))", JSONObject.class);
            buildEntityMethodSpec.addStatement(modifyStrategy.getRecoverName("obj").replace("$", "this." + renameStrategy.getName()));
            if (MapperUtils.loadTypeArguments(modifyStrategy.getModifyType()).size() > 0) {
                parseEntityStaticMethodSpec.addStatement(modifyStrategy.getRecoverName("obj").replace("$", "$T.parseObject(map.get(\"" + renameStrategy.getName() + "\"), new $T<$T>(){})"),
                        JSONObject.class, TypeReference.class, ClassName.get(modifyStrategy.getModifyType()));
            } else {
                parseEntityStaticMethodSpec.addStatement(modifyStrategy.getRecoverName("obj").replace("$", "$T.parseObject(map.get(\"" + renameStrategy.getName() + "\"), $T.class)"),
                        JSONObject.class, TypeName.get(modifyStrategy.getModifyType()));
            }
            copyEntityStaticMethodSpec.addStatement("to.set" + StringUtils.firstWordToUpperCase(field.getSimpleName()) + "(from.get" + StringUtils.firstWordToUpperCase(field.getSimpleName()) + "())");
        }

        // add return
        buildMapStaticMethodSpec.addStatement("return map");
        buildEntityMethodSpec.addStatement("return obj");
        parseEntityStaticMethodSpec.addStatement("return obj");

        // add method
        typeSpec.addMethod(buildMapStaticMethodSpec.build());
        typeSpec.addMethod(buildEntityMethodSpec.build());
        typeSpec.addMethod(parseEntityStaticMethodSpec.build());
        typeSpec.addMethod(copyEntityStaticMethodSpec.build());
        buildExtra(type, typeSpec, self);
        buildConvert(type, typeSpec, self);

        return typeSpec.build();

    }

    private static void buildExtra(MapperType type, TypeSpec.Builder typeSpec, ClassName self) {
        // init extra
        ExtraStrategy extraStrategy = (ExtraStrategy) type.getStrategies().get(MapperEnum.EXTRA.getName());
        if (extraStrategy.open()) {

            logger.info("Mapping build init buildMapExtra:" + type.getQualifiedName());
            String buildMapExtraStaticMethod = "buildMapExtra";
            MethodSpec.Builder buildMapExtraStaticMethodSpec = MethodSpec.methodBuilder(buildMapExtraStaticMethod)
                    .addModifiers(Modifier.PUBLIC)
                    .addModifiers(Modifier.STATIC)
                    .addParameter(self, "obj")
                    .returns(ClassUtils.getParameterizedMap());


            buildMapExtraStaticMethodSpec.addStatement("Map<String, String> map = buildMap(obj)");
            buildMapExtraStaticMethodSpec.addStatement("if (obj == null) return map");

            for (MapperField field : extraStrategy.getMapperFields()) {
                RenameStrategy renameStrategy = (RenameStrategy) field.getStrategies().get(MapperEnum.RENAME.getName());
                ModifyStrategy modifyStrategy = (ModifyStrategy) field.getStrategies().get(MapperEnum.MODIFY.getName());


                if (field.getList()) {
                    FieldSpec.Builder fieldSpec = FieldSpec.builder(
                            ClassUtils.getParameterizedList(TypeName.get(modifyStrategy.getModifyType())),
                            renameStrategy.getName(),
                            Modifier.PUBLIC);
                    typeSpec.addField(fieldSpec.build());
                    buildMapExtraStaticMethodSpec.addParameter(ClassUtils.getParameterizedList(TypeName.get(field.getType())), renameStrategy.getName());
                } else {
                    FieldSpec.Builder fieldSpec = FieldSpec.builder(
                            TypeName.get(modifyStrategy.getModifyType()),
                            renameStrategy.getName(),
                            Modifier.PUBLIC);
                    typeSpec.addField(fieldSpec.build());
                    buildMapExtraStaticMethodSpec.addParameter(TypeName.get(field.getType()), renameStrategy.getName());
                }

                buildMapExtraStaticMethodSpec.addStatement("map.put(\"" + renameStrategy.getName() + "\", $T.toJSONString(" + renameStrategy.getName() + "))", JSONObject.class);

            }
            buildMapExtraStaticMethodSpec.addStatement("return map");
            typeSpec.addMethod(buildMapExtraStaticMethodSpec.build());
        }
    }

    private static void buildConvert(MapperType type, TypeSpec.Builder typeSpec, ClassName self) {
        // init convert
        ConvertStrategy convertStrategy = (ConvertStrategy) type.getStrategies().get(MapperEnum.CONVERT.getName());

        if (convertStrategy.open()) {

            logger.info("Mapping build init convert:" + type.getQualifiedName());
            String convertMethod = "convert";
            for (TypeMirror convertStrategyType : convertStrategy.getTypes()) {

                Map<String, TypeMirror> fieldTypeMirrors = new HashMap<>();
                List<Element> elements = MapperUtils.loadTypeEnclosedElements(convertStrategyType);
                elements.forEach(element -> fieldTypeMirrors.put(element.getSimpleName().toString(), element.asType()));

                MethodSpec.Builder buildConvertAB = MethodSpec.methodBuilder(convertMethod)
                        .addModifiers(Modifier.PUBLIC)
                        .addModifiers(Modifier.STATIC)
                        .addParameter(self, "from")
                        .addParameter(TypeName.get(convertStrategyType), "to")
                        .returns(TypeName.get(convertStrategyType));
                buildConvertAB.addStatement("if (from == null || to == null) return to");

                MethodSpec.Builder buildConvertBA = MethodSpec.methodBuilder(convertMethod)
                        .addModifiers(Modifier.PUBLIC)
                        .addModifiers(Modifier.STATIC)
                        .addParameter(TypeName.get(convertStrategyType), "from")
                        .addParameter(self, "to")
                        .returns(self);
                buildConvertBA.addStatement("if (from == null || to == null) return to");

                for (MapperField field : type.getMapperFieldList()) {
                    IgnoreStrategy ignoreStrategy = (IgnoreStrategy) field.getStrategies().get(MapperEnum.IGNORE.getName());
                    RenameStrategy renameStrategy = (RenameStrategy) field.getStrategies().get(MapperEnum.RENAME.getName());
                    ModifyStrategy modifyStrategy = (ModifyStrategy) field.getStrategies().get(MapperEnum.MODIFY.getName());

                    String fieldUpperCase = StringUtils.firstWordToUpperCase(renameStrategy.getName());

                    if (fieldTypeMirrors.get(renameStrategy.getName()) == null) {
                        continue;
                    }

                    if (ignoreStrategy.ignore()) {
                        continue;
                    }

                    if (!modifyStrategy.getModifyType().toString().equals(fieldTypeMirrors.get(renameStrategy.getName()).toString())) {
                        logger.warn("Mapping build convert type not fit, try to convert:" + modifyStrategy.getModifyType().toString() + " to " + fieldTypeMirrors.get(renameStrategy.getName()));
                        String fixme = "// FIXME: " + type.getQualifiedName() + "[" + renameStrategy.getElementName() + "] do not fit " + convertStrategyType.toString() + "[" + renameStrategy.getName() + "]";
                        buildConvertAB.addStatement(fixme);
                        buildConvertAB.addStatement("// to.set" + fieldUpperCase + "(" + modifyStrategy.getModifyName("from") + ")");
                        buildConvertBA.addStatement(fixme);
                        buildConvertBA.addStatement("// " + modifyStrategy.getRecoverName("to").replace("$", "from.get" + fieldUpperCase + "()"));
                        continue;
                    }

                    buildConvertAB.addStatement("to.set" + fieldUpperCase + "(" + modifyStrategy.getModifyName("from") + ")");
                    buildConvertBA.addStatement(modifyStrategy.getRecoverName("to").replace("$", "from.get" + fieldUpperCase + "()"));
                }
                buildConvertAB.addStatement("return to");
                buildConvertBA.addStatement("return to");

                typeSpec.addMethod(buildConvertAB.build());
                typeSpec.addMethod(buildConvertBA.build());
            }

        }

    }

}
