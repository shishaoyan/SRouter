package com.ssy.srouter.api.template;

import com.ssy.srouter.model.RouteMeta;

import java.util.Map;

public interface IRouteGroup {

    void loadInto(Map<String, RouteMeta> atlas);
}
