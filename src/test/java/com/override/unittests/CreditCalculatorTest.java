package com.override.unittests;

import com.override.unittests.enums.ClientType;
import com.override.unittests.exception.CannotBePayedException;
import com.override.unittests.exception.CentralBankNotRespondingException;
import com.override.unittests.service.CentralBankService;
import com.override.unittests.service.CreditCalculator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCalculatorTest {

    @InjectMocks
    private CreditCalculator creditCalculator;

    @Mock
    private CentralBankService centralBankService;

    //многие из тестов ниже могут быть заменены на @ParameterizedTest  https://habr.com/ru/post/591007/
    @Test
    public void calculateOverpaymentGovermentTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 10000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.GOVERMENT);
        Assertions.assertEquals(10000d, result);
    }

    @Test
    public void calculateOverpaymentBusinessTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 10000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.BUSINESS);
        Assertions.assertEquals(10010d, result);
    }

    @Test
    public void calculateOverpaymentIndividualTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 10000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.INDIVIDUAL);
        Assertions.assertEquals(11040d, result);
    }

    @ParameterizedTest(name = "{index} - for client type {2} estimates overpayment correctly")
    @ArgumentsSource(CreditDetailsArgumentsProvider.class)
    public void calculateOverpaymentAllCustomerTypesTest(double amount, double monthPaymentAmount, ClientType type) {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, type);
        if (type == ClientType.GOVERMENT) {
            Assertions.assertEquals(10000d, result);
        }
        if (type == ClientType.BUSINESS) {
            Assertions.assertEquals(10010d, result);
        }
        if (type == ClientType.INDIVIDUAL) {
            Assertions.assertEquals(11040d, result);
        }
    }

    @Test
    public void calculateOverpaymentOnTooBigAmountTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 1000000000d;
        double monthPaymentAmount = 10000d;
        assertThrows(CannotBePayedException.class, () -> creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.GOVERMENT));
    }

    @Test
    public void calculateOverpaymentOnManyYearCreditTest() {
        when(centralBankService.getKeyRate()).thenReturn(10d);
        double amount = 100000d;
        double monthPaymentAmount = 1000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.GOVERMENT);
        Assertions.assertEquals(125681.8191031709d, result);
    }

    @Test
    public void calculateOverpaymentWhenNoConnectionTest() {
        when(centralBankService.getKeyRate()).thenThrow(CentralBankNotRespondingException.class);
        when(centralBankService.getDefaultCreditRate()).thenReturn(30d);
        double amount = 100000d;
        double monthPaymentAmount = 5000d;
        double result = creditCalculator.calculateOverpayment(amount, monthPaymentAmount, ClientType.GOVERMENT);
        Assertions.assertEquals(60300d, result);
    }
}

class CreditDetailsArgumentsProvider implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                Arguments.of(100000d, 10000d, ClientType.GOVERMENT),
                Arguments.of(100000d, 10000d, ClientType.BUSINESS),
                Arguments.of(100000d, 10000d, ClientType.INDIVIDUAL)
        );
    }
}
