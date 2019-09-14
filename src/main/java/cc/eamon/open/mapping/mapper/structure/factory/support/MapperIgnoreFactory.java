package cc.eamon.open.mapping.mapper.structure.factory.support;

import cc.eamon.open.mapping.mapper.MapperIgnore;
import cc.eamon.open.mapping.mapper.structure.detail.IgnoreDetail;
import cc.eamon.open.mapping.mapper.structure.detail.MapperDetail;
import cc.eamon.open.mapping.mapper.structure.factory.FieldFactory;
import cc.eamon.open.mapping.mapper.structure.factory.MapperBaseFactory;
import cc.eamon.open.mapping.mapper.structure.strategy.MapperStrategy;
import cc.eamon.open.mapping.mapper.structure.strategy.ignore.IgnoreNormalStrategy;
import cc.eamon.open.mapping.mapper.structure.strategy.ignore.IgnoreEnabledStrategy;

import javax.lang.model.element.Element;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Author: eamon
 * Email: eamon@eamon.cc
 * Time: 2019-09-07 14:25:07
 */
public class MapperIgnoreFactory extends MapperBaseFactory implements FieldFactory {

    @Override
    public Boolean allowValueRepeat() {
        return false;
    }

    @Override
    public List<MapperDetail> buildDetails(Annotation annotation, Element element, String mapper) {
        MapperIgnore mapperIgnore = (MapperIgnore) annotation;
        // check annotation para length
        if (mapperIgnore.value().length == 0) {
            return null;
        }

        // build detail
        List<MapperDetail> details = new ArrayList<>();
        for (int i = 0; i < mapperIgnore.value().length; i++) {

            // get current value
            if (!mapper.equals(mapperIgnore.value()[i])){
                continue;
            }

            // new detail
            details.add(new IgnoreDetail());
        }

        return details;
    }

    @Override
    public MapperStrategy buildStrategy(List<MapperDetail> details) {
        if (details == null){
            return new IgnoreNormalStrategy();
        }
        return new IgnoreEnabledStrategy();
    }

}