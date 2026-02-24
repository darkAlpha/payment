package org.tars;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

/**
 * TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
 * click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

 * TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
 * to see how IntelliJ IDEA suggests fixing it.
 */

//TIP Press <shortcut actionId="Debug"/> to start debugging your code. We have set one <icon src="AllIcons.Debugger.Db_set_breakpoint"/> breakpoint
// for you, but you can always add more by pressing <shortcut actionId="ToggleLineBreakpoint"/>.


@EnableDubbo
@SpringBootApplication
public class PaymentGatewayConnector {
    public static void main(String[] args) {
        SpringApplication.run(PaymentGatewayConnector.class);
    }
}