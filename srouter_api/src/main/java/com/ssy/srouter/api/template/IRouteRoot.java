package com.ssy.srouter.api.template;

import java.util.Map;

public interface IRouteRoot {

    void loadInto(Map<String,Class<? extends IRouteGroup>> routes);
}
