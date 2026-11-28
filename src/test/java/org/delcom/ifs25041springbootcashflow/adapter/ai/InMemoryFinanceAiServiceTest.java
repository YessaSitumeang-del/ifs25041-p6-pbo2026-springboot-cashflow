package org.delcom.ifs25041springbootcashflow.adapter.ai;

import org.delcom.ifs25041springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25041springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25041springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryFinanceAiServiceTest {

    @Test
    void returnsDefaultIncomeAndExpenseWhenNothingCanned() throws Exception {
        List<AiGeneratedTransaction> items = new InMemoryFinanceAiService().generateTransactions(" gajian ");

        assertEquals(2, items.size());
        assertEquals(TransactionType.INCOME, items.get(0).type());
        assertEquals("Pengeluaran AI: gajian", items.get(1).description());
    }

    @Test
    void returnsCannedResponsesWhenConfigured() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        service.addCannedResponse("Kopi", 15_000, TransactionType.EXPENSE);

        List<AiGeneratedTransaction> items = service.generateTransactions("apa saja");
        assertEquals(1, items.size());
        assertEquals("Kopi", items.get(0).description());
    }

    @Test
    void constructorAcceptsInitialResponsesAndNull() throws Exception {
        InMemoryFinanceAiService seeded = new InMemoryFinanceAiService(
                List.of(new AiGeneratedTransaction("A", 1, TransactionType.INCOME)));
        assertEquals("A", seeded.generateTransactions("x").get(0).description());

        InMemoryFinanceAiService empty = new InMemoryFinanceAiService(null);
        assertEquals(2, empty.generateTransactions("x").size());
    }

    @Test
    void rejectsBlankPromptAndCanSimulateFailure() {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        assertThrows(ValidationException.class, () -> service.generateTransactions(null));
        assertThrows(ValidationException.class, () -> service.generateTransactions("  "));

        service.setSimulateFailure(true);
        assertThrows(DomainException.class, () -> service.generateTransactions("x"));
    }
}
