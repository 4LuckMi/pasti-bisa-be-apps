package com.pasti_bisa.backend.modules.identity.adapter.out.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Disposable seed process; no public HTTP bootstrap endpoint exists. */
@Component
@Profile("demo-accounts")
public class DemoAccountSeedRunner implements ApplicationRunner {
    private static final Logger log=LoggerFactory.getLogger(DemoAccountSeedRunner.class);
    private final DemoInternalAccountSeeder seeder;private final ConfigurableApplicationContext context;
    private final String adminPassword,staffPassword;private final boolean exit;
    public DemoAccountSeedRunner(DemoInternalAccountSeeder seeder,ConfigurableApplicationContext context,
            @Value("${DEMO_ADMIN_PASSWORD:}") String adminPassword,@Value("${DEMO_STAFF_PASSWORD:}") String staffPassword,
            @Value("${pastisiap.demo.accounts.exit-after-seed:true}") boolean exit){
        this.seeder=seeder;this.context=context;this.adminPassword=adminPassword;this.staffPassword=staffPassword;this.exit=exit;
    }
    @Override public void run(ApplicationArguments args){
        for(var account:seeder.seed(adminPassword,staffPassword))
            log.info("Local demo account ready email={} userType={} role={} created={}",account.email(),account.userType(),account.role(),account.created());
        if(exit)System.exit(SpringApplication.exit(context,()->0));
    }
}
