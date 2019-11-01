package cc.eamon.open.mapping.mapper.support.factory;

import cc.eamon.open.mapping.mapper.structure.detail.MapperDetail;
import cc.eamon.open.mapping.mapper.structure.factory.FieldFactory;
import cc.eamon.open.mapping.mapper.structure.factory.MapperBaseFactory;
import cc.eamon.open.mapping.mapper.structure.factory.TypeFactory;
import cc.eamon.open.mapping.mapper.structure.strategy.MapperStrategy;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Author: eamon
 * Email: eamon@eamon.cc
 * Time: 2019-09-07 14:19:38
 */
public class MapperDocFactory extends MapperBaseFactory implements TypeFactory, FieldFactory {


    @Override
    public Boolean allowValueRepeat() {
        return false;
    }

    @Override
    public List<MapperDetail> buildDetails(Annotation annotation, AnnotationMirror annotationMirror, Element element, String mapper) {
        return null;
    }

    @Override
    public MapperStrategy buildStrategy(List<MapperDetail> details) {
        return null;
    }

}
