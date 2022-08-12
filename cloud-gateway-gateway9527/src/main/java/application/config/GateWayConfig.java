package application.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GateWayConfig {
    /*
    配置了一个id为path_route_guifeng的路由规则
    当访问地址为： http://localhost:9527/guonei时会自动转发到地址：http://news.baidu.com/guonei
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder routeLocatorBuilder){
        RouteLocatorBuilder.Builder routes = routeLocatorBuilder.routes();
        // http://news.baidu.com/guonei
        routes.route("path_route_guifeng",r -> r.path("/guonei").uri("http://news.baidu.com/guonei"));
        return routes.build();
    }
}
