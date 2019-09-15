package cc.eamon.open.mapping.mapper;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;


/**
 * Author: eamon
 * Email: eamon@eamon.cc
 * Time: 2019-09-15 03:02:15
 */
public class ClassUtil {

    public static ClassName get(String qualifiedName){
        return ClassName.get(StringUtil.packageNameFromQualifiedName(qualifiedName), StringUtil.classNameFromQualifiedName(qualifiedName));
    }

    public static ClassName getString(){
        return ClassName.get("java.lang", "String");
    }

    public static ClassName getMap(){
        return ClassName.get("java.util", "Map");
    }

    public static ClassName getList(){
        return ClassName.get("java.util", "List");
    }

    public static ClassName getLinkedHashMap(){
        return ClassName.get("java.util", "LinkedHashMap");
    }

    public static TypeName getParameterizedMap(){
        return ParameterizedTypeName.get(getMap(), getString(), getString());
    }

    public static TypeName getParameterizedList(ClassName className){
        return ParameterizedTypeName.get(getList(), className);
    }

}
