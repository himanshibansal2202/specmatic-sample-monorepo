package io.specmatic.sample.store;

import io.specmatic.test.SpecmaticContractTest;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = StoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class ContractTest implements SpecmaticContractTest {
}
