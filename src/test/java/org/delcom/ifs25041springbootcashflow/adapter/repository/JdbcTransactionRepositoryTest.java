package org.delcom.ifs25041springbootcashflow.adapter.repository;

import org.delcom.ifs25041springbootcashflow.domain.entity.Role;
import org.delcom.ifs25041springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25041springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25041springbootcashflow.domain.entity.User;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcTransactionRepositoryTest {

    private JdbcUserRepository userRepository;
    private JdbcTransactionRepository repository;
    private int ownerId;

    @BeforeEach
    void setUp() {
        DataSource dataSource = createDataSource();
        SchemaInitializer.initialize(dataSource);
        // idempotent: aman dipanggil berulang (CREATE TABLE IF NOT EXISTS)
        SchemaInitializer.initialize(dataSource);
        userRepository = new JdbcUserRepository(dataSource);
        repository = new JdbcTransactionRepository(dataSource);
        ownerId = userRepository.saveNew("owner", "hash", "Owner", Role.USER).getId();
    }

    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jdbc_tx_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Test
    void userCrudAndFinders() {
        User saved = userRepository.saveNew("andi", "hash", "Andi", Role.USER);
        assertTrue(saved.getId() > 0);

        assertTrue(userRepository.findByUsername("ANDI").isPresent());
        assertEquals(2, userRepository.findAll().size());
        assertTrue(userRepository.findById(saved.getId()).isPresent());
        assertEquals(1, userRepository.findBy(user -> user.getUsername().equals("andi")).size());

        saved.changeFullName("Andi Baru");
        saved.changePhotoPath("profiles/a.png");
        userRepository.update(saved);
        assertEquals("Andi Baru", userRepository.findById(saved.getId()).orElseThrow().getFullName());

        User viaSave = userRepository.save(new User(0, "budi", "h", "Budi", Role.USER, null));
        assertTrue(viaSave.getId() > 0);

        assertTrue(userRepository.deleteById(viaSave.getId()));
        assertFalse(userRepository.deleteById(viaSave.getId()));
        assertTrue(userRepository.findByUsername("tidakada").isEmpty());
        assertTrue(userRepository.findById(999).isEmpty());
    }

    @Test
    void savePersistsAllFieldsAndReturnsGeneratedId() {
        Transaction saved = repository.save(ownerId, "Gaji", 5_000_000L, TransactionType.INCOME, "proofs/a.png");

        assertTrue(saved.getId() > 0);
        Transaction loaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals(ownerId, loaded.getUserId());
        assertEquals("Gaji", loaded.getDescription());
        assertEquals(5_000_000L, loaded.getAmount());
        assertEquals(TransactionType.INCOME, loaded.getType());
        assertEquals("proofs/a.png", loaded.getProofPath());
        assertNotNull(loaded.getCreatedAt());
    }

    @Test
    void proofIsOptional() {
        Transaction saved = repository.save(ownerId, "Tanpa bukti", 10, TransactionType.EXPENSE, null);
        assertNull(repository.findById(saved.getId()).orElseThrow().getProofPath());
    }

    @Test
    void saveViaEntityDelegatesToTheSameInsert() {
        Transaction viaEntity = repository.save(
                new Transaction(0, ownerId, "Lewat entity", 77, TransactionType.EXPENSE, null, LocalDateTime.now()));

        assertTrue(viaEntity.getId() > 0);
        assertEquals(77, repository.findById(viaEntity.getId()).orElseThrow().getAmount());
    }

    @Test
    void listsAreNewestFirstAndScopedByUser() {
        int otherId = userRepository.saveNew("lain", "hash", "Lain", Role.USER).getId();
        Transaction first = repository.save(ownerId, "pertama", 1, TransactionType.INCOME, null);
        Transaction second = repository.save(ownerId, "kedua", 2, TransactionType.INCOME, null);
        repository.save(otherId, "orang lain", 3, TransactionType.INCOME, null);

        List<Transaction> mine = repository.findByUserId(ownerId);
        assertEquals(2, mine.size());
        assertEquals(second.getId(), mine.get(0).getId());
        assertEquals(first.getId(), mine.get(1).getId());

        assertEquals(3, repository.findAll().size());
        assertEquals(1, repository.findByUserId(otherId).size());
        assertTrue(repository.findByUserId(999).isEmpty());
        assertEquals(1, repository.findBy(transaction -> transaction.getDescription().equals("kedua")).size());
        assertTrue(repository.findById(999).isEmpty());
    }

    @Test
    void updateChangesEditableColumns() {
        Transaction saved = repository.save(ownerId, "Lama", 10, TransactionType.EXPENSE, null);

        repository.update(new Transaction(saved.getId(), ownerId, "Baru", 99, TransactionType.INCOME,
                "proofs/n.png", saved.getCreatedAt()));

        Transaction loaded = repository.findById(saved.getId()).orElseThrow();
        assertEquals("Baru", loaded.getDescription());
        assertEquals(99, loaded.getAmount());
        assertEquals(TransactionType.INCOME, loaded.getType());
        assertEquals("proofs/n.png", loaded.getProofPath());
    }

    @Test
    void deleteReportsWhetherARowWasRemoved() {
        Transaction saved = repository.save(ownerId, "Hapus", 10, TransactionType.EXPENSE, null);

        assertTrue(repository.deleteById(saved.getId()));
        assertFalse(repository.deleteById(saved.getId()));
        assertTrue(repository.findByUserId(ownerId).isEmpty());
    }
}
