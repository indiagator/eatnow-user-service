package com.secor.userservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Configuration
public class AppConfig {

    @Autowired
    EurekaDiscoveryClient discoveryClient;

    public ServiceInstance getServiceInstance(String serviceName) throws ServiceNotFoundException {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            throw new ServiceNotFoundException("No instances found for "+serviceName);
        }
        return instances.get(0); // LOAD BALANCING ALGORITHM WILL GO HERE
    }

    @Bean(name = "auth-service-validate")
    public WebClient webClientAuthService(WebClient.Builder webClientBuilder)
    {
        ServiceInstance instance = null;
        try
        {
            instance = getServiceInstance("eatnow-auth-service");
        }
        catch (ServiceNotFoundException e)
        { // Fallback to Server Side Discovery in case Client Side Discovery fails
            return webClientBuilder
                    .baseUrl("http://auth-service:8082/api/v1/validate")
                    .filter(new LoggingWebClientFilter())
                    .build();
        }

        String hostname = instance.getHost();
        int port = instance.getPort();

        return webClientBuilder // IMS Communication via Client Side Discovery bypassing the Service Proxies
                .baseUrl("http://"+hostname+":"+port+"/api/v1/validate")
                .filter(new LoggingWebClientFilter())
                .build();
    }


}
