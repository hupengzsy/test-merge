package org.srm.gateway;

import org.hzero.autoconfigure.gateway.EnableHZeroGateway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.srm.autoconfigure.gateway.EnableSrmGateway;

/**
 * description
 *
 * @author jinliang 2019/04/23 15:59
 */
@EnableSrmGateway
@EnableDiscoveryClient
@SpringBootApplication(
        exclude = {SecurityAutoConfiguration.class}
)
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class,args);
    }
}
