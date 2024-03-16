package io.github.yuokada;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

@ApplicationScoped
public class MemcachedService {

    private final static Logger logger = org.slf4j.LoggerFactory.getLogger(MemcachedService.class);

    @Inject
    public MemcachedService(@Named("host") String host, @Named("port") int port) {
        System.out.println("host: " + host);
        System.out.println("port: " + port);
        logger.info("Called");
    }

    public void print() {
        logger.info("print in MemcachedService");
    }
}
